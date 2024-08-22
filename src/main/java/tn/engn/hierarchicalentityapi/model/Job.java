package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;

import java.util.*;

/**
 * Represents a job entity in the system.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
public class Job extends HierarchyBaseEntity<Job> {

    /**
     * The parent job entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_id")
    private Job parentEntity;

    /**
     * The list of sub-jobs (children) of this job entity.
     */
    @Builder.Default
    @OneToMany(mappedBy = "parentEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Job> subEntities = new ArrayList<>();

    /**
     * The set of employees assigned to the job.
     */
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Employee> employees = new HashSet<>();

    /**
     * Adds a sub-job to this job entity.
     *
     * @param subEntity The sub-job to add.
     */
    @Override
    public void addSubEntity(Job subEntity) {
        subEntities.add(subEntity);
        subEntity.setParentId(this.id);
        subEntity.setParentEntity(this); // Establish bidirectional relationship
    }

    /**
     * Removes a sub-job from this job entity.
     *
     * @param subEntity The sub-job to remove.
     */
    @Override
    public void removeSubEntity(Job subEntity) {
        subEntities.remove(subEntity);
        subEntity.setParentId(null);
        subEntity.setParentEntity(null); // Remove bidirectional relationship
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Job that = (Job) o;
        // Compare additional fields specific to Job
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
