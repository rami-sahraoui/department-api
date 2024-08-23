package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents the closure table entity to manage hierarchical relationships between departments.
 * Each entry indicates that there is a path from the ancestor department to the descendant department.
 */
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class DepartmentClosure extends HierarchyBaseEntityClosure<DepartmentClosure> {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DepartmentClosure that = (DepartmentClosure) o;
        return true; // Additional specific checks if needed
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // Include superclass hash code
    }
}
