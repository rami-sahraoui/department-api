package tn.engn.departmentapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.service.AdjacencyListDepartmentService;

import java.util.List;

/**
 * REST controller for managing departments.
 */
@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Department Management", description = "Endpoints for managing departments")
public class DepartmentController {

    private final AdjacencyListDepartmentService departmentService;

    @Autowired
    public DepartmentController(AdjacencyListDepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * Creates a new department.
     *
     * @param departmentRequestDto Request body containing details of the new department.
     * @return ResponseEntity with the created department and HTTP status 201 (Created).
     * Throws ValidationException if validation fails.
     * Throws ParentDepartmentNotFoundException if parent department is not found.
     */
    @PostMapping
    @Operation(summary = "Create a new department")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Department created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DepartmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request format or validation error"),
            @ApiResponse(responseCode = "404", description = "Parent department not found")
    })
    public ResponseEntity<DepartmentResponseDto> createDepartment(@Valid @RequestBody DepartmentRequestDto departmentRequestDto) {
        DepartmentResponseDto createdDepartment = departmentService.createDepartment(departmentRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDepartment);
    }

    /**
     * Updates an existing department.
     *
     * @param id                   Department ID to update.
     * @param departmentRequestDto Request body containing updated details of the department.
     * @return ResponseEntity with the updated department and HTTP status 200 (OK).
     * Throws ValidationException if validation fails.
     * Throws DepartmentNotFoundException if department is not found.
     * Throws ParentDepartmentNotFoundException if parent department is not found.
     * Throws DataIntegrityException if there is a data integrity issue preventing the update.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing department by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Department updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DepartmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request format or validation error"),
            @ApiResponse(responseCode = "404", description = "Department or parent department not found"),
            @ApiResponse(responseCode = "409", description = "Data integrity issue preventing update")
    })
    public ResponseEntity<DepartmentResponseDto> updateDepartment(@PathVariable("id") Long id,
                                                                  @Valid @RequestBody DepartmentRequestDto departmentRequestDto) {
        DepartmentResponseDto updatedDepartment = departmentService.updateDepartment(id, departmentRequestDto);
        return ResponseEntity.ok(updatedDepartment);
    }

    /**
     * Deletes a department by ID.
     *
     * @param id Department ID to delete.
     * @return ResponseEntity with HTTP status 204 (No Content) on successful deletion.
     * Throws DepartmentNotFoundException if department is not found.
     * Throws DataIntegrityException if there is a data integrity issue preventing the deletion.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletes a department by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Department deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Department not found"),
            @ApiResponse(responseCode = "409", description = "Data integrity issue preventing deletion")
    })
    public ResponseEntity<Void> deleteDepartment(@PathVariable("id") Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all departments.
     *
     * @return ResponseEntity with a list of all departments and HTTP status 200 (OK).
     */
    @GetMapping
    @Operation(summary = "Retrieve all departments")
    @ApiResponse(responseCode = "200", description = "List of all departments retrieved successfully")
    public ResponseEntity<List<DepartmentResponseDto>> getAllDepartments() {
        List<DepartmentResponseDto> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    /**
     * Retrieves a department by ID.
     *
     * @param id Department ID to retrieve.
     * @return ResponseEntity with the department and HTTP status 200 (OK).
     * Throws DepartmentNotFoundException if department is not found.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Retrieve department by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Department retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<DepartmentResponseDto> getDepartmentById(@PathVariable("id") Long id) {
        DepartmentResponseDto department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(department);
    }

    /**
     * Retrieves sub-departments (children) of a department by parent ID.
     *
     * @param parentId Parent department ID to retrieve sub-departments.
     * @return ResponseEntity with a list of sub-departments and HTTP status 200 (OK).
     * Throws ParentDepartmentNotFoundException if parent department is not found.
     */
    @GetMapping("/{id}/sub-departments")
    @Operation(summary = "Retrieve sub-departments by parent ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-departments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Parent department not found")
    })
    public ResponseEntity<List<DepartmentResponseDto>> getSubDepartments(@PathVariable("id") Long parentId) {
        List<DepartmentResponseDto> subDepartments = departmentService.getSubDepartments(parentId);
        return ResponseEntity.ok(subDepartments);
    }

    /**
     * Retrieves the parent department of a department by its ID.
     *
     * @param departmentId Department ID to retrieve parent department.
     * @return ResponseEntity with the parent department and HTTP status 200 (OK).
     * Throws DepartmentNotFoundException if department is not found.
     * Throws ParentDepartmentNotFoundException if parent department is not found.
     */
    @GetMapping("/{id}/parent-department")
    @Operation(summary = "Retrieve parent department by department ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parent department retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Department or parent department not found")
    })
    public ResponseEntity<DepartmentResponseDto> getParentDepartment(@PathVariable("id") Long departmentId) {
        DepartmentResponseDto parentDepartment = departmentService.getParentDepartment(departmentId);
        return ResponseEntity.ok(parentDepartment);
    }

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a department by its ID.
     *
     * @param departmentId Department ID to retrieve descendants.
     * @return ResponseEntity with a list of descendants and HTTP status 200 (OK).
     * Throws DepartmentNotFoundException if department is not found.
     */
    @GetMapping("/{id}/descendants")
    @Operation(summary = "Retrieve all descendants of a department by department ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Descendants retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<List<DepartmentResponseDto>> getDescendants(@PathVariable("id") Long departmentId) {
        List<DepartmentResponseDto> descendants = departmentService.getDescendants(departmentId);
        return ResponseEntity.ok(descendants);
    }

    /**
     * Retrieves all ancestors (parent departments recursively) of a department by its ID.
     *
     * @param departmentId Department ID to retrieve ancestors.
     * @return ResponseEntity with a list of ancestors and HTTP status 200 (OK).
     * Throws DepartmentNotFoundException if department is not found.
     */
    @GetMapping("/{id}/ancestors")
    @Operation(summary = "Retrieve all ancestors of a department by department ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ancestors retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<List<DepartmentResponseDto>> getAncestors(@PathVariable("id") Long departmentId) {
        List<DepartmentResponseDto> ancestors = departmentService.getAncestors(departmentId);
        return ResponseEntity.ok(ancestors);
    }
}
