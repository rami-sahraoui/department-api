package tn.engn.assignmentapi.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.model.Team;

@Entity
@DiscriminatorValue("Team_Employee")
@NoArgsConstructor
@SuperBuilder
public class TeamManagerAssignment extends Assignment<Team, Employee> {}
