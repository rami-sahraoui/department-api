package tn.engn.departmentapi.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a department entity in the Materialized Path Model with support for hierarchical structures.
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

    /**
     * Path representing the hierarchical position of the department in the Materialized Path Model.
     * The path is a string of concatenated IDs of all ancestors including the department itself,
     * separated by a delimiter, e.g., "/1/2/3/".
     */
    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    // Additional fields, relationships, and methods if needed
}
