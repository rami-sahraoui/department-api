package tn.engn.hierarchicalentityapi.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.engn.hierarchicalentityapi.annotation.SubEntitiesPath;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.DataIntegrityException;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ParentEntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.service.HierarchyService;

import java.util.List;

/**
 * Generic REST controller for managing hierarchical entities with a dynamic version and plural name in the API path.
 */
@RequiredArgsConstructor
@Getter
@Setter
@Slf4j
public class HierarchyController<E extends HierarchyBaseEntity, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto> {

    private final HierarchyService<E, RD, SD> hierarchyService;

    /**
     * The dynamic plural name used in the API path.
     * Resolved based on the {@code @PluralName} annotation value.
     */
    private String subEntitiesPath;

    /**
     * Initializes the controller after construction.
     * Resolves the {@code parentPluralName} and {@code childPluralName} dynamically based on the {@code @PluralNames} annotation values.
     * Throws {@code IllegalArgumentException} if the controller class is not annotated with {@code @PluralNames}.
     */
    @PostConstruct
    private void init() {
        SubEntitiesPath path = getClass().getAnnotation(SubEntitiesPath.class);

        if (path != null) {
            subEntitiesPath = path.value();
        } else {
            subEntitiesPath = "sub-entities";
        }

        log.info("Current sub-entities Path: {}", subEntitiesPath);
    }

