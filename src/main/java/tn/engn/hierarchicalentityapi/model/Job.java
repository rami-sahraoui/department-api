package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a job entity in the Closure Table Model with support for hierarchical structures.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Job extends HierarchyBaseEntity<Job> {

    /**
     * The set of employees assigned to the job.
     */
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Employee> employees = new HashSet<>();

    /**
     * Adds an employee to the job.
     *
     * @param employee the employee to add
     */
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setJob(this);
    }

    /**
     * Removes an employee from the job.
     *
     * @param employee the employee to remove
     */
    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setJob(null);
    }

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