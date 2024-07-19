package tn.engn.hierarchicalentityapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object (DTO) for representing a hierarchical entity in responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HierarchyResponseDto {

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

    /**
     * Count of sub-entities (children) of this entity.
     */
    private int subEntitiesCount; // New attribute for count of sub-entities

    /**
     * List of sub-entities (children) of this entity.
     */
    private List<? extends HierarchyResponseDto> subEntities;
}
