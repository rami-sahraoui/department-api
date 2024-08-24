package tn.engn.assignmentapi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.assignmentapi.model.JobEmployeeAssignment;
import tn.engn.assignmentapi.service.JobEmployeeAssignmentService;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Job;

/**
 * Controller for managing assignments between jobs and employees.
 * This controller provides endpoints for assigning employees to jobs,
 * removing assignments, bulk operations, and retrieving assignments, both as
 * individual entities and in paginated forms.
 */
@RestController
@RequestMapping("/api/v1/job-employee-assignments")
@Tag(name = "JobEmployeeAssignment (OneToMany)", description = "API for managing assignments between jobs and employees")
public class JobEmployeeAssignmentController extends AbstractAssignmentController<
        JobEmployeeAssignment,
        Job,
        Employee,
        EmployeeRequestDto,
        EmployeeResponseDto,
        HierarchyRequestDto,
        HierarchyResponseDto> {

    /**
     * Constructor for JobEmployeeAssignmentController.
     *
     * @param assignmentService the service for managing job-employee assignments
     */
    public JobEmployeeAssignmentController(JobEmployeeAssignmentService assignmentService) {
        super(assignmentService);
    }

}
