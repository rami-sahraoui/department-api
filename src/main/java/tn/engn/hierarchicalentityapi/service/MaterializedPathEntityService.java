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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mapstruct.ap.shaded.freemarker.template.utility.StringUtil.capitalize;

/**
 * Service class for managing hierarchical entities using a materialized path model.
 *
 * @param <E>  the type of the entity
 * @param <RD> the type of the request DTO
 * @param <SD> the type of the response DTO
 */
@Service
@Slf4j
public abstract class MaterializedPathEntityService<E extends HierarchyBaseEntity, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto>
        implements HierarchyService<E, RD, SD> {

    protected final HierarchyBaseRepository<E> entityRepository;
    protected final HierarchyMapper<E, RD, SD> entityMapper;
    @PersistenceContext
    protected EntityManager entityManager;
    protected final JPAQueryFactory jpaQueryFactory;

    @Setter
    @Value("${entity.max-name-length}")
    private int maxNameLength;

    Class<E> entityClass;

    /**
     * Constructor for MaterializedPathEntityService.
     *
     * @param entityRepository the repository for the entity
     * @param entityMapper     the mapper for converting between entities and DTOs
     * @param jpaQueryFactory  the JPA query factory for executing queries
     * @param entityClass      the class type of the entity
     */
    public MaterializedPathEntityService(HierarchyBaseRepository<E> entityRepository,
                                         HierarchyMapper<E, RD, SD> entityMapper,
                                         JPAQueryFactory jpaQueryFactory,
                                         Class<E> entityClass) {
        this.entityRepository = entityRepository;
        this.entityMapper = entityMapper;
        this.jpaQueryFactory = jpaQueryFactory;
        this.entityClass = entityClass;
    }

    /**
     * Creates a new entity based on the provided DTO.
     *
     * @param requestDto the request DTO containing the entity's details
     * @return the created entity's response DTO
     * @throws ParentEntityNotFoundException if the parent entity specified in the DTO is not found
     * @throws ValidationException           if the DTO data is invalid
     * @throws DataIntegrityException        if there are issues with the data integrity (e.g., duplicate names)
     */
    @Override
    public SD createEntity(RD requestDto)
            throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        String name = requestDto.getName();
        Long parentId = requestDto.getParentEntityId();

        // Validate the entity name
        validateEntityName(name);

        log.info("Creating entity with name: {} and parentId: {}", name, parentId);

        // Create a new entity from the request DTO
        E entity = entityMapper.toEntity(requestDto);

        // Determine the path based on the parent entity
        String path = determinePath(parentId);

        // Set the path
        entity.setPath(path);

        // Save the entity to generate its ID
        E savedDepartment = entityRepository.save(entity);

        // Update the path with the generated ID
        savedDepartment.setPath(path + savedDepartment.getId() + "/");

        // Resave the entity with the updated path
        E finalSavedDepartment = entityRepository.save(savedDepartment);

        // Convert the saved entity to a response DTO and return it
        return entityMapper.toDto(finalSavedDepartment);
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
     * Determines the path for the new entity based on its parent entity.
     *
     * @param parentDepartmentId The ID of the parent entity, if any.
     * @return The path for the new entity.
     * @throws ParentEntityNotFoundException If the specified parent entity does not exist.
     */
    private String determinePath(Long parentDepartmentId) throws ParentEntityNotFoundException {
        if (parentDepartmentId == null) {
            // If there's no parent, the entity is a root entity
            return "/";
        } else {
            // Find the parent entity
            E parent = entityRepository.findById(parentDepartmentId)
                    .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found"));

            // Return the path of the parent entity
            return parent.getPath();
        }
    }

    /**
     * Updates an existing entity’s details based on the provided DTO.
     *
     * @param entityId         the ID of the entity to be updated
     * @param updatedEntityDto the DTO containing updated details for the entity
     * @return the updated entity's response DTO
     * @throws EntityNotFoundException       if the entity with the specified ID is not found
     * @throws ParentEntityNotFoundException if the parent entity specified in the DTO is not found
     * @throws DataIntegrityException        if there are issues with the data integrity (circular reference)
     * @throws ValidationException           if the DTO data is invalid
     */
    @Override
    @Transactional
    public SD updateEntity(Long entityId, RD updatedEntityDto)
            throws EntityNotFoundException, ParentEntityNotFoundException, DataIntegrityException, ValidationException {
        // Extract updated entity information from DTO
        String name = updatedEntityDto.getName();
        Long parentId = updatedEntityDto.getParentEntityId();

        // Validate the entity name
        validateEntityName(name);

        log.info("Updating entity with name: {} and parentId: {}", name, parentId);

        // Retrieve the existing entity
        E existingEntity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Check if the entity's name needs updating
        if (!existingEntity.getName().equals(name)) {
            existingEntity.setName(name);
        }

        // Check if the parent entity needs updating
        Long currentParentId = existingEntity.getParentId();
        if (parentId == null && currentParentId != null || parentId != null && !parentId.equals(currentParentId)) {
            updateParentEntity(existingEntity, parentId);
        }

        // Save the updated entity
        E updatedEntity = entityRepository.save(existingEntity);

        // Return the updated entity as HierarchyResponseDto
        return entityMapper.toDto(updatedEntity);
    }

    /**
     * Updates the parent entity of a given entity, including its descendants' paths.
     *
     * @param entity      the entity to update
     * @param newParentId the ID of the new parent entity
     * @throws ParentEntityNotFoundException if the specified parent entity does not exist
     * @throws DataIntegrityException        if a circular reference would be created by the update
     */
    private void updateParentEntity(E entity, Long newParentId)
            throws ParentEntityNotFoundException, DataIntegrityException {

        // Determine the new path prefix based on the new parent entity ID
        String newParentPath = determinePath(newParentId);
        String newPath = newParentPath + entity.getId() + "/";

        // Retrieve all descendants of the entity
        String currentPath = entity.getPath();

        List<E> descendants = getDescendants(currentPath);

        // Check for circular references in descendant paths
        if (newParentId != null && hasCircularReference(newParentPath, descendants)) {
            throw new DataIntegrityException("Circular reference detected: "
                    + "Moving entity " + entity.getName() + " under entity with ID " + newParentId
                    + " would create a circular reference.");
        }

        // Update paths of all descendants
        updateDescendantsPaths(descendants, currentPath, newPath);

        // Update parent ID of the entity
        entity.setParentId(newParentId);
        entity.setPath(newPath);
    }

    /**
     * Checks if there is a circular reference by verifying if any descendant's path starts with the new path.
     *
     * @param newPath     the new path prefix to check for circular references
     * @param descendants the list of descendant entities to check
     * @return true if a circular reference is detected, false otherwise
     */
    private boolean hasCircularReference(String newPath, List<E> descendants) {
        return descendants.stream()
                .anyMatch(descendant -> descendant.getPath().startsWith(newPath));
    }

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given entity.
     *
     * @param path the path of the entity
     * @return a list of all descendants as Entity
     */
    @Transactional(readOnly = true)
    protected List<E> getDescendants(String path) throws EntityNotFoundException {
        // Query descendants using QueryDSL
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        return (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(entity.path.startsWith(path)
                        .and(entity.instanceOf(entityClass)))
                .fetch();

    }

    /**
     * Updates paths of all descendants of a entity to reflect a new path prefix.
     *
     * @param descendants the list of descendants to update
     * @param currentPath the current path prefix
     * @param newPath     the new path prefix
     */
    private void updateDescendantsPaths(List<E> descendants, String currentPath, String newPath) {
        // Update paths of all descendants
        descendants.forEach(descendant -> {
            String descendantPath = descendant.getPath();
            if (descendantPath.startsWith(currentPath)) {
                descendant.setPath(newPath + descendantPath.substring(currentPath.length()));
            }
        });
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param entityId the ID of the entity to be deleted
     * @throws EntityNotFoundException if the entity with the specified ID is not found
     * @throws DataIntegrityException  if there are issues with data integrity (e.g., trying to delete a parent entity with children)
     */
    @Override
    @Transactional
    public void deleteEntity(Long entityId) throws EntityNotFoundException, DataIntegrityException {
        log.info("Updating entity with id: {}", entityId);

        // Retrieve the entity by ID, throw exception if not found
        E entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with ID: " + entityId));

        // Retrieve all descendants
        List<E> descendants = findAllDescendants(entity.getPath());

        // Check for circular references
        checkForCircularReferences(entity, descendants);

        // Delete all descendants and the entity
        entityRepository.deleteAll(descendants);
        entityRepository.delete(entity);
    }

    /**
     * Finds all descendants of a entity using its path.
     *
     * @param path the path of the entity
     * @return the list of descendants
     */
    private List<E> findAllDescendants(String path) {
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        return (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(
                        entity.path.startsWith(path)
                                .and(entity.path.notEqualsIgnoreCase(path))
                                .and(entity.instanceOf(entityClass))
                )
                .fetch();
    }

    /**
     * Checks for circular references in the descendant paths.
     *
     * @param parent      the parent entity
     * @param descendants the list of descendants
     * @throws DataIntegrityException if a circular reference is detected
     */
    private void checkForCircularReferences(E parent, List<E> descendants) throws DataIntegrityException {
        for (E descendant : descendants) {
            String[] ids = descendant.getPath().split("/");
            Set<String> uniqueIds = new HashSet<>(Arrays.asList(ids));

            // If there are duplicate IDs, it indicates a circular reference
            if (uniqueIds.size() < ids.length) {
                throw new DataIntegrityException("Circular reference detected in entity path: " + descendant.getPath());
            }
        }
    }

    /**
     * Retrieves all entities.
     *
     * @return a list of all entities' response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAllEntities() {
        return getAllEntities(true);
    }

    /**
     * Retrieves all entities with an option to fetch their sub-entities.
     *
     * @param fetchSubEntities flag indicating whether to fetch sub-entities
     * @return a list of all entities' response DTOs
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
     * Retrieves all entities with pagination and an option to fetch their sub-entities.
     *
     * @param pageable         the pagination information
     * @param fetchSubEntities flag indicating whether to fetch sub-entities
     * @return a paginated response containing all entities' response DTOs
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
     * @return a list of sub-entities' response DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the specified ID is not found
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
     * @param fetchSubEntities flag indicating whether to fetch sub-entities
     * @return a list of sub-entities' response DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the specified ID is not found
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
     * Retrieves sub-entities (children) of a given parent entity with pagination and an option to fetch sub-entities.
     *
     * @param parentId         the ID of the parent entity
     * @param pageable         the pagination information
     * @param fetchSubEntities flag indicating whether to fetch sub-entities
     * @return a paginated response containing sub-entities' response DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the specified ID is not found
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
     * @param id the ID of the entity to be retrieved
     * @return the response DTO of the entity with the specified ID
     * @throws EntityNotFoundException if no entity with the specified ID is found
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
     * Searches for entities by their name.
     *
     * @param name the name of the entities to be searched
     * @return a list of response DTOs for entities with names matching the search term
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
     * @param id the ID of the entity whose parent is to be retrieved
     * @return the response DTO of the parent entity
     * @throws EntityNotFoundException       if the entity with the specified ID is not found
     * @throws ParentEntityNotFoundException if the parent entity does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public SD getParentEntity(Long id) throws EntityNotFoundException, ParentEntityNotFoundException {
        return getParentEntity(id, true);
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
     * Retrieves the descendants of a given entity.
     *
     * @param entityId the ID of the entity whose descendants are to be retrieved
     * @return a list of response DTOs for the descendants of the entity
     * @throws EntityNotFoundException if the entity with the specified ID is not found
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
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        List<E> descendants = (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(entity.path.startsWith(foundEntity.getPath())
                        .and(entity.path.notEqualsIgnoreCase(foundEntity.getPath()))
                        .and(entity.instanceOf(entityClass)))
                .fetch();

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

        List<E> descendants = (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(entity.path.startsWith(foundEntity.getPath())
                        .and(entity.path.notEqualsIgnoreCase(foundEntity.getPath()))
                        .and(entity.instanceOf(entityClass)))
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
     * Retrieves the ancestors of a given entity.
     *
     * @param entityId the ID of the entity whose ancestors are to be retrieved
     * @return a list of response DTOs for the ancestors of the entity
     * @throws EntityNotFoundException if the entity with the specified ID is not found
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

        // Find the ancestors
        String[] pathSegments = entity.getPath().split("/");
        List<Long> ancestorIds = Arrays.stream(pathSegments)
                .filter(segment -> !segment.isEmpty())
                .map(Long::valueOf)
                .filter(id -> (long)id != entityId)
                .collect(Collectors.toList());

        // Query all ancestors from the repository
        List<E> ancestors = entityRepository.findAllById(ancestorIds);

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

        // Find the ancestors
        String[] pathSegments = foundEntity.getPath().split("/");
        List<Long> ancestorIds = Arrays.stream(pathSegments)
                .filter(segment -> !segment.isEmpty())
                .map(Long::valueOf)
                .filter(id -> (long)id != entityId)
                .collect(Collectors.toList());

        // Query all ancestors from the repository
        List<E> ancestors = entityRepository.findAllById(ancestorIds);

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
