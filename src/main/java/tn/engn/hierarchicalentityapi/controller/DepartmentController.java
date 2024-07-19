package tn.engn.hierarchicalentityapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.engn.hierarchicalentityapi.annotation.SubEntitiesPath;
import tn.engn.hierarchicalentityapi.dto.DepartmentRequestDto;
import tn.engn.hierarchicalentityapi.dto.DepartmentResponseDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.service.DepartmentService;
import tn.engn.hierarchicalentityapi.service.HierarchyService;

import java.util.List;

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
