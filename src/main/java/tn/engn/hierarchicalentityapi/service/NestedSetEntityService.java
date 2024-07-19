package tn.engn.hierarchicalentityapi.service;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.DataIntegrityException;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ParentEntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.model.QHierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mapstruct.ap.shaded.freemarker.template.utility.StringUtil.capitalize;

/**
 * Abstract service class for managing entities with a nested set model hierarchy.
 *
 * @param <E>  the type of the entity
 * @param <RD> the type of the request DTO
 * @param <SD> the type of the response DTO
 */
@Service
@Slf4j
public abstract class NestedSetEntityService<E extends HierarchyBaseEntity, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto>
        implements HierarchyService<E, RD, SD> {

    protected final HierarchyBaseRepository<E> entityRepository;
    protected final HierarchyMapper<E, RD, SD> entityMapper;
    @PersistenceContext
    @Setter
    protected EntityManager entityManager;
    protected final JPAQueryFactory jpaQueryFactory;

    @Setter
    @Value("${entity.max-name-length}")
    private int maxNameLength;

    Class<E> entityClass;


     /**
     * Constructor for NestedSetEntityService.
     *
     * @param entityRepository the repository for the entity
     * @param entityMapper     the mapper for converting between entities and DTOs
     * @param jpaQueryFactory  the JPA query factory for executing queries
     * @param entityClass      the class type of the entity
     */
    public NestedSetEntityService(HierarchyBaseRepository<E> entityRepository,
                                  HierarchyMapper<E, RD, SD> entityMapper,
                                  JPAQueryFactory jpaQueryFactory,
                                  Class<E> entityClass) {
        this.entityRepository = entityRepository;
        this.entityMapper = entityMapper;
        this.jpaQueryFactory = jpaQueryFactory;
        this.entityClass = entityClass;
    }

    /**
     * Creates a new entity in the hierarchical structure.
     *
     * @param requestDto The DTO containing data for the entity creation.
     * @return The DTO representing the created entity.
     * @throws ParentEntityNotFoundException If the parent entity specified in the request is not found.
     * @throws ValidationException           If the request data fails validation.
     * @throws DataIntegrityException        If there is an issue with data integrity during creation.
     */
    @Override
    @Transactional
    public SD createEntity(RD requestDto)
            throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        String name = requestDto.getName();
        Long parentId = requestDto.getParentEntityId();

        // Validate the entity request
        validateEntityName(name);

        log.info("Creating entity with name: {} and parentId: {}", name, parentId);

        Integer parentRightIndex = null;
        Integer parentLevel = null;
        Long rootId = null;

        if (parentId != null) {
            // Fetch and validate the parent entity
            E parentEntity = fetchAndValidateParentEntity(parentId);

            parentRightIndex = parentEntity.getRightIndex();
            parentLevel = parentEntity.getLevel();
            rootId = parentEntity.getRootId();

            // Update left and right indexes of existing entities
            updateIndexesForNewEntity(parentRightIndex);
            entityManager.flush();
        }

        // Map the request DTO to an entity
        E entity = entityMapper.toEntity(requestDto);

        if (parentRightIndex == null) {
            // Handle root entity creation
            handleRootDepartmentCreation(entity);
        } else {
            // Handle child entity creation
            handleChildDepartmentCreation(entity, parentRightIndex, parentLevel, rootId);
        }

        // Save the new entity
        E savedEntity = entityRepository.save(entity);

        entityManager.flush();
        entityManager.refresh(savedEntity);

        // Convert the saved entity to a response DTO and return it
        return entityMapper.toDto(savedEntity);

    }

    /**
     * Validates the entity name for length constraints.
     *
     * @param name the entity name to validate
     * @throws ValidationException if the entity name is empty, null, or exceeds max length
     */
    private void validateEntityName(String name) throws ValidationException {
        if (name == null || name.isEmpty()) {
            throw new ValidationException("Entity name cannot be null or empty.");
        }

        if (name.length() > maxNameLength) {
            throw new ValidationException("Entity name cannot be longer than " + maxNameLength + " characters.");
        }
    }

    /**
     * Fetches and validates the parent entity.
     *
     * @param parentId the ID of the parent entity
     * @return the parent entity
     * @throws ParentEntityNotFoundException if the parent entity is not found
     */
    @Transactional(readOnly = true)
    protected E fetchAndValidateParentEntity(Long parentId) throws ParentEntityNotFoundException {
        QHierarchyBaseEntity qHierarchyBaseEntity = QHierarchyBaseEntity.hierarchyBaseEntity;
        E parentEntity = (E) jpaQueryFactory.selectFrom(qHierarchyBaseEntity)
                .where(qHierarchyBaseEntity.id.eq(parentId))
                .fetchOne();

        if (parentEntity == null) {
            throw new ParentEntityNotFoundException("Parent entity not found with ID: " + parentId);
        }

        return parentEntity;
    }

    /**
     * Updates the left and right indexes of existing entities to make room for the new entity.
     *
     * @param parentRightIndex the right index of the parent entity
     */
    protected void updateIndexesForNewEntity(Integer parentRightIndex) {
        QHierarchyBaseEntity qHierarchyBaseEntity = QHierarchyBaseEntity.hierarchyBaseEntity;

        jpaQueryFactory.update(qHierarchyBaseEntity)
                .set(qHierarchyBaseEntity.leftIndex, qHierarchyBaseEntity.leftIndex.add(2))
                .where(qHierarchyBaseEntity.leftIndex.goe(parentRightIndex))
                .execute();

        jpaQueryFactory.update(qHierarchyBaseEntity)
                .set(qHierarchyBaseEntity.rightIndex, qHierarchyBaseEntity.rightIndex.add(2))
                .where(qHierarchyBaseEntity.rightIndex.goe(parentRightIndex))
                .execute();
    }

    /**
     * Handles the creation of a root entity.
     *
     * @param entity the entity to be created
     */
    protected void handleRootDepartmentCreation(E entity) {
        Integer maxRightIndex = jpaQueryFactory.select(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.max())
                .from(QHierarchyBaseEntity.hierarchyBaseEntity)
                .fetchOne();

        if (maxRightIndex == null) {
            maxRightIndex = 0;
        }

        entity.setLeftIndex(maxRightIndex + 1);
        entity.setRightIndex(maxRightIndex + 2);
        entity.setLevel(0);

        // Save the entity to get the actual ID
        entity = entityRepository.save(entity);
        log.info("Entity saved with ID: {}", entity.getId());

        // Set the rootId to its own ID after insertion
        entity.setRootId(entity.getId());
        log.info("Setting rootId to: {}", entity.getId());

        // Save the entity again to update the rootId
        entity = entityRepository.save(entity);
        log.info("Entity saved with rootId: {}", entity.getRootId());
    }

    /**
     * Handles the creation of a child entity.
     *
     * @param entity           the entity entity to be created
     * @param parentRightIndex the right index of the parent entity
     * @param parentLevel      the level of the parent entity
     * @param rootId           the root ID of the parent entity's tree
     */
    private void handleChildDepartmentCreation(E entity, Integer parentRightIndex, Integer parentLevel, Long rootId) {
        entity.setLeftIndex(parentRightIndex);
        entity.setRightIndex(parentRightIndex + 1);
        entity.setLevel(parentLevel + 1);
        entity.setRootId(rootId);
    }

    /**
     * Updates an existing entity in the hierarchical structure.
     *
     * @param entityId         The ID of the entity to update.
     * @param updatedEntityDto The DTO containing updated data for the entity.
     * @return The DTO representing the updated entity.
     * @throws EntityNotFoundException       If the entity with the specified ID is not found.
     * @throws ParentEntityNotFoundException If the parent entity specified in the request is not found.
     * @throws DataIntegrityException        If there is an issue with data integrity during update.
     * @throws ValidationException           If the request data fails validation.
     */
    @Override
    @Transactional
    public SD updateEntity(Long entityId, RD updatedEntityDto)
            throws EntityNotFoundException, ParentEntityNotFoundException, DataIntegrityException, ValidationException {
        String name = updatedEntityDto.getName();
        Long parentId = updatedEntityDto.getParentEntityId();

        // Fetch the existing entity by ID
        E existingEntity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("E not found with ID: " + entityId));

        // Validate the updated entity name
        validateEntityName(name);

        // Check if the entity's name needs updating
        if (!existingEntity.getName().equals(name)) {
            existingEntity.setName(name);
            entityManager.flush();
        }

        // Check if the parent entity is being changed
        if (parentId == null && existingEntity.getParentId() != null) {
            // Move to root entity
            moveToRoot(existingEntity);
        } else if (parentId != null && !parentId.equals(existingEntity.getParentId())) {
            // Fetch and validate the new parent entity
            E newParentEntity = entityRepository.findById(parentId)
                    .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with ID: " + parentId));

            // Validate parent entity to avoid circular references and other constraints
            validateParentDepartment(existingEntity, newParentEntity);

            // Adjust nested set indexes and levels for moving the entity
            moveSubtree(existingEntity, newParentEntity);
        }

        entityManager.refresh(existingEntity);

        // Save the updated entity
        E updatedEntity = entityRepository.save(existingEntity);

        // Return the updated entity DTO
        return entityMapper.toDto(updatedEntity);
    }

    /**
     * Validates the parent entity to avoid circular references and other constraints.
     *
     * @param entity          the entity to be updated
     * @param newParentEntity the new parent entity
     * @throws DataIntegrityException if the new parent entity is invalid
     */
    private void validateParentDepartment(E entity, E newParentEntity) throws DataIntegrityException {
        log.info("Validating new parent entity {} for entity {}", newParentEntity.getId(), entity.getId());

        // Self-reference or descendant check
        if (entity.getId().equals(newParentEntity.getId()) ||
                (newParentEntity.getLeftIndex() >= entity.getLeftIndex() && newParentEntity.getRightIndex() <= entity.getRightIndex())) {
            log.error("Invalid parent entity: Cannot set entity {} as its own descendant's parent.", entity.getId());
            throw new DataIntegrityException("Cannot set an entity as its own descendant's parent.");
        }

        // Immediate circular reference check
        if (newParentEntity.getParentId() != null && newParentEntity.getParentId().equals(entity.getId())) {
            log.error("Invalid parent entity: Circular reference detected for entity {}", entity.getId());
            throw new DataIntegrityException("Circular references.");
        }

        log.info("Parent entity validation successful for entity {}", entity.getId());
    }

    /**
     * Moves a subtree to a new parent.
     *
     * @param entity          the entity to be moved
     * @param newParentEntity the new parent entity
     */
    protected void moveSubtree(E entity, E newParentEntity) {
        int subtreeSize = entity.getRightIndex() - entity.getLeftIndex() + 1;
        int indexShift = newParentEntity.getRightIndex() - entity.getLeftIndex();
        if (indexShift < 0) {
            indexShift -= subtreeSize;
        }

        int levelShift = newParentEntity.getLevel() + 1 - entity.getLevel();

        // Shift indexes to make space for the new subtree
        shiftIndexes(newParentEntity.getRightIndex(), subtreeSize);
        entityManager.flush();

        // Move the subtree to the new space and update levels
        updateSubtreeIndexesAndLevels(entity, indexShift, levelShift);
        entityManager.flush();

        // Compact the space left by the moved subtree
        compactIndexes(entity.getLeftIndex(), subtreeSize);
        entityManager.flush();

        // Update parent and root IDs of the subtree
        updateSubtreeParentAndRootIds(entity, newParentEntity);
        entityManager.flush();
    }

    /**
     * Updates the levels, the left and right indexes of the subtree.
     *
     * @param entity     the entity to be moved
     * @param indexShift the amount to shift by the indexes
     * @param levelShift the amount to shift by the levels
     */
    protected void updateSubtreeIndexesAndLevels(E entity, int indexShift, int levelShift) {
        log.info("Updating subtree indexes and levels for entity {}. Index shift: {}, Level shift: {}",
                entity.getId(), indexShift, levelShift);

        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex, QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.add(indexShift))
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex, QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.add(indexShift))
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.level, QHierarchyBaseEntity.hierarchyBaseEntity.level.add(levelShift))
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.between(entity.getLeftIndex(), entity.getRightIndex()))
                .execute();
    }

    /**
     * Shifts indexes to make space for the new subtree.
     *
     * @param startIndex the starting index for the shift
     * @param shiftBy    the amount to shift by
     */
    protected void shiftIndexes(int startIndex, int shiftBy) {
        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex, QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.add(shiftBy))
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.goe(startIndex))
                .execute();

        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex, QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.add(shiftBy))
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.goe(startIndex))
                .execute();
    }

    /**
     * Compacts the space left by the moved subtree.
     *
     * @param startIndex the starting index for the compacting
     * @param compactBy  the amount to compact by
     */
    protected void compactIndexes(int startIndex, int compactBy) {
        log.info("Compacting indexes from {} by size {}", startIndex, compactBy);

        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex, QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.subtract(compactBy))
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.goe(startIndex))
                .execute();

        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex, QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.subtract(compactBy))
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.goe(startIndex))
                .execute();
    }

    /**
     * Updates the root ID and parent entity ID for a entity and its subtree.
     * <p>
     * This method is responsible for updating the `rootId` of all nodes in the subtree of the given entity to match
     * the `rootId` of the new parent entity. It also updates the `parentDepartmentId` of the entity being moved.
     *
     * @param entity          the entity being moved
     * @param newParentEntity the new parent entity for the moved entity
     */
    protected void updateSubtreeParentAndRootIds(E entity, E newParentEntity) {
        // Update the rootId for all nodes in the subtree
        log.info("Updating parent and root Ids of {} subtree to match the new parent {}", entity.getId(), newParentEntity.getId());
        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.rootId, newParentEntity.getRootId())
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.between(entity.getLeftIndex(), entity.getRightIndex()))
                .execute();

        // Update the parentDepartmentId for the entity being moved
        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.parentId, newParentEntity.getId())
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.id.eq(entity.getId()))
                .execute();
    }

    /**
     * Moves a entity to the root level.
     *
     * @param entity the entity to be moved to the root level
     */
    protected void moveToRoot(E entity) {
        int subtreeSize = entity.getRightIndex() - entity.getLeftIndex() + 1;
        int indexShift = -(entity.getLeftIndex() - 1);

        // Log current state
        log.info("Moving entity {} to root. Subtree size: {}, Index shift: {}",
                entity.getId(), subtreeSize, indexShift);

        // Shift indexes to make space for the new root subtree
        entityManager.flush();
        shiftIndexesForRoot(subtreeSize);
        entityManager.refresh(entity);

        // Move the subtree to the new root level and update levels
        entityManager.flush();
        updateSubtreeIndexesAndLevels(entity, indexShift, -entity.getLevel());
        entityManager.refresh(entity);

        // Compact the space left by the moved subtree
        entityManager.flush();
        compactIndexes(entity.getLeftIndex(), subtreeSize);
        entityManager.refresh(entity);

        // Update parent and root IDs of the subtree
        entityManager.flush();
        updateSubtreeToRoot(entity);
    }

    /**
     * Shifts indexes to make space for the new root subtree.
     *
     * @param shiftBy the amount to shift by
     */
    protected void shiftIndexesForRoot(int shiftBy) {

        log.info("Shifting indexes for root by {}", shiftBy);

        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex, QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.add(shiftBy))
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.goe(1))
                .execute();

        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex, QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.add(shiftBy))
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.goe(1))
                .execute();
    }

    /**
     * Updates the parent ID and root ID for a entity and its subtree to move it to the root level.
     *
     * @param entity the entity being moved to the root level
     */
    protected void updateSubtreeToRoot(E entity) {

        log.info("Updating subtree to root for entity {}", entity.getId());

        // Update the rootId for all nodes in the subtree to be their own ID
        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.rootId, QHierarchyBaseEntity.hierarchyBaseEntity.id)
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.between(entity.getLeftIndex(), entity.getRightIndex()))
                .execute();

        // Update the parentDepartmentId for the entity being moved to null
        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.parentId, (Long) null)
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.id.eq(entity.getId()))
                .execute();
    }

    /**
     * Deletes an entity from the hierarchical structure.
     *
     * @param id The ID of the entity to delete.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     * @throws DataIntegrityException  If there is an issue with data integrity during deletion.
     */
    @Override
    @Transactional
    public void deleteEntity(Long id) throws EntityNotFoundException, DataIntegrityException {
        // Retrieve the entity by ID
        E departmentToDelete = entityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entitynot found with ID: " + id));

        E parentDepartment = null;
        if (departmentToDelete.getParentId() != null) {
            parentDepartment = entityRepository.findById(departmentToDelete.getParentId())
                    .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with ID: " + departmentToDelete.getParentId()));
            validateParentDepartment(departmentToDelete, parentDepartment);
        }

        // Retrieve all sub-departments (descendants) of the entity to delete
        List<E> subDepartments = (List<E>) jpaQueryFactory.selectFrom(QHierarchyBaseEntity.hierarchyBaseEntity)
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.goe(departmentToDelete.getLeftIndex())
                        .and(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.loe(departmentToDelete.getRightIndex())))
                .fetch();

        // Delete the entity and its sub-departments
        subDepartments.forEach(entityRepository::delete);

        // Reorder the nested set structure after deletion
        reorderNestedSet();

    }

    /**
     * Reorders the nested set structure after modifications.
     */
    @Transactional
    public void reorderNestedSet() {
        List<E> departments = (List<E>) jpaQueryFactory.selectFrom(QHierarchyBaseEntity.hierarchyBaseEntity)
                .orderBy(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.asc())
                .fetch();

        int index = 1;
        for (E entity : departments) {
            int size = (entity.getRightIndex() - entity.getLeftIndex()) / 2;
            updateDepartmentIndexes(entity, index, size);
            index += size + 1;
        }
    }

    /**
     * Updates the left and right indexes of a entity and its subtree.
     *
     * @param entity the entity to update
     * @param index  the starting index for the entity
     * @param size   the size (number of nodes) of the subtree
     */
    @Transactional
    protected void updateDepartmentIndexes(E entity, int index, int size) {
        int delta = entity.getLeftIndex() - index;

        entity.setLeftIndex(entity.getLeftIndex() - delta);
        entity.setRightIndex(entity.getRightIndex() - delta);

        jpaQueryFactory.update(QHierarchyBaseEntity.hierarchyBaseEntity)
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex, QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.subtract(delta))
                .set(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex, QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.subtract(delta))
                .where(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.between(index, index + size))
                .execute();
    }

    /**
     * Retrieves all entities in the hierarchical structure.
     *
     * @return List of DTOs representing all entities.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAllEntities() {
        return getAllEntities(true);
    }

    /**
     * Retrieves all entities in the hierarchical structure.
     *
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return List of DTOs representing all entities.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAllEntities(boolean fetchSubEntities) {
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;

        List<E> entities = (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(entity.instanceOf(entityClass))
                .orderBy(entity.leftIndex.asc())
                .fetch();

        log.info("Retrieved {} entities.", entities.size());

        return entityMapper.toDtoList(entities, fetchSubEntities);
    }

    /**
     * Retrieves all entities in the hierarchical structure with pagination support.
     *
     * @param pageable         Pageable object containing page and size information.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return Paginated response containing entities for the requested page.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> getAllEntities(Pageable pageable, boolean fetchSubEntities) {
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        // Assuming HierarchyBaseEntity is your entity class
        PathBuilder<HierarchyBaseEntity> entityPath = new PathBuilder<>(HierarchyBaseEntity.class, "hierarchyBaseEntity");

        // Extract sorting criteria from pageable
        OrderSpecifier<?>[] orderBy = pageable.getSort().stream()
                .map(order -> {
                    PathBuilder<?> pathBuilder = entityPath.get(order.getProperty());
                    Expression expression = pathBuilder;
                    return new OrderSpecifier<>(order.isAscending() ? com.querydsl.core.types.Order.ASC
                            : com.querydsl.core.types.Order.DESC, expression);
                })
                .toArray(OrderSpecifier[]::new);

        // Fetch entities based on pagination and sorting parameters
        List<E> entities = (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(entity.instanceOf(entityClass))
                .orderBy(orderBy)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Fetch total count of entities for pagination metadata
        long totalCount = jpaQueryFactory.selectFrom(entity)
                .where(entity.instanceOf(entityClass))
                .fetchCount();

        log.info("Retrieved {} entities.", totalCount);

        // Map fetched entities to response DTOs using the abstract mapper
        List<SD> dtoList = entityMapper.toDtoList(entities, fetchSubEntities);

        // Create and return paginated response DTO using the abstract mapper
        return entityMapper.toPaginatedDtoList(entities, pageable, totalCount, fetchSubEntities);
    }

    /**
     * Retrieves sub-entities of a parent entity from the hierarchical structure.
     *
     * @param parentId The ID of the parent entity.
     * @return List of DTOs representing sub-entities.
     * @throws ParentEntityNotFoundException If the parent entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getSubEntities(Long parentId) throws ParentEntityNotFoundException {
        return getSubEntities(parentId, true);
    }

    /**
     * Retrieves sub-entities of a parent entity from the hierarchical structure.
     *
     * @param parentId         The ID of the parent entity.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return List of DTOs representing sub-entities.
     * @throws ParentEntityNotFoundException If the parent entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getSubEntities(Long parentId, boolean fetchSubEntities) throws ParentEntityNotFoundException {
        log.info("Retrieving sub-entities for parent entity with id: {}", parentId);

        // Check if the parent entity exists
        E parentEntity = entityRepository.findById(parentId)
                .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with ID: " + parentId));

        // Query sub-entities using QueryDSL
        List<E> subEntities = (List<E>) jpaQueryFactory.selectFrom(QHierarchyBaseEntity.hierarchyBaseEntity)
                .where(
                        QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.gt(parentEntity.getLeftIndex())
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.lt(parentEntity.getRightIndex()))
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.level.eq(parentEntity.getLevel() + 1))
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.instanceOf(entityClass))
                )
                .fetch();

        // Map entities to HierarchyResponseDto
        return entityMapper.toDtoList(subEntities, fetchSubEntities);
    }

    /**
     * Retrieves sub-entities of a parent entity from the hierarchical structure with pagination support.
     *
     * @param parentId         The ID of the parent entity.
     * @param pageable         Pageable object containing page and size information.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return Paginated response containing sub-entities for the requested page.
     * @throws ParentEntityNotFoundException If the parent entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> getSubEntities(Long parentId, Pageable pageable, boolean fetchSubEntities)
            throws ParentEntityNotFoundException {
        log.info("Retrieving paginated sub-entities for parent entity with id: {}", parentId);

        // Find the parent entity or throw an exception if not found
        E parentEntity = entityRepository.findById(parentId)
                .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with ID: " + parentId));

        log.info("Parent entity found: {}", parentEntity);

        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        PathBuilder<HierarchyBaseEntity> entityPath = new PathBuilder<>(HierarchyBaseEntity.class, "hierarchyBaseEntity");

        // Extract sorting criteria from pageable
        OrderSpecifier<?>[] orderBy = pageable.getSort().stream()
                .map(order -> {
                    PathBuilder<?> pathBuilder = entityPath.get(order.getProperty());
                    Expression expression = pathBuilder;
                    return new OrderSpecifier<>(order.isAscending() ? com.querydsl.core.types.Order.ASC
                            : com.querydsl.core.types.Order.DESC, expression);
                })
                .toArray(OrderSpecifier[]::new);

        log.info("Sorting criteria: {}", Arrays.toString(orderBy));

        // Fetch sub-entities based on pagination and sorting parameters
        BooleanExpression query = entity.leftIndex.gt(parentEntity.getLeftIndex())
                .and(entity.rightIndex.lt(parentEntity.getRightIndex()))
                .and(entity.level.eq(parentEntity.getLevel() + 1))
                .and(entity.instanceOf(entityClass));

        log.info("Executing QueryDSL query: {}", query);

        List<E> subEntities = (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(query)
                .orderBy(orderBy) // Apply sorting
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        log.info("Fetched sub-entities: {}", subEntities);

        // Fetch total count of sub-entities for pagination metadata
        long totalCount = jpaQueryFactory.selectFrom(entity)
                .where(entity.parentId.eq(parentId))
                .fetchCount();

        log.info("Total count of sub-entities: {}", totalCount);

        // Convert the sub-entities to DTOs using the abstract mapper
        return entityMapper.toPaginatedDtoList(subEntities, pageable, totalCount, fetchSubEntities);
    }

    /**
     * Retrieves an entity by its ID from the hierarchical structure.
     *
     * @param id The ID of the entity to retrieve.
     * @return The DTO representing the retrieved entity.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public SD getEntityById(Long id) throws EntityNotFoundException {
        return getEntityById(id, true);
    }

    /**
     * Retrieves an entity by its ID from the hierarchical structure.
     *
     * @param id               The ID of the entity to retrieve.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return The DTO representing the retrieved entity.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public SD getEntityById(Long id, boolean fetchSubEntities) throws EntityNotFoundException {
        log.info("Retrieving entity with id: {}", id);

        // Retrieve entity from repository
        E entity = entityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + id));

        // Map entity to HierarchyResponseDto
        return entityMapper.toDto(entity, fetchSubEntities);
    }

    /**
     * Searches entities by their name in the hierarchical structure.
     *
     * @param name The name to search for.
     * @return List of DTOs representing entities matching the search criteria.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> searchEntitiesByName(String name) {
        return searchEntitiesByName(name, true);
    }

    /**
     * Searches entities by their name in the hierarchical structure.
     *
     * @param name             The name to search for.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return List of DTOs representing entities matching the search criteria.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> searchEntitiesByName(String name, boolean fetchSubEntities) {
        log.info("Searching entities by name: {}", name);

        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        BooleanExpression query = entity.name.containsIgnoreCase(name)
                .and(entity.instanceOf(entityClass));

        List<E> entities = jpaQueryFactory.selectFrom(entity)
                .where(query)
                .fetch()
                .stream()
                .map(e -> (E) e)
                .collect(Collectors.toList());

        return entityMapper.toDtoList(entities, fetchSubEntities);
    }

    /**
     * Searches entities by their name in the hierarchical structure with pagination support.
     *
     * @param name             The name to search for.
     * @param pageable         Pageable object containing page and size information.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return Paginated response containing entities matching the search criteria for the requested page.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> searchEntitiesByName(String name, Pageable pageable, boolean fetchSubEntities) {
        log.info("Searching entities by name: {}, with pagination and sorting", name);

        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        PathBuilder<HierarchyBaseEntity> entityPath = new PathBuilder<>(HierarchyBaseEntity.class, "hierarchyBaseEntity");

        // Extract sorting criteria from pageable
        OrderSpecifier<?>[] orderBy = pageable.getSort().stream()
                .map(order -> {
                    PathBuilder<?> pathBuilder = entityPath.get(order.getProperty());
                    Expression expression = pathBuilder;
                    return new OrderSpecifier<>(order.isAscending() ? com.querydsl.core.types.Order.ASC
                            : com.querydsl.core.types.Order.DESC, expression);
                })
                .toArray(OrderSpecifier[]::new);

        BooleanExpression query = entity.name.containsIgnoreCase(name)
                .and(entity.instanceOf(entityClass));

        // Fetch entities based on name search, pagination, and sorting parameters
        List<E> entities = (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(query)
                .orderBy(orderBy) // Apply sorting
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Fetch total count of entities for pagination metadata
        long total = jpaQueryFactory.selectFrom(entity)
                .where(query)
                .fetchCount();

        // Convert the entities to DTOs using the abstract mapper
        return entityMapper.toPaginatedDtoList(entities, pageable, total, fetchSubEntities);
    }

    /**
     * Retrieves the parent entity of an entity in the hierarchical structure.
     *
     * @param entityId The ID of the entity to retrieve the parent for.
     * @return The DTO representing the parent entity.
     * @throws EntityNotFoundException       If the entity with the specified ID is not found.
     * @throws ParentEntityNotFoundException If the parent entity of the specified entity is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public SD getParentEntity(Long entityId) throws EntityNotFoundException, ParentEntityNotFoundException {
        return getParentEntity(entityId, true);
    }

    /**
     * Retrieves the parent entity of an entity in the hierarchical structure.
     *
     * @param entityId         The ID of the entity to retrieve the parent for.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return The DTO representing the parent entity.
     * @throws EntityNotFoundException       If the entity with the specified ID is not found.
     * @throws ParentEntityNotFoundException If the parent entity of the specified entity is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public SD getParentEntity(Long entityId, boolean fetchSubEntities)
            throws EntityNotFoundException, ParentEntityNotFoundException {
        log.info("Retrieving parent entity for entity with id: {}", entityId);

        // Check if the entity exists
        E entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Check if the entity has a parent
        if (entity.getParentId() == null) {
            throw new ParentEntityNotFoundException("Entity with ID " + entityId + " has no parent entity");
        }

        // Retrieve the parent entity
        E parentEntity = entityRepository.findById(entity.getParentId())
                .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with ID: " + entity.getParentId()));

        // Map parent entity to HierarchyResponseDto
        return entityMapper.toDto(parentEntity, fetchSubEntities);
    }

    /**
     * Retrieves all root departments (departments without a parent).
     *
     * @return a list of root departments as response DTOs
     */
    @Transactional(readOnly = true)
    public List<SD> getAllRootEntities() {
        // Query root entities using QueryDSL
        List<E> rootEntities = (List<E>) jpaQueryFactory.selectFrom(QHierarchyBaseEntity.hierarchyBaseEntity)
                .where(
                        QHierarchyBaseEntity.hierarchyBaseEntity.parentId.isNull()
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.instanceOf(entityClass))
                )
                .fetch();

        // Map entities toHierarchyResponseDto
        return rootEntities.stream()
                .map(entityMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves descendants of an entity in the hierarchical structure.
     *
     * @param entityId The ID of the entity to retrieve descendants for.
     * @return List of DTOs representing descendants of the entity.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getDescendants(Long entityId) throws EntityNotFoundException {
        return getDescendants(entityId, true);
    }

    /**
     * Retrieves descendants of an entity in the hierarchical structure.
     *
     * @param entityId         The ID of the entity to retrieve descendants for.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return List of DTOs representing descendants of the entity.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getDescendants(Long entityId, boolean fetchSubEntities) throws EntityNotFoundException {
        log.info("Fetching descendants for entity with id: {}", entityId);

        // Retrieve the entity by ID
        E entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Query all descendants using QueryDSL
        List<E> descendants = (List<E>) jpaQueryFactory.selectFrom(QHierarchyBaseEntity.hierarchyBaseEntity)
                .where(
                        QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.gt(entity.getLeftIndex())
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.lt(entity.getRightIndex()))
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.instanceOf(entityClass))
                )
                .fetch();

        // Map entities to HierarchyResponseDto
        return entityMapper.toDtoList(descendants, fetchSubEntities);
    }

    /**
     * Retrieves descendants of an entity in the hierarchical structure with pagination support.
     *
     * @param entityId         The ID of the entity to retrieve descendants for.
     * @param pageable         Pageable object containing page and size information.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return Paginated response containing descendants of the entity for the requested page.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> getDescendants(Long entityId, Pageable pageable, boolean fetchSubEntities)
            throws EntityNotFoundException {
        log.info("Fetching paginated descendants for entity with id: {}", entityId);

        // Retrieve the entity by ID
        E foundEntity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Query all descendants using QueryDSL
        List<E> descendants = (List<E>) jpaQueryFactory.selectFrom(QHierarchyBaseEntity.hierarchyBaseEntity)
                .where(
                        QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.gt(foundEntity.getLeftIndex())
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.lt(foundEntity.getRightIndex()))
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.instanceOf(entityClass))
                )
                .fetch();

        // Extract sorting criteria from pageable
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        OrderSpecifier<?>[] orderBy = pageable.getSort().stream()
                .map(order -> {
                    PathBuilder<?> pathBuilder = new PathBuilder<>(entity.getType(), entity.getMetadata());
                    Expression expression = pathBuilder.get(order.getProperty());
                    return new OrderSpecifier<>(order.isAscending() ? com.querydsl.core.types.Order.ASC
                            : com.querydsl.core.types.Order.DESC, expression);
                })
                .toArray(OrderSpecifier[]::new);

        // Sort the descendants using a generic comparator
        descendants.sort((e1, e2) -> {
            try {
                for (OrderSpecifier<?> order : orderBy) {
                    String propertyName = ((PathBuilder<?>) order.getTarget()).getMetadata().getName();
                    Method getter = entityClass.getMethod("get" + capitalize(propertyName));
                    Comparable<Object> value1 = (Comparable<Object>) getter.invoke(e1);
                    Comparable<Object> value2 = (Comparable<Object>) getter.invoke(e2);

                    int comparison = value1.compareTo(value2);
                    if (comparison != 0) {
                        return order.getOrder() == com.querydsl.core.types.Order.ASC ? comparison : -comparison;
                    }
                }
            } catch (ReflectiveOperationException ex) {
                log.error("Error during sorting descendants: {}", ex.getMessage(), ex);
                throw new RuntimeException("Error during sorting descendants", ex);
            }
            return 0;
        });

        // Calculate total elements and apply pagination
        int totalElements = descendants.size();
        int start = Math.min((int) pageable.getOffset(), totalElements);
        int end = Math.min((start + pageable.getPageSize()), totalElements);
        List<E> paginatedDescendants = descendants.subList(start, end);

        return entityMapper.toPaginatedDtoList(paginatedDescendants, pageable, totalElements, fetchSubEntities);
    }

    /**
     * Retrieves ancestors of an entity in the hierarchical structure.
     *
     * @param entityId The ID of the entity to retrieve ancestors for.
     * @return List of DTOs representing ancestors of the entity.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAncestors(Long entityId) throws EntityNotFoundException {
        return getAncestors(entityId, true);
    }

    /**
     * Retrieves ancestors of an entity in the hierarchical structure.
     *
     * @param entityId         The ID of the entity to retrieve ancestors for.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return List of DTOs representing ancestors of the entity.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAncestors(Long entityId, boolean fetchSubEntities) throws EntityNotFoundException {
        log.info("Fetching ancestors for entity with id: {}", entityId);

        // Retrieve the entity by ID
        E entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Query all ancestors using QueryDSL
        List<E> ancestors = (List<E>) jpaQueryFactory.selectFrom(QHierarchyBaseEntity.hierarchyBaseEntity)
                .where(
                        QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.lt(entity.getLeftIndex())
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.gt(entity.getRightIndex()))
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.instanceOf(entityClass))
                )
                .orderBy(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.asc())
                .fetch();

        // Map entities to HierarchyResponseDto
        return entityMapper.toDtoList(ancestors, fetchSubEntities);
    }

    /**
     * Retrieves ancestors of an entity in the hierarchical structure with pagination support.
     *
     * @param entityId         The ID of the entity to retrieve ancestors for.
     * @param pageable         Pageable object containing page and size information.
     * @param fetchSubEntities Whether to fetch sub-entities along with main entities.
     * @return Paginated response containing ancestors of the entity for the requested page.
     * @throws EntityNotFoundException If the entity with the specified ID is not found.
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> getAncestors(Long entityId, Pageable pageable, boolean fetchSubEntities)
            throws EntityNotFoundException {
        log.info("Fetching paginated ancestors for entity with id: {}", entityId);

        // Retrieve the entity by ID
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        E foundEntity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + entityId));

        // Fetch all ancestors within the same transaction
        List<E> ancestors = (List<E>) jpaQueryFactory.selectFrom(QHierarchyBaseEntity.hierarchyBaseEntity)
                .where(
                        QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.lt(foundEntity.getLeftIndex())
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.rightIndex.gt(foundEntity.getRightIndex()))
                                .and(QHierarchyBaseEntity.hierarchyBaseEntity.instanceOf(entityClass))
                )
                .orderBy(QHierarchyBaseEntity.hierarchyBaseEntity.leftIndex.asc())
                .fetch();

        // Extract sorting criteria from pageable
        OrderSpecifier<?>[] orderBy = pageable.getSort().stream()
                .map(order -> {
                    PathBuilder<?> pathBuilder = new PathBuilder<>(entity.getType(), entity.getMetadata());
                    Expression expression = pathBuilder.get(order.getProperty());
                    return new OrderSpecifier<>(order.isAscending() ? com.querydsl.core.types.Order.ASC
                            : com.querydsl.core.types.Order.DESC, expression);
                })
                .toArray(OrderSpecifier[]::new);

        // Sort the ancestors using a generic comparator
        ancestors.sort((e1, e2) -> {
            try {
                for (OrderSpecifier<?> order : orderBy) {
                    String propertyName = ((PathBuilder<?>) order.getTarget()).getMetadata().getName();
                    Method getter = entityClass.getMethod("get" + capitalize(propertyName));
                    Comparable<Object> value1 = (Comparable<Object>) getter.invoke(e1);
                    Comparable<Object> value2 = (Comparable<Object>) getter.invoke(e2);

                    int comparison = value1.compareTo(value2);
                    if (comparison != 0) {
                        return order.getOrder() == com.querydsl.core.types.Order.ASC ? comparison : -comparison;
                    }
                }
            } catch (ReflectiveOperationException ex) {
                log.error("Error during sorting ancestors: {}", ex.getMessage(), ex);
                throw new RuntimeException("Error during sorting ancestors", ex);
            }
            return 0;
        });


        // Calculate total elements and apply pagination
        int totalElements = ancestors.size();
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), totalElements);
        List<E> paginatedAncestors = ancestors.subList(start, end);

        return entityMapper.toPaginatedDtoList(paginatedAncestors, pageable, totalElements, fetchSubEntities);
    }
}
