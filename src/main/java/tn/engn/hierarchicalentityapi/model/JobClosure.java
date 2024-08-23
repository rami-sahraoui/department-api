package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents the closure table entity to manage hierarchical relationships between jobs.
 * Each entry indicates that there is a path from the ancestor job to the descendant job.
 */
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class JobClosure extends HierarchyBaseEntityClosure<JobClosure> {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JobClosure that = (JobClosure) o;
        return true; // Additional specific checks if needed
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // Include superclass hash code
    }
}
