package tn.engn.assignmentapi.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents metadata information associated with an assignment.
 * Metadata is stored as key-value pairs and linked to a specific assignment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class AssignmentMetadata {

    /**
     * The unique identifier for the assignment metadata.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The assignment to which this metadata is associated.
     * This is a many-to-one relationship, meaning multiple metadata entries can be associated with a single assignment.
     */
    @ManyToOne
    @JoinColumn(name = "assignment_id")
    @JsonBackReference
    private Assignment assignment;

    /**
     * The key of the metadata entry. This is a string that identifies the type of metadata.
     */
    @Column(name = "`key`")
    private String key;

    /**
     * The value associated with the metadata key. This can store information related to the assignment in various formats.
     */
    @Column(name = "`value`")
    private String value;

}

