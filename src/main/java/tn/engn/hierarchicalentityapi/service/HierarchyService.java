package tn.engn.hierarchicalentityapi.service;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.DataIntegrityException;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ParentEntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.util.List;

/**
 * Interface for managing hierarchical entities.
 *
 * @param <E>  the type of hierarchical base entity
 * @param <RD> the type of hierarchy request DTO
 * @param <SD> the type of hierarchy response DTO
 */
public interface HierarchyService<E extends HierarchyBaseEntity, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto> {

    /**
     * Creates a new entity based on the provided DTO.
     *
     * @param requestDto the DTO containing the new entity's details
     * @return the created entity as a response DTO
     * @throws ParentEntityNotFoundException if the parent entity is not found
     * @throws ValidationException           if the input data is invalid
     * @throws DataIntegrityException        if creating the entity violates data integrity constraints
     */
    SD createEntity(RD requestDto)
            throws ParentEntityNotFoundException, ValidationException, DataIntegrityException;

    /**
     * Updates an existing entity's name and optionally changes its parent entity.
     *
     * @param entityId        the ID of the entity to update
     * @param updatedEntityDto the DTO containing updated entity information
     * @return the updated entity DTO
     * @throws EntityNotFoundException       if the entity with the given ID is not found
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     * @throws DataIntegrityException        if updating the entity violates data integrity constraints
     * @throws ValidationException           if the updated entity name is invalid (empty, null, or exceeds max length)
     */
    @Transactional
    SD updateEntity(Long entityId, RD updatedEntityDto)
            throws EntityNotFoundException, ParentEntityNotFoundException, DataIntegrityException, ValidationException;

    /**
     * Deletes an entity by its ID.
     *
     * @param id the ID of the entity to delete
     * @throws EntityNotFoundException if the entity with the given ID is not found
     * @throws DataIntegrityException  if deletion would result in a circular dependency or if other constraints are violated
     */
    @Transactional
    void deleteEntity(Long id) throws EntityNotFoundException, DataIntegrityException;

    /**
     * Retrieves all entities.
     *
     * @return a list of all entities as response DTOs
     */
    @Transactional(readOnly = true)
    List<SD> getAllEntities();

    /**
     * Retrieves all entities with an option to fetch sub-entities.
     *
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of all entities as response DTOs
     */
    @Transactional(readOnly = true)
    List<SD> getAllEntities(boolean fetchSubEntities);

    /**
     * Retrieves all entities with an option to fetch sub-entities and apply pagination.
     *
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of entities as DTOs
     */
    @Transactional(readOnly = true)
    PaginatedResponseDto<SD> getAllEntities(Pageable pageable, boolean fetchSubEntities);

    /**
     * Retrieves sub-entities (children) of a given parent entity.
     *
     * @param parentId the ID of the parent entity
     * @return a list of sub-entities as response DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<SD> getSubEntities(Long parentId) throws ParentEntityNotFoundException;

    /**
     * Retrieves sub-entities (children) of a given parent entity with an option to fetch sub-entities.
     *
     * @param parentId         the ID of the parent entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of sub-entities as response DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<SD> getSubEntities(Long parentId, boolean fetchSubEntities) throws ParentEntityNotFoundException;

    /**
     * Retrieves sub-entities (children) of a given parent entity with an option to fetch sub-entities and apply pagination.
     *
     * @param parentId         the ID of the parent entity
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of sub-entities as DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    PaginatedResponseDto<SD> getSubEntities(Long parentId, Pageable pageable, boolean fetchSubEntities) throws ParentEntityNotFoundException;

    /**
     * Retrieves an entity by its ID.
     *
     * @param id the ID of the entity to retrieve
     * @return the entity with the specified ID as a response DTO
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    SD getEntityById(Long id) throws EntityNotFoundException;

    /**
     * Retrieves an entity by its ID with an option to fetch sub-entities.
     *
     * @param id               the ID of the entity to retrieve
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return the entity with the specified ID as a response DTO
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    SD getEntityById(Long id, boolean fetchSubEntities) throws EntityNotFoundException;

    /**
     * Searches entities by name.
     *
     * @param name entity name to search
     * @return list of entities matching the name
     */
    @Transactional(readOnly = true)
    List<SD> searchEntitiesByName(String name);

    /**
     * Searches entities by name with an option to fetch sub-entities.
     *
     * @param name             entity name to search
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return list of entities matching the name
     */
    @Transactional(readOnly = true)
    List<SD> searchEntitiesByName(String name, boolean fetchSubEntities);

    /**
     * Searches entities by name with an option to fetch sub-entities and apply pagination.
     *
     * @param name             entity name to search
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of entities as DTOs
     */
    @Transactional(readOnly = true)
    PaginatedResponseDto<SD> searchEntitiesByName(String name, Pageable pageable, boolean fetchSubEntities);

    /**
     * Retrieves the parent entity of a given entity.
     *
     * @param entityId the ID of the entity
     * @return the parent entity as a response DTO
     * @throws EntityNotFoundException       if the entity with the given ID is not found
     * @throws ParentEntityNotFoundException if the entity has no parent
     */
    @Transactional(readOnly = true)
    SD getParentEntity(Long entityId) throws EntityNotFoundException, ParentEntityNotFoundException;

    /**
     * Retrieves the parent entity of a given entity with an option to fetch sub-entities.
     *
     * @param entityId         the ID of the entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return the parent entity as a response DTO
     * @throws EntityNotFoundException       if the entity with the given ID is not found
     * @throws ParentEntityNotFoundException if the entity has no parent
     */
    @Transactional(readOnly = true)
    SD getParentEntity(Long entityId, boolean fetchSubEntities) throws EntityNotFoundException, ParentEntityNotFoundException;

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given entity.
     *
     * @param entityId the ID of the entity
     * @return a list of all descendants as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<SD> getDescendants(Long entityId) throws EntityNotFoundException;

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given entity with an option to fetch sub-entities.
     *
     * @param entityId         the ID of the entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of all descendants as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<SD> getDescendants(Long entityId, boolean fetchSubEntities) throws EntityNotFoundException;

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
    @Transactional(readOnly = true)
    PaginatedResponseDto<SD> getDescendants(Long entityId, Pageable pageable, boolean fetchSubEntities) throws EntityNotFoundException;

    /**
     * Retrieves all ancestors (parent, grandparent, etc.) of a given entity.
     *
     * @param entityId the ID of the entity
     * @return a list of all ancestors as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<SD> getAncestors(Long entityId) throws EntityNotFoundException;

    /**
     * Retrieves all ancestors (parent, grandparent, etc.) of a given entity with an option to fetch sub-entities.
     *
     * @param entityId         the ID of the entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of all ancestors as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<SD> getAncestors(Long entityId, boolean fetchSubEntities) throws EntityNotFoundException;

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
    @Transactional(readOnly = true)
    PaginatedResponseDto<SD> getAncestors(Long entityId, Pageable pageable, boolean fetchSubEntities) throws EntityNotFoundException;

}