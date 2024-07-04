package tn.engn.departmentapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import tn.engn.departmentapi.model.QDepartment;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service implementation for managing departments using the Nested Set Model.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NestedSetDepartmentService implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private DepartmentMapper departmentMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @Value("${department.max-name-length}")
    private int maxNameLength;

    /**
     * Creates a new department based on the provided DTO.
     *
     * @param departmentRequestDto the DTO containing the new department's details
     * @return the created department as a response DTO
     * @throws ValidationException if the department name is invalid
     * @throws ParentDepartmentNotFoundException if the parent department is not found
     * @throws DataIntegrityException if there is a data integrity violation
     */
    @Override
    @Transactional
    public DepartmentResponseDto createDepartment(DepartmentRequestDto departmentRequestDto)
            throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        String name = departmentRequestDto.getName();
        Long parentId = departmentRequestDto.getParentDepartmentId();

        // Validate the department request
        validateDepartmentName(name);

        log.info("Creating department with name: {} and parentId: {}", name, parentId);

        Integer parentRightIndex = null;
        Integer parentLevel = null;
        Long rootId = null;

        if (parentId != null) {
            // Fetch and validate the parent department
            Department parentDepartment = fetchAndValidateParentDepartment(parentId);

            parentRightIndex = parentDepartment.getRightIndex();
            parentLevel = parentDepartment.getLevel();
            rootId = parentDepartment.getRootId();

            // Update left and right indexes of existing departments
            updateIndexesForNewDepartment(parentRightIndex);
        }

        // Map the request DTO to an entity
        Department department = departmentMapper.toEntity(departmentRequestDto);

        if (parentRightIndex == null) {
            // Handle root department creation
            handleRootDepartmentCreation(department);
        } else {
            // Handle child department creation
            handleChildDepartmentCreation(department, parentRightIndex, parentLevel, rootId);
        }

        // Save the new department entity
        Department savedDepartment = departmentRepository.save(department);

        // Convert the saved entity to a response DTO and return it
        return departmentMapper.toDto(savedDepartment);
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
     * Fetches and validates the parent department.
     *
     * @param parentId the ID of the parent department
     * @return the parent department entity
     * @throws ParentDepartmentNotFoundException if the parent department is not found
     */
    @Transactional(readOnly = true)
    protected Department fetchAndValidateParentDepartment(Long parentId) throws ParentDepartmentNotFoundException {
        QDepartment qDepartment = QDepartment.department;
        Department parentDepartment = jpaQueryFactory.selectFrom(qDepartment)
                .where(qDepartment.id.eq(parentId))
                .fetchOne();

        if (parentDepartment == null) {
            throw new ParentDepartmentNotFoundException("Parent department not found with ID: " + parentId);
        }

        return parentDepartment;
    }

    /**
     * Updates the left and right indexes of existing departments to make room for the new department.
     *
     * @param parentRightIndex the right index of the parent department
     */
    @Transactional
    protected void updateIndexesForNewDepartment(Integer parentRightIndex) {
        QDepartment qDepartment = QDepartment.department;

        jpaQueryFactory.update(qDepartment)
                .set(qDepartment.leftIndex, qDepartment.leftIndex.add(2))
                .where(qDepartment.leftIndex.goe(parentRightIndex))
                .execute();

        jpaQueryFactory.update(qDepartment)
                .set(qDepartment.rightIndex, qDepartment.rightIndex.add(2))
                .where(qDepartment.rightIndex.goe(parentRightIndex))
                .execute();
    }

    /**
     * Handles the creation of a root department.
     *
     * @param department the department entity to be created
     */
    @Transactional
    protected void handleRootDepartmentCreation(Department department) {
        Integer maxRightIndex = jpaQueryFactory.select(QDepartment.department.rightIndex.max())
                .from(QDepartment.department)
                .fetchOne();

        if (maxRightIndex == null) {
            maxRightIndex = 0;
        }

        department.setLeftIndex(maxRightIndex + 1);
        department.setRightIndex(maxRightIndex + 2);
        department.setLevel(0);

        // Save the department to get the actual ID
        department = departmentRepository.save(department);
        log.info("Department saved with ID: {}", department.getId());

        // Set the rootId to its own ID after insertion
        department.setRootId(department.getId());
        log.info("Setting rootId to: {}", department.getId());

        // Save the department again to update the rootId
        department = departmentRepository.save(department);
        log.info("Department saved with rootId: {}", department.getRootId());

        // Ensure the entity is flushed and refreshed
        entityManager.flush();
        entityManager.refresh(department);
    }

    /**
     * Handles the creation of a child department.
     *
     * @param department the department entity to be created
     * @param parentRightIndex the right index of the parent department
     * @param parentLevel the level of the parent department
     * @param rootId the root ID of the parent department's tree
     */
    private void handleChildDepartmentCreation(Department department, Integer parentRightIndex, Integer parentLevel, Long rootId) {
        department.setLeftIndex(parentRightIndex);
        department.setRightIndex(parentRightIndex + 1);
        department.setLevel(parentLevel + 1);
        department.setRootId(rootId);
    }

    /**
     * Retrieves all departments.
     *
     * @return a list of all departments as response DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {
        QDepartment qDepartment = QDepartment.department;

        List<Department> departments = jpaQueryFactory.selectFrom(qDepartment)
                .orderBy(qDepartment.leftIndex.asc())
                .fetch();

        log.info("Retrieved {} departments.", departments.size());

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
        // Check if the parent department exists
        Department parentDepartment = departmentRepository.findById(parentId)
                .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with ID: " + parentId));

        // Query sub-departments using QueryDSL
        List<Department> subDepartments = jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.leftIndex.gt(parentDepartment.getLeftIndex())
                        .and(QDepartment.department.rightIndex.lt(parentDepartment.getRightIndex()))
                        .and(QDepartment.department.level.eq(parentDepartment.getLevel() + 1)))
                .fetch();

        // Map Department entities to DepartmentResponseDto
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
        // Retrieve department entity from repository
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + id));

        // Map Department entity to DepartmentResponseDto
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
        // Query departments using QueryDSL for departments matching the name
        List<Department> departments = jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.name.containsIgnoreCase(name))
                .fetch();

        // Map Department entities to DepartmentResponseDto
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
    @Transactional(readOnly = true)
    public DepartmentResponseDto getParentDepartment(Long departmentId) throws DepartmentNotFoundException, ParentDepartmentNotFoundException {
        // Check if the department exists
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        // Check if the department has a parent
        if (department.getParentDepartmentId() == null) {
            throw new ParentDepartmentNotFoundException("Department with ID " + departmentId + " has no parent department");
        }

        // Retrieve the parent department
        Department parentDepartment = departmentRepository.findById(department.getParentDepartmentId())
                .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with ID: " + department.getParentDepartmentId()));

        // Map parent department entity to DepartmentResponseDto
        return departmentMapper.toDto(parentDepartment);
    }

    /**
     * Retrieves all root departments (departments without a parent).
     *
     * @return a list of root departments as response DTOs
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllRootDepartments() {
        // Query root departments using QueryDSL
        List<Department> rootDepartments = jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.parentDepartmentId.isNull())
                .fetch();

        // Map Department entities to DepartmentResponseDto
        return rootDepartments.stream()
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
        // Retrieve the department entity by ID
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        // Query all descendants using QueryDSL
        List<Department> descendantDepartments = jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.leftIndex.gt(department.getLeftIndex())
                        .and(QDepartment.department.rightIndex.lt(department.getRightIndex())))
                .fetch();

        // Map Department entities to DepartmentResponseDto
        return descendantDepartments.stream()
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
        // Retrieve the department entity by ID
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        // Query all ancestors using QueryDSL
        List<Department> ancestorDepartments = jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.leftIndex.lt(department.getLeftIndex())
                        .and(QDepartment.department.rightIndex.gt(department.getRightIndex())))
                .orderBy(QDepartment.department.leftIndex.asc())
                .fetch();

        // Map Department entities to DepartmentResponseDto
        return ancestorDepartments.stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a department by its ID.
     *
     * @param id the ID of the department to delete
     * @throws DepartmentNotFoundException if the department with the given ID is not found
     * @throws ParentDepartmentNotFoundException if the parent department with the given ID is not found
     * @throws DataIntegrityException      if deletion would result in a circular dependency or if other constraints are violated
     */
    @Override
    @Transactional
    public void deleteDepartment(Long id) throws DepartmentNotFoundException, DataIntegrityException {
        // Retrieve the department entity by ID
        Department departmentToDelete = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + id));

        Department parentDepartment = null;
        if (departmentToDelete.getParentDepartmentId() != null) {
            parentDepartment = departmentRepository.findById(departmentToDelete.getParentDepartmentId())
                    .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with ID: " + departmentToDelete.getParentDepartmentId()));
           validateParentDepartment(departmentToDelete, parentDepartment);
        }

        // Retrieve all sub-departments (descendants) of the department to delete
        List<Department> subDepartments = jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.leftIndex.goe(departmentToDelete.getLeftIndex())
                        .and(QDepartment.department.rightIndex.loe(departmentToDelete.getRightIndex())))
                .fetch();

        // Delete the department and its sub-departments
        subDepartments.forEach(departmentRepository::delete);

        // Reorder the nested set structure after deletion
        reorderNestedSet();
    }


    /**
     * Reorders the nested set structure after modifications.
     */
    @Transactional
    public void reorderNestedSet() {
        List<Department> departments = jpaQueryFactory.selectFrom(QDepartment.department)
                .orderBy(QDepartment.department.leftIndex.asc())
                .fetch();

        int index = 1;
        for (Department department : departments) {
            int size = (department.getRightIndex() - department.getLeftIndex()) / 2;
            updateDepartmentIndexes(department, index, size);
            index += size + 1;
        }
    }

    /**
     * Updates the left and right indexes of a department and its subtree.
     *
     * @param department the department entity to update
     * @param index      the starting index for the department
     * @param size       the size (number of nodes) of the subtree
     */
    @Transactional
    protected void updateDepartmentIndexes(Department department, int index, int size) {
        int delta = department.getLeftIndex() - index;

        department.setLeftIndex(department.getLeftIndex() - delta);
        department.setRightIndex(department.getRightIndex() - delta);

        jpaQueryFactory.update(QDepartment.department)
                .set(QDepartment.department.leftIndex, QDepartment.department.leftIndex.subtract(delta))
                .set(QDepartment.department.rightIndex, QDepartment.department.rightIndex.subtract(delta))
                .where(QDepartment.department.leftIndex.between(index, index + size))
                .execute();
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
    @Transactional
    @Override
    public DepartmentResponseDto updateDepartment(Long departmentId, DepartmentRequestDto updatedDepartmentDto)
            throws DepartmentNotFoundException, ParentDepartmentNotFoundException, DataIntegrityException, ValidationException {
        String name = updatedDepartmentDto.getName();
        Long parentId = updatedDepartmentDto.getParentDepartmentId();
        // Fetch the existing department by ID
        Department existingDepartment = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException("Department not found with ID: " + departmentId));

        // Validate the updated department name
        validateDepartmentName(name);

        // Check if the department's name needs updating
        if (!existingDepartment.getName().equals(name)) {
            existingDepartment.setName(name);
        }

        // Check if the parent department is being changed
        if (parentId != null && !parentId.equals(existingDepartment.getParentDepartmentId())) {

            // Fetch and validate the new parent department
            Department newParentDepartment = departmentRepository.findById(parentId)
                    .orElseThrow(() -> new ParentDepartmentNotFoundException("Parent department not found with ID: " + parentId));

            // Validate parent department to avoid circular references and other constraints
            validateParentDepartment(existingDepartment, newParentDepartment);

            // Adjust nested set indexes and levels for moving the department
            moveSubtree(existingDepartment, newParentDepartment);

            // Fetch updated entities
            entityManager.refresh(existingDepartment);
            entityManager.refresh(newParentDepartment);

        }

        // Save the updated department
        Department updatedDepartment = departmentRepository.save(existingDepartment);

        // Return the updated department DTO
        return departmentMapper.toDto(updatedDepartment);
    }

    /**
     * Validates the parent department to avoid circular references and other constraints.
     *
     * @param department         the department to be updated
     * @param newParentDepartment the new parent department
     * @throws DataIntegrityException if the new parent department is invalid
     */
    private void validateParentDepartment(Department department, Department newParentDepartment) throws DataIntegrityException {
        log.info("Validating new parent department {} for department {}", newParentDepartment.getId(), department.getId());

        // Self-reference or descendant check
        if (department.getId().equals(newParentDepartment.getId()) ||
                (newParentDepartment.getLeftIndex() >= department.getLeftIndex() && newParentDepartment.getRightIndex() <= department.getRightIndex())) {
            log.error("Invalid parent department: Cannot set department {} as its own descendant's parent.", department.getId());
            throw new DataIntegrityException("Cannot set a department as its own descendant's parent.");
        }

        // Immediate circular reference check
        if (newParentDepartment.getParentDepartmentId() != null && newParentDepartment.getParentDepartmentId().equals(department.getId())) {
            log.error("Invalid parent department: Circular reference detected for department {}", department.getId());
            throw new DataIntegrityException("Circular references.");
        }

        log.info("Parent department validation successful for department {}", department.getId());
    }

    /**
     * Moves a subtree to a new parent.
     *
     * @param department         the department to be moved
     * @param newParentDepartment the new parent department
     */
    @Transactional
    protected void moveSubtree(Department department, Department newParentDepartment) {
        int subtreeSize = department.getRightIndex() - department.getLeftIndex() + 1;
        int indexShift = newParentDepartment.getRightIndex() - department.getLeftIndex();
        if (indexShift < 0) {
            indexShift -= subtreeSize;
        }

        int levelShift = newParentDepartment.getLevel() + 1 - department.getLevel();

        // Shift indexes to make space for the new subtree
        shiftIndexes(newParentDepartment.getRightIndex(), subtreeSize);

        // Move the subtree to the new space and update levels
        updateSubtreeIndexesAndLevels(department, indexShift, levelShift);

        // Compact the space left by the moved subtree
        compactIndexes(department.getLeftIndex(), subtreeSize);

        // Update parent and root IDs of the subtree
        updateSubtreeParentAndRootIds(department, newParentDepartment);
    }

    /**
     * Updates the levels, the left and right indexes of the subtree.
     *
     * @param department the department to be moved
     * @param indexShift    the amount to shift by the indexes
     * @param levelShift    the amount to shift by the levels
     */
    @Transactional
    protected void updateSubtreeIndexesAndLevels(Department department, int indexShift, int levelShift) {
        jpaQueryFactory.update(QDepartment.department)
                .set(QDepartment.department.leftIndex, QDepartment.department.leftIndex.add(indexShift))
                .set(QDepartment.department.rightIndex, QDepartment.department.rightIndex.add(indexShift))
                .set(QDepartment.department.level, QDepartment.department.level.add(levelShift))
                .where(QDepartment.department.leftIndex.between(department.getLeftIndex(), department.getRightIndex()))
                .execute();
    }

    /**
     * Shifts indexes to make space for the new subtree.
     *
     * @param startIndex the starting index for the shift
     * @param shiftBy    the amount to shift by
     */
    @Transactional
    protected void shiftIndexes(int startIndex, int shiftBy) {
        jpaQueryFactory.update(QDepartment.department)
                .set(QDepartment.department.leftIndex, QDepartment.department.leftIndex.add(shiftBy))
                .where(QDepartment.department.leftIndex.goe(startIndex))
                .execute();

        jpaQueryFactory.update(QDepartment.department)
                .set(QDepartment.department.rightIndex, QDepartment.department.rightIndex.add(shiftBy))
                .where(QDepartment.department.rightIndex.goe(startIndex))
                .execute();
    }

    /**
     * Compacts the space left by the moved subtree.
     *
     * @param startIndex the starting index for the compacting
     * @param compactBy  the amount to compact by
     */
    @Transactional
    protected void compactIndexes(int startIndex, int compactBy) {
        jpaQueryFactory.update(QDepartment.department)
                .set(QDepartment.department.leftIndex, QDepartment.department.leftIndex.subtract(compactBy))
                .where(QDepartment.department.leftIndex.goe(startIndex))
                .execute();

        jpaQueryFactory.update(QDepartment.department)
                .set(QDepartment.department.rightIndex, QDepartment.department.rightIndex.subtract(compactBy))
                .where(QDepartment.department.rightIndex.goe(startIndex))
                .execute();
    }

    /**
     * Updates the root ID and parent department ID for a department and its subtree.
     *
     * This method is responsible for updating the `rootId` of all nodes in the subtree of the given department to match
     * the `rootId` of the new parent department. It also updates the `parentDepartmentId` of the department being moved.
     *
     * @param department          the department being moved
     * @param newParentDepartment the new parent department for the moved department
     */
    @Transactional
    protected void updateSubtreeParentAndRootIds(Department department, Department newParentDepartment) {
        // Update the rootId for all nodes in the subtree
        jpaQueryFactory.update(QDepartment.department)
                .set(QDepartment.department.rootId, newParentDepartment.getRootId())
                .where(QDepartment.department.leftIndex.between(department.getLeftIndex(), department.getRightIndex()))
                .execute();

        // Update the parentDepartmentId for the department being moved
        jpaQueryFactory.update(QDepartment.department)
                .set(QDepartment.department.parentDepartmentId, newParentDepartment.getId())
                .where(QDepartment.department.id.eq(department.getId()))
                .execute();
    }


}
