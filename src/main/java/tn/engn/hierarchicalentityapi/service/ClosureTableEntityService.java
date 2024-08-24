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
import tn.engn.hierarchicalentityapi.factory.HierarchyClosureFactory;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntityClosure;
import tn.engn.hierarchicalentityapi.model.QHierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseClosureRepository;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.mapstruct.ap.shaded.freemarker.template.utility.StringUtil.capitalize;

/**
 * Service class for managing hierarchical entities using the Closure Table Model.
 *
 * @param <E>  the type of the entity
 * @param <C>  the type of the entity closure
 * @param <RD> the type of the request DTO
 * @param <SD> the type of the response DTO
 */
@Service
@Slf4j
public abstract class ClosureTableEntityService<E extends HierarchyBaseEntity, C extends HierarchyBaseEntityClosure, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto>
        implements HierarchyService<E, RD, SD> {

    protected final HierarchyBaseRepository<E> entityRepository;
    protected final HierarchyBaseClosureRepository<C> entityClosureRepository;
    protected final HierarchyMapper<E, RD, SD> entityMapper;
    @PersistenceContext
    protected EntityManager entityManager;
    protected final JPAQueryFactory jpaQueryFactory;

    private HierarchyClosureFactory hierarchyClosureFactory;

    @Setter
    @Value("${entity.max-name-length}")
    private int maxNameLength;

    Class<E> entityClass;
    Class<C> entityClosureClass;

    /**
     * Constructor for ClosureTableEntityService.
     *
     * @param entityRepository        the repository for the entity
     * @param entityClosureRepository the repository for the entity closure
     * @param entityMapper            the mapper for converting between entities and DTOs
     * @param jpaQueryFactory         the JPA query factory for executing queries
     * @param hierarchyClosureFactory the factory for creating hierarchical entity closures instances
     * @param entityClass             the class type of the entity
     * @param entityClosureClass      the class type of the entity closure
     */
    public ClosureTableEntityService(HierarchyBaseRepository<E> entityRepository,
                                     HierarchyBaseClosureRepository<C> entityClosureRepository,
                                     HierarchyMapper<E, RD, SD> entityMapper,
                                     JPAQueryFactory jpaQueryFactory,
                                     HierarchyClosureFactory hierarchyClosureFactory,
                                     Class<E> entityClass,
                                     Class<C> entityClosureClass) {
        this.entityRepository = entityRepository;
        this.entityClosureRepository = entityClosureRepository;
        this.entityMapper = entityMapper;
        this.jpaQueryFactory = jpaQueryFactory;
        this.hierarchyClosureFactory = hierarchyClosureFactory;
        this.entityClass = entityClass;
        this.entityClosureClass = entityClosureClass;
    }

    /**
     * Creates a new entity based on the provided DTO.
     *
     * @param requestDto the DTO containing the new entity's details
     * @return the created entity as a response DTO
     * @throws ParentEntityNotFoundException if the parent entity is not found
     * @throws ValidationException           if the input data is invalid
     * @throws DataIntegrityException        if creating the entity violates data integrity constraints
     */
    @Override
    public SD createEntity(RD requestDto) throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Validate input data
        validateCreateDepartmentRequest(requestDto);

        // Map DTO to entity
        E entity = entityMapper.toEntity(requestDto);

        // Save the new entity
        E savedDepartment = entityRepository.save(entity);

        // Create closure table entries
        createClosureEntries(savedDepartment);

        // Map saved entity to response DTO
        return entityMapper.toDto(savedDepartment);
    }

    /**
     * Validates the input data for creating a new entity.
     *
     * @param requestDto the DTO containing the new entity's details
     * @throws ValidationException           if the input data is invalid
     * @throws ParentEntityNotFoundException if the parent entity is not found
     */
    private void validateCreateDepartmentRequest(HierarchyRequestDto requestDto) throws ValidationException {
        String name = requestDto.getName();
        Long parentId = requestDto.getParentEntityId();

        // Validate the entity name
        validateDepartmentName(name);

        // Find the parent entity
        if (parentId != null) {
            E parent = entityRepository.findById(parentId)
                    .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found."));
        }

        log.info("Creating entity with name: {} and parentId: {}", name, parentId);
    }

    /**
     * Validates the entity name for length constraints.
     *
     * @param name the entity name to validate
     * @throws ValidationException if the entity name is empty, null, or exceeds max length
     */
    private void validateDepartmentName(String name) throws ValidationException {
        if (name == null || name.isEmpty()) {
            throw new ValidationException("Entity name cannot be null or empty.");
        }

        if (name.length() > maxNameLength) {
            throw new ValidationException("Entity name cannot be longer than " + maxNameLength + " characters.");
        }
    }

    /**
     * Creates the closure table entries for the newly created entity.
     *
     * @param entity the newly created entity
     * @throws ParentEntityNotFoundException if the parent entity is not found
     */
    private void createClosureEntries(E entity)
            throws ParentEntityNotFoundException {

        Set<C> closureEntries = new HashSet<>();

        // Add self-closure entry
        closureEntries.add(
                (C) hierarchyClosureFactory.createClosure(
                        entity.getId(),
                        entity.getId(),
                        0
                )
        );

        // If the entity has a parent, add entries for all ancestors
        if (entity.getParentId() != null) {
            List<C> parentClosures = findClosureAncestors(entity.getParentId());

            if (parentClosures.isEmpty()) {
                entityRepository.delete(entity);
                throw new ParentEntityNotFoundException("Parent entity not found.");
            }

            for (C parentClosure : parentClosures) {
                closureEntries.add(
                        (C) hierarchyClosureFactory.createClosure(
                                parentClosure.getAncestorId(),
                                entity.getId(),
                                parentClosure.getLevel() + 1
                        )
                );
            }
        }

        // Save all closure entries (sorted for testing reasons)
        entityClosureRepository.saveAll(
                closureEntries.stream()
                        .sorted(
                                Comparator.comparingInt(C::getLevel)
                        )
                        .collect(Collectors.toList())
        );
    }

    /**
     * Updates an existing entity's name and optionally changes its parent entity.
     *
     * @param entityId         the ID of the entity to update
     * @param updatedEntityDto the DTO containing updated entity information
     * @return the updated entity DTO
     * @throws EntityNotFoundException       if the entity with the given ID is not found
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     * @throws DataIntegrityException        if updating the entity violates data integrity constraints
     * @throws ValidationException           if the updated entity name is invalid (empty, null, or exceeds max length)
     */
    @Override
    @Transactional
    public SD updateEntity(Long entityId, RD updatedEntityDto)
            throws EntityNotFoundException, ParentEntityNotFoundException, DataIntegrityException, ValidationException {
        // Extract updated entity information from DTO
        String name = updatedEntityDto.getName();
        Long parentId = updatedEntityDto.getParentEntityId();

        // Validate input data and get existing entity
        E existingDepartment = validateUpdateDepartmentRequest(entityId, updatedEntityDto);

        // Check if the parent ID or name has been modified
        boolean isParentModified = !Objects.equals(existingDepartment.getParentId(), updatedEntityDto.getParentEntityId());
        boolean isNameModified = !Objects.equals(existingDepartment.getName(), updatedEntityDto.getName());

        if (!isParentModified && !isNameModified) {
            log.info("No changes detected for entity with ID: {}", entityId);
            return entityMapper.toDto(existingDepartment);
        }

        // If parent is modified, check for circular dependency and update closure entries
        if (isParentModified) {
            Long newParentId = updatedEntityDto.getParentEntityId();
            if (newParentId != null && hasCircularDependency(entityId, newParentId)) {
                throw new DataIntegrityException("Circular dependency detected: Entity cannot be its own ancestor.");
            }
            updateClosureEntries(entityId, newParentId);
            existingDepartment.setParentId(newParentId);
        }

        // If name is modified, update entity name
        if (isNameModified) {
            existingDepartment.setName(updatedEntityDto.getName());
        }

        entityRepository.save(existingDepartment);
        log.info("Successfully updated entity with ID: {}", entityId);

        return entityMapper.toDto(existingDepartment);
    }

    /**
     * Checks if updating a entity to have the given new parent would create a circular dependency.
     *
     * @param entityId the ID of the entity being updated
     * @param newParentId  the ID of the new parent entity
     * @return true if a circular dependency is detected, false otherwise
     */
    private boolean hasCircularDependency(Long entityId, Long newParentId) {
        List<C> ancestors = findClosureAncestors(newParentId);

        if (ancestors.isEmpty()) {
            throw new ParentEntityNotFoundException("Parent entity not found.");
        }

        return ancestors.stream().anyMatch(closure -> closure.getAncestorId().equals(entityId));
    }

    /**
     * Finds all ancestor entities of a given entity.
     *
     * @param parentId the ID of the parent entity whose ancestors are to be found
     * @return a list of ancestor entities
     */
    protected List<C> findClosureAncestors(Long parentId) {
        QHierarchyBaseEntityClosure entityClosure = QHierarchyBaseEntityClosure.hierarchyBaseEntityClosure;

        // Fetching all entities where the given parentId is a descendant
        List<C> ancestors = (List<C>) jpaQueryFactory.selectFrom(entityClosure)
                .where(entityClosure.descendantId.eq(parentId)
                        .and(entityClosure.instanceOf(entityClosureClass)))
                .fetch();
        return ancestors;
    }

    /**
     * Updates the closure table entries for the updated entity and its descendants.
     *
     * @param entityId       the updated entity ID
     * @param parentEntityId the updated entity parent ID
     * @throws ParentEntityNotFoundException if the parent entity closure entries are not found
     */
    private void updateClosureEntries(Long entityId, Long parentEntityId) {
        log.info("Updating closure entries for entity ID: {} with new parent ID: {}", entityId, parentEntityId);

        // Get the descendants of the entity
        List<C> descendants = findClosureDescendants(entityId);

        // Remove old closure records for the entity and its descendants
        for (C descendant : descendants) {
            deleteClosuresByDescendantId(descendant.getDescendantId());
        }

        Set<C> closureEntries = new HashSet<>();

        // Add closure entries for the entity and its descendants
        for (C descendant : descendants) {
            closureEntries.add(
                    (C) hierarchyClosureFactory.createClosure(
                            entityId,
                            descendant.getDescendantId(),
                            descendant.getLevel()
                    )
            );

            // Adding self-closure for descendants
            closureEntries.add(
                    (C) hierarchyClosureFactory.createClosure(
                            descendant.getDescendantId(),
                            descendant.getDescendantId(),
                            0
                    )
            );
        }

        // If the entity has a new parent, add entries for all ancestors
        if (parentEntityId != null) {
            List<C> parentClosures = findClosureAncestors(parentEntityId);

            if (parentClosures.isEmpty()) {
                throw new ParentEntityNotFoundException("Parent entity not found.");
            }

            for (C parentClosure : parentClosures) {
                for (C descendant : descendants) {
                    closureEntries.add(
                            (C) hierarchyClosureFactory.createClosure(
                                    parentClosure.getAncestorId(),
                                    descendant.getDescendantId(),
                                    parentClosure.getLevel() + 1 + descendant.getLevel()
                            )
                    );
                }

                // Adding closure for the entity itself
                closureEntries.add(
                        (C) hierarchyClosureFactory.createClosure(
                                parentClosure.getAncestorId(),
                                entityId,
                                parentClosure.getLevel() + 1
                        )
                );
            }
        }

        // Adding self-closure for the entity
        closureEntries.add(
                (C) hierarchyClosureFactory.createClosure(
                        entityId,
                        entityId,
                        0
                )
        );

        // Save all closure entries (sorted for testing reasons)
        entityClosureRepository.saveAll(
                closureEntries.stream()
                        .sorted(Comparator.comparingInt(C::getLevel)
                                .thenComparingLong(C::getAncestorId))
                        .collect(Collectors.toList())
        );

        log.info("Closure entries updated for entity ID: {}", entityId);
    }

    /**
     * Finds all descendant entities of a given entity.
     *
     * @param entityId the ID of the entity whose descendants are to be found
     * @return a list of descendant entities
     */
    protected List<C> findClosureDescendants(Long entityId) {
        QHierarchyBaseEntityClosure entityClosure = QHierarchyBaseEntityClosure.hierarchyBaseEntityClosure;

        // Fetching all entities where the given entityId is an ancestor
        List<C> descendants = (List<C>) jpaQueryFactory.selectFrom(entityClosure)
                .where(entityClosure.ancestorId.eq(entityId)
                        .and(entityClosure.instanceOf(entityClosureClass)))
                .fetch();
        return descendants;
    }

    /**
     * Deletes all entity closure records for a given descendant ID.
     *
     * @param descendantId the ID of the descendant entity to delete closure records for
     */
    public void deleteClosuresByDescendantId(Long descendantId) {
        QHierarchyBaseEntityClosure entityClosure = QHierarchyBaseEntityClosure.hierarchyBaseEntityClosure;

        // Deleting all entity closure records where the given descendantId matches
        jpaQueryFactory
                .delete(entityClosure)
                .where(entityClosure.descendantId.eq(descendantId)
                        .and(entityClosure.instanceOf(entityClosureClass)))
                .execute();
    }

    /**
     * Validates the input data for updating an existing entity.
     *
     * @param entityId         the ID of the entity to be updated
     * @param updatedEntityDto the DTO containing the new entity's details
     * @return the existing entity
     * @throws ValidationException               if the input data is invalid
     * @throws EntityNotFoundException       if the entity with given ID is not found
     * @throws ParentEntityNotFoundException if the parent entity is not found
     */
    private E validateUpdateDepartmentRequest(Long entityId, HierarchyRequestDto updatedEntityDto) throws ValidationException {
        String name = updatedEntityDto.getName();

        // Validate the entity name
        validateDepartmentName(name);

        // Find the existing entity
        E existingDepartment = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found."));

        // Check if the parent entity exists if a parent ID is provided
        Long parentDepartmentId = updatedEntityDto.getParentEntityId();
        if (parentDepartmentId != null) {
            entityRepository.findById(parentDepartmentId)
                    .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found."));
        }

        return existingDepartment;
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param entityId the ID of the entity to delete
     * @throws EntityNotFoundException if the entity with the given ID is not found
     * @throws DataIntegrityException  if deletion would result in a circular dependency or if other constraints are violated
     */
    @Override
    @Transactional
    public void deleteEntity(Long entityId) throws EntityNotFoundException, DataIntegrityException {
        log.info("Updating entity with id: {}", entityId);

        E entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + entityId));

        // Check for circular dependencies before deleting
        if (entity.getParentId() != null && hasCircularDependencyToDelete(entityId, entity.getParentId())) {
            throw new DataIntegrityException("Circular dependency detected: Entity cannot be its own ancestor.");
        }

        // Recursively delete sub-entities
        deleteSubDepartments(entityId);

        // Deleting entity closure entries
        deleteClosuresByAncestorId(entityId);
        deleteClosuresByDescendantId(entityId);

        // Deleting the entity itself
        entityRepository.delete(entity);
    }

    /**
     * Checks if deleting the entity would result in a circular dependency.
     *
     * @param id the ID of the entity to check
     * @return true if deleting the entity would create a circular dependency, false otherwise
     */
    private boolean hasCircularDependencyToDelete(Long id, Long newParentId) {
        if (id.equals(newParentId)) {
            return true;
        }

        List<C> descendantClosures = findClosureAncestors(newParentId);
        return descendantClosures.stream()
                .anyMatch(dc -> dc.getAncestorId().equals(id));
    }

    /**
     * Deletes all sub-entities and their closure entries recursively.
     *
     * @param parentId the ID of the entity whose sub-entities are to be deleted
     */
    @Transactional
    public void deleteSubDepartments(Long parentId) throws EntityNotFoundException, DataIntegrityException {
        List<C> subDepartments = findClosureDescendants(parentId);

        // Sort sub-entities by level in descending order to delete leaf nodes first
        subDepartments.sort(Comparator.comparingInt(C::getLevel).reversed());

        for (C closure : subDepartments) {
            if (!closure.getDescendantId().equals(parentId)) {
                deleteEntity(closure.getDescendantId());
            }
        }
    }

    /**
     * Deletes all entity closure records for a given descendant ID.
     *
     * @param ancestorId the ID of the descendant entity to delete closure records for
     */
    public void deleteClosuresByAncestorId(Long ancestorId) {
        QHierarchyBaseEntityClosure entityClosure = QHierarchyBaseEntityClosure.hierarchyBaseEntityClosure;

        // Deleting all entity closure records where the given ancestorId matches
        jpaQueryFactory
                .delete(entityClosure)
                .where(entityClosure.ancestorId.eq(ancestorId)
                        .and(entityClosure.instanceOf(entityClosureClass)))
                .execute();
    }

    /**
     * Retrieves all entities.
     *
     * @return a list of all entities as response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAllEntities() {
        return getAllEntities(true);
    }

    /**
     * Retrieves all entities with an option to fetch sub-entities.
     *
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of all entities as response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAllEntities(boolean fetchSubEntities) {
        // Retrieve all entities from the repository
        List<E> entities = entityRepository.findAll();

        log.info("Retrieved {} entities.", entities.size());

        // Map each entity to a response DTO
        return entityMapper.toDtoList(entities, fetchSubEntities);
    }

    /**
     * Retrieves all entities with an option to fetch sub-entities and apply pagination.
     *
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of entities as DTOs
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
     * Retrieves sub-entities (children) of a given parent entity.
     *
     * @param parentId the ID of the parent entity
     * @return a list of sub-entities as response DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getSubEntities(Long parentId) throws ParentEntityNotFoundException {
        return getSubEntities(parentId, true);
    }

    /**
     * Retrieves sub-entities (children) of a given parent entity with an option to fetch sub-entities.
     *
     * @param parentId         the ID of the parent entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of sub-entities as response DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getSubEntities(Long parentId, boolean fetchSubEntities) throws ParentEntityNotFoundException {
        log.info("Retrieving sub-entities for parent entity with id: {}", parentId);

        // Retrieve the entity by ID, throw exception if not found
        E parentEntity = entityRepository.findById(parentId)
                .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with id: " + parentId));

        // Query sub-entities using QueryDSL
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        List<E> subDepartments = (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(entity.parentId.eq(parentId).and(entity.instanceOf(entityClass)))
                .fetch();

        // Map each entity to a response DTO and return the list
        return entityMapper.toDtoList(subDepartments, fetchSubEntities);
    }

    /**
     * Retrieves sub-entities (children) of a given parent entity with an option to fetch sub-entities and apply pagination.
     *
     * @param parentId         the ID of the parent entity
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of sub-entities as DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> getSubEntities(Long parentId, Pageable pageable, boolean fetchSubEntities) throws ParentEntityNotFoundException {
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
        BooleanExpression query = entity.parentId.eq(parentId).and(entity.instanceOf(entityClass));

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
     * Retrieves an entity by its ID.
     *
     * @param id the ID of the entity to retrieve
     * @return the entity with the specified ID as a response DTO
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public SD getEntityById(Long id) throws EntityNotFoundException {
        return getEntityById(id, true);
    }

    /**
     * Retrieves an entity by its ID with an option to fetch sub-entities.
     *
     * @param id               the ID of the entity to retrieve
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return the entity with the specified ID as a response DTO
     * @throws EntityNotFoundException if the entity with the given ID is not found
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
     * Searches entities by name.
     *
     * @param name entity name to search
     * @return list of entities matching the name
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> searchEntitiesByName(String name) {
        return searchEntitiesByName(name, true);
    }

    /**
     * Searches entities by name with an option to fetch sub-entities.
     *
     * @param name             entity name to search
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return list of entities matching the name
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
     * Searches entities by name with an option to fetch sub-entities and apply pagination.
     *
     * @param name             entity name to search
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of entities as DTOs
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
     * Retrieves the parent entity of a given entity.
     *
     * @param entityId the ID of the entity
     * @return the parent entity as a response DTO
     * @throws EntityNotFoundException       if the entity with the given ID is not found
     * @throws ParentEntityNotFoundException if the entity has no parent
     */
    @Override
    @Transactional(readOnly = true)
    public SD getParentEntity(Long entityId) throws EntityNotFoundException, ParentEntityNotFoundException {
        return getParentEntity(entityId, true);
    }

    /**
     * Retrieves the parent entity of a given entity with an option to fetch sub-entities.
     *
     * @param entityId         the ID of the entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return the parent entity as a response DTO
     * @throws EntityNotFoundException       if the entity with the given ID is not found
     * @throws ParentEntityNotFoundException if the entity has no parent
     */
    @Override
    @Transactional(readOnly = true)
    public SD getParentEntity(Long entityId, boolean fetchSubEntities) throws EntityNotFoundException, ParentEntityNotFoundException {
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
     * Retrieves all descendants (children, grandchildren, etc.) of a given entity.
     *
     * @param entityId the ID of the entity
     * @return a list of all descendants as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getDescendants(Long entityId) throws EntityNotFoundException {
        return getDescendants(entityId, true);
    }

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given entity with an option to fetch sub-entities.
     *
     * @param entityId         the ID of the entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of all descendants as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getDescendants(Long entityId, boolean fetchSubEntities) throws EntityNotFoundException {
        log.info("Fetching descendants for entity with id: {}", entityId);

        // Retrieve the entity by ID
        E foundEntity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Query all descendants using QueryDSL
        List<E> descendants = findClosureDescendants(entityId).stream()
                .map(C::getDescendantId)
                .filter(id -> !id.equals(entityId)) // Exclude self
                .map(id -> entityRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id)))
                .collect(Collectors.toList());

        // Map entities to HierarchyResponseDto
        return entityMapper.toDtoList(descendants, fetchSubEntities);
    }

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given entity with an option to fetch sub-entities
     * and apply pagination.
     *
     * @param entityId         the ID of the entity
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of descendants as DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> getDescendants(Long entityId, Pageable pageable, boolean fetchSubEntities) throws EntityNotFoundException {
        log.info("Fetching paginated descendants for entity with id: {}", entityId);

        // Retrieve the entity by ID
        E foundEntity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Query all descendants using QueryDSL
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;

        List<E> descendants = findClosureDescendants(entityId).stream()
                .map(C::getDescendantId)
                .filter(id -> !id.equals(entityId)) // Exclude self
                .map(id -> entityRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id)))
                .collect(Collectors.toList());;

        // Extract sorting criteria from pageable
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
     * Retrieves all ancestors (parent, grandparent, etc.) of a given entity.
     *
     * @param entityId the ID of the entity
     * @return a list of all ancestors as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAncestors(Long entityId) throws EntityNotFoundException {
        return getAncestors(entityId, true);
    }

    /**
     * Retrieves all ancestors (parent, grandparent, etc.) of a given entity with an option to fetch sub-entities.
     *
     * @param entityId         the ID of the entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of all ancestors as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAncestors(Long entityId, boolean fetchSubEntities) throws EntityNotFoundException {
        log.info("Fetching ancestors for entity with id: {}", entityId);

        // Retrieve the entity by ID
        E entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Query all ancestors from the repository
        List<E> ancestors = findClosureAncestors(entityId).stream()
                .filter(closure -> !closure.getAncestorId().equals(entityId))  // Exclude self-closure
                .map(closure -> entityRepository.findById(closure.getAncestorId())
                        .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found."))
                )  // Fetch Entity
                .collect(Collectors.toList());

        // Map entities to HierarchyResponseDto
        return entityMapper.toDtoList(ancestors, fetchSubEntities);
    }

    /**
     * Retrieves all ancestors (parent, grandparent, etc.) of a given entity with an option to fetch sub-entities
     * and apply pagination.
     *
     * @param entityId         the ID of the entity
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of ancestors as DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> getAncestors(Long entityId, Pageable pageable, boolean fetchSubEntities) throws EntityNotFoundException {
        log.info("Fetching paginated ancestors for entity with id: {}", entityId);

        // Retrieve the entity by ID
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        E foundEntity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + entityId));

        // Query all ancestors from the repository
        List<E> ancestors = findClosureAncestors(entityId).stream()
                .filter(closure -> !closure.getAncestorId().equals(entityId))  // Exclude self-closure
                .map(closure -> entityRepository.findById(closure.getAncestorId())
                        .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found."))
                )  // Fetch Entity
                .collect(Collectors.toList());

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
