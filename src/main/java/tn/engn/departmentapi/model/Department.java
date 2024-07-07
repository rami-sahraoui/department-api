package tn.engn.departmentapi.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a department entity in the Closure Table Model with support for hierarchical structures.
 */
@Entity
@Table(name = "departments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    /**
     * Unique identifier for the department.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the department.
     */
    @Column(nullable = false)
    private String name;

    /**
     * ID of the parent department. Null if this department is a root department.
     */
    @Column(name = "parent_department_id")
    private Long parentDepartmentId;

    // Additional fields, relationships, and methods if needed
}
