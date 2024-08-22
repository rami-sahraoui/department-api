package tn.engn.assignmentapi.mapper;

import tn.engn.assignmentapi.dto.AssignmentMetadataRequestDto;
import tn.engn.assignmentapi.dto.AssignmentMetadataResponseDto;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.assignmentapi.model.AssignmentMetadata;

/**
 * A manual mapper for converting between AssignmentMetadata entities and their corresponding DTOs.
 */
public class AssignmentMetadataMapper {

    /**
     * Converts a request DTO to an AssignmentMetadata entity.
     *
     * @param requestDto The request DTO containing the metadata key and value.
     * @param assignment The assignment entity to which this metadata belongs.
     * @return The AssignmentMetadata entity.
     */
    public static AssignmentMetadata toEntity(AssignmentMetadataRequestDto requestDto, Assignment assignment) {
        if (requestDto == null || assignment == null) {
            return null;
        }

        return AssignmentMetadata.builder()
                .key(requestDto.getKey())
                .value(requestDto.getValue())
                .assignment(assignment)
                .build();
    }

    /**
     * Converts an AssignmentMetadata entity to a response DTO.
     *
     * @param metadata The AssignmentMetadata entity.
     * @return The response DTO containing metadata details.
     */
    public static AssignmentMetadataResponseDto toDto(AssignmentMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        return AssignmentMetadataResponseDto.builder()
                .id(metadata.getId())
                .key(metadata.getKey())
                .value(metadata.getValue())
                .build();
    }
}
