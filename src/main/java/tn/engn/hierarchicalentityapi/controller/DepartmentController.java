package tn.engn.hierarchicalentityapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.hierarchicalentityapi.annotation.SubEntitiesPath;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.service.HierarchyService;

/**
 * REST controller for managing departments.
 */
@RestController
@RequestMapping("/api/v1/departments")
@SubEntitiesPath("sub-departments")
public class DepartmentController extends HierarchyController<Department, HierarchyRequestDto, HierarchyResponseDto> {
    public DepartmentController(HierarchyService<Department, HierarchyRequestDto, HierarchyResponseDto> hierarchyService) {
        super(hierarchyService);
    }
}
