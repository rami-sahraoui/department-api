package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a team entity in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Team extends HierarchyBaseEntity<Team> {

    /**
     * The parent team entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_id")
    private Team parentEntity;

    /**
     * The list of subprojects (children) of this team entity.
     */
    @Builder.Default
    @OneToMany(mappedBy = "parentEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Team> subEntities = new ArrayList<>();

    /**
     * The manager of the team.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    /**
     * Adds a sub-team to this team entity.
     *
     * @param subEntity The sub-team to add.
     */
    @Override
    public void addSubEntity(Team subEntity) {
        subEntities.add(subEntity);
        subEntity.setParentId(this.id);
        subEntity.setParentEntity(this); // Establish bidirectional relationship
    }

    /**
     * Removes a sub-team from this team entity.
     *
     * @param subEntity The sub-team to remove.
     */
    @Override
    public void removeSubEntity(Team subEntity) {
        subEntities.remove(subEntity);
        subEntity.setParentId(null);
        subEntity.setParentEntity(null); // Remove bidirectional relationship
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Team that = (Team) o;
        // Compare additional fields specific to Team
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
