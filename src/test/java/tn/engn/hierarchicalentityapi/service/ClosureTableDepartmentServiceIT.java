package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.hierarchicalentityapi.TestContainerSetup;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.DataIntegrityException;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ParentEntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.model.DepartmentClosure;
import tn.engn.hierarchicalentityapi.repository.DepartmentClosureRepository;
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MaterializedPathDepartmentService.
 * Tests CRUD operations and business logic within the Materialized Path Model.
 */
@SpringBootTest
@Slf4j
//@ActiveProfiles("test-real-db")
//public class MaterializedPathDepartmentServiceIT {
@ActiveProfiles("test-container")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ClosureTableDepartmentServiceIT extends TestContainerSetup {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentClosureRepository departmentClosureRepository;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @Autowired
    private ClosureTableDepartmentService departmentService;

    @Autowired
    private Environment environment;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${entity.max-name-length}")
    private int maxNameLength;

    /**
     * Clean up the database before each test to ensure isolation.
     */
    @BeforeEach
    public void setup() {
        departmentRepository.deleteAll();
        departmentClosureRepository.deleteAll();
    }

    /**
     * Clean up the database after each test to ensure isolation.
     */
    @AfterEach
    public void cleanUp() {
        departmentRepository.deleteAll();
        departmentClosureRepository.deleteAll();
    }

    /**
     * Test method to verify if the 'test' profile is active.
     * <p>
     * This test checks if the 'test' profile is correctly activated for the integration test.
     * It retrieves the active profiles from the Spring environment, logs them, and asserts
     * that the 'test' profile is present among the active profiles.
     */
    @Test
    public void testProfileActivation() {
        // Retrieve active profiles from the environment
        String[] activeProfiles = environment.getActiveProfiles();

        // Log active profiles to provide visibility during test execution
        log.info("Active Profiles: {}", Arrays.toString(activeProfiles));

        // Assertion to check if 'test' profile is active
        assertThat(activeProfiles).contains("test-container");
    }

    /**
     * Helper method to create and save a new Department entity in the database.
     *
     * @param id        the unique identifier of the department
     * @param name      the name of the department
     * @param parentId  the unique identifier of the parent department, or null if this is a root department
     * @return the created Department entity
     *
     * This method constructs a new Department entity using the provided parameters and
     * saves it to the database. The method ensures that the entity is properly built
     * using the Department builder and then persisted using the departmentRepository.
     * It is a utility method intended for use in test cases to set up initial test data.
     */
    private Department createDepartment(Long id, String name, Long parentId) {
        Department department = Department.builder()
                .id(id)
                .name(name)
                .parentId(parentId)
                .build();
        return departmentRepository.save(department);
    }

    private DepartmentClosure createDepartmentClosure(Long ancestorId, Long descendantId, int Level) {
        DepartmentClosure departmentClosure = DepartmentClosure.builder()
                .ancestorId(ancestorId)
                .descendantId(descendantId)
                .level(Level)
                .build();
        return departmentClosureRepository.save(departmentClosure);
    }

    /**
     * Integration test for creating a root department successfully.
     */
    @Test
    public void testCreateRootDepartment_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for a root department.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Root Department");

        // When: Creating the root department.
        HierarchyResponseDto result = departmentService.createEntity(requestDto);

        // Then: Verify the department is created successfully.
        assertNotNull(result);
        assertEquals("Root Department", result.getName());
        assertNull(result.getParentEntityId());

        List<DepartmentClosure> closureEntities = departmentService.findClosureAncestors(result.getId());
        assertNotNull(closureEntities);
        assertEquals(1, closureEntities.size());
        assertEquals(result.getId(), closureEntities.get(0).getAncestorId());
        assertEquals(result.getId(), closureEntities.get(0).getDescendantId());
        assertEquals(0, closureEntities.get(0).getLevel());
    }

    /**
     * Integration test for creating a child department successfully.
     */
    @Test
    public void testCreateChildDepartment_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for a child department.
        String parentName = "Parent Department";
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name(parentName)
                .build();

        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);

        String childName = "Child Department";
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name(childName)
                .parentEntityId(parentResponseDto.getId())
                .build();

        // When: Creating the child department.
        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        // Then: Verify the departments are created successfully.
        assertNotNull(parentResponseDto);
        assertEquals(parentName, parentResponseDto.getName());
        assertNull(parentResponseDto.getParentEntityId());

        List<DepartmentClosure> parentClosureEntities = departmentService.findClosureAncestors(parentResponseDto.getId());
        assertNotNull(parentClosureEntities);
        assertEquals(1, parentClosureEntities.size());

        assertEquals(parentResponseDto.getId(), parentClosureEntities.get(0).getAncestorId());
        assertEquals(parentResponseDto.getId(), parentClosureEntities.get(0).getDescendantId());
        assertEquals(0, parentClosureEntities.get(0).getLevel());

        assertNotNull(childResponseDto);
        assertEquals(childName, childResponseDto.getName());
        assertEquals(parentResponseDto.getId(), childResponseDto.getParentEntityId());

        List<DepartmentClosure> childClosureEntities = departmentService.findClosureAncestors(childResponseDto.getId());
        assertNotNull(childClosureEntities);
        assertEquals(2, childClosureEntities.size());

        assertEquals(childResponseDto.getId(), childClosureEntities.get(0).getAncestorId());
        assertEquals(childResponseDto.getId(), childClosureEntities.get(0).getDescendantId());
        assertEquals(0, childClosureEntities.get(0).getLevel());

        assertEquals(parentResponseDto.getId(), childClosureEntities.get(1).getAncestorId());
        assertEquals(childResponseDto.getId(), childClosureEntities.get(1).getDescendantId());
        assertEquals(1, childClosureEntities.get(1).getLevel());
    }

    /**
     * Integration test for creating a real subtree successfully.
     */
    @Test
    public void testCreateRealSubtree_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for a real subtree.
        String rootName = "Root";
        HierarchyRequestDto rootRequestDto = HierarchyRequestDto.builder()
                .name(rootName)
                .build();

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        String anotherRootName = "Another Root";
        HierarchyRequestDto anotherRootRequestDto = HierarchyRequestDto.builder()
                .name(anotherRootName)
                .build();

        HierarchyResponseDto anotherRootResponseDto = departmentService.createEntity(anotherRootRequestDto);

        String childName = "Child";
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name(childName)
                .parentEntityId(rootResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        String grandChildName = "Grand Child";
        HierarchyRequestDto grandChildRequestDto = HierarchyRequestDto.builder()
                .name(grandChildName)
                .parentEntityId(childResponseDto.getId())
                .build();

        // When: Creating the grand child department.
        HierarchyResponseDto grandChildResponseDto = departmentService.createEntity(grandChildRequestDto);

        // Then: Verify the departments are created successfully.
        // Verify root department
        assertNotNull(rootResponseDto);
        assertEquals(rootName, rootResponseDto.getName());
        assertNull(rootResponseDto.getParentEntityId());

        List<DepartmentClosure> rootClosureEntities = departmentService.findClosureAncestors(rootResponseDto.getId());
        assertNotNull(rootClosureEntities);
        assertEquals(1, rootClosureEntities.size());

        assertEquals(rootResponseDto.getId(), rootClosureEntities.get(0).getAncestorId());
        assertEquals(rootResponseDto.getId(), rootClosureEntities.get(0).getDescendantId());
        assertEquals(0, rootClosureEntities.get(0).getLevel());

        // Verify child department
        assertNotNull(childResponseDto);
        assertEquals(childName, childResponseDto.getName());
        assertEquals(rootResponseDto.getId(), childResponseDto.getParentEntityId());

        List<DepartmentClosure> childClosureEntities = departmentService.findClosureAncestors(childResponseDto.getId());
        assertNotNull(childClosureEntities);
        assertEquals(2, childClosureEntities.size());

        assertEquals(childResponseDto.getId(), childClosureEntities.get(0).getAncestorId());
        assertEquals(childResponseDto.getId(), childClosureEntities.get(0).getDescendantId());
        assertEquals(0, childClosureEntities.get(0).getLevel());

        assertEquals(rootResponseDto.getId(), childClosureEntities.get(1).getAncestorId());
        assertEquals(childResponseDto.getId(), childClosureEntities.get(1).getDescendantId());
        assertEquals(1, childClosureEntities.get(1).getLevel());

        // Verify grand child department
        assertNotNull(grandChildResponseDto);
        assertEquals(grandChildName, grandChildResponseDto.getName());
        assertEquals(childResponseDto.getId(), grandChildResponseDto.getParentEntityId());

        List<DepartmentClosure> grandChildClosureEntities = departmentService.findClosureAncestors(grandChildResponseDto.getId());
        assertNotNull(grandChildClosureEntities);
        assertEquals(3, grandChildClosureEntities.size());

        assertEquals(grandChildResponseDto.getId(), grandChildClosureEntities.get(0).getAncestorId());
        assertEquals(grandChildResponseDto.getId(), grandChildClosureEntities.get(0).getDescendantId());
        assertEquals(0, grandChildClosureEntities.get(0).getLevel());

        assertEquals(childResponseDto.getId(), grandChildClosureEntities.get(1).getAncestorId());
        assertEquals(grandChildResponseDto.getId(), grandChildClosureEntities.get(1).getDescendantId());
        assertEquals(1, grandChildClosureEntities.get(1).getLevel());

        assertEquals(rootResponseDto.getId(), grandChildClosureEntities.get(2).getAncestorId());
        assertEquals(grandChildResponseDto.getId(), grandChildClosureEntities.get(2).getDescendantId());
        assertEquals(2, grandChildClosureEntities.get(2).getLevel());

        // Verify anotherRoot department
        assertNotNull(anotherRootResponseDto);
        assertEquals(anotherRootName, anotherRootResponseDto.getName());
        assertNull(anotherRootResponseDto.getParentEntityId());

        List<DepartmentClosure> anotherRootClosureEntities = departmentService.findClosureAncestors(anotherRootResponseDto.getId());
        assertNotNull(anotherRootClosureEntities);
        assertEquals(1, anotherRootClosureEntities.size());

        assertEquals(anotherRootResponseDto.getId(), anotherRootClosureEntities.get(0).getAncestorId());
        assertEquals(anotherRootResponseDto.getId(), anotherRootClosureEntities.get(0).getDescendantId());
        assertEquals(0, anotherRootClosureEntities.get(0).getLevel());
    }

    /**
     * Integration test for creating a child department with empty parent closure.
     */
    @Test
    public void testCreateChildDepartment_EmptyParentClosure() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for a child department.
        String parentName = "Parent Department";
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name(parentName)
                .build();

        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);
        departmentClosureRepository.deleteAll();

        String childName = "Child Department";
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name(childName)
                .parentEntityId(parentResponseDto.getId())
                .build();

        // When: Creating the child department.
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.createEntity(childRequestDto);
        });

        // Then: Verify the ParentEntityNotFoundException is thrown.
        assertEquals("Parent entity not found.", exception.getMessage());
    }

    /**
     * Integration test for creating a department with an invalid name.
     */
    @Test
    public void testCreateDepartment_InvalidName() {
        // Given: A HierarchyRequestDto with an invalid name.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("");

        // When: Creating the department.
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then: Verify the ValidationException is thrown.
        assertEquals("Entity name cannot be null or empty.", exception.getMessage());
    }

    /**
     * Integration test for creating a department with a parent not found.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given: A HierarchyRequestDto with a non-existent parent ID.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("New Department");
        requestDto.setParentEntityId(-1L);

        // When: Creating the department.
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then: Verify the ParentEntityNotFoundException is thrown.
        assertEquals("Parent entity not found.", exception.getMessage());
    }

    /**
     * Integration test for creating a department with a name that is too long.
     */
    @Test
    public void testCreateDepartment_NameTooLong() {
        // Given: A department name exceeding the maximum length
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("A".repeat(maxNameLength + 1));

        // When: Attempting to create the department
        ValidationException exception = assertThrows(ValidationException.class,
                () -> departmentService.createEntity(requestDto));

        // Then: Verify the ValidationException is thrown.
        assertEquals("Entity name cannot be longer than " + maxNameLength + " characters.", exception.getMessage());
    }

    /**
     * Integration test for updating a department successfully.
     */
    @Test
    public void testUpdateDepartment_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: Create an initial root department
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        // And: Create a child department under the root department
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Child Department")
                .parentEntityId(rootResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        // When: Update the child department's name
        childRequestDto.setId(childResponseDto.getId());
        childRequestDto.setName("Updated Child Department");

        HierarchyResponseDto updatedChildResponseDto = departmentService.updateEntity(childResponseDto.getId(), childRequestDto);

        // Then: Verify the department is updated successfully
        assertNotNull(updatedChildResponseDto);
        assertEquals("Updated Child Department", updatedChildResponseDto.getName());
        assertEquals(rootResponseDto.getId(), updatedChildResponseDto.getParentEntityId());

        // And: Verify the closure records are updated correctly
        List<DepartmentClosure> childClosureEntities = departmentService.findClosureAncestors(updatedChildResponseDto.getId());
        assertNotNull(childClosureEntities);
        assertEquals(2, childClosureEntities.size());

        assertEquals(updatedChildResponseDto.getId(), childClosureEntities.get(0).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), childClosureEntities.get(0).getDescendantId());
        assertEquals(0, childClosureEntities.get(0).getLevel());

        assertEquals(rootResponseDto.getId(), childClosureEntities.get(1).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), childClosureEntities.get(1).getDescendantId());
        assertEquals(1, childClosureEntities.get(1).getLevel());
    }

    /**
     * Integration test for updating a department to be a new root department.
     */
    @Test
    public void testUpdateDepartment_ToRoot_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: Create an initial root department
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        // And: Create a child department under the root department
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Child Department")
                .parentEntityId(rootResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        // When: Update the child department to be a root department
        childRequestDto.setId(childResponseDto.getId());
        childRequestDto.setName("Updated Child Department");
        childRequestDto.setParentEntityId(null);

        HierarchyResponseDto updatedChildResponseDto = departmentService.updateEntity(childResponseDto.getId(), childRequestDto);

        // Then: Verify the department is updated successfully
        assertNotNull(updatedChildResponseDto);
        assertEquals("Updated Child Department", updatedChildResponseDto.getName());
        assertNull(updatedChildResponseDto.getParentEntityId());

        // And: Verify the closure records are updated correctly
        List<DepartmentClosure> childClosureEntities = departmentService.findClosureAncestors(updatedChildResponseDto.getId());
        assertNotNull(childClosureEntities);
        assertEquals(1, childClosureEntities.size());

        assertEquals(updatedChildResponseDto.getId(), childClosureEntities.get(0).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), childClosureEntities.get(0).getDescendantId());
        assertEquals(0, childClosureEntities.get(0).getLevel());
    }

    /**
     * Integration test for updating a department with an invalid name.
     */
    @Test
    public void testUpdateDepartment_InvalidName() {
        // Given: Create an initial root department
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        // When: Attempt to update the department with an invalid name
        rootRequestDto.setId(rootResponseDto.getId());
        rootRequestDto.setName("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> departmentService.updateEntity(rootResponseDto.getId(), rootRequestDto));

        // Then: Verify the ValidationException is thrown
        assertEquals("Entity name cannot be null or empty.", exception.getMessage());
    }

    /**
     * Integration test for updating a department with a non-existent parent.
     */
    @Test
    public void testUpdateDepartment_ParentNotFound() {
        // Given: Create an initial root department
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        // When: Attempt to update the department with a non-existent parent
        rootRequestDto.setId(rootResponseDto.getId());
        rootRequestDto.setName("Updated Root Department");
        rootRequestDto.setParentEntityId(-1L);

        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class,
                () -> departmentService.updateEntity(rootResponseDto.getId(), rootRequestDto));

        // Then: Verify the ParentEntityNotFoundException is thrown
        assertEquals("Parent entity not found.", exception.getMessage());
    }

    /**
     * Integration test for updating a department and verifying closure table updates.
     */
    @Test
    public void testUpdateDepartmentRealSubtree_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for creating a real subtree.
        String rootName = "Root";
        HierarchyRequestDto rootRequestDto = HierarchyRequestDto.builder()
                .name(rootName)
                .build();

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        String anotherRootName = "Another Root";
        HierarchyRequestDto anotherRootRequestDto = HierarchyRequestDto.builder()
                .name(anotherRootName)
                .build();

        HierarchyResponseDto anotherRootResponseDto = departmentService.createEntity(anotherRootRequestDto);

        String childName = "Child";
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name(childName)
                .parentEntityId(rootResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        String grandChildName = "Grand Child";
        HierarchyRequestDto grandChildRequestDto = HierarchyRequestDto.builder()
                .name(grandChildName)
                .parentEntityId(childResponseDto.getId())
                .build();

        HierarchyResponseDto grandChildResponseDto = departmentService.createEntity(grandChildRequestDto);

        // When: Updating the child department's name and parent
        String updatedChildName = "Updated Child";
        HierarchyRequestDto updatedChildRequestDto = HierarchyRequestDto.builder()
                .id(childResponseDto.getId())
                .name(updatedChildName)
                .parentEntityId(anotherRootResponseDto.getId()) // Changing parent
                .build();

        HierarchyResponseDto updatedChildResponseDto = departmentService.updateEntity(childResponseDto.getId(), updatedChildRequestDto);

        // Then: Verify the departments are updated successfully.

        // Verify root department (unchanged)
        assertNotNull(rootResponseDto);
        assertEquals(rootName, rootResponseDto.getName());
        assertNull(rootResponseDto.getParentEntityId());

        List<DepartmentClosure> rootClosureEntities = departmentService.findClosureAncestors(rootResponseDto.getId());
        assertNotNull(rootClosureEntities);
        assertEquals(1, rootClosureEntities.size());

        assertEquals(rootResponseDto.getId(), rootClosureEntities.get(0).getAncestorId());
        assertEquals(rootResponseDto.getId(), rootClosureEntities.get(0).getDescendantId());
        assertEquals(0, rootClosureEntities.get(0).getLevel());

        // Verify updated child department
        assertNotNull(updatedChildResponseDto);
        assertEquals(updatedChildName, updatedChildResponseDto.getName());
        assertEquals(anotherRootResponseDto.getId(), updatedChildResponseDto.getParentEntityId());

        List<DepartmentClosure> updatedChildClosureEntities = departmentService.findClosureAncestors(updatedChildResponseDto.getId());
        assertNotNull(updatedChildClosureEntities);
        assertEquals(2, updatedChildClosureEntities.size());

        assertEquals(updatedChildResponseDto.getId(), updatedChildClosureEntities.get(0).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), updatedChildClosureEntities.get(0).getDescendantId());
        assertEquals(0, updatedChildClosureEntities.get(0).getLevel());

        assertEquals(anotherRootResponseDto.getId(), updatedChildClosureEntities.get(1).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), updatedChildClosureEntities.get(1).getDescendantId());
        assertEquals(1, updatedChildClosureEntities.get(1).getLevel());

        // Verify grand child department (parent updated)
        List<DepartmentClosure> grandChildClosureEntities = departmentService.findClosureAncestors(grandChildResponseDto.getId());
        assertNotNull(grandChildClosureEntities);
        assertEquals(3, grandChildClosureEntities.size());

        assertEquals(grandChildResponseDto.getId(), grandChildClosureEntities.get(0).getAncestorId());
        assertEquals(grandChildResponseDto.getId(), grandChildClosureEntities.get(0).getDescendantId());
        assertEquals(0, grandChildClosureEntities.get(0).getLevel());

        assertEquals(updatedChildResponseDto.getId(), grandChildClosureEntities.get(1).getAncestorId());
        assertEquals(grandChildResponseDto.getId(), grandChildClosureEntities.get(1).getDescendantId());
        assertEquals(1, grandChildClosureEntities.get(1).getLevel());

        assertEquals(anotherRootResponseDto.getId(), grandChildClosureEntities.get(2).getAncestorId());
        assertEquals(grandChildResponseDto.getId(), grandChildClosureEntities.get(2).getDescendantId());
        assertEquals(2, grandChildClosureEntities.get(2).getLevel());

        // Verify anotherRoot department (unchanged)
        assertNotNull(anotherRootResponseDto);
        assertEquals(anotherRootName, anotherRootResponseDto.getName());
        assertNull(anotherRootResponseDto.getParentEntityId());

        List<DepartmentClosure> anotherRootClosureEntities = departmentService.findClosureAncestors(anotherRootResponseDto.getId());
        assertNotNull(anotherRootClosureEntities);
        assertEquals(1, anotherRootClosureEntities.size());

        assertEquals(anotherRootResponseDto.getId(), anotherRootClosureEntities.get(0).getAncestorId());
        assertEquals(anotherRootResponseDto.getId(), anotherRootClosureEntities.get(0).getDescendantId());
        assertEquals(0, anotherRootClosureEntities.get(0).getLevel());
    }

    /**
     * Integration test for updating a child department with empty parent closure.
     */
    @Test
    public void testUpdateChildDepartment_EmptyParentClosure() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: Create an initial root department
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);
        departmentClosureRepository.deleteAll();

        // And: Create a child department under the root department
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Child Department")
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        childRequestDto.setId(childResponseDto.getId());
        childRequestDto.setParentEntityId(rootResponseDto.getId());

        // When: Update the child department's parent.
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.updateEntity(childResponseDto.getId(), childRequestDto);
        });

        // Then: Verify the ParentEntityNotFoundException is thrown.
        assertEquals("Parent entity not found.", exception.getMessage());
    }

    /**
     * Integration test for updating a department successfully.
     */
    @Test
    public void testUpdateDepartment_NoModification() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: Create an initial root department
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        // And: Create a child department under the root department
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Child Department")
                .parentEntityId(rootResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        // When: Update the child department's name
        childRequestDto.setId(childResponseDto.getId());
        childRequestDto.setName("Child Department");

        HierarchyResponseDto updatedChildResponseDto = departmentService.updateEntity(childResponseDto.getId(), childRequestDto);

        // Then: Verify the department is updated successfully
        assertNotNull(updatedChildResponseDto);
        assertEquals("Child Department", updatedChildResponseDto.getName());
        assertEquals(rootResponseDto.getId(), updatedChildResponseDto.getParentEntityId());

        // And: Verify the closure records are updated correctly
        List<DepartmentClosure> childClosureEntities = departmentService.findClosureAncestors(updatedChildResponseDto.getId());
        assertNotNull(childClosureEntities);
        assertEquals(2, childClosureEntities.size());

        assertEquals(updatedChildResponseDto.getId(), childClosureEntities.get(0).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), childClosureEntities.get(0).getDescendantId());
        assertEquals(0, childClosureEntities.get(0).getLevel());

        assertEquals(rootResponseDto.getId(), childClosureEntities.get(1).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), childClosureEntities.get(1).getDescendantId());
        assertEquals(1, childClosureEntities.get(1).getLevel());
    }

    /**
     * Integration test for updating a department with circular dependency.
     */
    @Test
    void testUpdateDepartmentHasCircularDependency() {
        // Given: Create an initial root department
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        // And: Create a child department under the root department
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Child Department")
                .parentEntityId(rootResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        HierarchyRequestDto updateRootRequestDto = HierarchyRequestDto.builder()
                .id(rootResponseDto.getId())
                .name(rootResponseDto.getName())
                .parentEntityId(childResponseDto.getId())
                .build();


        // When: Update the child department's parent.
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateEntity(rootResponseDto.getId(), updateRootRequestDto);
        });

        // Then: Verify the DataIntegrityException is thrown.
        assertEquals("Circular dependency detected: Entity cannot be its own ancestor.", exception.getMessage());
    }

    /**
     * Integration test for deleting a department with non-existing id.
     */
    @Test
    void testDeleteDepartment_NonExistingId_IT() {
        // Given: A non-existing department ID
        Long nonExistingId = -1L;

        // When: Deleting the non-existing department
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.deleteEntity(nonExistingId);
        });

        // Then: Verify it should throw EntityNotFoundException
        assertEquals("Entity not found with id: " + nonExistingId, exception.getMessage());
    }


    /**
     * Integration test for deleting a department with  circular reference.
     */
    @Test
    void testDeleteDepartment_CircularReference_IT() {
        // Given: Create a circular dependency scenario
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Child Department")
                .parentEntityId(rootResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        departmentClosureRepository.save(
                DepartmentClosure.builder()
                        .ancestorId(childResponseDto.getId())
                        .descendantId(rootResponseDto.getId())
                        .level(1)
                        .build()
        );

        // When: Deleting the root department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteEntity(rootResponseDto.getId());
        });

        // Then: Verify it should throw DataIntegrityException due to circular reference
        assertEquals("Circular dependency detected: Entity cannot be its own ancestor.", exception.getMessage());

    }

    /**
     * Integration test for deleting a department with  circular reference.
     */
    @Test
    void testDeleteDepartment_SelfCircularReference_IT() {
        // Given: Create a circular dependency scenario
        HierarchyRequestDto rootRequestDto = new HierarchyRequestDto();
        rootRequestDto.setName("Root Department");

        HierarchyResponseDto rootResponseDto = departmentService.createEntity(rootRequestDto);

        Department department = departmentRepository.findById(rootResponseDto.getId()).get();

        department.setParentId(department.getId());

        departmentRepository.save(department);

        departmentClosureRepository.save(
                DepartmentClosure.builder()
                        .ancestorId(rootResponseDto.getId())
                        .descendantId(rootResponseDto.getId())
                        .level(0)
                        .build()
        );

        // When: Deleting the root department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteEntity(rootResponseDto.getId());
        });

        // Then: Verify it should throw DataIntegrityException due to circular reference
        assertEquals("Circular dependency detected: Entity cannot be its own ancestor.", exception.getMessage());

    }

    /**
     * Integration test for deleting a department with  its descendants successfully.
     */
    @Test
    public void testDeleteDepartment_ExistingId_WithDescendants() throws ParentEntityNotFoundException, DataIntegrityException {
        // Given: A valid parent and child department.
        String parentName = "Parent Department";
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name(parentName)
                .build();

        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);

        String childName = "Child Department";
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name(childName)
                .parentEntityId(parentResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        // When: Deleting the parent department.
        departmentService.deleteEntity(parentResponseDto.getId());

        // Then: Verify the departments are created successfully.
        Department parentDepartment = departmentRepository.findById(parentResponseDto.getId()).orElse(null);
        Department childDepartment = departmentRepository.findById(childResponseDto.getId()).orElse(null);

        assertNull(parentDepartment);
        assertNull(childDepartment);

        List<DepartmentClosure> parentClosureEntities = departmentService.findClosureAncestors(parentResponseDto.getId());
        List<DepartmentClosure> childClosureEntities = departmentService.findClosureAncestors(childResponseDto.getId());

        assertNotNull(parentClosureEntities);
        assertTrue(parentClosureEntities.isEmpty());

        assertNotNull(childClosureEntities);
        assertTrue(childClosureEntities.isEmpty());
    }
    @Test
    public void testGetAllDepartments_noDepartments_IT() {
        // Given: No departments in the repository

        // When: Retrieving all departments
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then: Verify the result is an empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAllDepartments_withDepartments_IT() throws ParentEntityNotFoundException, DataIntegrityException {
        // Given: A list of departments in the repository
        HierarchyRequestDto requestDto1 = HierarchyRequestDto.builder()
                .name("Department 1")
                .build();

        HierarchyRequestDto requestDto2 = HierarchyRequestDto.builder()
                .name("Department 2")
                .build();

        departmentService.createEntity(requestDto1);
        departmentService.createEntity(requestDto2);

        // When: Retrieving all departments
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then: Verify the result contains the expected departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dept -> "Department 1".equals(dept.getName())));
        assertTrue(result.stream().anyMatch(dept -> "Department 2".equals(dept.getName())));
    }

    /**
     * Test for retrieving all departments paginated when the repository is populated.
     */
    @Test
    @Transactional
    public void testGetAllEntities_Paginated() {
        // Given: Prepare test data
        createTestDepartments();

        // Prepare pagination and sorting
        Pageable pageable = PageRequest.of(0, 5, Sort.by("name").descending()); // First page, 5 items per page, sorted by name
        boolean fetchSubEntities = true; // Example: Fetch sub-entities

        entityManager.flush();
        // When: Invoke the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getAllEntities(pageable, fetchSubEntities);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(5, result.getContent().size()); // Expecting 5 items on the first page
        assertEquals(30, result.getTotalElements()); // Total number of departments should be 20

        // Verify the order of the returned entities
        List<String> expectedNames = List.of("Grand Child of Department 9", "Grand Child of Department 8", "Grand Child of Department 7", "Grand Child of Department 6", "Grand Child of Department 5");
        List<String> actualNames = new ArrayList<>();
        for (HierarchyResponseDto dto : result.getContent()) {
            actualNames.add(dto.getName());
        }
        assertEquals(expectedNames, actualNames);

        // Verify sub-entities are fetched correctly
        for (HierarchyResponseDto dto : result.getContent()) {
            if (fetchSubEntities) {
                assertNotNull(dto.getSubEntities());
                dto.getSubEntities().forEach(subEntity ->
                        assertNotNull(subEntity.getSubEntities())
                );
            } else {
                assertNotNull(dto.getSubEntities());
                dto.getSubEntities().forEach(subEntity ->
                        assertNull(subEntity.getSubEntities())
                );
            }
        }
    }

    /**
     * Helper method to create a department and return its DTO.
     *
     * @param name     the name of the department
     * @param parentId the ID of the parent department (nullable)
     * @return the created department DTO
     */
    @Transactional
    protected HierarchyResponseDto createEntity(String name, Long parentId) {
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(name)
                .parentEntityId(parentId)
                .build();

        return departmentService.createEntity(requestDto);
    }

    // Helper method to create test departments
    private List<HierarchyResponseDto> createTestDepartments() {
        List<HierarchyResponseDto> departments = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            HierarchyResponseDto parent = createEntity("Department " + i, null);
            departments.add(parent);
            HierarchyResponseDto child = createEntity("Child of Department " + i, parent.getId());
            departments.add(child);
            HierarchyResponseDto grandChild = createEntity("Grand Child of Department " + i, child.getId());
            departments.add(grandChild);
        }
        return departments;
    }

    @Test
    public void testGetSubDepartments_existingParent_IT() throws ParentEntityNotFoundException, DataIntegrityException {
        // Given: An existing parent department and its sub-departments
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name("Parent Department")
                .build();

        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);

        HierarchyRequestDto subRequestDto1 = HierarchyRequestDto.builder()
                .name("Sub Department 1")
                .parentEntityId(parentResponseDto.getId())
                .build();

        HierarchyRequestDto subRequestDto2 = HierarchyRequestDto.builder()
                .name("Sub Department 2")
                .parentEntityId(parentResponseDto.getId())
                .build();

        departmentService.createEntity(subRequestDto1);
        departmentService.createEntity(subRequestDto2);

        // When: Retrieving sub-departments of the parent department
        List<HierarchyResponseDto> result = departmentService.getSubEntities(parentResponseDto.getId());

        // Then: Verify the result contains the expected sub-departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dept -> "Sub Department 1".equals(dept.getName())));
        assertTrue(result.stream().anyMatch(dept -> "Sub Department 2".equals(dept.getName())));
    }

    @Test
    public void testGetSubDepartments_nonExistingParent_IT() {
        // Given: A non-existent parent department ID
        Long nonExistingParentId = -1L;

        // When & Then: Retrieving sub-departments should throw ParentEntityNotFoundException
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.getSubEntities(nonExistingParentId);
        });

        // Then: Verify it should throw ParentEntityNotFoundException
        assertEquals("Parent entity not found with id: " + nonExistingParentId, exception.getMessage());
    }

    /**
     * Integration test for retrieving paginated sub-entities of a parent entity.
     */
    @Test
    @Transactional
    public void testGetSubEntities_Paginated() throws ParentEntityNotFoundException {
        // Given: Create a hierarchy of entities with a parent and its children
        HierarchyRequestDto parent = HierarchyRequestDto.builder().name("Parent Department").build();
        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parent);

        HierarchyRequestDto child1 = HierarchyRequestDto.builder().name("Child Department 1").parentEntityId(parentResponseDto.getId()).build();
        HierarchyResponseDto child1ResponseDto = departmentService.createEntity(child1);

        HierarchyRequestDto child2 = HierarchyRequestDto.builder().name("Child Department 2").parentEntityId(parentResponseDto.getId()).build();
        HierarchyResponseDto child2ResponseDto = departmentService.createEntity(child2);

        HierarchyRequestDto grandChild1 = HierarchyRequestDto.builder().name("Grand Child Department 1").parentEntityId(child1ResponseDto.getId()).build();
        HierarchyResponseDto grandChild1ResponseDto = departmentService.createEntity(grandChild1);

        HierarchyRequestDto grandChild2 = HierarchyRequestDto.builder().name("Grand Child Department 2").parentEntityId(child2ResponseDto.getId()).build();
        HierarchyResponseDto grandChild2ResponseDto = departmentService.createEntity(grandChild2);

        // Verify the entities are correctly created in the repository
        Department parentDepartment = departmentRepository.findById(parentResponseDto.getId()).orElseThrow();
        Department child1Department = departmentRepository.findById(child1ResponseDto.getId()).orElseThrow();
        Department child2Department = departmentRepository.findById(child2ResponseDto.getId()).orElseThrow();
        Department grandChild1Department = departmentRepository.findById(grandChild1ResponseDto.getId()).orElseThrow();
        Department grandChild2Department = departmentRepository.findById(grandChild2ResponseDto.getId()).orElseThrow();

        assertEquals(parentDepartment.getId(), child1Department.getParentId(), "Child 1 parent ID mismatch");
        assertEquals(parentDepartment.getId(), child2Department.getParentId(), "Child 2 parent ID mismatch");
        assertEquals(child1Department.getId(), grandChild1Department.getParentId(), "Grand Child 1 parent ID mismatch");
        assertEquals(child2Department.getId(), grandChild2Department.getParentId(), "Grand Child 2 parent ID mismatch");

        // Log the current state of the entities
        log.info("Parent Department ID: {}", parentDepartment.getId());
        log.info("Child 1 Department: {}, Parent ID: {}", child1Department.getId(), child1Department.getParentId());
        log.info("Child 2 Department: {}, Parent ID: {}", child2Department.getId(), child2Department.getParentId());
        log.info("Grand Child 1 Department: {}, Parent ID: {}", grandChild1Department.getId(), grandChild1Department.getParentId());
        log.info("Grand Child 2 Department: {}, Parent ID: {}", grandChild2Department.getId(), grandChild2Department.getParentId());

        // Prepare pagination and sorting
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());

        // Flush and clear the persistence context to ensure entities are synchronized with the database
        entityManager.flush();
        entityManager.clear();

        // When: Retrieving sub-entities of the parent entity with pagination and sorting
        PaginatedResponseDto<HierarchyResponseDto> response = departmentService.getSubEntities(parentResponseDto.getId(), pageable, false);

        // Then: The response should contain the correct sub-entities in the correct order
        assertNotNull(response, "Response is null");
        assertNotNull(response.getContent(), "Response content is null");
        assertEquals(2, response.getContent().size(), "Number of sub-entities mismatch");

        // Validate child entities in ascending order by name
        assertEquals(child1ResponseDto.getId(), response.getContent().get(0).getId(), "Child 1 ID mismatch");
        assertEquals(child1ResponseDto.getName(), response.getContent().get(0).getName(), "Child 1 name mismatch");

        assertEquals(child2ResponseDto.getId(), response.getContent().get(1).getId(), "Child 2 ID mismatch");
        assertEquals(child2ResponseDto.getName(), response.getContent().get(1).getName(), "Child 2 name mismatch");

        // No sub-entities of children should be included since fetchSubEntities is false
        assertNotNull(response.getContent());
        response.getContent().forEach(dto -> {
            assertNotNull(dto.getSubEntities());
            dto.getSubEntities().forEach(subEntities ->
                    assertNull(subEntities.getSubEntities(), "Sub-entities should not be fetched")
            );
        });
    }

    @Test
    public void testGetDepartmentById_ExistingDepartment_IT() throws EntityNotFoundException {
        // Given: An existing department
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("Engineering")
                .build();

        HierarchyResponseDto createdDto = departmentService.createEntity(requestDto);

        // When: Retrieving the department by ID
        HierarchyResponseDto result = departmentService.getEntityById(createdDto.getId());

        // Then: Verify the result matches the created department
        assertNotNull(result);
        assertEquals(createdDto.getId(), result.getId());
        assertEquals("Engineering", result.getName());
    }

    @Test
    public void testGetDepartmentById_NonExistingDepartment_IT() {
        // Given: A non-existent department ID
        Long nonExistingId = -1L;

        // When & Then: Retrieving the department should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getEntityById(nonExistingId);
        });

        // Then: Verify it should throw EntityNotFoundException
        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());
    }

    @Test
    public void testSearchDepartmentsByName_ExistingName_IT() {
        // Given: Existing departments with matching names
        String searchName = "Engineering";
        HierarchyRequestDto requestDto1 = HierarchyRequestDto.builder().name("Software Engineering").build();
        HierarchyRequestDto requestDto2 = HierarchyRequestDto.builder().name("Engineering Team").build();

        departmentService.createEntity(requestDto1);
        departmentService.createEntity(requestDto2);

        // When: Searching departments by name
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(searchName);

        // Then: Verify the result matches the expected departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.getName().equals("Software Engineering")));
        assertTrue(result.stream().anyMatch(dto -> dto.getName().equals("Engineering Team")));
    }

    @Test
    public void testSearchDepartmentsByName_NonExistingName_IT() {
        // Given: No departments with the search name
        String searchName = "Marketing";

        // When: Searching departments by name
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(searchName);

        // Then: Verify the result is empty
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Integration test for searching entities by name with pagination and sorting.
     */
    @Test
    @Transactional
    public void testSearchEntitiesByName_Paginated() {
        // Given: Create a hierarchy of entities with names for searching
        HierarchyRequestDto parent1 = HierarchyRequestDto.builder().name("Parent 1 Department").build();
        HierarchyResponseDto parent1ResponseDto = departmentService.createEntity(parent1);

        HierarchyRequestDto parent2 = HierarchyRequestDto.builder().name("Parent 1 Department").build();
        HierarchyResponseDto parent2ResponseDto = departmentService.createEntity(parent2);

        HierarchyRequestDto child1 = HierarchyRequestDto.builder().name("Child Department 1").parentEntityId(parent1ResponseDto.getId()).build();
        HierarchyResponseDto child1ResponseDto = departmentService.createEntity(child1);

        HierarchyRequestDto child2 = HierarchyRequestDto.builder().name("Child Department 2").parentEntityId(parent2ResponseDto.getId()).build();
        HierarchyResponseDto child2ResponseDto = departmentService.createEntity(child2);

        HierarchyRequestDto grandChild1 = HierarchyRequestDto.builder().name("Grand Child Department 1").parentEntityId(child1ResponseDto.getId()).build();
        HierarchyResponseDto grandChild1ResponseDto = departmentService.createEntity(grandChild1);

        HierarchyRequestDto grandChild2 = HierarchyRequestDto.builder().name("Grand Child Department 2").parentEntityId(child2ResponseDto.getId()).build();
        HierarchyResponseDto grandChild2ResponseDto = departmentService.createEntity(grandChild2);

        // Prepare pagination and sorting
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());

        // When: Searching entities by name with pagination and sorting
        PaginatedResponseDto<HierarchyResponseDto> response = departmentService.searchEntitiesByName("Parent", pageable, false);

        // Then: The response should contain the correct entities in the correct order
        assertNotNull(response);
        assertEquals(2, response.getContent().size());

        // Validate parent entities in ascending order by name
        assertEquals(parent1ResponseDto.getId(), response.getContent().get(0).getId());
        assertEquals(parent1ResponseDto.getName(), response.getContent().get(0).getName());

        assertEquals(parent2ResponseDto.getId(), response.getContent().get(1).getId());
        assertEquals(parent2ResponseDto.getName(), response.getContent().get(1).getName());

        // No children should be included since fetchSubEntities is false
        assertNotNull(response.getContent());
        response.getContent().forEach(dto -> {
            assertNotNull(dto.getSubEntities());
            dto.getSubEntities().forEach(subEntities ->
                    assertNull(subEntities.getSubEntities(), "Sub-entities should not be fetched")
            );
        });
    }

    @Test
    public void testGetParentDepartment_existingParent_IT() {
        // Given: Create parent and child departments
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name("Parent Department")
                .build();

        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);

        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Child Department")
                .parentEntityId(parentResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        // When: Retrieving parent department for child department
        HierarchyResponseDto result = departmentService.getParentEntity(childResponseDto.getId());

        // Then: Verify the parent department is returned correctly
        assertNotNull(result);
        assertEquals(parentResponseDto.getId(), result.getId());
        assertEquals(parentResponseDto.getName(), result.getName());
    }

    @Test
    public void testGetParentDepartment_nonExistingDepartment_IT() {
        // Given: Non-existing department ID
        Long nonExistingId = -1L;

        // When & Then: Retrieving parent department should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getParentEntity(nonExistingId);
        });

        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());
    }

    @Test
    public void testGetParentDepartment_noParent_IT() {
        // Given: Create a department with no parent
        Department department = Department.builder()
                .name("Single Department")
                .build();

        Department savedDepartment = departmentRepository.save(department);


        // When & Then: Retrieving parent department should throw ParentEntityNotFoundException
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.getParentEntity(savedDepartment.getId());
        });

        assertEquals("Entity with ID " + savedDepartment.getId() + " has no parent entity", exception.getMessage());
    }

    @Test
    public void testGetDescendants_existingDepartment_IT() {
        // Given: Create a department with descendants
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name("Parent Department")
                .build();

        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);

        HierarchyRequestDto child1RequestDto = HierarchyRequestDto.builder()
                .name("Child Department 1")
                .parentEntityId(parentResponseDto.getId())
                .build();

        HierarchyResponseDto child1ResponseDto = departmentService.createEntity(child1RequestDto);

        HierarchyRequestDto child2RequestDto = HierarchyRequestDto.builder()
                .name("Child Department 2")
                .parentEntityId(parentResponseDto.getId())
                .build();

        HierarchyResponseDto child2ResponseDto = departmentService.createEntity(child2RequestDto);

        // When: Retrieving descendants for parent department
        List<HierarchyResponseDto> result = departmentService.getDescendants(parentResponseDto.getId());

        // Then: Verify descendants are returned correctly
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.getId().equals(child1ResponseDto.getId())));
        assertTrue(result.stream().anyMatch(dto -> dto.getId().equals(child2ResponseDto.getId())));
        assertFalse(result.stream().anyMatch(dto -> dto.getId().equals(parentResponseDto.getId()))); // Ensure parent is not included
    }

    @Test
    public void testGetDescendants_nonExistingDepartment_IT() {
        // Given: Non-existing department ID
        Long nonExistingId = -1L;

        // When & Then: Retrieving descendants should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistingId);
        });

        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());
    }

    /**
     * Test the retrieval of paginated descendants for a given entity.
     */
    @Test
    public void testGetDescendants_Paginated() {

        // Given: Create a hierarchy of entities
        HierarchyRequestDto grandparent = HierarchyRequestDto.builder().name("Grandparent Department").build();
        HierarchyResponseDto grandparentResponseDto = departmentService.createEntity(grandparent);

        HierarchyRequestDto parent = HierarchyRequestDto.builder().name("Parent Department").parentEntityId(grandparentResponseDto.getId()).build();
        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parent);

        HierarchyRequestDto child1 = HierarchyRequestDto.builder().name("Child Department 1").parentEntityId(parentResponseDto.getId()).build();
        HierarchyResponseDto child1ResponseDto = departmentService.createEntity(child1);

        HierarchyRequestDto child2 = HierarchyRequestDto.builder().name("Child Department 2").parentEntityId(parentResponseDto.getId()).build();
        HierarchyResponseDto child2ResponseDto = departmentService.createEntity(child2);

        HierarchyRequestDto grandchild = HierarchyRequestDto.builder().name("Grandchild Department").parentEntityId(child1ResponseDto.getId()).build();
        HierarchyResponseDto grandchildResponseDto = departmentService.createEntity(grandchild);

        // Prepare pagination and sorting
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());

        // When: Fetching the descendants of the parent entity with pagination and sorting
        PaginatedResponseDto<HierarchyResponseDto> response = departmentService.getDescendants(parentResponseDto.getId(), pageable, false);

        // Then: The response should contain the correct descendants in the correct order
        assertNotNull(response);
        assertEquals(3, response.getContent().size());

        // Validate child1
        assertEquals(child1ResponseDto.getId(), response.getContent().get(0).getId());
        assertEquals(child1ResponseDto.getName(), response.getContent().get(0).getName());

        // Validate child2
        assertEquals(child2ResponseDto.getId(), response.getContent().get(1).getId());
        assertEquals(child2ResponseDto.getName(), response.getContent().get(1).getName());

        // Validate grandchild
        assertEquals(grandchildResponseDto.getId(), response.getContent().get(2).getId());
        assertEquals(grandchildResponseDto.getName(), response.getContent().get(2).getName());
    }

    @Test
    void testGetAncestors_existingDepartment() throws EntityNotFoundException {
        // Given: Create a department hierarchy
        String grandParentName = "Grand Parent Department";
        HierarchyRequestDto grandParentRequestDto = HierarchyRequestDto.builder()
                .name(grandParentName)
                .build();

        HierarchyResponseDto grandParentResponseDto = departmentService.createEntity(grandParentRequestDto);

        String parentName = "Parent Department";
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name(parentName)
                .parentEntityId(grandParentResponseDto.getId())
                .build();

        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);

        String childName = "Child Department";
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name(childName)
                .parentEntityId(parentResponseDto.getId())
                .build();

        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        // When: Retrieve ancestors
        List<HierarchyResponseDto> ancestors = departmentService.getAncestors(childResponseDto.getId());

        // Then: Verify ancestors
        assertEquals(2, ancestors.size());
        assertEquals(parentName, ancestors.get(0).getName());
        assertEquals(grandParentName, ancestors.get(1).getName());
    }

    @Test
    void testGetAncestors_nonExistingDepartment() {
        // When & Then: Retrieve ancestors for a non-existing department
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getAncestors(-1L);
        });

        assertEquals("Entity not found with ID: -1", exception.getMessage());
    }

    /**
     * Test the retrieval of paginated ancestors for a given entity.
     */
    @Test
    public void testGetAncestors_Paginated() {
        // Prepare data
        HierarchyRequestDto grandparent = HierarchyRequestDto.builder().name("Grandparent Department").build();
        HierarchyResponseDto grandparentResponseDto = departmentService.createEntity(grandparent);

        HierarchyRequestDto parent = HierarchyRequestDto.builder().name("Parent Department").parentEntityId(grandparentResponseDto.getId()).build();
        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parent);

        HierarchyRequestDto child = HierarchyRequestDto.builder().name("Child Department").parentEntityId(parentResponseDto.getId()).build();
        HierarchyResponseDto childResponseDto = departmentService.createEntity(child);

        HierarchyRequestDto grandchild = HierarchyRequestDto.builder().name("Grandchild Department").parentEntityId(childResponseDto.getId()).build();
        HierarchyResponseDto grandchildResponseDto = departmentService.createEntity(grandchild);

        // Prepare pagination and sorting
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());

        // Call the service method
        PaginatedResponseDto<HierarchyResponseDto> response = departmentService.getAncestors(grandchildResponseDto.getId(), pageable, false);

        // Assertions
        assertNotNull(response);
        assertEquals(3, response.getContent().size());

        // Validate grandparent
        assertEquals(grandparentResponseDto.getId(), response.getContent().get(0).getId());
        assertEquals(grandparentResponseDto.getName(), response.getContent().get(0).getName());

        // Validate parent
        assertEquals(parentResponseDto.getId(), response.getContent().get(1).getId());
        assertEquals(parentResponseDto.getName(), response.getContent().get(1).getName());

        // Validate child
        assertEquals(childResponseDto.getId(), response.getContent().get(2).getId());
        assertEquals(childResponseDto.getName(), response.getContent().get(2).getName());
    }
}
