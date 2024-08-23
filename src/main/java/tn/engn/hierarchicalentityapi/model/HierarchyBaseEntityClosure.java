package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

/**
 * Represents the closure table entity to manage hierarchical relationships between hierarchical entities.
 * Each entry indicates that there is a path from the ancestor entity to the descendant entity.
 *
 * @param <C> The type of the hierarchical entity closure itself, for recursive relationships.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Only include explicitly included fields
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class HierarchyBaseEntityClosure<C extends HierarchyBaseEntityClosure<C>> {

    /**
     * Unique identifier for the closure table entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    private Long id;

    /**
     * ID of the ancestor entity in the hierarchy.
     */
    @Column(name = "ancestor_id", nullable = false)
    private Long ancestorId;

    /**
     * ID of the descendant entity in the hierarchy.
     */
    @Column(name = "descendant_id", nullable = false)
    private Long descendantId;

    /**
     * Level of the relationship, representing the distance between the ancestor and descendant.
     * A direct parent-child relationship has a level of 1.
     */
    @Column(nullable = false)
    private int level;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HierarchyBaseEntityClosure<?> that = (HierarchyBaseEntityClosure<?>) o;
        return level == that.level && Objects.equals(id, that.id) && Objects.equals(ancestorId, that.ancestorId) && Objects.equals(descendantId, that.descendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ancestorId, descendantId, level);
    }
}
