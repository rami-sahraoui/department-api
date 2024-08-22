package tn.engn.assignmentapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;

import java.util.List;

/**
 * Data Transfer Object for assignment responses.
 * Contains the hierarchical entity, assignable entity, and metadata.
 *
 * @param <H> the type of the hierarchical entity response DTO
 * @param <D> the type of the assignable entity response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponseDto<H extends HierarchyResponseDto, D extends AssignableEntityResponseDto> {
    /**
     * The unique identifier of the assignment.
     */
    private Long id;
    /**
     * The hierarchical entity involved in the assignment.
     */
    private H hierarchicalEntity;

    /**
     * The assignable entity involved in the assignment.
     */
    private D assignableEntity;

    /**
     * Metadata associated with the assignment.
     * Contains additional information in key-value pairs.
     */
    private List<AssignmentMetadataResponseDto> metadata;

    public AssignmentResponseDto(H hierarchicalEntity, D assignableEntity) {
        this.hierarchicalEntity = hierarchicalEntity;
        this.assignableEntity = assignableEntity;
    }
}
