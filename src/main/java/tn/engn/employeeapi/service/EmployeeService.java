package tn.engn.employeeapi.service;

import org.springframework.data.domain.Pageable;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;

import java.util.List;

/**
 * Service interface for managing Employee operations.
 */
public interface EmployeeService {

    /**
     * Creates a new employee.
     * @param employeeRequestDto the EmployeeRequestDto containing employee details
     * @return the EmployeeResponseDto of the created employee
     */
    EmployeeResponseDto createEmployee(EmployeeRequestDto employeeRequestDto);

    /**
     * Updates an existing employee.
     * @param id the employee ID
     * @param employeeRequestDto the EmployeeRequestDto containing updated employee details
     * @return the EmployeeResponseDto of the updated employee
     */
    EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto employeeRequestDto);

    /**
     * Deletes an employee by ID.
     * @param id the employee ID
     */
    void deleteEmployee(Long id);

    /**
     * Retrieves an employee by ID.
     * @param id the employee ID
     * @return the EmployeeResponseDto of the retrieved employee
     */
    EmployeeResponseDto getEmployeeById(Long id);

    /**
     * Retrieves all employees without pagination.
     * @return the list of EmployeeResponseDto
     */
    List<EmployeeResponseDto> getAllEmployees();

    /**
     * Retrieves all employees with pagination.
     * @param pageable the Pageable object containing pagination information
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    PaginatedResponseDto<EmployeeResponseDto> getAllEmployees(Pageable pageable);

    /**
     * Retrieves employees by first name without pagination.
     * @param firstName the first name of the employees
     * @return the list of EmployeeResponseDto
     */
    List<EmployeeResponseDto> getEmployeesByFirstName(String firstName);

    /**
     * Retrieves employees by first name with pagination.
     * @param firstName the first name of the employees
     * @param pageable the Pageable object containing pagination information
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    PaginatedResponseDto<EmployeeResponseDto> getEmployeesByFirstName(String firstName, Pageable pageable);

    /**
     * Retrieves employees by last name without pagination.
     * @param lastName the last name of the employees
     * @return the list of EmployeeResponseDto
     */
    List<EmployeeResponseDto> getEmployeesByLastName(String lastName);

    /**
     * Retrieves employees by last name with pagination.
     * @param lastName the last name of the employees
     * @param pageable the Pageable object containing pagination information
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    PaginatedResponseDto<EmployeeResponseDto> getEmployeesByLastName(String lastName, Pageable pageable);

    /**
     * Retrieves employees by email without pagination.
     * @param email the email of the employees
     * @return the list of EmployeeResponseDto
     */
    List<EmployeeResponseDto> getEmployeesByEmail(String email);

    /**
     * Retrieves employees by email with pagination.
     * @param email the email of the employees
     * @param pageable the Pageable object containing pagination information
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    PaginatedResponseDto<EmployeeResponseDto> getEmployeesByEmail(String email, Pageable pageable);
}
