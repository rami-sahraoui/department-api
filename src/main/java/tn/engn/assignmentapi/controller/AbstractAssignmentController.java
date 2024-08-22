package tn.engn.assignmentapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.engn.assignmentapi.dto.*;
import tn.engn.assignmentapi.model.AssignableEntity;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.assignmentapi.service.BaseAssignmentService;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.ErrorResponse;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.util.List;

/**
 * Abstract controller for managing assignments between hierarchical entities and assignable entities.
 * This controller provides endpoints for assigning entities, removing assignments, bulk operations,
 * and retrieving assignments, both as individual entities and in paginated forms.
 *
 * @param <A> the type of the assignment entity
 * @param <HE> the type of the hierarchical entity
 * @param <AE> the type of the assignable entity
 * @param <R> the type of the assignable entity request DTO
 * @param <D> the type of the assignable entity response DTO
 * @param <HR> the type of the hierarchical entity request DTO
 * @param <H> the type of the hierarchical entity response DTO
 */

@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public abstract class AbstractAssignmentController<A extends Assignment<HE,AE>, HE extends HierarchyBaseEntity<HE>, AE extends AssignableEntity<AE>, R extends AssignableEntityRequestDto, D extends AssignableEntityResponseDto,HR extends HierarchyRequestDto, H extends HierarchyResponseDto> {

    private final BaseAssignmentService<A, HE, AE, R, D, HR, H> assignmentService;

    /**
     * Assigns an assignable entity to a hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId the ID of the assignable entity
     * @param metadata the metadata associated with the assignment
     * @return the response containing details of the assignment
     */
    @PostMapping("/assign")
    @Operation(summary = "Assign an entity to a hierarchical entity",
            description = "Associates an assignable entity with a hierarchical entity, along with optional metadata.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment created successfully", content = @Content(schema = @Schema(implementation = AssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity or assignable entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssignmentResponseDto<H, D>> assignEntityToHierarchicalEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity", required = true) Long hierarchicalEntityId,
            @RequestParam @Parameter(description = "ID of the assignable entity", required = true) Long assignableEntityId,
            @RequestBody @Parameter(description = "Metadata associated with the assignment", required = true) List<AssignmentMetadataRequestDto> metadata) {

        AssignmentResponseDto<H, D> response = assignmentService.assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadata);
        return ResponseEntity.ok(response);
    }

    /**
     * Removes an assignable entity from a hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId the ID of the assignable entity
     * @return the response containing details of the removed assignment
     */
    @DeleteMapping("/remove")
    @Operation(summary = "Remove an entity from a hierarchical entity",
            description = "Disassociates an assignable entity from a hierarchical entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assignment removed successfully", content = @Content(schema = @Schema(implementation = AssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity or assignable entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssignmentResponseDto<H, D>> removeEntityFromHierarchicalEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity", required = true) Long hierarchicalEntityId,
            @RequestParam @Parameter(description = "ID of the assignable entity", required = true) Long assignableEntityId) {

        AssignmentResponseDto<H, D> response = assignmentService.removeEntityFromHierarchicalEntity(hierarchicalEntityId, assignableEntityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk assigns multiple assignable entities to a single hierarchical entity.
     *
     * @param request the bulk assignment request containing assignable entity IDs and metadata
     * @return the response containing details of the bulk assignments
     */
    @PostMapping("/bulk-assign-to-hierarchical-entity")
    @Operation(summary = "Bulk assign assignable entities to a hierarchical entity",
            description = "Associates multiple assignable entities with a hierarchical entity in a single operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk assignment completed successfully", content = @Content(schema = @Schema(implementation = BulkAssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity or assignable entities not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BulkAssignmentResponseDto<H, D>> bulkAssignAssignableEntitiesToHierarchicalEntity(
            @RequestBody @Parameter(description = "Bulk assignment request containing entity IDs and metadata", required = true) @Valid BulkAssignmentToHierarchicalEntityRequestDto request) {

        Long hierarchicalEntityId = request.getHierarchicalEntityId();
        List<Long> assignableEntityIds = request.getAssignableEntityIds();
        List<AssignmentMetadataRequestDto> metadata = request.getMetadata();

        BulkAssignmentResponseDto<H, D> response = assignmentService.bulkAssignAssignableEntitiesToHierarchicalEntity(hierarchicalEntityId, assignableEntityIds, metadata);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk removes multiple assignable entities from a single hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityIds  the IDs of the assignable entities to be removed
     * @return the response containing details of the bulk removals
     */
    @DeleteMapping("/bulk-remove-from-hierarchical-entity")
    @Operation(summary = "Bulk remove assignable entities from a hierarchical entity",
            description = "Disassociates multiple assignable entities from a hierarchical entity in a single operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk removal completed successfully", content = @Content(schema = @Schema(implementation = BulkAssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity or assignable entities not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BulkAssignmentResponseDto<H, D>> bulkRemoveAssignableEntitiesFromHierarchicalEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity", required = true) Long hierarchicalEntityId,
            @RequestBody @Parameter(description = "List of assignable entity IDs to be removed", required = true) List<Long> assignableEntityIds) {

        BulkAssignmentResponseDto<H, D> response = assignmentService.bulkRemoveAssignableEntitiesFromHierarchicalEntity(hierarchicalEntityId, assignableEntityIds);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk assigns multiple hierarchical entities to a single assignable entity.
     *
     * @param request the bulk assignment request containing hierarchical entity IDs and metadata
     * @return a BulkAssignmentResponseDto containing the assignable entity and the list of hierarchical entities with metadata
     */
    @PostMapping("/bulk-assign-to-assignable-entity")
    @Operation(summary = "Bulk assign hierarchical entities to an assignable entity",
            description = "Associates multiple hierarchical entities with a single assignable entity in a single operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk assignment completed successfully", content = @Content(schema = @Schema(implementation = BulkAssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignable entity or hierarchical entities not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BulkAssignmentResponseDto<H, D>> bulkAssignHierarchicalEntitiesToAssignableEntity(
            @RequestBody @Parameter(description = "Bulk assignment request containing hierarchical entity IDs and metadata", required = true) @Valid BulkAssignmentToAssignableEntityRequestDto request) {

        BulkAssignmentResponseDto<H, D> response = assignmentService.bulkAssignHierarchicalEntitiesToAssignableEntity(
                request.getAssignableEntityId(),
                request.getHierarchicalEntityIds(),
                request.getMetadata()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk removes multiple hierarchical entities from a single assignable entity.
     *
     * @param assignableEntityId     the ID of the assignable entity
     * @param hierarchicalEntityIds a list of IDs of the hierarchical entities to be removed
     * @return a BulkAssignmentResponseDto containing the assignable entity and the list of hierarchical entities
     */
    @DeleteMapping("/bulk-remove-from-assignable-entity")
    @Operation(summary = "Bulk remove hierarchical entities from an assignable entity",
            description = "Disassociates multiple hierarchical entities from a single assignable entity in a single operation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk removal completed successfully", content = @Content(schema = @Schema(implementation = BulkAssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignable entity or hierarchical entities not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<BulkAssignmentResponseDto<H, D>> bulkRemoveHierarchicalEntitiesFromAssignableEntity(
            @RequestParam @Parameter(description = "ID of the assignable entity", required = true) Long assignableEntityId,
            @RequestBody @Parameter(description = "List of hierarchical entity IDs to be removed", required = true) List<Long> hierarchicalEntityIds) {

        BulkAssignmentResponseDto<H, D> response = assignmentService.bulkRemoveHierarchicalEntitiesFromAssignableEntity(assignableEntityId, hierarchicalEntityIds);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a list of assignable entities associated with a hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @return the list of assignable entities associated with the hierarchical entity
     */
    @GetMapping("/assignable-entities")
    @Operation(summary = "Get assignable entities by hierarchical entity ID",
            description = "Retrieve all assignable entities associated with a specific hierarchical entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assignable entities",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssignableEntityResponseDto.class)))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<D>> getAssignableEntitiesByHierarchicalEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity", required = true) Long hierarchicalEntityId) {

        List<D> response = assignmentService.getAssignableEntitiesByHierarchicalEntity(hierarchicalEntityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of assignable entities associated with a hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param pageable the pagination and sorting information
     * @return the paginated response containing assignable entities
     */
    @GetMapping("/assignable-entities/paginated")
    @Operation(summary = "Get assignable entities by hierarchical entity",
            description = "Retrieves a paginated list of assignable entities associated with a specific hierarchical entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated assignable entities",
                    content = @Content(schema = @Schema(implementation = PaginatedResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaginatedResponseDto<D>> getAssignableEntitiesByHierarchicalEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity") Long hierarchicalEntityId,
            @Parameter(description = "Pagination information") Pageable pageable) {

        PaginatedResponseDto<D> response = assignmentService.getAssignableEntitiesByHierarchicalEntity(hierarchicalEntityId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the count of assignable entities associated with a hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @return the count of assignable entities associated with the hierarchical entity
     */
    @GetMapping("/assignable-entity-count")
    @Operation(summary = "Get count of assignable entities by hierarchical entity",
            description = "Retrieves the count of assignable entities associated with a specific hierarchical entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved count of assignable entities",
                    content = @Content(schema = @Schema(type = "integer"))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Integer> getAssignableEntityCountByHierarchicalEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity") Long hierarchicalEntityId) {

        int count = assignmentService.getAssignableEntityCountByHierarchicalEntity(hierarchicalEntityId);
        return ResponseEntity.ok(count);
    }

    /**
     * Retrieves the count of hierarchical entities associated with an assignable entity.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @return the count of hierarchical entities associated with an assignable entity
     */
    @GetMapping("/hierarchical-entity-count")
    @Operation(summary = "Get count of hierarchical entities by assignable entity",
            description = "Retrieves the count of hierarchical entities associated with a specific assignable entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved count of hierarchical entities",
                    content = @Content(schema = @Schema(type = "integer"))),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignable entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Integer> getHierarchicalEntityCountByAssignableEntity(
            @RequestParam @Parameter(description = "ID of the assignable entity") Long assignableEntityId) {

        int count = assignmentService.getHierarchicalEntityCountByAssignableEntity(assignableEntityId);
        return ResponseEntity.ok(count);
    }

    /**
     * Retrieves a list of hierarchical entities associated with a specific assignable entity.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @return the list of hierarchical entities associated with the assignable entity
     */
    @GetMapping("/hierarchical-entities")
    @Operation(summary = "Get hierarchical entities by assignable entity ID",
            description = "Retrieve all hierarchical entities associated with a specific assignable entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved hierarchical entities",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HierarchyResponseDto.class)))),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignable entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<H>> getHierarchicalEntitiesForAssignableEntity(
            @RequestParam @Parameter(description = "ID of the assignable entity", required = true) Long assignableEntityId) {

        List<H> response = assignmentService.getHierarchicalEntitiesForAssignableEntity(assignableEntityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of hierarchical entities associated with a specific assignable entity.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @param pageable the pagination and sorting information
     * @return the paginated response containing hierarchical entities
     */
    @GetMapping("/hierarchical-entities/paginated")
    @Operation(summary = "Get hierarchical entities for assignable entity",
            description = "Retrieves a paginated list of hierarchical entities associated with a specific assignable entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated hierarchical entities",
                    content = @Content(schema = @Schema(implementation = PaginatedResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignable entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaginatedResponseDto<H>> getHierarchicalEntitiesForAssignableEntity(
            @RequestParam @Parameter(description = "ID of the assignable entity", required = true) Long assignableEntityId,
            Pageable pageable) {

        PaginatedResponseDto<H> response = assignmentService.getHierarchicalEntitiesForAssignableEntity(assignableEntityId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all assignments.
     *
     * @return the list of all assignments
     */
    @GetMapping
    @Operation(summary = "Get all assignments",
            description = "Retrieves a list of all assignments between hierarchical entities and assignable entities. Returns an empty list if no assignments are found.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all assignments. Returns an empty list if no assignments are found.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssignmentResponseDto.class)))),
    })
    public ResponseEntity<List<AssignmentResponseDto<H, D>>> getAllAssignments() {

        List<AssignmentResponseDto<H, D>> response = assignmentService.getAssignmentsByEntityClasses();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of all assignments.
     *
     * @param pageable the pagination and sorting information
     * @return the paginated response containing assignments
     */
    @GetMapping("/paginated")
    @Operation(summary = "Get paginated assignments",
            description = "Retrieve paginated assignments of hierarchical entities and assignable entities.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated assignments.",
                    content = @Content(schema = @Schema(implementation = PaginatedResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<AssignmentResponseDto<H, D>>> getAllAssignments(
            Pageable pageable) {

        PaginatedResponseDto<AssignmentResponseDto<H, D>> response = assignmentService.getAssignmentsByEntityClasses(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing assignment.
     *
     * @param updateRequest the updated assignment details
     * @return the response containing details of the updated assignment
     */
    @PutMapping("/update")
    @Operation(summary = "Update an assignment",
            description = "Updates the details of an existing assignment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated the assignment.",
                    content = @Content(schema = @Schema(implementation = AssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - The assignment to be updated was not found.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssignmentResponseDto<H, D>> updateAssignment(
            @RequestBody @Parameter(description = "Updated assignment details") AssignmentRequestDto updateRequest) {

        AssignmentResponseDto<H, D> response = assignmentService.updateAssignment(updateRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to remove metadata from an assignment by metadata key.
     *
     * @param assignmentId The ID of the assignment.
     * @param metadataKey The key of the metadata to remove.
     * @return ResponseEntity indicating the result.
     */
    @Operation(summary = "Remove metadata by key",
            description = "Removes a specific metadata entry from an assignment using its key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content - Metadata removed successfully"),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignment or metadata not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{assignmentId}/metadata")
    public ResponseEntity<Void> removeMetadataByKey(
            @Parameter(description = "The ID of the assignment", required = true) @PathVariable Long assignmentId,
            @Parameter(description = "The key of the metadata to remove", required = true) @RequestParam String metadataKey) {

        assignmentService.removeMetadataByKey(assignmentId, metadataKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to remove metadata from an assignment by metadata ID.
     *
     * @param assignmentId The ID of the assignment.
     * @param metadataId The ID of the metadata to remove.
     * @return ResponseEntity indicating the result.
     */
    @Operation(summary = "Remove metadata by ID",
            description = "Removes a specific metadata entry from an assignment using its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content - Metadata removed successfully"),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignment or metadata not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{assignmentId}/metadata/{metadataId}")
    public ResponseEntity<Void> removeMetadataById(
            @Parameter(description = "The ID of the assignment", required = true) @PathVariable Long assignmentId,
            @Parameter(description = "The ID of the metadata to remove", required = true) @PathVariable Long metadataId) {
        assignmentService.removeMetadataById(assignmentId, metadataId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to remove metadata from an assignment by hierarchical entity ID, assignable entity ID, and metadata ID.
     *
     * @param hierarchicalEntityId The ID of the hierarchical entity.
     * @param assignableEntityId The ID of the assignable entity.
     * @param metadataId The ID of the metadata to remove.
     * @return ResponseEntity indicating the result.
     */
    @Operation(summary = "Remove metadata by hierarchical and assignable entity IDs",
            description = "Removes a specific metadata entry from an assignment by providing the hierarchical entity ID, assignable entity ID, and metadata ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content - Metadata removed successfully"),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity, assignable entity, or metadata not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata/{metadataId}")
    public ResponseEntity<Void> removeMetadataByHierarchicalAndAssignableEntityId(
            @Parameter(description = "The ID of the hierarchical entity", required = true) @PathVariable Long hierarchicalEntityId,
            @Parameter(description = "The ID of the assignable entity", required = true) @PathVariable Long assignableEntityId,
            @Parameter(description = "The ID of the metadata to remove", required = true) @PathVariable Long metadataId) {
        assignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to remove metadata from an assignment by hierarchical entity ID, assignable entity ID, and metadata key.
     *
     * @param hierarchicalEntityId The ID of the hierarchical entity.
     * @param assignableEntityId The ID of the assignable entity.
     * @param metadataKey The key of the metadata to remove.
     * @return ResponseEntity indicating the result.
     */
    @Operation(summary = "Remove metadata by hierarchical and assignable entity IDs using a key",
            description = "Removes a specific metadata entry from an assignment by providing the hierarchical entity ID, assignable entity ID, and metadata key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content - Metadata removed successfully"),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity, assignable entity, or metadata not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata")
    public ResponseEntity<Void> removeMetadataByHierarchicalAndAssignableEntityKey(
            @Parameter(description = "The ID of the hierarchical entity", required = true) @PathVariable Long hierarchicalEntityId,
            @Parameter(description = "The ID of the assignable entity", required = true) @PathVariable Long assignableEntityId,
            @Parameter(description = "The key of the metadata to remove", required = true) @RequestParam String metadataKey) {
        assignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a list of assignments associated with a specific hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @return the list of assignments associated with the hierarchical entity
     */
    @GetMapping("/by-hierarchical-entity")
    @Operation(summary = "Get assignments by hierarchical entity (non-paginated)",
            description = "Retrieves a list of assignments associated with a specific hierarchical entity without pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assignments",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssignmentResponseDto.class)))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<AssignmentResponseDto<H, D>>> getAssignmentsByHierarchicalEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity", required = true) Long hierarchicalEntityId) {

        List<AssignmentResponseDto<H, D>> response = assignmentService.getAssignmentsByHierarchicalEntity(hierarchicalEntityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of assignments associated with a specific hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param pageable the pagination and sorting information
     * @return the paginated response containing assignments
     */
    @GetMapping("/by-hierarchical-entity/paginated")
    @Operation(summary = "Get assignments by hierarchical entity (paginated)",
            description = "Retrieves a paginated list of assignments associated with a specific hierarchical entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated assignments",
                    content = @Content(schema = @Schema(implementation = PaginatedResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Hierarchical entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaginatedResponseDto<AssignmentResponseDto<H, D>>> getAssignmentsByHierarchicalEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity", required = true) Long hierarchicalEntityId,
            Pageable pageable) {

        PaginatedResponseDto<AssignmentResponseDto<H, D>> response = assignmentService.getAssignmentsByHierarchicalEntity(hierarchicalEntityId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a list of assignments associated with a specific assignable entity.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @return the list of assignments associated with the assignable entity
     */
    @GetMapping("/by-assignable-entity")
    @Operation(summary = "Get assignments by assignable entity (non-paginated)",
            description = "Retrieves a list of assignments associated with a specific assignable entity without pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assignments",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssignmentResponseDto.class)))),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignable entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<AssignmentResponseDto<H, D>>> getAssignmentsByAssignableEntity(
            @RequestParam @Parameter(description = "ID of the assignable entity", required = true) Long assignableEntityId) {

        List<AssignmentResponseDto<H, D>> response = assignmentService.getAssignmentsByAssignableEntity(assignableEntityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of assignments associated with a specific assignable entity.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @param pageable the pagination and sorting information
     * @return the paginated response containing assignments
     */
    @GetMapping("/by-assignable-entity/paginated")
    @Operation(summary = "Get assignments by assignable entity",
            description = "Retrieves a paginated list of assignments associated with a specific assignable entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated assignments",
                    content = @Content(schema = @Schema(implementation = PaginatedResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignable entity not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaginatedResponseDto<AssignmentResponseDto<H, D>>> getAssignmentsByAssignableEntity(
            @RequestParam @Parameter(description = "ID of the assignable entity", required = true) Long assignableEntityId,
            Pageable pageable) {

        PaginatedResponseDto<AssignmentResponseDto<H, D>> response = assignmentService.getAssignmentsByAssignableEntity(assignableEntityId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves an assignment associated with a specific hierarchical entity and assignable entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId the ID of the assignable entity
     * @return the assignment associated with the hierarchical and assignable entities
     */
    @GetMapping("/by-hierarchical-and-assignable-entity")
    @Operation(summary = "Get assignment by hierarchical and assignable entities",
            description = "Retrieves an assignment associated with a specific hierarchical entity and assignable entity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assignment",
                    content = @Content(schema = @Schema(implementation = AssignmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Not Found - Assignment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssignmentResponseDto<H, D>> getAssignmentByHierarchicalAndAssignableEntity(
            @RequestParam @Parameter(description = "ID of the hierarchical entity", required = true) Long hierarchicalEntityId,
            @RequestParam @Parameter(description = "ID of the assignable entity", required = true) Long assignableEntityId) {

        AssignmentResponseDto<H, D> response = assignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId);
        return ResponseEntity.ok(response);
    }
}
