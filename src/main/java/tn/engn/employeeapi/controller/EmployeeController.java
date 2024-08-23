package tn.engn.employeeapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.employeeapi.service.EmployeeService;

import java.util.List;

/**
 * REST controller for managing Employee entities.
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "Endpoints for managing employees.")
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Creates a new employee.
     * @param employeeRequestDto the EmployeeRequestDto containing employee details
     * @return the ResponseEntity containing the EmployeeResponseDto of the created employee
     */
    @PostMapping
    @Operation(summary = "Create a new employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee created", content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    public ResponseEntity<EmployeeResponseDto> createEmployee(@Valid @RequestBody EmployeeRequestDto employeeRequestDto) {
        EmployeeResponseDto employeeResponseDto = employeeService.createEmployee(employeeRequestDto);
        return new ResponseEntity<>(employeeResponseDto, HttpStatus.CREATED);
    }

    /**
     * Updates an existing employee.
     * @param id the employee ID
     * @param employeeRequestDto the EmployeeRequestDto containing updated employee details
     * @return the ResponseEntity containing the EmployeeResponseDto of the updated employee
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee updated", content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content)
    })
    public ResponseEntity<EmployeeResponseDto> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequestDto employeeRequestDto) {
        EmployeeResponseDto employeeResponseDto = employeeService.updateEmployee(id, employeeRequestDto);
        return ResponseEntity.ok(employeeResponseDto);
    }

    /**
     * Deletes an employee by ID.
     * @param id the employee ID
     * @return the ResponseEntity with HTTP status 204 (No Content)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Employee deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content)
    })
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves an employee by ID.
     * @param id the employee ID
     * @return the ResponseEntity containing the EmployeeResponseDto of the retrieved employee
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee found", content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content)
    })
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(@PathVariable Long id) {
        EmployeeResponseDto employeeResponseDto = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employeeResponseDto);
    }

    /**
     * Retrieves all employees without pagination.
     * @return the ResponseEntity containing the list of EmployeeResponseDto
     */
    @GetMapping
    @Operation(summary = "Get all employees")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employees retrieved", content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class)))
    })
    public ResponseEntity<List<EmployeeResponseDto>> getAllEmployees() {
        List<EmployeeResponseDto> employeeResponseDtoList = employeeService.getAllEmployees();
        return ResponseEntity.ok(employeeResponseDtoList);
    }

    /**
     * Retrieves all employees with pagination.
     * @param pageable the Pageable object containing pagination information
     * @return the ResponseEntity containing the PaginatedResponseDto with the list of EmployeeResponseDto
     */
    @GetMapping("/paginated")
    @Operation(summary = "Get all employees with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employees retrieved", content = @Content(schema = @Schema(implementation = PaginatedResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<EmployeeResponseDto>> getAllEmployees(Pageable pageable) {
        PaginatedResponseDto<EmployeeResponseDto> employeeResponseDtoPage = employeeService.getAllEmployees(pageable);
        return ResponseEntity.ok(employeeResponseDtoPage);
    }

    /**
     * Retrieves employees by first name without pagination.
     * @param firstName the first name of the employees
     * @return the ResponseEntity containing the list of EmployeeResponseDto
     */
    @GetMapping("/first-name")
    @Operation(summary = "Get employees by first name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employees retrieved", content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class)))
    })
    public ResponseEntity<List<EmployeeResponseDto>> getEmployeesByFirstName(@RequestParam String firstName) {
        List<EmployeeResponseDto> employeeResponseDtoList = employeeService.getEmployeesByFirstName(firstName);
        return ResponseEntity.ok(employeeResponseDtoList);
    }

    /**
     * Retrieves employees by first name with pagination.
     * @param firstName the first name of the employees
     * @param pageable the Pageable object containing pagination information
     * @return the ResponseEntity containing the PaginatedResponseDto with the list of EmployeeResponseDto
     */
    @GetMapping("/first-name/paginated")
    @Operation(summary = "Get employees by first name with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employees retrieved", content = @Content(schema = @Schema(implementation = PaginatedResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<EmployeeResponseDto>> getEmployeesByFirstName(@RequestParam String firstName, Pageable pageable) {
        PaginatedResponseDto<EmployeeResponseDto> employeeResponseDtoPage = employeeService.getEmployeesByFirstName(firstName, pageable);
        return ResponseEntity.ok(employeeResponseDtoPage);
    }

    /**
     * Retrieves employees by last name without pagination.
     * @param lastName the last name of the employees
     * @return the ResponseEntity containing the list of EmployeeResponseDto
     */
    @GetMapping("/last-name")
    @Operation(summary = "Get employees by last name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employees retrieved", content = @Content(schema = @Schema(implementation = EmployeeResponseDto.class)))
    })
    public ResponseEntity<List<EmployeeResponseDto>> getEmployeesByLastName(@RequestParam String lastName) {
        List<EmployeeResponseDto> employeeResponseDtoList = employeeService.getEmployeesByLastName(lastName);
        return ResponseEntity.ok(employeeResponseDtoList);
    }

    /**
     * Retrieves employees by last name with pagination.
     * @param lastName the last name of the employees
     * @param pageable the Pageable object containing pagination information
     * @return the ResponseEntity containing the PaginatedResponseDto with the list of EmployeeResponseDto
     */
    @GetMapping("/last-name/paginated")
    @Operation(summary = "Get employees by last name with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employees retrieved", content = @Content(schema = @Schema(implementation = PaginatedResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<EmployeeResponseDto>> getEmployeesByLastName(@RequestParam String lastName, Pageable pageable) {
        PaginatedResponseDto<EmployeeResponseDto> employeeResponseDtoPage = employeeService.getEmployeesByLastName(lastName, pageable);
        return ResponseEntity.ok(employeeResponseDtoPage);
    }
}
