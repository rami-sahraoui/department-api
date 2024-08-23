package tn.engn.assignmentapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for receiving metadata information associated with an assignment.
 * This DTO is used when creating or updating assignment metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentMetadataRequestDto {

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
