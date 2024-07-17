package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a job entity in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("Job")
@Table(name = "jobs")
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
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
