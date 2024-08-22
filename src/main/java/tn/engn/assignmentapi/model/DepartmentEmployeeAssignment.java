package tn.engn.assignmentapi.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.model.Department;

import java.time.LocalDate;
import java.util.Map;

@Entity
@DiscriminatorValue("Department_Employee")
@NoArgsConstructor
@SuperBuilder
public class DepartmentEmployeeAssignment extends Assignment<Department, Employee> {}
