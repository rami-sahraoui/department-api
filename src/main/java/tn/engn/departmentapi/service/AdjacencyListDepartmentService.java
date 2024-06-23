package tn.engn.departmentapi.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for managing departments using the adjacency list model.
 */
@Service
@Slf4j // Lombok annotation for logging
public class AdjacencyListDepartmentService implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Value("${department.max-name-length}")
    @Getter
    @Setter
    private int maxNameLength;

    /**
     * Creates a new department with the given name and optional parent ID.
     * If a parent ID is provided, establishes a bidirectional relationship between
     * the new department and its parent.
     *
     * @param name     the name of the new department (must not be empty and should not exceed a certain length)
     * @param parentId the ID of the parent department (optional)
     * @return the created department
     * @throws IllegalArgumentException if the department name is empty, null, or exceeds the maximum allowed length
     * @throws EntityNotFoundException if the specified parent department with the given ID is not found
     */
    @Override
    @Transactional
    public Department createDepartment(String name, Long parentId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Department name must not be empty");
        }

        if (name.length() > getMaxNameLength()) {
            throw new IllegalArgumentException("Department name exceeds maximum allowed length of " + getMaxNameLength() + " characters");
        }

        log.info("Creating department with name: {} and parentId: {}", name, parentId);
        Department department = new Department();
        department.setName(name);

        if (parentId != null) {
            // Retrieve the parent department if parentId is provided
            Department parentDepartment = departmentRepository.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("Parent department not found with id: " + parentId));

            // Establish bidirectional relationship: set child department and add to parent's sub-departments
            parentDepartment.addSubDepartment(department);
        }

        // Save and return the created department
        department = departmentRepository.save(department);
        return department;
    }

    /**
     * Updates the name and parent of an existing department, ensuring no circular dependencies are created.
     *
     * @param id       the ID of the department to update
     * @param name     the new name of the department
     * @param parentId the new parent ID of the department
     * @return the updated department
     * @throws EntityNotFoundException  if the department with the given ID or the specified parent department with the given ID is not found
     * @throws IllegalArgumentException if setting the new parent would create a circular dependency
     */
    @Override
    @Transactional
    public Department updateDepartment(Long id, String name, Long parentId) {
        log.info("Updating department with id: {}, name: {} and parentId: {}", id, name, parentId);

        // Retrieve the department to update
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));

        // Check if the department's name needs updating
        if (!department.getName().equals(name)) {
            department.setName(name);
        }

        // Check if the parent department needs updating
        if (parentId != null) {
            Department newParent = departmentRepository.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("Parent department not found with id: " + parentId));

            // Check for circular dependency
            if (hasCircularDependency(department, newParent)) {
                throw new IllegalArgumentException("Circular dependency detected.");
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

        // Save and return the updated department
        return departmentRepository.save(department);
    }

    /**
     * Checks if setting {@code newParent} as parent of {@code current} would create a circular dependency.
     *
     * @param current   the department entity to check
     * @param newParent the new parent department entity to set
     * @return {@code true} if setting {@code newParent} would create a circular dependency, otherwise {@code false}
     */
    private boolean hasCircularDependency(Department current, Department newParent) {
        List<Department> descendants = getDescendants(current.getId());
        return descendants.contains(newParent);
    }

    /**
     * Deletes a department by its ID, maintaining bidirectional relationships if applicable.
     *
     * @param id the ID of the department to delete
     * @throws EntityNotFoundException if the department with the given ID is not found
     */
    public void deleteDepartment(Long id) {
        // Retrieve the department from the repository by its ID
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));

        // Retrieve the parent department, if any
        Department parent = department.getParentDepartment();

        // If the department has a parent, remove this department from the parent's list of sub-departments
        if (parent != null) {
            parent.getSubDepartments().remove(department);
        }

        // Delete the department from the repository
        departmentRepository.delete(department);
    }

    /**
     * Retrieves all departments.
     *
     * @return a list of all departments
     */
    @Override
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        log.info("Retrieving all departments");
        // Fetch and return all departments from the repository
        return departmentRepository.findAll();
    }

    /**
     * Retrieves sub-departments (children) of a given parent department.
     *
     * @param parentId the ID of the parent department
     * @return a list of sub-departments
     * @throws EntityNotFoundException if the parent department with the given ID is not found
     */
    @Override
    @Transactional
    public List<Department> getSubDepartments(Long parentId) {
        log.info("Retrieving sub-departments for parent department with id: {}", parentId);
        // Retrieve the parent department by ID
        Department parentDepartment = departmentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent department not found with id: " + parentId));

        // Initialize the collection to avoid LazyInitializationException
        parentDepartment.getSubDepartments().size();

        // Return the list of sub-departments of the parent department
        return parentDepartment.getSubDepartments();
    }

    /**
     * Retrieves a department by its ID.
     *
     * @param id the ID of the department to retrieve
     * @return the department with the specified ID
     * @throws EntityNotFoundException if the department with the given ID is not found
     */
    @Override
    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id) {
        log.info("Retrieving department with id: {}", id);
        // Retrieve and return the department by its ID
        return departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
    }

    /**
     * Retrieves the parent department of a given department.
     *
     * @param departmentId the ID of the department
     * @return the parent department
     * @throws EntityNotFoundException if the department with the given ID is not found or if the department has no parent
     */
    @Override
    @Transactional
    public Department getParentDepartment(Long departmentId) {
        log.info("Retrieving parent department for department with id: {}", departmentId);
        // Retrieve the department by ID
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + departmentId));

        // Retrieve and return the parent department
        Department parentDepartment = department.getParentDepartment();
        if (parentDepartment == null) {
            throw new EntityNotFoundException("Department with id: " + departmentId + " has no parent department");
        }
        return parentDepartment;
    }

    /**
     * Retrieves all descendants (sub-departments and their sub-departments recursively) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of descendants
     * @throws EntityNotFoundException if the department with the given ID is not found
     */
    @Override
    @Transactional
    public List<Department> getDescendants(Long departmentId) {
        log.info("Retrieving all descendants for department with id: {}", departmentId);
        // Retrieve the department by ID
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + departmentId));

        // Fetch all descendants within the same transaction
        return getAllDescendants(department);
    }

    /**
     * Retrieves all ancestors (parent departments recursively) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of ancestors
     * @throws EntityNotFoundException if the department with the given ID is not found
     */
    @Override
    @Transactional
    public List<Department> getAncestors(Long departmentId) {
        log.info("Retrieving all ancestors for department with id: {}", departmentId);
        // Retrieve the department by ID
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + departmentId));

        // Fetch all ancestors within the same transaction
        return getAllAncestors(department);
    }

    /**
     * Helper method to recursively fetch all descendants of a department.
     *
     * @param department the department whose descendants are to be fetched
     * @return a list of all descendants
     */
    private List<Department> getAllDescendants(Department department) {
        List<Department> descendants = new ArrayList<>();
        List<Department> subDepartments = department.getSubDepartments();
        descendants.addAll(subDepartments);
        for (Department subDepartment : subDepartments) {
            descendants.addAll(getAllDescendants(subDepartment));
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
}
