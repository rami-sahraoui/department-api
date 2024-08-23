package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents the closure table entity to manage hierarchical relationships between teams.
 * Each entry indicates that there is a path from the ancestor team to the descendant team.
 */
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class TeamClosure extends HierarchyBaseEntityClosure<TeamClosure> {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TeamClosure that = (TeamClosure) o;
        return true; // Additional specific checks if needed
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // Include superclass hash code
    }
}
