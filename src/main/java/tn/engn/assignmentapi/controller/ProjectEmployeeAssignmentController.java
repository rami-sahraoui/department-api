package tn.engn.assignmentapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.engn.assignmentapi.dto.AssignmentMetadataRequestDto;
import tn.engn.assignmentapi.dto.AssignmentResponseDto;
import tn.engn.assignmentapi.dto.BulkAssignmentToHierarchicalEntityRequestDto;
import tn.engn.assignmentapi.dto.BulkAssignmentResponseDto;
import tn.engn.assignmentapi.model.ProjectEmployeeAssignment;
import tn.engn.assignmentapi.service.ProjectEmployeeAssignmentService;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.model.Project;

import java.util.List;

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
