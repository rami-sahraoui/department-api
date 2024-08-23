package tn.engn.hierarchicalentityapi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Project;
import tn.engn.hierarchicalentityapi.service.ProjectService;

/**
 * REST controller for managing projects.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Hierarchy Management", description = "Endpoints for managing hierarchical project entities.")
public class ProjectController extends HierarchyController<Project, HierarchyRequestDto, HierarchyResponseDto> {

    /**
     * Constructs the ProjectController with the specified service.
     *
     * @param hierarchyService the service for managing project hierarchy
     */
    public ProjectController(ProjectService hierarchyService) {
        super(hierarchyService);
    }
}
