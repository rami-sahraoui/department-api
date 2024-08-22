package tn.engn.assignmentapi.dto;

import lombok.*;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;

import java.util.List;

/**
 * Data Transfer Object for bulk assignment responses.
 * Contains both the hierarchical entities and assignable entities with metadata.
 *
 * @param <H> the type of the hierarchical entity response DTO
 * @param <D> the type of the assignable entity response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAssignmentResponseDto<H extends HierarchyResponseDto, D extends AssignableEntityResponseDto> {
    /**
     * The list of hierarchical entities involved in the bulk assignment.
     */
    private List<H> hierarchicalEntities;

    /**
     * The list of assignable entities involved in the bulk assignment.
     */
    private List<D> assignableEntities;

    /**
     * Metadata associated with the bulk assignments.
     * Contains additional information in key-value pairs.
     */
    private List<AssignmentMetadataResponseDto> metadata;

    public BulkAssignmentResponseDto(List<H> hierarchicalEntities, List<D> assignableEntities) {
        this.hierarchicalEntities = hierarchicalEntities;
        this.assignableEntities = assignableEntities;
    }
}
