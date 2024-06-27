package tn.engn.departmentapi.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.exception.DataIntegrityException;
import tn.engn.departmentapi.exception.DepartmentNotFoundException;
import tn.engn.departmentapi.exception.ParentDepartmentNotFoundException;
import tn.engn.departmentapi.exception.ValidationException;
import tn.engn.departmentapi.mapper.DepartmentMapper;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for managing departments using the adjacency list model.
 */
@Service
@Slf4j // Lombok annotation for logging
public class AdjacencyListDepartmentService implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Value("${department.max-name-length}")
    @Getter
    @Setter
    private int maxNameLength;

    /**
     * Creates a new department based on the provided DTO.
     *
     * @param departmentRequestDto the DTO containing the new department's details
     * @return the created department as a response DTO
     * @throws ValidationException if the department name is empty, null, or exceeds the maximum allowed length
     * @throws ParentDepartmentNotFoundException if the specified parent department with the given ID is not found
     */
    @Override
    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentRequestDto departmentRequestDto) {
        String name = departmentRequestDto.getName();
        Long parentId = departmentRequestDto.getParentDepartmentId();

        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Department name must not be empty");
        }

        if (name.length() > getMaxNameLength()) {
            throw new ValidationException("Department name exceeds maximum allowed length of " + getMaxNameLength() + " characters");
        }

        log.info("Creating department with name: {} and parentId: {}", name, parentId);
        Department department = new Department();
        department.setName(name);

        if (parentId != null) {
            // Retrieve the parent department if parentId is provided
            Department parentDepartment = departmentRepository.findById(parentId)
                    .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with id: " + parentId));

            // Establish bidirectional relationship: set child department and add to parent's sub-departments
            parentDepartment.addSubDepartment(department);
        }

        // Save and return the created department converted to response DTO
        department = departmentRepository.save(department);
        return departmentMapper.toDto(department);
    }

    /**
     * Updates an existing department based on the provided DTO.
     *
     * @param id                   the ID of the department to update
     * @param departmentRequestDto the DTO containing the updated department's details
     * @return the updated department as a response DTO
     * @throws ValidationException if the department name is empty, null, or exceeds the maximum allowed length
     * @throws DepartmentNotFoundException  if the department with the given ID is not found
     * @throws ParentDepartmentNotFoundException  if the specified parent department with the given ID is not found
     * @throws DataIntegrityException if setting the new parent would create a circular dependency
     */
    @Override
    @Transactional
    public DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto departmentRequestDto) {
        String name = departmentRequestDto.getName();
        Long parentId = departmentRequestDto.getParentDepartmentId();

        log.info("Updating department with id: {}, name: {} and parentId: {}", id, name, parentId);

        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Department name must not be empty");
        }

        if (name.length() > getMaxNameLength()) {
            throw new ValidationException("Department name exceeds maximum allowed length of " + getMaxNameLength() + " characters");
        }

        // Retrieve the department to update
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));

        // Check if the department's name needs updating
        if (!department.getName().equals(name)) {
            department.setName(name);
        }

        // Check if the parent department needs updating
        if (parentId != null) {
            Department newParent = departmentRepository.findById(parentId)
                    .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with id: " + parentId));

            // Check for circular dependency
            if (hasCircularDependency(department, newParent)) {
                throw new DataIntegrityException("Circular dependency detected.");
            }

            // Avoid unnecessary updates
            if (department.getParentDepartment() == null || !department.getParentDepartment().equals(newParent)) {
                // Remove from current parent's sub-departments if exists
                if (department.getParentDepartment() != null) {
                    department.getParentDepartment().removeSubDepartment(department);
                }

                // Update parent and add to new parent's sub-departments
                department.setParentDepartment(newParent);
                newParent.addSubDepartment(department);
            }
        } else {
            // Case where parent ID is null, meaning no parent should be assigned
            if (department.getParentDepartment() != null) {
                department.getParentDepartment().removeSubDepartment(department);
                department.setParentDepartment(null);
            }
        }

        // Save and return the updated department converted to response DTO
        return departmentMapper.toDto(departmentRepository.save(department));
    }

    /**
     * Deletes a department by its ID.
     *
     * @param id the ID of the department to delete
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     * @throws DataIntegrityException if deletion would result in a circular dependency or if other constraints are violated
     */
    @Transactional
    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));

        // Check for circular dependencies before deletion
        if (hasCircularDependencyOnDelete(department)) {
            throw new DataIntegrityException("Deleting this department would create a circular dependency.");
        }

        // Handle sub-departments deletion or reassignment
        if (!department.getSubDepartments().isEmpty()) {
            handleSubDepartmentsOnDelete(department);
        }

        Department parent = department.getParentDepartment();
        if (parent != null) {
            parent.removeSubDepartment(department);
        }

        departmentRepository.delete(department);
    }

    /**
     * Helper method to handle deletion or reassignment of sub-departments when deleting a department.
     *
     * @param department the department being deleted
     */
    protected void handleSubDepartmentsOnDelete(Department department) {
        // Get a copy of sub-departments to avoid concurrent modification issues
        List<Department> subDepartments = new ArrayList<>(department.getSubDepartments());

        // Remove the department from each sub-department's parent reference
        for (Department subDept : subDepartments) {
            subDept.setParentDepartment(null);
        }

        // Clear the sub-departments list from the parent department
        department.getSubDepartments().clear();

        // Delete sub-departments from the database
        for (Department subDept : subDepartments) {
            departmentRepository.delete(subDept);
        }
    }


    /**
     * Retrieves all departments.
     *
     * @return a list of all departments as response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {
        log.info("Retrieving all departments");
        // Fetch and return all departments from the repository, converted to response DTOs
        List<Department> departments = departmentRepository.findAll();
        return departmentMapper.toDtoList(departments);
    }

    /**
     * Retrieves sub-departments (children) of a given parent department.
     *
     * @param parentId the ID of the parent department
     * @return a list of sub-departments as response DTOs
     * @throws ParentDepartmentNotFoundException if the parent department with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getSubDepartments(Long parentId) {
        log.info("Retrieving sub-departments for parent department with id: {}", parentId);
        // Retrieve the parent department by ID
        Department parentDepartment = departmentRepository.findById(parentId)
                .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with id: " + parentId));

        // Initialize the collection to avoid LazyInitializationException
        if (!Hibernate.isInitialized(parentDepartment.getSubDepartments())) {
            Hibernate.initialize(parentDepartment.getSubDepartments());
        }

        if (parentDepartment.getSubDepartments() == null) {
            parentDepartment.setSubDepartments(new ArrayList<>()); // Initialize if null
        }

        int size = parentDepartment.getSubDepartments().size();

        // Convert sub-departments to response DTOs and return
        return departmentMapper.toDtoList(parentDepartment.getSubDepartments());
    }

    /**
     * Retrieves a department by its ID.
     *
     * @param id the ID of the department to retrieve
     * @return the department with the specified ID as a response DTO
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public DepartmentResponseDto getDepartmentById(Long id) {
        log.info("Retrieving department with id: {}", id);
        // Retrieve the department by ID
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + id));

        // Convert the department to a response DTO and return
        return departmentMapper.toDto(department);
    }

    /**
     * Searches departments by name.
     *
     * @param name department name to search
     * @return list of departments matching the name
     */
    @Override
    @Transactional(readOnly = true)
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
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     * @throws ParentDepartmentNotFoundException if the department has no parent
     */
    @Override
    @Transactional(readOnly = true)
    public DepartmentResponseDto getParentDepartment(Long departmentId) {
        log.info("Retrieving parent department for department with id: {}", departmentId);
        // Retrieve the department by ID
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));

        // Retrieve the parent department
        Department parentDepartment = department.getParentDepartment();
        if (parentDepartment == null) {
            throw new ParentDepartmentNotFoundException("Department with id: " + departmentId + " has no parent department");
        }

        // Convert the parent department to a response DTO and return
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
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getDescendants(Long departmentId) {
        log.info("Retrieving all descendants for department with id: {}", departmentId);
        // Retrieve the department by ID
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));

        // Fetch all descendants within the same transaction
        List<Department> descendants = getAllDescendants(department);

        // Convert descendants to response DTOs and return
        return departmentMapper.toDtoList(descendants);
    }

    /**
     * Retrieves all ancestors (parent departments recursively) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all ancestors as response DTOs
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAncestors(Long departmentId) {
        log.info("Retrieving all ancestors for department with id: {}", departmentId);
        // Retrieve the department by ID
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with id: " + departmentId));

        // Fetch all ancestors within the same transaction
        List<Department> ancestors = getAllAncestors(department);

        // Convert ancestors to response DTOs and return
        return ancestors.isEmpty() ? new ArrayList<>() : departmentMapper.toDtoList(ancestors);
    }

    /**
     * Helper method to recursively fetch all descendants of a department.
     *
     * @param department the department whose descendants are to be fetched
     * @return a list of all descendants
     */
    protected List<Department> getAllDescendants(Department department) {
        List<Department> descendants = new ArrayList<>();
        List<Department> subDepartments = department.getSubDepartments();

        // Check if subDepartments is null or empty to avoid NPE
        if (subDepartments != null && !subDepartments.isEmpty()) {
            descendants.addAll(subDepartments);
            for (Department subDepartment : subDepartments) {
                descendants.addAll(getAllDescendants(subDepartment));
            }
        }

        return descendants;
    }

    /**
     * Helper method to recursively fetch all ancestors of a department.
     *
     * @param department the department whose ancestors are to be fetched
     * @return a list of all ancestors
     */
    private List<Department> getAllAncestors(Department department) {
        List<Department> ancestors = new ArrayList<>();
        Department parentDepartment = department.getParentDepartment();
        if (parentDepartment != null) {
            ancestors.add(parentDepartment);
            ancestors.addAll(getAllAncestors(parentDepartment));
        }
        return ancestors;
    }

    /**
     * Helper method to check if a department is among the descendants of another department.
     *
     * @param department the department whose descendants are to be checked
     * @param target the department to look for among the descendants
     * @return true if the target department is a descendant, false otherwise
     */
    private boolean isDescendant(Department department, Department target) {
        // Use a set to track visited departments to avoid revisiting
        Set<Long> visited = new HashSet<>();
        return isDescendantRecursive(department, target, visited);
    }

    /**
     * Recursive helper method to check for descendants.
     *
     * @param current the current department being checked
     * @param target the department to look for among the descendants
     * @param visited set of visited departments to prevent revisiting
     * @return true if the target department is a descendant, false otherwise
     */
    private boolean isDescendantRecursive(Department current, Department target, Set<Long> visited) {
        // If current department is already visited, return false to prevent infinite loop
        if (!visited.add(current.getId())) {
            return false;
        }

        // If the target is found among the current department's sub-departments, return true
        for (Department sub : current.getSubDepartments()) {
            if (sub.equals(target) || isDescendantRecursive(sub, target, visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method to check if setting {@code newParent} as parent of {@code current} would create a circular dependency.
     *
     * @param current the department entity to check
     * @param newParent the new parent department entity to set
     * @return {@code true} if setting {@code newParent} would create a circular dependency, otherwise {@code false}
     */
    private boolean hasCircularDependency(Department current, Department newParent) {
        return isDescendant(current, newParent);
    }

    /**
     * Helper method to check if deleting the given department would create a circular dependency.
     *
     * @param department the department to delete
     * @return true if deleting the department would create a circular dependency, false otherwise
     */
    private boolean hasCircularDependencyOnDelete(Department department) {
        // No need for a separate visited set here as we are using the same department
        return isDescendant(department, department);
    }

}
