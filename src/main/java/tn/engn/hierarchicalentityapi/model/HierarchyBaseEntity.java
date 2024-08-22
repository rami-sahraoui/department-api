package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Objects;

/**
 * Represents a hierarchical entity in the system.
 *
 * @param <E> The type of the hierarchical entity itself, for recursive relationships.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Only include explicitly included fields
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
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
     * The parent entity ID.
     */
    @Column(name = "parent_id")
    protected Long parentId;

    /**
     * The class type of the entity.
     */
    @Transient
    protected Class<E> entityType;

    public HierarchyBaseEntity(Class<E> entityType) {
        this.entityType = entityType;
    }

    /**
     * Retrieves the parent entity of this hierarchical entity.
     *
     * @return The parent entity.
     */
    public abstract E getParentEntity();

    /**
     * Sets the parent entity of this hierarchical entity.
     *
     * @param parentEntity The parent entity to set.
     */
    public abstract void setParentEntity(E parentEntity);

    /**
     * Retrieves the list of sub-entities (children) of this hierarchical entity.
     *
     * @return The list of sub-entities.
     */
    public abstract List<E> getSubEntities();

    /**
     * Sets the list of sub-entities (children) of this hierarchical entity.
     *
     * @param subEntities The list of sub-entities to set.
     */
    public abstract void setSubEntities(List<E> subEntities);

    /**
     * Adds a sub-entity to this hierarchical entity.
     *
     * @param subEntity The sub-entity to add.
     */
    public abstract void addSubEntity(E subEntity);

    /**
     * Removes a sub-entity from this hierarchical entity.
     *
     * @param subEntity The sub-entity to remove.
     */
    public abstract void removeSubEntity(E subEntity);

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
