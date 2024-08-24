package tn.engn.assignmentapi.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.model.Project;

@Entity
@DiscriminatorValue("Project_Employee")
@NoArgsConstructor
@SuperBuilder
public class ProjectEmployeeAssignment extends Assignment<Project, Employee> {}
