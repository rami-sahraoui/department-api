package tn.engn.departmentapi.service;

import org.springframework.transaction.annotation.Transactional;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.exception.DataIntegrityException;
import tn.engn.departmentapi.exception.DepartmentNotFoundException;
import tn.engn.departmentapi.exception.ParentDepartmentNotFoundException;
import tn.engn.departmentapi.exception.ValidationException;

import java.util.List;

public interface DepartmentService {

    /**
     * Creates a new department based on the provided DTO.
     *
     * @param departmentRequestDto the DTO containing the new department's details
     * @return the created department as a response DTO
     * @throws ParentDepartmentNotFoundException if the parent department is not found
     * @throws ValidationException               if the input data is invalid
     * @throws DataIntegrityException            if there is a data integrity violation
     */
    DepartmentResponseDto createDepartment(DepartmentRequestDto departmentRequestDto)
            throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException;

    /**
     * Updates an existing department's name and optionally changes its parent department.
     *
     * @param departmentId         the ID of the department to update
     * @param updatedDepartmentDto the DTO containing updated department information
     * @return the updated department DTO
     * @throws DepartmentNotFoundException       if the department with given ID is not found
     * @throws ParentDepartmentNotFoundException if the parent department with given ID is not found
     * @throws DataIntegrityException            if updating the department violates data integrity constraints
     * @throws ValidationException               if the updated department name is invalid (empty, null, or exceeds max length)
     */
    @Transactional
    DepartmentResponseDto updateDepartment(Long departmentId, DepartmentRequestDto updatedDepartmentDto)
            throws DepartmentNotFoundException, ParentDepartmentNotFoundException, DataIntegrityException, ValidationException;

    /**
     * Deletes a department by its ID.
     *
     * @param id the ID of the department to delete
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     * @throws DataIntegrityException      if deletion would result in a circular dependency or if other constraints are violated
     */
    @Transactional
    void deleteDepartment(Long id) throws DepartmentNotFoundException, DataIntegrityException;

    /**
     * Retrieves all departments.
     *
     * @return a list of all departments as response DTOs
     */
    @Transactional(readOnly = true)
    List<DepartmentResponseDto> getAllDepartments();

    /**
     * Retrieves sub-departments (children) of a given parent department.
     *
     * @param parentId the ID of the parent department
     * @return a list of sub-departments as response DTOs
     * @throws ParentDepartmentNotFoundException if the parent department with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<DepartmentResponseDto> getSubDepartments(Long parentId) throws ParentDepartmentNotFoundException;

    /**
     * Retrieves a department by its ID.
     *
     * @param id the ID of the department to retrieve
     * @return the department with the specified ID as a response DTO
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     */
    @Transactional(readOnly = true)
    DepartmentResponseDto getDepartmentById(Long id) throws DepartmentNotFoundException;

    /**
     * Searches departments by name.
     *
     * @param name department name to search
     * @return list of departments matching the name
     */
    @Transactional(readOnly = true)
    List<DepartmentResponseDto> searchDepartmentsByName(String name);

    /**
     * Retrieves the parent department of a given department.
     *
     * @param departmentId the ID of the department
     * @return the parent department as a response DTO
     * @throws DepartmentNotFoundException       if the department with the given ID is not found
     * @throws ParentDepartmentNotFoundException if the department has no parent
     */
    @Transactional(readOnly = true)
    DepartmentResponseDto getParentDepartment(Long departmentId) throws DepartmentNotFoundException, ParentDepartmentNotFoundException;

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all descendants as response DTOs
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<DepartmentResponseDto> getDescendants(Long departmentId) throws DepartmentNotFoundException;

    /**
     * Retrieves all ancestors (parent departments recursively) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all ancestors as response DTOs
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     */
    @Transactional(readOnly = true)
    List<DepartmentResponseDto> getAncestors(Long departmentId) throws DepartmentNotFoundException;
}
