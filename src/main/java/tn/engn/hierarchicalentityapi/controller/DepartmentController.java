package tn.engn.hierarchicalentityapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.hierarchicalentityapi.annotation.SubEntitiesPath;
import tn.engn.hierarchicalentityapi.service.DepartmentService;

/**
 * REST controller for managing departments.
 */
@RestController
@RequestMapping("/api/v1/departments")
@SubEntitiesPath("sub-departments")
public class DepartmentController extends HierarchyController{
    public DepartmentController(DepartmentService hierarchyService) {
        super(hierarchyService);
    }
}
