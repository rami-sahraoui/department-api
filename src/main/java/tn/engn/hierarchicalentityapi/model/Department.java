package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a department entity in the Nested Set Model with support for multi-tree structures.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Department extends HierarchyBaseEntity<Department> {

    /**
     * The set of employees assigned to the department.
     */
    @ManyToMany(mappedBy = "departments", fetch = FetchType.EAGER)
    private Set<Employee> employees = new HashSet<>();

    /**
     * Adds an employee to the department.
     *
     * @param employee the employee to add
     */
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.getDepartments().add(this);
    }

    /**
     * Removes an employee from the department.
     *
     * @param employee the employee to remove
     */
    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.getDepartments().remove(this);
    }

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