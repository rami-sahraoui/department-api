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
import tn.engn.assignmentapi.model.TeamManagerAssignment;
import tn.engn.assignmentapi.service.TeamManagerAssignmentService;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.model.Team;

import java.util.List;

/**
 * Controller for managing assignments between teams and managers.
 * This controller provides endpoints for assigning managers to teams,
 * removing assignments, bulk operations, and retrieving assignments, both as
 * individual entities and in paginated forms.
 */
@RestController
@RequestMapping("/api/v1/team-manager-assignment")
@Tag(name = "TeamManagerAssignment (ManyToOne)", description = "API for managing assignments between teams and managers")
public class TeamManagerAssignmentController extends AbstractAssignmentController<
        TeamManagerAssignment,
        Team,
        Employee,
        EmployeeRequestDto,
        EmployeeResponseDto,
        HierarchyRequestDto,
        HierarchyResponseDto> {

    /**
     * Constructor for TeamManagerAssignmentController.
     *
     * @param assignmentService the service for managing team-manager assignments
     */
    public TeamManagerAssignmentController(TeamManagerAssignmentService assignmentService) {
        super(assignmentService);
    }

}
