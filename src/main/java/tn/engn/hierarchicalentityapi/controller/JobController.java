package tn.engn.hierarchicalentityapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.hierarchicalentityapi.annotation.SubEntitiesPath;
import tn.engn.hierarchicalentityapi.service.JobService;

/**
 * REST controller for managing jobs.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@SubEntitiesPath("sub-jobs")
public class JobController extends HierarchyController{
    public JobController(JobService hierarchyService) {
        super(hierarchyService);
    }
}
