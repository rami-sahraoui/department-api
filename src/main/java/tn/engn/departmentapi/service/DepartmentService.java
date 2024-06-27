package tn.engn.departmentapi.service;

import org.springframework.transaction.annotation.Transactional;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;

import java.util.List;

public interface DepartmentService {

    /**
     * Creates a new department based on the provided DTO.
     *
     * @param departmentRequestDto the DTO containing the new department's details
     * @return the created department as a response DTO
     */
    DepartmentResponseDto createDepartment(DepartmentRequestDto departmentRequestDto);

    /**
     * Updates an existing department based on the provided DTO.
     *
     * @param id                   the ID of the department to update
     * @param departmentRequestDto the DTO containing the updated department's details
     * @return the updated department as a response DTO
     */
    DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto departmentRequestDto);

    /**
     * Deletes a department by its ID.
     *
     * @param id the ID of the department to delete
     */
    void deleteDepartment(Long id);

    /**
     * Retrieves all departments.
     *
     * @return a list of all departments as response DTOs
     */
    List<DepartmentResponseDto> getAllDepartments();

    /**
     * Retrieves sub-departments (children) of a given parent department.
     *
     * @param parentId the ID of the parent department
     * @return a list of sub-departments as response DTOs
     */
    List<DepartmentResponseDto> getSubDepartments(Long parentId);

    /**
     * Retrieves a department by its ID.
     *
     * @param id the ID of the department to retrieve
     * @return the department with the specified ID as a response DTO
     */
    DepartmentResponseDto getDepartmentById(Long id);

    /**
     * Searches departments by name.
     *
     * @param name department name to search
     * @return list of departments matching the name
     */
    List<DepartmentResponseDto> searchDepartmentsByName(String name);

    /**
     * Retrieves the parent department of a given department.
     *
     * @param departmentId the ID of the department
     * @return the parent department as a response DTO
     */
    DepartmentResponseDto getParentDepartment(Long departmentId);

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all descendants as response DTOs
     */
    List<DepartmentResponseDto> getDescendants(Long departmentId);

    /**
     * Retrieves all ancestors (parent, grandparent, etc.) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all ancestors as response DTOs
     */
    List<DepartmentResponseDto> getAncestors(Long departmentId);
}
