package tn.engn.hierarchicalentityapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for creating or updating a hierarchical entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchyRequestDto {

    /**
     * Unique identifier of the entity.
     */
    private Long id;

    /**
     * Name of the entity.
     */
    private String name;

    /**
     * ID of the parent entity. Null if this entity is a root entity.
     */
    private Long parentEntityId;
}
