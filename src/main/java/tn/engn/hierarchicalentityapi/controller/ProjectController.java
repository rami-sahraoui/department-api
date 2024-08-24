package tn.engn.hierarchicalentityapi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.hierarchicalentityapi.service.ProjectService;

/**
 * REST controller for managing projects.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Hierarchy Management", description = "Endpoints for managing hierarchical project entities.")
public class ProjectController extends HierarchyController {

    /**
     * Constructs the ProjectController with the specified service.
     *
     * @param hierarchyService the service for managing project hierarchy
     */
    public ProjectController(ProjectService hierarchyService) {
        super(hierarchyService);
    }
}
