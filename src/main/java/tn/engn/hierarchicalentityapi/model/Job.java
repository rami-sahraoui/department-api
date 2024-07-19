package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a department entity in the Materialized Path Model with support for hierarchical structures.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("Job")
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Job extends HierarchyBaseEntity<Job> {

    /**
     * Override method to return the entity type class.
     */
    @Override
    public Class<Job> getEntityType() {
        return Job.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Job that = (Job) o;
        return true; // Additional specific checks if needed
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // Include superclass hash code
    }
}