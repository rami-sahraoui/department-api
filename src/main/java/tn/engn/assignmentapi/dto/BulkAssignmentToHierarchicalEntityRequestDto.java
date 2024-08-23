package tn.engn.assignmentapi.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object (DTO) for bulk assignment requests to a hierarchical entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAssignmentToHierarchicalEntityRequestDto {
    /**
     * The ID of the hierarchical entity to which the assignable entities are being assigned.
     */
    @NotNull(message = "Hierarchical entity ID cannot be null")
    private Long hierarchicalEntityId;

    /**
     * The list of IDs of the assignable entities being assigned to the hierarchical entity.
     */
    @NotEmpty(message = "Assignable entity IDs cannot be empty")
    private List<Long> assignableEntityIds;

    /**
     * Metadata associated with the bulk assignments.
     */
    private List<AssignmentMetadataRequestDto> metadata;

}
