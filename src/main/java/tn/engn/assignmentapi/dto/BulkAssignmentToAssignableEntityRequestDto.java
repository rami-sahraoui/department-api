package tn.engn.assignmentapi.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object (DTO) for bulk assignment requests to an assignable entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAssignmentToAssignableEntityRequestDto {
    /**
     * The ID of the assignable entity to which the hierarchical entities are being assigned.
     */
    @NotNull(message = "Assignable entity ID cannot be null")
    private Long assignableEntityId;

    /**
     * The list of IDs of the hierarchical entities being assigned to the assignable entity.
     */
    @NotEmpty(message = "Hierarchical entity IDs cannot be empty")
    private List<Long> hierarchicalEntityIds;

    /**
     * Metadata associated with the bulk assignments.
     */
    private List<AssignmentMetadataRequestDto> metadata;
}

