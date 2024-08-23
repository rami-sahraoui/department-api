package tn.engn.assignmentapi.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an assignment linking a hierarchical entity with an assignable entity.
 * <p>
 * This class serves as the base entity for assignments, providing a mapping between a hierarchical
 * entity and an assignable entity. It includes metadata and timestamps for tracking the assignment's
 * lifecycle.
 * </p>
 *
 * @param <E> the type of the hierarchical entity
 * @param <A> the type of the assignable entity
 */
@Entity(name = "assignments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "assignment_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Assignment<E extends HierarchyBaseEntity<E>, A extends AssignableEntity<A>> {

    /**
     * The unique identifier of the assignment.
     * <p>
     * This field is auto-generated and serves as the primary key for the assignment entity.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The hierarchical entity associated with this assignment.
     * <p>
     * This represents the entity in the hierarchy that is linked to the assignable entity.
     * </p>
     */
    @ManyToOne(targetEntity = HierarchyBaseEntity.class)
    private E hierarchicalEntity;

    /**
     * The assignable entity associated with this assignment.
     * <p>
     * This represents the entity that is being assigned within the hierarchical structure.
     * </p>
     */
    @ManyToOne(targetEntity = AssignableEntity.class)
    private A assignableEntity;

    /**
     * The start date of the assignment.
     * <p>
     * This field records when the assignment was created or became active.
     * It is automatically set when the assignment entity is persisted.
     * </p>
     */
    @CreatedDate
    private LocalDate startDate;

    /**
     * The last update date of the assignment.
     * <p>
     * This field is automatically updated whenever the assignment entity is modified.
     * </p>
     */
    @UpdateTimestamp
    private LocalDate updateDate;

    /**
     * The metadata associated with this assignment.
     * <p>
     * This set contains additional information or attributes related to the assignment.
     * The relationship is bidirectional, and any changes to the metadata are cascaded to the assignment.
     * </p>
     */
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private Set<AssignmentMetadata> metadata = new HashSet<>();

    /**
     * Adds metadata to this assignment.
     * <p>
     * This method maintains the bidirectional relationship between the assignment and its metadata.
     * It ensures that the back reference in the metadata is properly set to this assignment.
     * </p>
     *
     * @param metadata the metadata to be added to this assignment
     */
    public void addMetadata(AssignmentMetadata metadata) {
        metadata.setAssignment(this);  // Set the back reference
        this.metadata.add(metadata);
    }

    /**
     * Removes metadata from this assignment.
     * <p>
     * This method maintains the bidirectional relationship between the assignment and its metadata.
     * It ensures that the back reference in the metadata is properly cleared.
     * </p>
     *
     * @param metadata the metadata to be removed from this assignment
     */
    public void removeMetadata(AssignmentMetadata metadata) {
        this.metadata.remove(metadata);
        metadata.setAssignment(null);  // Clear the back reference
    }
}
