package tn.engn.hierarchicalentityapi.service;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.mapstruct.ap.shaded.freemarker.template.utility.StringUtil.capitalize;

/**
 * Abstract service implementation for managing entities using the adjacency list model.
 *
 * @param <E>  the entity type managed by this service
 * @param <RD> the request DTO type used for creating or updating the entity
 * @param <SD> the response DTO type used for retrieving the entity
 */
@Service
@Slf4j // Lombok annotation for logging
public abstract class AdjacencyListEntityService<E extends HierarchyBaseEntity, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto>
        implements HierarchyService<E, RD, SD> {

    protected final HierarchyBaseRepository<E> entityRepository;
    protected final HierarchyMapper<E, RD, SD> entityMapper;
    protected final JPAQueryFactory jpaQueryFactory;

    protected final Class<E> entityClass;
    // For Testing
    @Setter
    @Value("${entity.max-name-length}")
    private int maxNameLength;

    /**
     * Constructs the service with necessary dependencies.
     *
     * @param entityRepository the repository for managing entities
     * @param entityMapper     the mapper for converting entities to DTOs and vice versa
     * @param jpaQueryFactory  the JPA query factory for executing dynamic queries
     * @param entityClass      the current entity class
     */
    public AdjacencyListEntityService(HierarchyBaseRepository<E> entityRepository,
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
     * @param requestDto the DTO containing the new entity's details
     * @return the created entity as a response DTO
     * @throws ParentEntityNotFoundException if the parent entity is not found
     * @throws ValidationException           if the input data is invalid
     * @throws DataIntegrityException        if creating the entity violates data integrity constraints
     */
    @Override
    @Transactional
    public SD createEntity(HierarchyRequestDto requestDto)
            throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        String name = requestDto.getName();
        Long parentId = requestDto.getParentEntityId();

        // Validate entity name
        validateEntityName(name);

        log.info("Creating entity with name: {} and parentId: {}", name, parentId);

        // Handle parent entity
        E parentEntity = null;
        if (parentId != null) {
            parentEntity = entityRepository.findById(parentId)
                    .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with id: " + parentId));
        }

        // Save the entity and return the response DTO
        E entity = (E) entityMapper.toEntity((RD) requestDto);
        entity.setParentEntity(parentEntity);

        if (parentEntity != null) {
            parentEntity.addSubEntity(entity);
        }

        entity.setEntityType(entityClass);
        E savedEntity = entityRepository.save(entity);
        log.info("Created Entity: {}", entity);
        SD responseDto = entityMapper.toDto(savedEntity);
        entityMapper.setSubEntities((SD) responseDto, entity); // Ensure subEntities are set correctly

        return responseDto;
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
        String name = updatedEntityDto.getName();
        Long parentId = updatedEntityDto.getParentEntityId();

        log.info("Updating entity with id: {}, name: {} and parentId: {}", entityId, name, parentId);

        // Validate entity name
        validateEntityName(name);

        // Retrieve the entity to update
        E entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + entityId));

        // Check if the entity's name needs updating
        if (!entity.getName().equals(name)) {
            entity.setName(name);
        }

        // Handle parent entity
        if (parentId != null) {
            E newParent = entityRepository.findById(parentId)
                    .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with id: " + parentId));

            // Check for circular dependency
            if (hasCircularDependency(entity, newParent)) {
                throw new DataIntegrityException("Circular dependency detected.");
            }

            // Avoid unnecessary updates
            if (entity.getParentId() == null || !entity.getParentId().equals(parentId)) {
                if (entity.getParentId() != null) {
                    entityRepository.findById(entity.getParentId())
                            .ifPresent(parent -> parent.removeSubEntity(entity));
                }

                // Update parent and add to new parent's sub-entities
                newParent.addSubEntity(entity);
            }
        } else {
            // Case where parent ID is null, meaning no parent should be assigned
            if (entity.getParentId() != null) {
                entityRepository.findById(entity.getParentId())
                        .ifPresent(parent -> parent.removeSubEntity(entity));
                entity.setParentEntity(null); // Clear parent entity
                entity.setParentId(null);
            }
        }

        // Save the updated entity and return the response DTO
        entityRepository.save(entity);

        SD responseDto = entityMapper.toDto(entity);
        entityMapper.setSubEntities(responseDto, entity); // Ensure subEntities are set correctly

        return responseDto;
    }

    /**
     * Validates the name of the entity.
     *
     * @param name the name to validate
     * @throws ValidationException if the name is empty, null, or exceeds the maximum length
     */
    protected void validateEntityName(String name) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Entity name must not be empty");
        }

        if (name.length() > maxNameLength) {
            throw new ValidationException("Entity name exceeds maximum allowed length of " + maxNameLength + " characters");
        }
    }

    /**
     * Checks for circular dependency when adding a new parent to an entity.
     *
     * @param entity    the entity being updated
     * @param newParent the new parent entity
     * @return true if a circular dependency is detected, false otherwise
     */
    private boolean hasCircularDependency(E entity, E newParent) {
        Set<E> visitedEntities = new HashSet<>();
        E current = newParent;

        while (current != null) {
            if (!visitedEntities.add(current)) {
                return true; // Circular dependency detected
            }

            if (current.getId().equals(entity.getId())) {
                return true; // Circular dependency detected
            }

            current = current.getParentId() != null ? entityRepository.findById(current.getParentId()).orElseThrow() : null;
        }

        return false; // No circular dependency detected
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id the ID of the entity to delete
     * @throws EntityNotFoundException if the entity with the given ID is not found
     * @throws DataIntegrityException  if deletion would result in a circular dependency or if other constraints are violated
     */
    @Override
    @Transactional
    public void deleteEntity(Long id) throws EntityNotFoundException, DataIntegrityException {
        log.info("Deleting entity with id: {}", id);

        E entity = entityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id));

        // Check for circular dependency before deleting the entity
        if (hasCircularDependencyOnDelete(entity)) {
            throw new DataIntegrityException("Deleting this entity would create a circular dependency.");
        }

        // Handle sub-entities before deleting the entity
        if (!entity.getSubEntities().isEmpty()) {
            handleSubEntitiesOnDelete(entity);
        }

        // Remove the entity from its parent's sub-entities if it has a parent
        if (entity.getParentId() != null) {
            entityRepository.findById(entity.getParentId())
                    .ifPresent(parent -> parent.removeSubEntity(entity));
        }

        // Delete the entity
        entityRepository.delete(entity);
    }

    /**
     * Checks for circular dependency when deleting an entity.
     *
     * @param entity the entity being deleted
     * @return true if a circular dependency is detected, false otherwise
     */
    private boolean hasCircularDependencyOnDelete(E entity) {
        Set<E> visitedEntities = new HashSet<>();
        return checkSubEntitiesForCircularDependency(entity, visitedEntities);
    }

    /**
     * Recursively checks for circular dependency in sub-entities.
     *
     * @param entity          the current entity being checked
     * @param visitedEntities the set of visited entities to detect cycles
     * @return true if a circular dependency is detected, false otherwise
     */
    private boolean checkSubEntitiesForCircularDependency(E entity, Set<E> visitedEntities) {
        if (visitedEntities.contains(entity)) {
            return true; // Circular dependency detected
        }

        visitedEntities.add(entity);

        if (entity.getSubEntities() != null) {
            for (Object subEntity : entity.getSubEntities()) {
                if (checkSubEntitiesForCircularDependency((E) subEntity, visitedEntities)) {
                    return true; // Circular dependency detected
                }
            }
        }

        return false; // No circular dependency detected
    }

    /**
     * Handles sub-entities when deleting a parent entity.
     *
     * @param parentEntity the parent entity being deleted
     */
    protected void handleSubEntitiesOnDelete(E parentEntity) {
        if (parentEntity.getSubEntities() != null) {
            for (Object subEntity : parentEntity.getSubEntities()) {
                ((E) subEntity).setParentId(null); // Set sub-entity's parent ID to null
                ((E) subEntity).setParentEntity(null); // Set sub-entity's parent ID to null
                entityRepository.save((E) subEntity); // Save the updated sub-entity
            }
        }
    }

    /**
     * Retrieves all entities without fetching sub-entities.
     *
     * @return List of all entities as response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAllEntities() {
        return getAllEntities(true);
    }

    /**
     * Retrieves all entities with an option to fetch sub-entities.
     *
     * @param fetchSubEntities Flag to indicate whether to fetch sub-entities
     * @return List of all entities as response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<SD> getAllEntities(boolean fetchSubEntities) {
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;

        // Construct QueryDSL query to fetch all entities
        List<E> entities = (List<E>) jpaQueryFactory.selectFrom(entity)
                .where(entity.instanceOf(entityClass))
                .fetch();

        // Map fetched entities to response DTOs using the abstract mapper
        return entityMapper.toDtoList(entities, fetchSubEntities);
    }

    /**
     * Retrieves all entities with pagination, sorting, and an option to fetch sub-entities.
     *
     * @param pageable         Pageable information for pagination and sorting
     * @param fetchSubEntities Flag to indicate whether to fetch sub-entities
     * @return Paginated response of entities as DTOs
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

        // Find the parent entity or throw an exception if not found
        E parentEntity = entityRepository.findById(parentId)
                .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with id: " + parentId));

        // Initialize sub-entities if they are not already initialized
        if (!Hibernate.isInitialized(parentEntity.getSubEntities())) {
            Hibernate.initialize(parentEntity.getSubEntities());
        }

        // Ensure sub-entities are not null
        if (parentEntity.getSubEntities() == null) {
            parentEntity.setSubEntities(new ArrayList<>());
        }

        // Convert the sub-entities to DTOs
        return entityMapper.toDtoList((List<E>) parentEntity.getSubEntities(), fetchSubEntities);
    }

    /**
     * Retrieves sub-entities (children) of a given parent entity with an option to fetch sub-entities,
     * apply pagination, and sort.
     *
     * @param parentId         the ID of the parent entity
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of sub-entities as DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponseDto<SD> getSubEntities(Long parentId, Pageable pageable, boolean fetchSubEntities)
            throws ParentEntityNotFoundException {
        log.info("Retrieving paginated sub-entities for parent entity with id: {}", parentId);

        // Find the parent entity or throw an exception if not found
        E parentEntity = entityRepository.findById(parentId)
                .orElseThrow(() -> new ParentEntityNotFoundException("Parent entity not found with id: " + parentId));

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
        BooleanExpression query = entity.parentId.eq(parentId)
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

        // Find the entity or throw an exception if not found
        E entity = entityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found with id: " + id));

        // Convert the entity and its sub-entities to a response DTO
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
     * Searches entities by name with an option to fetch sub-entities, apply pagination, and sort.
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

        // Retrieve the entity by ID
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        E foundEntity = (E) jpaQueryFactory.selectFrom(entity)
                .where(entity.id.eq(entityId).and(entity.instanceOf(entityClass)))
                .fetchOne();

        if (foundEntity == null) {
            throw new EntityNotFoundException("Entity not found with id: " + entityId);
        }

        // Retrieve the parent entity
        E parentEntity = (E) foundEntity.getParentEntity();

        if (parentEntity == null) {
            throw new ParentEntityNotFoundException("Parent entity not found for entity with id: " + entityId);
        }

        // Convert the parent entity to a response DTO and return
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
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        E foundEntity = (E) jpaQueryFactory.selectFrom(entity)
                .where(entity.id.eq(entityId).and(entity.instanceOf(entityClass)))
                .fetchOne();

        if (foundEntity == null) {
            throw new EntityNotFoundException("Entity not found with id: " + entityId);
        }

        // Fetch all descendants within the same transaction
        List<E> descendants = getAllDescendants(foundEntity);

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
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        E foundEntity = (E) jpaQueryFactory.selectFrom(entity)
                .where(entity.id.eq(entityId).and(entity.instanceOf(entityClass)))
                .fetchOne();

        if (foundEntity == null) {
            throw new EntityNotFoundException("Entity not found with id: " + entityId);
        }

        // Fetch all descendants within the same transaction
        List<E> descendants = getAllDescendants(foundEntity);

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
     * Helper method to recursively fetch all descendants of an entity.
     *
     * @param entity the entity whose descendants are to be fetched
     * @return a list of all descendants
     */
    protected List<E> getAllDescendants(E entity) {
        List<E> descendants = new ArrayList<>();
        List<E> subEntities = entity.getSubEntities();

        // Check if subEntities is null or empty to avoid NPE
        if (subEntities != null && !subEntities.isEmpty()) {
            descendants.addAll(subEntities);
            for (E subEntity : subEntities) {
                descendants.addAll(getAllDescendants(subEntity));
            }
        }

        return descendants;
    }

    /**
     * Retrieves all ancestors (parent entities recursively) of a given entity.
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
        QHierarchyBaseEntity entity = QHierarchyBaseEntity.hierarchyBaseEntity;
        E foundEntity = (E) jpaQueryFactory.selectFrom(entity)
                .where(entity.id.eq(entityId).and(entity.instanceOf(entityClass)))
                .fetchOne();

        if (foundEntity == null) {
            throw new EntityNotFoundException("Entity not found with id: " + entityId);
        }

        // Fetch all ancestors within the same transaction
        List<E> ancestors = getAllAncestors(foundEntity);

        // Convert ancestors to response DTOs and return
        return ancestors.isEmpty() ? new ArrayList<>() : entityMapper.toDtoList(ancestors, fetchSubEntities);
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

        // Fetch all ancestors within the same transaction
        List<E> ancestors = getAllAncestors(foundEntity);

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


    /**
     * Helper method to recursively fetch all ancestors of an entity.
     *
     * @param entity the entity whose ancestors are to be fetched
     * @return a list of all ancestors
     */
    private List<E> getAllAncestors(E entity) {
        List<E> ancestors = new ArrayList<>();
        E parentEntity = (E) entity.getParentEntity();

        // Check if parentEntity is null to avoid NPE
        if (parentEntity != null) {
            ancestors.add(parentEntity);
            ancestors.addAll(getAllAncestors(parentEntity));
        }
        return ancestors;
    }
}
