package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;

import java.util.*;

/**
 * Represents a project entity in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Project extends HierarchyBaseEntity<Project> {

    /**
     * The employee assigned to this project.
     */
    @OneToOne(mappedBy = "project", fetch = FetchType.LAZY)
    private Employee employee;

    /**
     * Override method to return the entity type class.
     */
    @Override
    public Class<Project> getEntityType() {
        return Project.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Project that = (Project) o;
        // Compare additional fields specific to Project
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
