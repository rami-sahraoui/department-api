package tn.engn.assignmentapi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.assignmentapi.model.TeamManagerAssignment;
import tn.engn.assignmentapi.service.TeamManagerAssignmentService;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Team;

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
