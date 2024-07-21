package tn.engn.hierarchicalentityapi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.hierarchicalentityapi.annotation.SubEntitiesPath;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Job;
import tn.engn.hierarchicalentityapi.service.HierarchyService;

/**
 * REST controller for managing jobs.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@SubEntitiesPath("sub-jobs")
@Tag(name = "Job Hierarchy Management", description = "Endpoints for managing hierarchical job entities.")
public class JobController extends HierarchyController<Job, HierarchyRequestDto, HierarchyResponseDto> {
    public JobController(HierarchyService<Job, HierarchyRequestDto, HierarchyResponseDto> hierarchyService) {
        super(hierarchyService);
    }
}
