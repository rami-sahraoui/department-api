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
//@Entity
@Entity(name = "hierarchy_entities")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "entity_type", discriminatorType = DiscriminatorType.STRING)

//@Table(name = "hierarchy_base_entities")
public class HierarchyBaseEntity<E extends HierarchyBaseEntity<E>> {

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
    public E getParentEntity() {
        // This will be overridden in the subclasses
        throw new UnsupportedOperationException("This method should be overridden in the subclass.");
    }

    /**
     * Sets the parent entity of this hierarchical entity.
     *
     * @param parentEntity The parent entity to set.
     */
    public void setParentEntity(E parentEntity) {
        // This will be overridden in the subclasses
        throw new UnsupportedOperationException("This method should be overridden in the subclass.");
    }

    /**
     * Retrieves the list of sub-entities (children) of this hierarchical entity.
     *
     * @return The list of sub-entities.
     */
    public List<E> getSubEntities() {
        // This will be overridden in the subclasses
        throw new UnsupportedOperationException("This method should be overridden in the subclass.");
    }

    /**
     * Sets the list of sub-entities (children) of this hierarchical entity.
     *
     * @param subEntities The list of sub-entities to set.
     */
    public void setSubEntities(List<E> subEntities) {
        // This will be overridden in the subclasses
        throw new UnsupportedOperationException("This method should be overridden in the subclass.");
    }

    /**
     * Adds a sub-entity to this hierarchical entity.
     *
     * @param subEntity The sub-entity to add.
     */
    public void addSubEntity(E subEntity) {
        // This will be overridden in the subclasses
        throw new UnsupportedOperationException("This method should be overridden in the subclass.");
    }

    /**
     * Removes a sub-entity from this hierarchical entity.
     *
     * @param subEntity The sub-entity to remove.
     */
    public void removeSubEntity(E subEntity) {
        // This will be overridden in the subclasses
        throw new UnsupportedOperationException("This method should be overridden in the subclass.");
    }

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
