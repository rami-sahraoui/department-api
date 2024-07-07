package tn.engn.departmentapi.service;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tn.engn.departmentapi.TestContainerSetup;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.exception.DataIntegrityException;
import tn.engn.departmentapi.exception.DepartmentNotFoundException;
import tn.engn.departmentapi.exception.ParentDepartmentNotFoundException;
import tn.engn.departmentapi.exception.ValidationException;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.model.DepartmentClosure;
import tn.engn.departmentapi.repository.DepartmentClosureRepository;
import tn.engn.departmentapi.repository.DepartmentRepository;

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

    @Value("${department.max-name-length}")
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
                .parentDepartmentId(parentId)
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
    public void testCreateRootDepartment_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for a root department.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Root Department");

        // When: Creating the root department.
        DepartmentResponseDto result = departmentService.createDepartment(requestDto);

        // Then: Verify the department is created successfully.
        assertNotNull(result);
        assertEquals("Root Department", result.getName());
        assertNull(result.getParentDepartmentId());

        List<DepartmentClosure> closureEntities = departmentClosureRepository.findByDescendantId(result.getId());
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
    public void testCreateChildDepartment_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for a child department.
        String parentName = "Parent Department";
        DepartmentRequestDto parentRequestDto = DepartmentRequestDto.builder()
                .name(parentName)
                .build();

        DepartmentResponseDto parentResponseDto = departmentService.createDepartment(parentRequestDto);

        String childName = "Child Department";
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name(childName)
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        // When: Creating the child department.
        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        // Then: Verify the departments are created successfully.
        assertNotNull(parentResponseDto);
        assertEquals(parentName, parentResponseDto.getName());
        assertNull(parentResponseDto.getParentDepartmentId());

        List<DepartmentClosure> parentClosureEntities = departmentClosureRepository.findByDescendantId(parentResponseDto.getId());
        assertNotNull(parentClosureEntities);
        assertEquals(1, parentClosureEntities.size());

        assertEquals(parentResponseDto.getId(), parentClosureEntities.get(0).getAncestorId());
        assertEquals(parentResponseDto.getId(), parentClosureEntities.get(0).getDescendantId());
        assertEquals(0, parentClosureEntities.get(0).getLevel());

        assertNotNull(childResponseDto);
        assertEquals(childName, childResponseDto.getName());
        assertEquals(parentResponseDto.getId(), childResponseDto.getParentDepartmentId());

        List<DepartmentClosure> childClosureEntities = departmentClosureRepository.findByDescendantId(childResponseDto.getId());
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
    public void testCreateRealSubtree_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for a real subtree.
        String rootName = "Root";
        DepartmentRequestDto rootRequestDto = DepartmentRequestDto.builder()
                .name(rootName)
                .build();

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        String anotherRootName = "Another Root";
        DepartmentRequestDto anotherRootRequestDto = DepartmentRequestDto.builder()
                .name(anotherRootName)
                .build();

        DepartmentResponseDto anotherRootResponseDto = departmentService.createDepartment(anotherRootRequestDto);

        String childName = "Child";
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name(childName)
                .parentDepartmentId(rootResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        String grandChildName = "Grand Child";
        DepartmentRequestDto grandChildRequestDto = DepartmentRequestDto.builder()
                .name(grandChildName)
                .parentDepartmentId(childResponseDto.getId())
                .build();

        // When: Creating the grand child department.
        DepartmentResponseDto grandChildResponseDto = departmentService.createDepartment(grandChildRequestDto);

        // Then: Verify the departments are created successfully.
        // Verify root department
        assertNotNull(rootResponseDto);
        assertEquals(rootName, rootResponseDto.getName());
        assertNull(rootResponseDto.getParentDepartmentId());

        List<DepartmentClosure> rootClosureEntities = departmentClosureRepository.findByDescendantId(rootResponseDto.getId());
        assertNotNull(rootClosureEntities);
        assertEquals(1, rootClosureEntities.size());

        assertEquals(rootResponseDto.getId(), rootClosureEntities.get(0).getAncestorId());
        assertEquals(rootResponseDto.getId(), rootClosureEntities.get(0).getDescendantId());
        assertEquals(0, rootClosureEntities.get(0).getLevel());

        // Verify child department
        assertNotNull(childResponseDto);
        assertEquals(childName, childResponseDto.getName());
        assertEquals(rootResponseDto.getId(), childResponseDto.getParentDepartmentId());

        List<DepartmentClosure> childClosureEntities = departmentClosureRepository.findByDescendantId(childResponseDto.getId());
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
        assertEquals(childResponseDto.getId(), grandChildResponseDto.getParentDepartmentId());

        List<DepartmentClosure> grandChildClosureEntities = departmentClosureRepository.findByDescendantId(grandChildResponseDto.getId());
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
        assertNull(anotherRootResponseDto.getParentDepartmentId());

        List<DepartmentClosure> anotherRootClosureEntities = departmentClosureRepository.findByDescendantId(anotherRootResponseDto.getId());
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
    public void testCreateChildDepartment_EmptyParentClosure() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for a child department.
        String parentName = "Parent Department";
        DepartmentRequestDto parentRequestDto = DepartmentRequestDto.builder()
                .name(parentName)
                .build();

        DepartmentResponseDto parentResponseDto = departmentService.createDepartment(parentRequestDto);
        departmentClosureRepository.deleteAll();

        String childName = "Child Department";
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name(childName)
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        // When: Creating the child department.
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.createDepartment(childRequestDto);
        });

        // Then: Verify the ParentDepartmentNotFoundException is thrown.
        assertEquals("Parent department not found.", exception.getMessage());
    }

    /**
     * Integration test for creating a department with an invalid name.
     */
    @Test
    public void testCreateDepartment_InvalidName() {
        // Given: A DepartmentRequestDto with an invalid name.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("");

        // When: Creating the department.
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then: Verify the ValidationException is thrown.
        assertEquals("Department name cannot be null or empty.", exception.getMessage());
    }

    /**
     * Integration test for creating a department with a parent not found.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given: A DepartmentRequestDto with a non-existent parent ID.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("New Department");
        requestDto.setParentDepartmentId(-1L);

        // When: Creating the department.
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then: Verify the ParentDepartmentNotFoundException is thrown.
        assertEquals("Parent department not found.", exception.getMessage());
    }

    /**
     * Integration test for creating a department with a name that is too long.
     */
    @Test
    public void testCreateDepartment_NameTooLong() {
        // Given: A department name exceeding the maximum length
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("A".repeat(maxNameLength + 1));

        // When: Attempting to create the department
        ValidationException exception = assertThrows(ValidationException.class,
                () -> departmentService.createDepartment(requestDto));

        // Then: Verify the ValidationException is thrown.
        assertEquals("Department name cannot be longer than " + maxNameLength + " characters.", exception.getMessage());
    }

    /**
     * Integration test for updating a department successfully.
     */
    @Test
    public void testUpdateDepartment_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: Create an initial root department
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        // And: Create a child department under the root department
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name("Child Department")
                .parentDepartmentId(rootResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        // When: Update the child department's name
        childRequestDto.setId(childResponseDto.getId());
        childRequestDto.setName("Updated Child Department");

        DepartmentResponseDto updatedChildResponseDto = departmentService.updateDepartment(childResponseDto.getId(), childRequestDto);

        // Then: Verify the department is updated successfully
        assertNotNull(updatedChildResponseDto);
        assertEquals("Updated Child Department", updatedChildResponseDto.getName());
        assertEquals(rootResponseDto.getId(), updatedChildResponseDto.getParentDepartmentId());

        // And: Verify the closure records are updated correctly
        List<DepartmentClosure> childClosureEntities = departmentClosureRepository.findByDescendantId(updatedChildResponseDto.getId());
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
    public void testUpdateDepartment_ToRoot_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: Create an initial root department
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        // And: Create a child department under the root department
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name("Child Department")
                .parentDepartmentId(rootResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        // When: Update the child department to be a root department
        childRequestDto.setId(childResponseDto.getId());
        childRequestDto.setName("Updated Child Department");
        childRequestDto.setParentDepartmentId(null);

        DepartmentResponseDto updatedChildResponseDto = departmentService.updateDepartment(childResponseDto.getId(), childRequestDto);

        // Then: Verify the department is updated successfully
        assertNotNull(updatedChildResponseDto);
        assertEquals("Updated Child Department", updatedChildResponseDto.getName());
        assertNull(updatedChildResponseDto.getParentDepartmentId());

        // And: Verify the closure records are updated correctly
        List<DepartmentClosure> childClosureEntities = departmentClosureRepository.findByDescendantId(updatedChildResponseDto.getId());
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
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        // When: Attempt to update the department with an invalid name
        rootRequestDto.setId(rootResponseDto.getId());
        rootRequestDto.setName("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> departmentService.updateDepartment(rootResponseDto.getId(), rootRequestDto));

        // Then: Verify the ValidationException is thrown
        assertEquals("Department name cannot be null or empty.", exception.getMessage());
    }

    /**
     * Integration test for updating a department with a non-existent parent.
     */
    @Test
    public void testUpdateDepartment_ParentNotFound() {
        // Given: Create an initial root department
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        // When: Attempt to update the department with a non-existent parent
        rootRequestDto.setId(rootResponseDto.getId());
        rootRequestDto.setName("Updated Root Department");
        rootRequestDto.setParentDepartmentId(-1L);

        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class,
                () -> departmentService.updateDepartment(rootResponseDto.getId(), rootRequestDto));

        // Then: Verify the ParentDepartmentNotFoundException is thrown
        assertEquals("Parent department not found.", exception.getMessage());
    }

    /**
     * Integration test for updating a department and verifying closure table updates.
     */
    @Test
    public void testUpdateDepartmentRealSubtree_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for creating a real subtree.
        String rootName = "Root";
        DepartmentRequestDto rootRequestDto = DepartmentRequestDto.builder()
                .name(rootName)
                .build();

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        String anotherRootName = "Another Root";
        DepartmentRequestDto anotherRootRequestDto = DepartmentRequestDto.builder()
                .name(anotherRootName)
                .build();

        DepartmentResponseDto anotherRootResponseDto = departmentService.createDepartment(anotherRootRequestDto);

        String childName = "Child";
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name(childName)
                .parentDepartmentId(rootResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        String grandChildName = "Grand Child";
        DepartmentRequestDto grandChildRequestDto = DepartmentRequestDto.builder()
                .name(grandChildName)
                .parentDepartmentId(childResponseDto.getId())
                .build();

        DepartmentResponseDto grandChildResponseDto = departmentService.createDepartment(grandChildRequestDto);

        // When: Updating the child department's name and parent
        String updatedChildName = "Updated Child";
        DepartmentRequestDto updatedChildRequestDto = DepartmentRequestDto.builder()
                .id(childResponseDto.getId())
                .name(updatedChildName)
                .parentDepartmentId(anotherRootResponseDto.getId()) // Changing parent
                .build();

        DepartmentResponseDto updatedChildResponseDto = departmentService.updateDepartment(childResponseDto.getId(), updatedChildRequestDto);

        // Then: Verify the departments are updated successfully.

        // Verify root department (unchanged)
        assertNotNull(rootResponseDto);
        assertEquals(rootName, rootResponseDto.getName());
        assertNull(rootResponseDto.getParentDepartmentId());

        List<DepartmentClosure> rootClosureEntities = departmentClosureRepository.findByDescendantId(rootResponseDto.getId());
        assertNotNull(rootClosureEntities);
        assertEquals(1, rootClosureEntities.size());

        assertEquals(rootResponseDto.getId(), rootClosureEntities.get(0).getAncestorId());
        assertEquals(rootResponseDto.getId(), rootClosureEntities.get(0).getDescendantId());
        assertEquals(0, rootClosureEntities.get(0).getLevel());

        // Verify updated child department
        assertNotNull(updatedChildResponseDto);
        assertEquals(updatedChildName, updatedChildResponseDto.getName());
        assertEquals(anotherRootResponseDto.getId(), updatedChildResponseDto.getParentDepartmentId());

        List<DepartmentClosure> updatedChildClosureEntities = departmentClosureRepository.findByDescendantId(updatedChildResponseDto.getId());
        assertNotNull(updatedChildClosureEntities);
        assertEquals(2, updatedChildClosureEntities.size());

        assertEquals(updatedChildResponseDto.getId(), updatedChildClosureEntities.get(0).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), updatedChildClosureEntities.get(0).getDescendantId());
        assertEquals(0, updatedChildClosureEntities.get(0).getLevel());

        assertEquals(anotherRootResponseDto.getId(), updatedChildClosureEntities.get(1).getAncestorId());
        assertEquals(updatedChildResponseDto.getId(), updatedChildClosureEntities.get(1).getDescendantId());
        assertEquals(1, updatedChildClosureEntities.get(1).getLevel());

        // Verify grand child department (parent updated)
        List<DepartmentClosure> grandChildClosureEntities = departmentClosureRepository.findByDescendantId(grandChildResponseDto.getId());
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
        assertNull(anotherRootResponseDto.getParentDepartmentId());

        List<DepartmentClosure> anotherRootClosureEntities = departmentClosureRepository.findByDescendantId(anotherRootResponseDto.getId());
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
    public void testUpdateChildDepartment_EmptyParentClosure() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: Create an initial root department
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);
        departmentClosureRepository.deleteAll();

        // And: Create a child department under the root department
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name("Child Department")
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        childRequestDto.setId(childResponseDto.getId());
        childRequestDto.setParentDepartmentId(rootResponseDto.getId());

        // When: Update the child department's parent.
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.updateDepartment(childResponseDto.getId(), childRequestDto);
        });

        // Then: Verify the ParentDepartmentNotFoundException is thrown.
        assertEquals("Parent department not found.", exception.getMessage());
    }

    /**
     * Integration test for updating a department successfully.
     */
    @Test
    public void testUpdateDepartment_NoModification() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: Create an initial root department
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        // And: Create a child department under the root department
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name("Child Department")
                .parentDepartmentId(rootResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        // When: Update the child department's name
        childRequestDto.setId(childResponseDto.getId());
        childRequestDto.setName("Child Department");

        DepartmentResponseDto updatedChildResponseDto = departmentService.updateDepartment(childResponseDto.getId(), childRequestDto);

        // Then: Verify the department is updated successfully
        assertNotNull(updatedChildResponseDto);
        assertEquals("Child Department", updatedChildResponseDto.getName());
        assertEquals(rootResponseDto.getId(), updatedChildResponseDto.getParentDepartmentId());

        // And: Verify the closure records are updated correctly
        List<DepartmentClosure> childClosureEntities = departmentClosureRepository.findByDescendantId(updatedChildResponseDto.getId());
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
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        // And: Create a child department under the root department
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name("Child Department")
                .parentDepartmentId(rootResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        DepartmentRequestDto updateRootRequestDto = DepartmentRequestDto.builder()
                .id(rootResponseDto.getId())
                .name(rootResponseDto.getName())
                .parentDepartmentId(childResponseDto.getId())
                .build();


        // When: Update the child department's parent.
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateDepartment(rootResponseDto.getId(), updateRootRequestDto);
        });

        // Then: Verify the DataIntegrityException is thrown.
        assertEquals("Circular dependency detected: Department cannot be its own ancestor.", exception.getMessage());
    }

    /**
     * Integration test for deleting a department with non-existing id.
     */
    @Test
    void testDeleteDepartment_NonExistingId_IT() {
        // Given: A non-existing department ID
        Long nonExistingId = -1L;

        // When: Deleting the non-existing department
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.deleteDepartment(nonExistingId);
        });

        // Then: Verify it should throw DepartmentNotFoundException
        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());
    }


    /**
     * Integration test for deleting a department with  circular reference.
     */
    @Test
    void testDeleteDepartment_CircularReference_IT() {
        // Given: Create a circular dependency scenario
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name("Child Department")
                .parentDepartmentId(rootResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        departmentClosureRepository.save(
                DepartmentClosure.builder()
                        .ancestorId(childResponseDto.getId())
                        .descendantId(rootResponseDto.getId())
                        .level(1)
                        .build()
        );

        // When: Deleting the root department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteDepartment(rootResponseDto.getId());
        });

        // Then: Verify it should throw DataIntegrityException due to circular reference
        assertEquals("Circular dependency detected: Department cannot be its own ancestor.", exception.getMessage());

    }

    /**
     * Integration test for deleting a department with  circular reference.
     */
    @Test
    void testDeleteDepartment_SelfCircularReference_IT() {
        // Given: Create a circular dependency scenario
        DepartmentRequestDto rootRequestDto = new DepartmentRequestDto();
        rootRequestDto.setName("Root Department");

        DepartmentResponseDto rootResponseDto = departmentService.createDepartment(rootRequestDto);

        Department department = departmentRepository.findById(rootResponseDto.getId()).get();

        department.setParentDepartmentId(department.getId());

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
            departmentService.deleteDepartment(rootResponseDto.getId());
        });

        // Then: Verify it should throw DataIntegrityException due to circular reference
        assertEquals("Circular dependency detected: Department cannot be its own ancestor.", exception.getMessage());

    }

    /**
     * Integration test for deleting a department with  its descendants successfully.
     */
    @Test
    public void testDeleteDepartment_ExistingId_WithDescendants() throws ParentDepartmentNotFoundException, DataIntegrityException {
        // Given: A valid parent and child department.
        String parentName = "Parent Department";
        DepartmentRequestDto parentRequestDto = DepartmentRequestDto.builder()
                .name(parentName)
                .build();

        DepartmentResponseDto parentResponseDto = departmentService.createDepartment(parentRequestDto);

        String childName = "Child Department";
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name(childName)
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        // When: Deleting the parent department.
        departmentService.deleteDepartment(parentResponseDto.getId());

        // Then: Verify the departments are created successfully.
        Department parentDepartment = departmentRepository.findById(parentResponseDto.getId()).orElse(null);
        Department childDepartment = departmentRepository.findById(childResponseDto.getId()).orElse(null);

        assertNull(parentDepartment);
        assertNull(childDepartment);

        List<DepartmentClosure> parentClosureEntities = departmentClosureRepository.findByDescendantId(parentResponseDto.getId());
        List<DepartmentClosure> childClosureEntities = departmentClosureRepository.findByDescendantId(childResponseDto.getId());

        assertNotNull(parentClosureEntities);
        assertTrue(parentClosureEntities.isEmpty());

        assertNotNull(childClosureEntities);
        assertTrue(childClosureEntities.isEmpty());
    }
    @Test
    public void testGetAllDepartments_noDepartments_IT() {
        // Given: No departments in the repository

        // When: Retrieving all departments
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then: Verify the result is an empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAllDepartments_withDepartments_IT() throws ParentDepartmentNotFoundException, DataIntegrityException {
        // Given: A list of departments in the repository
        DepartmentRequestDto requestDto1 = DepartmentRequestDto.builder()
                .name("Department 1")
                .build();

        DepartmentRequestDto requestDto2 = DepartmentRequestDto.builder()
                .name("Department 2")
                .build();

        departmentService.createDepartment(requestDto1);
        departmentService.createDepartment(requestDto2);

        // When: Retrieving all departments
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then: Verify the result contains the expected departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dept -> "Department 1".equals(dept.getName())));
        assertTrue(result.stream().anyMatch(dept -> "Department 2".equals(dept.getName())));
    }

    @Test
    public void testGetSubDepartments_existingParent_IT() throws ParentDepartmentNotFoundException, DataIntegrityException {
        // Given: An existing parent department and its sub-departments
        DepartmentRequestDto parentRequestDto = DepartmentRequestDto.builder()
                .name("Parent Department")
                .build();

        DepartmentResponseDto parentResponseDto = departmentService.createDepartment(parentRequestDto);

        DepartmentRequestDto subRequestDto1 = DepartmentRequestDto.builder()
                .name("Sub Department 1")
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        DepartmentRequestDto subRequestDto2 = DepartmentRequestDto.builder()
                .name("Sub Department 2")
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        departmentService.createDepartment(subRequestDto1);
        departmentService.createDepartment(subRequestDto2);

        // When: Retrieving sub-departments of the parent department
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(parentResponseDto.getId());

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

        // When & Then: Retrieving sub-departments should throw ParentDepartmentNotFoundException
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.getSubDepartments(nonExistingParentId);
        });

        // Then: Verify it should throw ParentDepartmentNotFoundException
        assertEquals("Parent department not found with id: " + nonExistingParentId, exception.getMessage());
    }

    @Test
    public void testGetDepartmentById_ExistingDepartment_IT() throws DepartmentNotFoundException {
        // Given: An existing department
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Engineering")
                .build();

        DepartmentResponseDto createdDto = departmentService.createDepartment(requestDto);

        // When: Retrieving the department by ID
        DepartmentResponseDto result = departmentService.getDepartmentById(createdDto.getId());

        // Then: Verify the result matches the created department
        assertNotNull(result);
        assertEquals(createdDto.getId(), result.getId());
        assertEquals("Engineering", result.getName());
    }

    @Test
    public void testGetDepartmentById_NonExistingDepartment_IT() {
        // Given: A non-existent department ID
        Long nonExistingId = -1L;

        // When & Then: Retrieving the department should throw DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDepartmentById(nonExistingId);
        });

        // Then: Verify it should throw DepartmentNotFoundException
        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());
    }

    @Test
    public void testSearchDepartmentsByName_ExistingName_IT() {
        // Given: Existing departments with matching names
        String searchName = "Engineering";
        DepartmentRequestDto requestDto1 = DepartmentRequestDto.builder().name("Software Engineering").build();
        DepartmentRequestDto requestDto2 = DepartmentRequestDto.builder().name("Engineering Team").build();

        departmentService.createDepartment(requestDto1);
        departmentService.createDepartment(requestDto2);

        // When: Searching departments by name
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(searchName);

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
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(searchName);

        // Then: Verify the result is empty
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetParentDepartment_existingParent_IT() {
        // Given: Create parent and child departments
        DepartmentRequestDto parentRequestDto = DepartmentRequestDto.builder()
                .name("Parent Department")
                .build();

        DepartmentResponseDto parentResponseDto = departmentService.createDepartment(parentRequestDto);

        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name("Child Department")
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        // When: Retrieving parent department for child department
        DepartmentResponseDto result = departmentService.getParentDepartment(childResponseDto.getId());

        // Then: Verify the parent department is returned correctly
        assertNotNull(result);
        assertEquals(parentResponseDto.getId(), result.getId());
        assertEquals(parentResponseDto.getName(), result.getName());
    }

    @Test
    public void testGetParentDepartment_nonExistingDepartment_IT() {
        // Given: Non-existing department ID
        Long nonExistingId = -1L;

        // When & Then: Retrieving parent department should throw DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(nonExistingId);
        });

        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());
    }

    @Test
    public void testGetParentDepartment_noParent_IT() {
        // Given: Create a department with no parent
        Department department = Department.builder()
                .name("Single Department")
                .build();

        Department savedDepartment = departmentRepository.save(department);


        // When & Then: Retrieving parent department should throw ParentDepartmentNotFoundException
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(savedDepartment.getId());
        });

        assertEquals("Department has no parent.", exception.getMessage());
    }

    @Test
    public void testGetDescendants_existingDepartment_IT() {
        // Given: Create a department with descendants
        DepartmentRequestDto parentRequestDto = DepartmentRequestDto.builder()
                .name("Parent Department")
                .build();

        DepartmentResponseDto parentResponseDto = departmentService.createDepartment(parentRequestDto);

        DepartmentRequestDto child1RequestDto = DepartmentRequestDto.builder()
                .name("Child Department 1")
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        DepartmentResponseDto child1ResponseDto = departmentService.createDepartment(child1RequestDto);

        DepartmentRequestDto child2RequestDto = DepartmentRequestDto.builder()
                .name("Child Department 2")
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        DepartmentResponseDto child2ResponseDto = departmentService.createDepartment(child2RequestDto);

        // When: Retrieving descendants for parent department
        List<DepartmentResponseDto> result = departmentService.getDescendants(parentResponseDto.getId());

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

        // When & Then: Retrieving descendants should throw DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistingId);
        });

        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());
    }

    @Test
    void testGetAncestors_existingDepartment() throws DepartmentNotFoundException {
        // Given: Create a department hierarchy
        String grandParentName = "Grand Parent Department";
        DepartmentRequestDto grandParentRequestDto = DepartmentRequestDto.builder()
                .name(grandParentName)
                .build();

        DepartmentResponseDto grandParentResponseDto = departmentService.createDepartment(grandParentRequestDto);

        String parentName = "Parent Department";
        DepartmentRequestDto parentRequestDto = DepartmentRequestDto.builder()
                .name(parentName)
                .parentDepartmentId(grandParentResponseDto.getId())
                .build();

        DepartmentResponseDto parentResponseDto = departmentService.createDepartment(parentRequestDto);

        String childName = "Child Department";
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name(childName)
                .parentDepartmentId(parentResponseDto.getId())
                .build();

        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        // When: Retrieve ancestors
        List<DepartmentResponseDto> ancestors = departmentService.getAncestors(childResponseDto.getId());

        // Then: Verify ancestors
        assertEquals(2, ancestors.size());
        assertEquals(parentName, ancestors.get(0).getName());
        assertEquals(grandParentName, ancestors.get(1).getName());
    }

    @Test
    void testGetAncestors_nonExistingDepartment() {
        // When & Then: Retrieve ancestors for a non-existing department
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getAncestors(-1L);
        });

        assertEquals("Department not found with id: -1", exception.getMessage());
    }
}