    /**
     * Creates a new entity.
     *
     * @param requestDto Request body containing details of the new entity.
     * @return ResponseEntity with the created entity and HTTP status 201 (Created).
     * @throws ValidationException           if validation fails.
     * @throws ParentEntityNotFoundException if parent entity is not found.
     */
    @PostMapping
    @Operation(summary = "Create a new entity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Entity created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request format or validation error"),
            @ApiResponse(responseCode = "404", description = "Parent entity not found")
    })
    public ResponseEntity<SD> createEntity(@Valid @RequestBody RD requestDto) {
        SD createdEntity = hierarchyService.createEntity(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEntity);
    }

    /**
     * Updates an existing entity.
     *
     * @param id         Entity ID to update.
     * @param requestDto Request body containing updated details of the entity.
     * @return ResponseEntity with the updated entity and HTTP status 200 (OK).
     * @throws ValidationException           if validation fails.
     * @throws EntityNotFoundException       if entity is not found.
     * @throws ParentEntityNotFoundException if parent entity is not found.
     * @throws DataIntegrityException        if there is a data integrity issue preventing the update.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing entity by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request format or validation error"),
            @ApiResponse(responseCode = "404", description = "Entity or parent entity not found"),
            @ApiResponse(responseCode = "409", description = "Data integrity issue preventing update")
    })
    public ResponseEntity<SD> updateEntity(@PathVariable("id") Long id,
                                           @Valid @RequestBody RD requestDto) {
        SD updatedEntity = hierarchyService.updateEntity(id, requestDto);
        return ResponseEntity.ok(updatedEntity);
    }

    /**
     * Deletes an entity by ID.
     *
     * @param id Entity ID to delete.
     * @return ResponseEntity with HTTP status 204 (No Content) on successful deletion.
     * @throws EntityNotFoundException if entity is not found.
     * @throws DataIntegrityException  if there is a data integrity issue preventing the deletion.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes an entity by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Entity deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Entity not found"),
            @ApiResponse(responseCode = "409", description = "Data integrity issue preventing deletion")
    })
    public ResponseEntity<Void> deleteEntity(@PathVariable("id") Long id) {
        hierarchyService.deleteEntity(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all entities.
     *
     * @return ResponseEntity with a list of all entities and HTTP status 200 (OK).
     */
    @GetMapping
    @Operation(summary = "Retrieve all entities")
    @ApiResponse(responseCode = "200", description = "List of all entities retrieved successfully")
    public ResponseEntity<List<SD>> getAllEntities() {
        List<SD> entities = hierarchyService.getAllEntities();
        return ResponseEntity.ok(entities);
    }

    /**
     * Retrieves all entities with an option to fetch sub-entities.
     *
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return ResponseEntity with a list of all entities and HTTP status 200 (OK).
     */
    @GetMapping("/with-sub-entities")
    @Operation(summary = "Retrieve all entities with sub-entities",
            description = "Retrieves all entities with an option to fetch sub-entities.")
    @ApiResponse(responseCode = "200", description = "List of all entities retrieved successfully with optional sub-entities")
    public ResponseEntity<List<SD>> getAllEntitiesWithSubEntities(
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        List<SD> entities = hierarchyService.getAllEntities(fetchSubEntities);
        return ResponseEntity.ok(entities);
    }

    /**
     * Retrieves all entities with an option to fetch sub-entities and apply pagination.
     *
     * @param pageable pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return ResponseEntity with a paginated response of entities and HTTP status 200 (OK).
     */
    @GetMapping("/with-sub-entities/paginated")
    @Operation(summary = "Retrieve paginated entities with sub-entities",
            description = "Retrieves all entities with an option to fetch sub-entities and apply pagination.")
    @ApiResponse(responseCode = "200", description = "Paginated list of all entities retrieved successfully with optional sub-entities")
    public ResponseEntity<PaginatedResponseDto<SD>> getAllEntitiesPaginated(
            @ParameterObject Pageable pageable,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        PaginatedResponseDto<SD> paginatedEntities = hierarchyService.getAllEntities(pageable, fetchSubEntities);
        return ResponseEntity.ok(paginatedEntities);
    }

    /**
     * Retrieves an entity by ID.
     *
     * @param id Entity ID to retrieve.
     * @return ResponseEntity with the entity and HTTP status 200 (OK).
     * @throws EntityNotFoundException if entity is not found.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Retrieve entity by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<SD> getEntityById(@PathVariable("id") Long id) {
        SD entity = hierarchyService.getEntityById(id, true);
        return ResponseEntity.ok(entity);
    }

    /**
     * Retrieves an entity by its ID with an option to fetch sub-entities.
     *
     * @param id               the ID of the entity to retrieve
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return ResponseEntity with the entity and HTTP status 200 (OK).
     * @throws EntityNotFoundException if the entity with the given ID is not found.
     */
    @GetMapping("/{id}/with-sub-entities")
    @Operation(summary = "Retrieve entity by ID with sub-entities",
            description = "Retrieves an entity by its ID with an option to fetch sub-entities.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entity retrieved successfully with optional sub-entities"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<SD> getEntityByIdWithSubEntities(
            @PathVariable("id") Long id,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        SD entity = hierarchyService.getEntityById(id, fetchSubEntities);
        return ResponseEntity.ok(entity);
    }

    /**
     * Retrieves entities by name.
     *
     * @param name Entity name to search.
     * @return ResponseEntity with the list of entities and HTTP status 200 (OK).
     */
    @GetMapping("/search")
    @Operation(summary = "Search entities by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entities retrieved successfully"),
    })
    public ResponseEntity<List<SD>> searchEntitiesByName(@RequestParam String name) {
        List<SD> entities = hierarchyService.searchEntitiesByName(name);
        return ResponseEntity.ok(entities);
    }

    /**
     * Searches entities by name with an option to fetch sub-entities.
     *
     * @param name             entity name to search
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return list of entities matching the name
     */
    @GetMapping("/search/with-sub-entities")
    @Operation(summary = "Search entities by name with sub-entities",
            description = "Searches entities by name with an option to fetch sub-entities.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entities retrieved successfully with optional sub-entities"),
    })
    public ResponseEntity<List<SD>> searchEntitiesByNameWithSubEntities(
            @RequestParam String name,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        List<SD> entities = hierarchyService.searchEntitiesByName(name, fetchSubEntities);
        return ResponseEntity.ok(entities);
    }

    /**
     * Searches entities by name with an option to fetch sub-entities and apply pagination.
     *
     * @param name             entity name to search
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of entities as DTOs
     */
    @GetMapping("/search/with-sub-entities/paginated")
    @Operation(summary = "Search entities by name with pagination and sub-entities",
            description = "Searches entities by name with an option to fetch sub-entities and apply pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated response of entities retrieved successfully with optional sub-entities"),
    })
    public ResponseEntity<PaginatedResponseDto<SD>> searchEntitiesByNameWithPaginationAndSubEntities(
            @RequestParam String name,
            @ParameterObject Pageable pageable,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        PaginatedResponseDto<SD> paginatedResponse = hierarchyService.searchEntitiesByName(name, pageable, fetchSubEntities);
        return ResponseEntity.ok(paginatedResponse);
    }

    /**
     * Retrieves sub-entities (children) of an entity by parent ID.
     *
     * @param parentId Parent entity ID to retrieve sub-entities.
     * @return ResponseEntity with a list of sub-entities and HTTP status 200 (OK).
     * @throws ParentEntityNotFoundException if parent entity is not found.
     */
    @GetMapping("/{id}/{subEntitiesPath}")
    @Operation(summary = "Retrieve sub-entities by parent ID",
            description = "Retrieves sub-entities (children) of an entity by parent ID using a dynamic plural name in the API path.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-entities retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Parent entity not found")
    })
    public ResponseEntity<List<SD>> getSubEntities(@PathVariable("id") Long parentId) {
        List<SD> subEntities = hierarchyService.getSubEntities(parentId);
        return ResponseEntity.ok(subEntities);
    }

    /**
     * Retrieves sub-entities (children) of a given parent entity with an option to fetch sub-entities.
     *
     * @param parentId         the ID of the parent entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of sub-entities as response DTOs
     * @throws ParentEntityNotFoundException if the parent entity with the given ID is not found
     */
    @GetMapping("/{parentId}/sub-entities/with-sub-entities")
    @Operation(summary = "Retrieve sub-entities by parent ID with sub-entities",
            description = "Retrieves sub-entities (children) of a given parent entity with an option to fetch sub-entities.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-entities retrieved successfully with optional sub-entities"),
            @ApiResponse(responseCode = "404", description = "Parent entity not found")
    })
    public ResponseEntity<List<SD>> getSubEntitiesWithSubEntities(
            @PathVariable("parentId") Long parentId,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        List<SD> subEntities = hierarchyService.getSubEntities(parentId, fetchSubEntities);
        return ResponseEntity.ok(subEntities);
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
    @GetMapping("/{parentId}/sub-entities/with-sub-entities/paginated")
    @Operation(summary = "Retrieve sub-entities by parent ID with pagination and sub-entities",
            description = "Retrieves sub-entities (children) of a given parent entity with an option to fetch sub-entities and apply pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated response of sub-entities retrieved successfully with optional sub-entities"),
            @ApiResponse(responseCode = "404", description = "Parent entity not found")
    })
    public ResponseEntity<PaginatedResponseDto<SD>> getSubEntitiesWithPaginationAndSubEntities(
            @PathVariable("parentId") Long parentId,
            @ParameterObject Pageable pageable,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        PaginatedResponseDto<SD> paginatedResponse = hierarchyService.getSubEntities(parentId, pageable, fetchSubEntities);
        return ResponseEntity.ok(paginatedResponse);
    }

    /**
     * Retrieves the parent entity of an entity by its ID.
     *
     * @param entityId Entity ID to retrieve parent entity.
     * @return ResponseEntity with the parent entity and HTTP status 200 (OK).
     * @throws EntityNotFoundException       if entity is not found.
     * @throws ParentEntityNotFoundException if parent entity is not found.
     */
    @GetMapping("/{id}/parent")
    @Operation(summary = "Retrieve parent entity by entity ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parent entity retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Entity or parent entity not found")
    })
    public ResponseEntity<SD> getParentEntity(@PathVariable("id") Long entityId) {
        SD parentEntity = hierarchyService.getParentEntity(entityId);
        return ResponseEntity.ok(parentEntity);
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
    @GetMapping("/{entityId}/parent/with-sub-entities")
    @Operation(summary = "Retrieve parent entity by entity ID with sub-entities",
            description = "Retrieves the parent entity of a given entity with an option to fetch sub-entities.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parent entity retrieved successfully with optional sub-entities"),
            @ApiResponse(responseCode = "404", description = "Entity or parent entity not found")
    })
    public ResponseEntity<SD> getParentEntityWithSubEntities(
            @PathVariable("entityId") Long entityId,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        SD parentEntity = hierarchyService.getParentEntity(entityId, fetchSubEntities);
        return ResponseEntity.ok(parentEntity);
    }

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of an entity by its ID.
     *
     * @param entityId Entity ID to retrieve descendants.
     * @return ResponseEntity with a list of descendants and HTTP status 200 (OK).
     * @throws EntityNotFoundException if entity is not found.
     */
    @GetMapping("/{id}/descendants")
    @Operation(summary = "Retrieve all descendants of an entity by entity ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descendants retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<List<SD>> getDescendants(@PathVariable("id") Long entityId) {
        List<SD> descendants = hierarchyService.getDescendants(entityId);
        return ResponseEntity.ok(descendants);
    }

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given entity with an option to fetch sub-entities.
     *
     * @param entityId         the ID of the entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of all descendants as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @GetMapping("/{entityId}/descendants/with-sub-entities")
    @Operation(summary = "Retrieve all descendants of an entity by entity ID with sub-entities",
            description = "Retrieves all descendants (children, grandchildren, etc.) of a given entity with an option to fetch sub-entities.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descendants retrieved successfully with optional sub-entities"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<List<SD>> getDescendantsWithSubEntities(
            @PathVariable("entityId") Long entityId,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        List<SD> descendants = hierarchyService.getDescendants(entityId, fetchSubEntities);
        return ResponseEntity.ok(descendants);
    }

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given entity with an option to fetch sub-entities and apply pagination.
     *
     * @param entityId         the ID of the entity
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of descendants as DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @GetMapping("/{entityId}/descendants/with-sub-entities/paginated")
    @Operation(summary = "Retrieve all descendants of an entity by entity ID with sub-entities and pagination",
            description = "Retrieves all descendants (children, grandchildren, etc.) of a given entity with an option to fetch sub-entities and apply pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated descendants retrieved successfully with optional sub-entities"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<PaginatedResponseDto<SD>> getDescendantsWithSubEntitiesAndPagination(
            @PathVariable("entityId") Long entityId,
            @ParameterObject Pageable pageable,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        PaginatedResponseDto<SD> paginatedDescendants = hierarchyService.getDescendants(entityId, pageable, fetchSubEntities);
        return ResponseEntity.ok(paginatedDescendants);
    }

    /**
     * Retrieves all ancestors (parent entities recursively) of an entity by its ID.
     *
     * @param entityId Entity ID to retrieve ancestors.
     * @return ResponseEntity with a list of ancestors and HTTP status 200 (OK).
     * @throws EntityNotFoundException if entity is not found.
     */
    @GetMapping("/{id}/ancestors")
    @Operation(summary = "Retrieve all ancestors of an entity by entity ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ancestors retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<List<SD>> getAncestors(@PathVariable("id") Long entityId) {
        List<SD> ancestors = hierarchyService.getAncestors(entityId);
        return ResponseEntity.ok(ancestors);
    }

    /**
     * Retrieves all ancestors (parent entities recursively) of a given entity with an option to fetch sub-entities.
     *
     * @param entityId         the ID of the entity
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a list of all ancestors as response DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @GetMapping("/{entityId}/ancestors/with-sub-entities")
    @Operation(summary = "Retrieve all ancestors of an entity by entity ID with sub-entities",
            description = "Retrieves all ancestors (parent, grandparent, etc.) of a given entity with an option to fetch sub-entities.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ancestors retrieved successfully with optional sub-entities"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<List<SD>> getAncestorsWithSubEntities(
            @PathVariable("entityId") Long entityId,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        List<SD> ancestors = hierarchyService.getAncestors(entityId, fetchSubEntities);
        return ResponseEntity.ok(ancestors);
    }

    /**
     * Retrieves all ancestors (parent, grandparent, etc.) of a given entity with an option to fetch sub-entities and apply pagination.
     *
     * @param entityId         the ID of the entity
     * @param pageable         pageable information for pagination and sorting
     * @param fetchSubEntities flag to indicate whether to fetch sub-entities
     * @return a paginated response of ancestors as DTOs
     * @throws EntityNotFoundException if the entity with the given ID is not found
     */
    @GetMapping("/{entityId}/ancestors/with-sub-entities/paginated")
    @Operation(summary = "Retrieve all ancestors of an entity by entity ID with sub-entities and pagination",
            description = "Retrieves all ancestors (parent, grandparent, etc.) of a given entity with an option to fetch sub-entities and apply pagination.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated ancestors retrieved successfully with optional sub-entities"),
            @ApiResponse(responseCode = "404", description = "Entity not found")
    })
    public ResponseEntity<PaginatedResponseDto<SD>> getAncestorsWithSubEntitiesAndPagination(
            @PathVariable("entityId") Long entityId,
            @ParameterObject Pageable pageable,
            @RequestParam(name = "fetchSubEntities", defaultValue = "false") boolean fetchSubEntities) {
        PaginatedResponseDto<SD> paginatedAncestors = hierarchyService.getAncestors(entityId, pageable, fetchSubEntities);
        return ResponseEntity.ok(paginatedAncestors);
    }
}

