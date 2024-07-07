package tn.engn.departmentapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.exception.DataIntegrityException;
import tn.engn.departmentapi.exception.DepartmentNotFoundException;
import tn.engn.departmentapi.exception.ParentDepartmentNotFoundException;
import tn.engn.departmentapi.exception.ValidationException;
import tn.engn.departmentapi.mapper.DepartmentMapper;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.model.DepartmentClosure;
import tn.engn.departmentapi.repository.DepartmentClosureRepository;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing departments using the Closure Table Model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClosureTableDepartmentService implements DepartmentService {
    private final DepartmentRepository departmentRepository;

    private final DepartmentClosureRepository departmentClosureRepository;

    private final DepartmentMapper departmentMapper;

    private final JPAQueryFactory jpaQueryFactory;

    // Setter for testing purposes
    @Setter
    @Value("${department.max-name-length}")
    private int maxNameLength;

    /**
     * Creates a new department based on the provided DTO.
     *
     * @param departmentRequestDto the DTO containing the new department's details
     * @return the created department as a response DTO
     * @throws ParentDepartmentNotFoundException if the parent department is not found
     * @throws ValidationException               if the input data is invalid
     */
    @Override
    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentRequestDto departmentRequestDto)
            throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {

        // Validate input data
        validateCreateDepartmentRequest(departmentRequestDto);

        // Map DTO to entity
        Department department = departmentMapper.toEntity(departmentRequestDto);

        // Save the new department
        Department savedDepartment = departmentRepository.save(department);

        // Create closure table entries
        createClosureEntries(savedDepartment);

        // Map saved entity to response DTO
        return departmentMapper.toDto(savedDepartment);
    }

    /**
     * Validates the input data for creating a new department.
     *
     * @param departmentRequestDto the DTO containing the new department's details
     * @throws ValidationException               if the input data is invalid
     * @throws ParentDepartmentNotFoundException if the parent department is not found
     */
    private void validateCreateDepartmentRequest(DepartmentRequestDto departmentRequestDto) throws ValidationException {
        String name = departmentRequestDto.getName();
        Long parentId = departmentRequestDto.getParentDepartmentId();

        // Validate the department name
        validateDepartmentName(name);

        // Find the parent department
        if (parentId != null) {
            Department parent = departmentRepository.findById(parentId)
                    .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found."));
        }
    }

    /**
     * Validates the department name for length constraints.
     *
     * @param name the department name to validate
     * @throws ValidationException if the department name is empty, null, or exceeds max length
     */
    private void validateDepartmentName(String name) throws ValidationException {
        if (name == null || name.isEmpty()) {
            throw new ValidationException("Department name cannot be null or empty.");
        }

        if (name.length() > maxNameLength) {
            throw new ValidationException("Department name cannot be longer than " + maxNameLength + " characters.");
        }
    }

    /**
     * Creates the closure table entries for the newly created department.
     *
     * @param department the newly created department entity
     * @throws ParentDepartmentNotFoundException if the parent department is not found
     */
    private void createClosureEntries(Department department)
            throws ParentDepartmentNotFoundException {

        Set<DepartmentClosure> closureEntries = new HashSet<>();

        // Add self-closure entry
        closureEntries.add(
                DepartmentClosure.builder()
                        .ancestorId(department.getId())
                        .descendantId(department.getId())
                        .level(0)
                        .build()
        );

        // If the department has a parent, add entries for all ancestors
        if (department.getParentDepartmentId() != null) {
            List<DepartmentClosure> parentClosures = departmentClosureRepository
                    .findByDescendantId(department.getParentDepartmentId());

            if (parentClosures.isEmpty()) {
                departmentRepository.delete(department);
                throw new ParentDepartmentNotFoundException("Parent department not found.");
            }

            for (DepartmentClosure parentClosure : parentClosures) {
                closureEntries.add(
                        DepartmentClosure.builder()
                                .ancestorId(parentClosure.getAncestorId())
                                .descendantId(department.getId())
                                .level(parentClosure.getLevel() + 1)
                                .build()
                );
            }
        }

        // Save all closure entries (sorted for testing reasons)
        departmentClosureRepository.saveAll(
                closureEntries.stream()
                        .sorted(
                                Comparator.comparingInt(DepartmentClosure::getLevel)
                        )
                        .collect(Collectors.toList())
        );
    }

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
    @Override
    @Transactional
    public DepartmentResponseDto updateDepartment(Long departmentId, DepartmentRequestDto updatedDepartmentDto)
            throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {

        log.info("Updating department with ID: {}", departmentId);

        // Validate input data and get existing department
        Department existingDepartment = validateUpdateDepartmentRequest(departmentId, updatedDepartmentDto);

        // Check if the parent ID or name has been modified
        boolean isParentModified = !Objects.equals(existingDepartment.getParentDepartmentId(), updatedDepartmentDto.getParentDepartmentId());
        boolean isNameModified = !Objects.equals(existingDepartment.getName(), updatedDepartmentDto.getName());

        if (!isParentModified && !isNameModified) {
            log.info("No changes detected for department with ID: {}", departmentId);
            return departmentMapper.toDto(existingDepartment);
        }

        // If parent is modified, check for circular dependency and update closure entries
        if (isParentModified) {
            Long newParentId = updatedDepartmentDto.getParentDepartmentId();
            if (newParentId != null && hasCircularDependency(departmentId, newParentId)) {
                throw new DataIntegrityException("Circular dependency detected: Department cannot be its own ancestor.");
            }
            updateClosureEntries(departmentId, newParentId);
            existingDepartment.setParentDepartmentId(newParentId);
        }

        // If name is modified, update department name
        if (isNameModified) {
            existingDepartment.setName(updatedDepartmentDto.getName());
        }

        departmentRepository.save(existingDepartment);
        log.info("Successfully updated department with ID: {}", departmentId);

        return departmentMapper.toDto(existingDepartment);
    }

    /**
     * Checks if updating a department to have the given new parent would create a circular dependency.
     *
     * @param departmentId the ID of the department being updated
     * @param newParentId  the ID of the new parent department
     * @return true if a circular dependency is detected, false otherwise
     */
    private boolean hasCircularDependency(Long departmentId, Long newParentId) {
        List<DepartmentClosure> ancestors = departmentClosureRepository.findByDescendantId(newParentId);
        return ancestors.stream().anyMatch(closure -> closure.getAncestorId().equals(departmentId));
    }

    /**
     * Updates the closure table entries for the updated department and its descendants.
     *
     * @param departmentId       the updated department ID
     * @param parentDepartmentId the updated department parent ID
     * @throws ParentDepartmentNotFoundException if the parent department closure entries are not found
     */
    private void updateClosureEntries(Long departmentId, Long parentDepartmentId) {
        log.info("Updating closure entries for department ID: {} with new parent ID: {}", departmentId, parentDepartmentId);

        // Get the descendants of the department
        List<DepartmentClosure> descendants = departmentClosureRepository.findByAncestorId(departmentId);

        // Remove old closure records for the department and its descendants
        for (DepartmentClosure descendant : descendants) {
            departmentClosureRepository.deleteByDescendantId(descendant.getDescendantId());
        }

        Set<DepartmentClosure> closureEntries = new HashSet<>();

        // Add closure entries for the department and its descendants
        for (DepartmentClosure descendant : descendants) {
            closureEntries.add(
                    DepartmentClosure.builder()
                            .ancestorId(departmentId)
                            .descendantId(descendant.getDescendantId())
                            .level(descendant.getLevel())
                            .build()
            );

            // Adding self-closure for descendants
            closureEntries.add(
                    DepartmentClosure.builder()
                            .ancestorId(descendant.getDescendantId())
                            .descendantId(descendant.getDescendantId())
                            .level(0)
                            .build()
            );
        }

        // If the department has a new parent, add entries for all ancestors
        if (parentDepartmentId != null) {
            List<DepartmentClosure> parentClosures = departmentClosureRepository.findByDescendantId(parentDepartmentId);

            if (parentClosures.isEmpty()) {
                throw new ParentDepartmentNotFoundException("Parent department not found.");
            }

            for (DepartmentClosure parentClosure : parentClosures) {
                for (DepartmentClosure descendant : descendants) {
                    closureEntries.add(
                            DepartmentClosure.builder()
                                    .ancestorId(parentClosure.getAncestorId())
                                    .descendantId(descendant.getDescendantId())
                                    .level(parentClosure.getLevel() + 1 + descendant.getLevel())
                                    .build()
                    );
                }

                // Adding closure for the department itself
                closureEntries.add(
                        DepartmentClosure.builder()
                                .ancestorId(parentClosure.getAncestorId())
                                .descendantId(departmentId)
                                .level(parentClosure.getLevel() + 1)
                                .build()
                );
            }
        }

        // Adding self-closure for the department
        closureEntries.add(
                DepartmentClosure.builder()
                        .ancestorId(departmentId)
                        .descendantId(departmentId)
                        .level(0)
                        .build()
        );

        // Save all closure entries (sorted for testing reasons)
        departmentClosureRepository.saveAll(
                closureEntries.stream()
                        .sorted(Comparator.comparingInt(DepartmentClosure::getLevel)
                                .thenComparingLong(DepartmentClosure::getAncestorId))
                        .collect(Collectors.toList())
        );

        log.info("Closure entries updated for department ID: {}", departmentId);
    }

    /**
     * Validates the input data for updating an existing department.
     *
     * @param departmentId         the ID of the department to be updated
     * @param updatedDepartmentDto the DTO containing the new department's details
     * @return the existing department entity
     * @throws ValidationException               if the input data is invalid
     * @throws DepartmentNotFoundException       if the department with given ID is not found
     * @throws ParentDepartmentNotFoundException if the parent department is not found
     */
    private Department validateUpdateDepartmentRequest(Long departmentId, DepartmentRequestDto updatedDepartmentDto) throws ValidationException {
        String name = updatedDepartmentDto.getName();

        // Validate the department name
        validateDepartmentName(name);

        // Find the existing department
        Department existingDepartment = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found."));

        // Check if the parent department exists if a parent ID is provided
        Long parentDepartmentId = updatedDepartmentDto.getParentDepartmentId();
        if (parentDepartmentId != null) {
            departmentRepository.findById(parentDepartmentId)
                    .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found."));
        }

        return existingDepartment;
    }

    /**
     * Deletes a department by its ID.
     *
     * @param id the ID of the department to delete
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     * @throws DataIntegrityException      if deletion would result in a circular dependency or if other constraints are violated
     */
    @Transactional()
    public void deleteDepartment(Long id) throws DepartmentNotFoundException, DataIntegrityException {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));

        // Check for circular dependencies before deleting
        if (department.getParentDepartmentId() != null && hasCircularDependencyToDelete(id, department.getParentDepartmentId())) {
            throw new DataIntegrityException("Circular dependency detected: Department cannot be its own ancestor.");
        }

        // Recursively delete sub-departments
        deleteSubDepartments(id);

        // Deleting department closure entries
        departmentClosureRepository.deleteByAncestorId(id);
        departmentClosureRepository.deleteByDescendantId(id);

        // Deleting the department itself
        departmentRepository.delete(department);
    }

    /**
     * Checks if deleting the department would result in a circular dependency.
     *
     * @param id the ID of the department to check
     * @return true if deleting the department would create a circular dependency, false otherwise
     */
    private boolean hasCircularDependencyToDelete(Long id, Long newParentId) {
        if (id.equals(newParentId)) {
            return true;
        }

        List<DepartmentClosure> descendantClosures = departmentClosureRepository.findByDescendantId(newParentId);
        return descendantClosures.stream()
                .anyMatch(dc -> dc.getAncestorId().equals(id));
    }

    /**
     * Deletes all sub-departments and their closure entries recursively.
     *
     * @param parentId the ID of the department whose sub-departments are to be deleted
     */
    @Transactional
    public void deleteSubDepartments(Long parentId) throws DepartmentNotFoundException, DataIntegrityException {
        List<DepartmentClosure> subDepartments = departmentClosureRepository.findByAncestorId(parentId);

        // Sort sub-departments by level in descending order to delete leaf nodes first
        subDepartments.sort(Comparator.comparingInt(DepartmentClosure::getLevel).reversed());

        for (DepartmentClosure closure : subDepartments) {
            if (!closure.getDescendantId().equals(parentId)) {
                deleteDepartment(closure.getDescendantId());
            }
        }
    }

    /**
     * Retrieves all departments.
     *
     * @return a list of all departments as response DTOs
     */
    @Override
    public List<DepartmentResponseDto> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departments.stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves sub-departments (children) of a given parent department.
     *
     * @param parentId the ID of the parent department
     * @return a list of sub-departments as response DTOs
     * @throws ParentDepartmentNotFoundException if the parent department with the given ID is not found
     */
    @Override
    public List<DepartmentResponseDto> getSubDepartments(Long parentId) throws ParentDepartmentNotFoundException {
        departmentRepository.findById(parentId)
                .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with id: " + parentId));

        List<Department> subDepartments = departmentRepository.findByParentDepartmentId(parentId);
        return subDepartments.stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a department by its ID.
     *
     * @param id the ID of the department to retrieve
     * @return the department with the specified ID as a response DTO
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     */
    @Override
    public DepartmentResponseDto getDepartmentById(Long id) throws DepartmentNotFoundException {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));

        return departmentMapper.toDto(department);
    }

    /**
     * Searches departments by name.
     *
     * @param name department name to search
     * @return list of departments matching the name
     */
    @Override
    public List<DepartmentResponseDto> searchDepartmentsByName(String name) {
        List<Department> departments = departmentRepository.findByNameContainingIgnoreCase(name);
        return departments.stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
    }


    /**
     * Retrieves the parent department of a given department.
     *
     * @param departmentId the ID of the department
     * @return the parent department as a response DTO
     * @throws DepartmentNotFoundException       if the department with the given ID is not found
     * @throws ParentDepartmentNotFoundException if the department has no parent
     */
    @Override
    public DepartmentResponseDto getParentDepartment(Long departmentId) throws DepartmentNotFoundException, ParentDepartmentNotFoundException {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));

        Long parentDepartmentId = department.getParentDepartmentId();
        if (parentDepartmentId == null) {
            throw new ParentDepartmentNotFoundException("Department has no parent.");
        }

        Department parentDepartment = departmentRepository.findById(parentDepartmentId)
                .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with id: " + parentDepartmentId));

        return departmentMapper.toDto(parentDepartment);
    }


    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all descendants as response DTOs
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     */
    @Override
    public List<DepartmentResponseDto> getDescendants(Long departmentId) throws DepartmentNotFoundException {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));

        List<Department> descendants = departmentClosureRepository.findByAncestorId(departmentId).stream()
                .map(DepartmentClosure::getDescendantId)
                .filter(id -> !id.equals(departmentId)) // Exclude self
                .map(id -> departmentRepository.findById(id)
                        .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id)))
                .collect(Collectors.toList());

        return descendants.stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all ancestors (parent departments recursively) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all ancestors as response DTOs
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     */
    @Override
    public List<DepartmentResponseDto> getAncestors(Long departmentId) throws DepartmentNotFoundException {
        // Check if the department exists
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));

        // Retrieve ancestors using the closure table, excluding self-closure
        List<DepartmentClosure> ancestorsClosures = departmentClosureRepository.findByDescendantId(departmentId);

        // Map to DepartmentResponseDto
        return ancestorsClosures.stream()
                .filter(closure -> !closure.getAncestorId().equals(departmentId))  // Exclude self-closure
                .map(closure -> departmentMapper.toDto(departmentRepository.findById(closure.getAncestorId())
                        .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found."))))  // Fetch Department and map to DTO
                .collect(Collectors.toList());
    }
}
