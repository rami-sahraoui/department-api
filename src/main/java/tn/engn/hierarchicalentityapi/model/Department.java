package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents a department entity in the Materialized Path Model with support for hierarchical structures.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("Department")
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Department extends HierarchyBaseEntity<Department> {

    /**
     * Override method to return the entity type class.
     */
    @Override
    public Class<Department> getEntityType() {
        return Department.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Department that = (Department) o;
        return true; // Additional specific checks if needed
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // Include superclass hash code
    }
}