package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a department entity in the system.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("Department")

//@Table(name = "departments")
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Department extends HierarchyBaseEntity<Department> {

    /**
     * The parent department entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_entity_id")
    private Department parentEntity;

    /**
     * The list of sub-departments (children) of this department entity.
     */
    @Builder.Default
    @OneToMany(mappedBy = "parentEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Department> subEntities = new ArrayList<>();

    /**
     * Adds a sub-department to this department entity.
     *
     * @param subEntity The sub-department to add.
     */
    @Override
    public void addSubEntity(Department subEntity) {
        subEntities.add(subEntity);
        subEntity.setParentId(this.id);
        subEntity.setParentEntity(this); // Establish bidirectional relationship
    }

    /**
     * Removes a sub-department from this department entity.
     *
     * @param subEntity The sub-department to remove.
     */
    @Override
    public void removeSubEntity(Department subEntity) {
        subEntities.remove(subEntity);
        subEntity.setParentId(null);
        subEntity.setParentEntity(null); // Remove bidirectional relationship
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Department that = (Department) o;
        // Compare additional fields specific to Department
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }
}
