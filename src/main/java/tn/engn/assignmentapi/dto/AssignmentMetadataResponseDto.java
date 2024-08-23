package tn.engn.assignmentapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for sending metadata information associated with an assignment.
 * This DTO is used when retrieving assignment metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentMetadataResponseDto {

    /**
     * The unique identifier for the assignment metadata.
     */
    private Long id;

    /**
     * The key of the metadata entry.
     * This is a string that identifies the type of metadata.
     */
    private String key;

    /**
     * The value associated with the metadata key.
     * This stores information related to the assignment.
     */
    private String value;
}
