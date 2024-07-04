package tn.engn.departmentapi.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a department entity in the Nested Set Model with support for multi-tree structures.
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
     * ID of the root or master department to which this department belongs.
     */
    @Column(name = "root_id")
    private Long rootId;

    /**
     * Left index in the Nested Set Model.
     */
    @Column(name = "left_index", nullable = false)
    private Integer leftIndex;

    /**
     * Right index in the Nested Set Model.
     */
    @Column(name = "right_index", nullable = false)
    private Integer rightIndex;

    /**
     * Level of the department in the hierarchy.
     */
    @Column(name = "level", nullable = false)
    private Integer level;

    // Additional fields, relationships, and methods if needed
}