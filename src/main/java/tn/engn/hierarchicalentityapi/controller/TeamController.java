package tn.engn.hierarchicalentityapi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Team;
import tn.engn.hierarchicalentityapi.service.TeamService;

/**
 * REST controller for managing teams.
 */
@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Team Hierarchy Management", description = "Endpoints for managing hierarchical team entities.")
public class TeamController extends HierarchyController<Team, HierarchyRequestDto, HierarchyResponseDto> {

    /**
     * Constructs the TeamController with the specified service.
     *
     * @param hierarchyService the service for managing team hierarchy
     */
    public TeamController(TeamService hierarchyService) {
        super(hierarchyService);
    }
}
