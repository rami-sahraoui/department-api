package tn.engn.assignmentapi.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.model.Job;

@Entity
@DiscriminatorValue("Job_Employee")
@NoArgsConstructor
@SuperBuilder
public class JobEmployeeAssignment extends Assignment<Job, Employee> {}
