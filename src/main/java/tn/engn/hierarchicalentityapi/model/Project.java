package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;

import java.util.*;

/**
 * Represents a project entity in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Project extends HierarchyBaseEntity<Project> {

    /**
     * The parent project entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_id")
    private Project parentEntity;

    /**
     * The list of subprojects (children) of this project entity.
     */
    @Builder.Default
    @OneToMany(mappedBy = "parentEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Project> subEntities = new ArrayList<>();

    /**
     * The employee assigned to this project.
     */
    @OneToOne(mappedBy = "project", fetch = FetchType.LAZY)
    private Employee employee;

    /**
     * Adds a subproject to this project entity.
     *
     * @param subEntity The subproject to add.
     */
    @Override
    public void addSubEntity(Project subEntity) {
        subEntities.add(subEntity);
        subEntity.setParentId(this.id);
        subEntity.setParentEntity(this); // Establish bidirectional relationship
    }

    /**
     * Removes a subproject from this project entity.
     *
     * @param subEntity The subproject to remove.
     */
    @Override
    public void removeSubEntity(Project subEntity) {
        subEntities.remove(subEntity);
        subEntity.setParentId(null);
        subEntity.setParentEntity(null); // Remove bidirectional relationship
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Project that = (Project) o;
        // Compare additional fields specific to Project
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
