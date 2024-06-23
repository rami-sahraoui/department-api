package tn.engn.departmentapi.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parentDepartment;

    @OneToMany(mappedBy = "parentDepartment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Department> subDepartments = new ArrayList<>();

    // Utility methods to manage bidirectional relationships
    public void addSubDepartment(Department subDepartment) {
        subDepartments.add(subDepartment);
        subDepartment.setParentDepartment(this); // Establish bidirectional relationship
    }

    public void removeSubDepartment(Department subDepartment) {
        subDepartments.remove(subDepartment);
        subDepartment.setParentDepartment(null); // Remove bidirectional relationship
    }
}
