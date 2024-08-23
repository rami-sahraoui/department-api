package tn.engn.assignmentapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for assignment requests.
 * Contains the hierarchical entity ID, assignable entity ID, and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentRequestDto {
    /**
     * The ID of the hierarchical entity to which the assignable entity is being assigned.
     */
    private Long hierarchicalEntityId;

    /**
     * The ID of the assignable entity that is being assigned.
     */
    private Long assignableEntityId;

    /**
     * List of metadata associated with the assignment.
     * Contains additional information with keys and values.
     */
    private List<AssignmentMetadataRequestDto> metadata;

    public AssignmentRequestDto(Long hierarchicalEntityId, Long assignableEntityId) {
        this.hierarchicalEntityId = hierarchicalEntityId;
        this.assignableEntityId = assignableEntityId;
    }
}
