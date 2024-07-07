package tn.engn.departmentapi.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents the closure table entity to manage hierarchical relationships between departments.
 * Each entry indicates that there is a path from the ancestor department to the descendant department.
 */
@Entity
@Table(name = "department_closure")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentClosure {

    /**
     * Unique identifier for the closure table entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the ancestor department in the hierarchy.
     */
    @Column(name = "ancestor_id", nullable = false)
    private Long ancestorId;

    /**
     * ID of the descendant department in the hierarchy.
     */
    @Column(name = "descendant_id", nullable = false)
    private Long descendantId;

    /**
     * Level of the relationship, representing the distance between the ancestor and descendant.
     * A direct parent-child relationship has a level of 1.
     */
    @Column(nullable = false)
    private int level;
}
