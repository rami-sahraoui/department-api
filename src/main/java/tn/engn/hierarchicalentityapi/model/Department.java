package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;

import java.util.*;

/**
 * Represents a department entity in the system.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
public class Department extends HierarchyBaseEntity<Department> {

    /**
     * The parent department entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_id")
    private Department parentEntity;

    /**
     * The list of sub-departments (children) of this department entity.
     */
    @Builder.Default
    @OneToMany(mappedBy = "parentEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Department> subEntities = new ArrayList<>();

    /**
     * The set of employees assigned to the department.
     */
    @ManyToMany(mappedBy = "departments", fetch = FetchType.EAGER)
    private Set<Employee> employees = new HashSet<>();

    /**
     * Adds a sub-department to this department entity.
     *
     * @param subEntity The sub-department to add.
     */
    @Override
    public void addSubEntity(Department subEntity) {
        subEntities.add(subEntity);
        subEntity.setParentId(this.id);
        subEntity.setParentEntity(this); // Establish bidirectional relationship
    }

    /**
     * Removes a sub-department from this department entity.
     *
     * @param subEntity The sub-department to remove.
     */
    @Override
    public void removeSubEntity(Department subEntity) {
        subEntities.remove(subEntity);
        subEntity.setParentId(null);
        subEntity.setParentEntity(null); // Remove bidirectional relationship
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Department that = (Department) o;
        // Compare additional fields specific to Department
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
