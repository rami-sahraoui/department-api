package tn.engn.assignmentapi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.assignmentapi.model.ProjectEmployeeAssignment;
import tn.engn.assignmentapi.service.ProjectEmployeeAssignmentService;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Project;

/**
 * Controller for managing assignments between projects and employees.
 * This controller provides endpoints for assigning employees to projects,
 * removing assignments, bulk operations, and retrieving assignments, both as
 * individual entities and in paginated forms.
 */
@RestController
@RequestMapping("/api/v1/project-employee-assignment")
@Tag(name = "ProjectEmployeeAssignment (OneToOne)", description = "API for managing assignments between projects and employees")
public class ProjectEmployeeAssignmentController extends AbstractAssignmentController<
        ProjectEmployeeAssignment,
        Project,
        Employee,
        EmployeeRequestDto,
        EmployeeResponseDto,
        HierarchyRequestDto,
        HierarchyResponseDto> {

    /**
     * Constructor for ProjectEmployeeAssignmentController.
     *
     * @param assignmentService the service for managing project-employee assignments
     */
    public ProjectEmployeeAssignmentController(ProjectEmployeeAssignmentService assignmentService) {
        super(assignmentService);
    }

}
