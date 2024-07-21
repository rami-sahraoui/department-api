package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

/**
 * Represents a hierarchical entity in the system using Materialized Path Model attributes.
 *
 * @param <E> The type of the hierarchical entity itself, for recursive relationships.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Only include explicitly included fields
@Entity(name = "hierarchy_entities")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "entity_type", discriminatorType = DiscriminatorType.STRING)
public abstract class HierarchyBaseEntity<E extends HierarchyBaseEntity<E>> {

    /**
     * The unique identifier of the hierarchical entity.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    protected Long id;

    /**
     * The name of the hierarchical entity.
     */
    @Column(nullable = false)
    protected String name;

    /**
     * ID of the parent entity. Null if this entity is a root entity.
     */
    @Column(name = "parent_id")
    protected Long parentId;

    /**
     * Abstract method to return the entity type class.
     * Subclasses must implement this method to return their specific class type.
     */
    public abstract Class<E> getEntityType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HierarchyBaseEntity<?> that = (HierarchyBaseEntity<?>) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
