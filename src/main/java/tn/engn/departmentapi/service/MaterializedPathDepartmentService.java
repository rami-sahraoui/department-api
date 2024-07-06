package tn.engn.departmentapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import tn.engn.departmentapi.model.QDepartment;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for managing departments using the Materialized Path Model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaterializedPathDepartmentService implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    private final DepartmentMapper departmentMapper;

    private final JPAQueryFactory jpaQueryFactory;

    // Setter for testing purposes
    @Setter
    @Value("${department.max-name-length}")
    private int maxNameLength;

    /**
     * Creates a new department based on the provided DepartmentRequestDto.
     *
     * @param requestDto The data transfer object containing the details of the department to be created.
     * @return The response DTO representing the created department.
     * @throws ValidationException If the department name is invalid.
     * @throws ParentDepartmentNotFoundException If the specified parent department does not exist.
     */
    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentRequestDto requestDto)
            throws ValidationException, ParentDepartmentNotFoundException {

        String name = requestDto.getName();
        Long parentId = requestDto.getParentDepartmentId();

        // Validate the department name
        validateDepartmentName(name);

        log.info("Creating department with name: {} and parentId: {}", name, parentId);

        // Create a new Department entity from the request DTO
        Department department = departmentMapper.toEntity(requestDto);

        // Determine the path based on the parent department
        String path = determinePath(parentId);

        // Set the path
        department.setPath(path);

        // Save the department to generate its ID
        Department savedDepartment = departmentRepository.save(department);

        // Update the path with the generated ID
        savedDepartment.setPath(path + savedDepartment.getId() + "/");

        // Resave the department with the updated path
        Department finalSavedDepartment = departmentRepository.save(savedDepartment);

        // Convert the saved department entity to a response DTO and return it
        return departmentMapper.toDto(finalSavedDepartment);
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
     * Determines the path for the new department based on its parent department.
     *
     * @param parentDepartmentId The ID of the parent department, if any.
     * @return The path for the new department.
     * @throws ParentDepartmentNotFoundException If the specified parent department does not exist.
     */
    private String determinePath(Long parentDepartmentId) throws ParentDepartmentNotFoundException {
        if (parentDepartmentId == null) {
            // If there's no parent, the department is a root department
            return "/";
        } else {
            // Find the parent department
            Department parent = departmentRepository.findById(parentDepartmentId)
                    .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found"));

            // Return the path of the parent department
            return parent.getPath();
        }
    }

    /**
     * Updates an existing department.
     *
     * @param departmentId        the ID of the department to update
     * @param updatedDepartmentDto the DTO containing the updated department data
     * @return the updated department as a DepartmentResponseDto
     * @throws DepartmentNotFoundException      if the department is not found
     * @throws ParentDepartmentNotFoundException if the parent department is not found
     * @throws ValidationException              if the department name is invalid
     * @throws DataIntegrityException           if there is a data integrity violation, such as circular references
     */
    @Override
    @Transactional
    public DepartmentResponseDto updateDepartment(Long departmentId, DepartmentRequestDto updatedDepartmentDto)
            throws DepartmentNotFoundException, ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {

        // Extract updated department information from DTO
        String name = updatedDepartmentDto.getName();
        Long parentId = updatedDepartmentDto.getParentDepartmentId();

        // Validate the department name
        validateDepartmentName(name);

        log.info("Updating department with name: {} and parentId: {}", name, parentId);

        // Retrieve the existing department
        Department existingDepartment = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        // Check if the department's name needs updating
        if (!existingDepartment.getName().equals(name)) {
            existingDepartment.setName(name);
        }

        // Check if the parent department needs updating
        Long currentParentId = existingDepartment.getParentDepartmentId();
        if (parentId == null && currentParentId != null || parentId != null && !parentId.equals(currentParentId)) {
            updateParentDepartment(existingDepartment, parentId);
        }

        // Save the updated department
        Department updatedDepartment = departmentRepository.save(existingDepartment);

        // Return the updated department as DepartmentResponseDto
        return departmentMapper.toDto(updatedDepartment);
    }

    /**
     * Updates the parent department of a given department, including its descendants' paths.
     *
     * @param department   the department to update
     * @param newParentId  the ID of the new parent department
     * @throws ParentDepartmentNotFoundException if the specified parent department does not exist
     * @throws DataIntegrityException           if a circular reference would be created by the update
     */
    private void updateParentDepartment(Department department, Long newParentId)
            throws ParentDepartmentNotFoundException, DataIntegrityException {

        // Determine the new path prefix based on the new parent department ID
        String newParentPath = determinePath(newParentId);
        String newPath = newParentPath + department.getId() + "/";

        // Retrieve all descendants of the department
        String currentPath = department.getPath();

        List<Department> descendants = getDescendants(currentPath);

        // Check for circular references in descendant paths
        if (newParentId != null && hasCircularReference(newParentPath, descendants)) {
            throw new DataIntegrityException("Circular reference detected: "
                    + "Moving department " + department.getName() + " under department with ID " + newParentId
                    + " would create a circular reference.");
        }

        // Update paths of all descendants
        updateDescendantsPaths(descendants, currentPath, newPath);

        // Update parent ID of the department
        department.setParentDepartmentId(newParentId);
        department.setPath(newPath);
    }

    /**
     * Checks if there is a circular reference by verifying if any descendant's path starts with the new path.
     *
     * @param newPath      the new path prefix to check for circular references
     * @param descendants  the list of descendant departments to check
     * @return true if a circular reference is detected, false otherwise
     */
    private static boolean hasCircularReference(String newPath, List<Department> descendants) {
        return descendants.stream()
                .anyMatch(descendant -> descendant.getPath().startsWith(newPath));
    }

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given department.
     *
     * @param path the path of the department
     * @return a list of all descendants as Department
     */
    @Transactional(readOnly = true)
    protected List<Department> getDescendants(String path) throws DepartmentNotFoundException {
        // Query descendants using QueryDSL
         return jpaQueryFactory.selectFrom(QDepartment.department)
                 .where(QDepartment.department.path.startsWith(path))
                 .fetch();

    }

    /**
     * Updates paths of all descendants of a department to reflect a new path prefix.
     *
     * @param descendants  the list of descendants to update
     * @param currentPath  the current path prefix
     * @param newPath      the new path prefix
     */
    private void updateDescendantsPaths(List<Department> descendants, String currentPath, String newPath) {
        // Update paths of all descendants
        descendants.forEach(descendant -> {
            String descendantPath = descendant.getPath();
            if (descendantPath.startsWith(currentPath)) {
                descendant.setPath(newPath + descendantPath.substring(currentPath.length()));
            }
        });
    }

    /**
     * Deletes a department by its ID along with all its descendants.
     *
     * @param id the ID of the department to delete
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     * @throws DataIntegrityException      if deletion would result in a circular dependency or if other constraints are violated
     */
    @Override
    @Transactional
    public void deleteDepartment(Long id) throws DepartmentNotFoundException, DataIntegrityException {
        // Retrieve the department by ID, throw exception if not found
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + id));

        // Retrieve all descendants
        List<Department> descendants = findAllDescendants(department.getPath());

        // Check for circular references
        checkForCircularReferences(department, descendants);

        // Delete all descendants and the department
        departmentRepository.deleteAll(descendants);
        departmentRepository.delete(department);
    }

    /**
     * Finds all descendants of a department using its path.
     *
     * @param path the path of the department
     * @return the list of descendants
     */
    private List<Department> findAllDescendants(String path) {
        return jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.path.startsWith(path).and(QDepartment.department.path.notEqualsIgnoreCase(path)))
                .fetch();
    }

    /**
     * Checks for circular references in the descendant paths.
     *
     * @param parent      the parent department
     * @param descendants the list of descendants
     * @throws DataIntegrityException if a circular reference is detected
     */
    private void checkForCircularReferences(Department parent, List<Department> descendants) throws DataIntegrityException {
        for (Department descendant : descendants) {
            String[] ids = descendant.getPath().split("/");
            Set<String> uniqueIds = new HashSet<>(Arrays.asList(ids));

            // If there are duplicate IDs, it indicates a circular reference
            if (uniqueIds.size() < ids.length) {
                throw new DataIntegrityException("Circular reference detected in department path: " + descendant.getPath());
            }
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
        // Retrieve all departments from the repository
        List<Department> departments = departmentRepository.findAll();

        log.info("Retrieved {} departments.", departments.size());

        // Map each department entity to a response DTO
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
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getSubDepartments(Long parentId) throws ParentDepartmentNotFoundException {
        // Retrieve the department by ID, throw exception if not found
        Department parentDepartment = departmentRepository.findById(parentId)
                .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with id: " + parentId));

        List<Department> subDepartments = departmentRepository.findByParentDepartmentId(parentId);

        // Map each department entity to a response DTO and return the list
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
    @Transactional(readOnly = true)
    public DepartmentResponseDto getDepartmentById(Long id) throws DepartmentNotFoundException {
        // Retrieve the department by ID, throw exception if not found
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + id));

        // Map the department entity to a response DTO and return it
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
        // Retrieve departments with names containing the given name from the repository
        List<Department> departments = departmentRepository.findByNameContaining(name);

        // Map each department entity to a response DTO and return the list
        return departments.stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
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
    public List<DepartmentResponseDto> getDescendants(Long departmentId) throws DepartmentNotFoundException {
        // Retrieve the department by ID, throw exception if not found
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        // Find the descendants
        List<Department> descendants = departmentRepository.findByPathStartingWith(department.getPath());

        // Map each department entity to a response DTO and return the list
        return descendants.stream()
                .filter(d -> d.getId() != departmentId)
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
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAncestors(Long departmentId) throws DepartmentNotFoundException {
        // Retrieve the department by ID, throw exception if not found
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        // Find the ancestors
        String[] pathSegments = department.getPath().split("/");
        List<Long> ancestorIds = Arrays.stream(pathSegments)
                .filter(segment -> !segment.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<Department> ancestors = departmentRepository.findAllById(ancestorIds);

        // Map each department entity to a response DTO and return the list
        return ancestors.stream()
                .filter(d -> d.getId() != departmentId)
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
    @Transactional(readOnly = true)
    public DepartmentResponseDto getParentDepartment(Long departmentId) throws DepartmentNotFoundException, ParentDepartmentNotFoundException {
        // Retrieve the department by ID, throw exception if not found
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        // Check for null parent id
        if (department.getParentDepartmentId() == null) {
            throw new ParentDepartmentNotFoundException("Department with id: " + departmentId + " has no parent.");
        }

        // Retrieve the parent department by ID, throw exception if not found
        Department parentDepartment = departmentRepository.findById(department.getParentDepartmentId())
                .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with ID: " + department.getParentDepartmentId()));

        // Map the department entity to a response DTO and return it
        return departmentMapper.toDto(parentDepartment);
    }

}
