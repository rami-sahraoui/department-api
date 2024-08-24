package tn.engn.assignmentapi.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.model.Department;

@Entity
@DiscriminatorValue("Department_Employee")
@NoArgsConstructor
@SuperBuilder
public class DepartmentEmployeeAssignment extends Assignment<Department, Employee> {}
