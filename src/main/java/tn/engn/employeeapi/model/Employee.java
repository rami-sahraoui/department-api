package tn.engn.employeeapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;
import tn.engn.assignmentapi.model.AssignableEntity;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.model.Job;
import tn.engn.hierarchicalentityapi.model.Project;
import tn.engn.hierarchicalentityapi.model.Team;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an employee entity.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Employee extends AssignableEntity<Employee> {

    /**
     * The first name of the employee.
     */
    @NotBlank(message = "First name is required")
    @Length(min = 3, message = "First name must be at least 3 characters long")
    private String firstName;

    /**
     * The last name of the employee.
     */
    @NotBlank(message = "Last name is required")
    @Length(min = 3, message = "Last name must be at least 3 characters long")
    private String lastName;

    /**
     * The email address of the employee.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$",
            message = "Email should be valid"
    )
    private String email;

    /**
     * The date of birth of the employee.
     */
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    /**
     * The position held by the employee.
     */
    private String position;

    /**
     * The set of departments the employee is associated with.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "department_employee",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> departments = new HashSet<>();

    /**
     * The job associated with the employee.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id")
    private Job job;

    /**
     * The project associated with the employee.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * The set of teams managed by the employee.
     */
    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Team> teams = new HashSet<>();

    /**
     * The full name of the employee, which is not stored in the database.
     */
    @Transient
    private String Name;

    public String getName() {
        return firstName + " " + lastName;
    }

    /**
     * The age of the employee, which is not stored in the database.
     */
    @Transient
    private int age;

    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    @PrePersist
    @PreUpdate
    private void validateAge() {
        if (getAge() < 20) {
            throw new ValidationException("Employee must be at least 20 years old");
        }
    }

    /**
     * Adds a team to the manager.
     *
     * This method establishes a bidirectional relationship between the employee (manager)
     * and the team. It adds the team to the set of teams managed by the employee and
     * sets the manager of the team to this employee.
     *
     * @param team the team to add
     */
    public void addTeam(Team team) {
        // Adds the team to the set of teams managed by this employee.
        teams.add(team);

        // Sets this employee as the manager of the team.
        team.setManager(this);
    }

    /**
     * Removes a team from the manager.
     *
     * This method breaks the bidirectional relationship between the employee (manager)
     * and the team. It removes the team from the set of teams managed by the employee
     * and sets the manager of the team to null.
     *
     * @param team the team to remove
     */
    public void removeTeam(Team team) {
        // Removes the team from the set of teams managed by this employee.
        teams.remove(team);

        // Sets the manager of the team to null, breaking the relationship.
        team.setManager(null);
    }
}
