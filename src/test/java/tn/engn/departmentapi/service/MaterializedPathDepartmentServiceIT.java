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
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
public class MaterializedPathDepartmentServiceIT extends TestContainerSetup {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @Autowired
    private MaterializedPathDepartmentService departmentService;

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
    }

    /**
     * Clean up the database after each test to ensure isolation.
     */
    @AfterEach
    public void cleanUp() {
        departmentRepository.deleteAll();
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
     * @param path      the path representing the hierarchical position of the department
     * @param parentId  the unique identifier of the parent department, or null if this is a root department
     * @return the created Department entity
     *
     * This method constructs a new Department entity using the provided parameters and
     * saves it to the database. The method ensures that the entity is properly built
     * using the Department builder and then persisted using the departmentRepository.
     * It is a utility method intended for use in test cases to set up initial test data.
     */
    private Department createDepartment(Long id, String name, String path, Long parentId) {
        Department department = Department.builder()
                .id(id)
                .name(name)
                .path(path)
                .parentDepartmentId(parentId)
                .build();
        return departmentRepository.save(department);
    }


    /**
     * Integration test for successfully creating a child department.
     */
    @Test
    public void testCreateChildDepartment_Success() {
        // Given: A root department
        Department rootDepartment = createDepartment(1L, "Root", "/1/", null);

        // When: Creating a child department under the root
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Child");
        requestDto.setParentDepartmentId(rootDepartment.getId());
        DepartmentResponseDto responseDto = departmentService.createDepartment(requestDto);

        // Then: Verify the child department is created successfully
        assertNotNull(responseDto.getId());
        assertEquals("Child", responseDto.getName());
        Department childDepartment = departmentRepository.findById(responseDto.getId()).orElse(null);
        assertNotNull(childDepartment);
        assertEquals("/1/" + childDepartment.getId() + "/", childDepartment.getPath());
    }

    /**
     * Integration test for creating a department with an invalid name.
     */
    @Test
    public void testCreateDepartment_InvalidName() {
        // Given: An invalid department name (null)
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName(null);

        // When: Attempting to create the department
        // Then: A ValidationException should be thrown
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));
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
        // Then: A ValidationException should be thrown
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));
    }

    /**
     * Integration test for creating a department with a parent that does not exist.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given: A non-existent parent department ID
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("New Department");
        requestDto.setParentDepartmentId(-1L); // Assuming -1L does not exist

        // When: Attempting to create the department
        // Then: A ParentDepartmentNotFoundException should be thrown
        assertThrows(ParentDepartmentNotFoundException.class, () -> departmentService.createDepartment(requestDto));
    }

    /**
     * Integration test for successfully creating a root department.
     */
    @Test
    public void testCreateRootDepartment_Success() {
        // Given: A request to create a root department
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Root Department");

        // When: Creating the root department
        DepartmentResponseDto responseDto = departmentService.createDepartment(requestDto);

        // Then: Verify the root department is created successfully
        assertNotNull(responseDto.getId());
        assertEquals("Root Department", responseDto.getName());
        Department rootDepartment = departmentRepository.findById(responseDto.getId()).orElse(null);
        assertNotNull(rootDepartment);
        assertEquals("/" + rootDepartment.getId() + "/", rootDepartment.getPath());
    }

    /**
     * Integration test for updating a department's name only.
     */
    @Test
    public void testUpdateDepartment_NameOnly() {
        // Given: An existing department with a specified ID and name "Engineering".
        Department existingDepartment = createDepartment(1L, "Engineering", "/1/", null);

        // When: Updating the department name to "IT".
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("IT");
        DepartmentResponseDto updatedResponse = departmentService.updateDepartment(existingDepartment.getId(), updatedDto);

        // Then: Verify that the department's name is updated correctly to "IT".
        assertEquals("IT", updatedResponse.getName());
        Department updatedDepartment = departmentRepository.findById(existingDepartment.getId()).orElse(null);
        assertNotNull(updatedDepartment);
        assertEquals("IT", updatedDepartment.getName());
    }

    /**
     * Integration test for updating a department's name in a subtree.
     */
    @Test
    public void testUpdateDepartment_NameOnly_InSubtree() {
        // Given: A root department and a child department
        Department rootDepartment = createDepartment(1L, "Root", "/1/", null);
        Department childDepartment = createDepartment(2L, "Child", "/1/2/", rootDepartment.getId());

        // When: Updating the child department name
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Updated Child");
        updatedDto.setParentDepartmentId(rootDepartment.getId());

        DepartmentResponseDto updatedResponse = departmentService.updateDepartment(childDepartment.getId(), updatedDto);

        // Then: Verify that the child department's name is updated correctly
        assertEquals("Updated Child", updatedResponse.getName());
        Department updatedChild = departmentRepository.findById(childDepartment.getId()).orElse(null);
        assertNotNull(updatedChild);
        assertEquals("Updated Child", updatedChild.getName());
    }

    /**
     * Integration test for updating a child department to be a root department.
     */
    @Test
    public void testUpdateDepartment_ChildToRoot() {
        // Given: A root department and a child department
        Department rootDepartment = createDepartment(1L, "Root", "/1/", null);
        Department childDepartment = createDepartment(2L, "Child", "/1/2/", rootDepartment.getId());

        // When: Updating the child department's to be a root department
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Updated Child");
        updatedDto.setParentDepartmentId(null);
        DepartmentResponseDto updatedResponse = departmentService.updateDepartment(childDepartment.getId(), updatedDto);

        // Then: Verify that the child department is updated correctly to be a root department
        assertEquals("Updated Child", updatedResponse.getName());
        Department updatedChild = departmentRepository.findById(childDepartment.getId()).orElse(null);
        assertNotNull(updatedChild);
        assertEquals("Updated Child", updatedChild.getName());
        assertEquals("/" + childDepartment.getId() + "/", updatedChild.getPath());
    }

    /**
     * Integration test for updating a department's parent.
     */
    @Test
    public void testUpdateDepartment_ChangeParent() {
        // Given: A root department and two child departments
        Department rootDepartment = createDepartment(1L, "Root", "/1/", null);
        Department child1 = createDepartment(2L, "Child1", "/1/2/", rootDepartment.getId());
        Department child2 = createDepartment(3L, "Child2", "/1/3/", rootDepartment.getId());

        // When: Changing the parent of Child2 to Child1
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Child2");
        updatedDto.setParentDepartmentId(child1.getId());

        DepartmentResponseDto updatedResponse = departmentService.updateDepartment(child2.getId(), updatedDto);

        // Then: Verify that the parent of Child2 is changed to Child1
        assertEquals(child1.getId(), updatedResponse.getParentDepartmentId());
        Department updatedChild2 = departmentRepository.findById(child2.getId()).orElse(null);
        assertNotNull(updatedChild2);
        assertEquals(child1.getId(), updatedChild2.getParentDepartmentId());
        assertEquals(child1.getPath() + child2.getId() + "/", updatedChild2.getPath());
    }

    /**
     * Integration test for updating a department's parent in a subtree.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree() {
        // Given: A root department and two child departments
        Department rootDepartment = createDepartment(1L, "Root", "/1/", null);
        Department child1 = createDepartment(2L, "Child1", "/1/2/", rootDepartment.getId());
        Department child2 = createDepartment(3L, "Child2", "/1/3/", rootDepartment.getId());
        Department grandchild = createDepartment(4L, "Grandchild", "/1/3/4/", child2.getId());

        // When: Changing the parent of Grandchild to Child1
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Grandchild");
        updatedDto.setParentDepartmentId(child1.getId());
        DepartmentResponseDto updatedResponse = departmentService.updateDepartment(grandchild.getId(), updatedDto);

        // Then: Verify that the parent of Grandchild is changed to Child1
        assertEquals(child1.getId(), updatedResponse.getParentDepartmentId());
        Department updatedGrandchild = departmentRepository.findById(grandchild.getId()).orElse(null);
        assertNotNull(updatedGrandchild);
        assertEquals(child1.getId(), updatedGrandchild.getParentDepartmentId());
        assertEquals(child1.getPath() + grandchild.getId() + "/", updatedGrandchild.getPath());
    }

    /**
     * Integration test for circular reference detection during department update.
     */
    @Test
    public void testUpdateDepartment_CircularReferences() {
        // Given: A root department and a child department
        Department rootDepartment = createDepartment(1L, "Root", "/1/", null);
        Department child = createDepartment(2L, "Child", "/1/2/", rootDepartment.getId());

        // When: Attempting to make the root department a child of its own child
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Root");
        updatedDto.setParentDepartmentId(child.getId());

        // Then: A DataIntegrityException should be thrown
        assertThrows(DataIntegrityException.class, () -> departmentService.updateDepartment(rootDepartment.getId(), updatedDto));
    }

    /**
     * Integration test for data integrity violation during department update.
     */
    @Test
    public void testUpdateDepartment_DataIntegrityException() {
        // Given: Two departments with the same parent
        Department parent = createDepartment(1L, "Parent", "/1/", null);
        Department child1 = createDepartment(2L, "Child1", "/1/2/", parent.getId());
        Department child2 = createDepartment(3L, "Child2", "/1/3/", parent.getId());

        // When: Attempting to update Child1 to have the same path as Child2
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Parent");
        updatedDto.setParentDepartmentId(child1.getId());

        // Then: A DataIntegrityException should be thrown
        assertThrows(DataIntegrityException.class, () -> departmentService.updateDepartment(parent.getId(), updatedDto));
    }

    /**
     * Integration test for updating a department with a non-existent parent.
     */
    @Test
    public void testUpdateDepartment_ParentDepartmentNotFoundException() {
        // Given: An existing department
        Department existingDepartment = createDepartment(1L, "Existing", "/1/", null);

        // When: Attempting to set a non-existent parent department
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Existing");
        updatedDto.setParentDepartmentId(-1L); // Assuming -1L does not exist

        // Then: A ParentDepartmentNotFoundException should be thrown
        assertThrows(ParentDepartmentNotFoundException.class, () -> departmentService.updateDepartment(existingDepartment.getId(), updatedDto));
    }

    /**
     * Integration test for updating a department with a validation exception.
     */
    @Test
    public void testUpdateDepartment_ValidationException() {
        // Given: An existing department
        Department existingDepartment = createDepartment(1L, "Existing", "/1/", null);

        // When: Attempting to update the department with an invalid name
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName(""); // Invalid name

        // Then: A ValidationException should be thrown
        assertThrows(ValidationException.class, () -> departmentService.updateDepartment(existingDepartment.getId(), updatedDto));
    }

    /**
     * Integration test for updating a department with its sub-departments.
     */
    @Test
    public void testUpdateDepartment_ParentDepartment_SubDepartments() {
        // Given: A parent department and its child departments
        Department parent = createDepartment(1L, "Parent", "/1/", null);
        Department child1 = createDepartment(2L, "Child1", "/1/2/", parent.getId());
        Department child2 = createDepartment(3L, "Child2", "/1/3/", parent.getId());

        // When: Updating the parent department's name
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Updated Parent");
        DepartmentResponseDto updatedResponse = departmentService.updateDepartment(parent.getId(), updatedDto);

        // Then: Verify that the parent department and its sub-departments are updated correctly
        assertEquals("Updated Parent", updatedResponse.getName());
        Department updatedParent = departmentRepository.findById(parent.getId()).orElse(null);
        assertNotNull(updatedParent);
        assertEquals("Updated Parent", updatedParent.getName());

        List<Department> subDepartments = departmentRepository.findByParentDepartmentId(parent.getId());
        assertEquals(2, subDepartments.size());
    }

    /**
     * Integration test for deleting a department with an existent id and with descendants.
     */
    @Test
    public void testDeleteDepartment_ExistingId_WithDescendants() {
        // Given: An existing department with descendants in the database
        Long departmentId = 1L;
        String departmentPath = "/1/";

        Department department = createDepartment(departmentId, "Software", departmentPath, null);
        Department child1 = createDepartment(2L, "Backend", "/1/2/", departmentId);
        Department child2 = createDepartment(3L, "Frontend", "/1/3/", departmentId);

        // When: Deleting the department
        departmentService.deleteDepartment(department.getId());

        // Then: Verify the department and its descendants are deleted from the database
        assertFalse(departmentRepository.findById(departmentId).isPresent());
        assertFalse(departmentRepository.findById(child1.getId()).isPresent());
        assertFalse(departmentRepository.findById(child2.getId()).isPresent());
    }

    /**
     * Integration test for deleting a department with a non-existent id.
     */
    @Test
    public void testDeleteDepartment_NonExistingId_IT() {
        // Given: A non-existing department ID
        Long nonExistingId = -1L;

        // When: Deleting the non-existing department
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.deleteDepartment(nonExistingId);
        });

        // Then: Verify the exception message
        assertEquals("Department not found with ID: " + nonExistingId, exception.getMessage());
    }


    /**
     * Integration test for circular reference detection during department delete.
     */
    @Test
    public void testDeleteDepartment_CircularReference_IT() {
        // Given: A department with a circular reference in its path
        Long parentId = 1L;
        Long childId = 2L;
        Long grandChildId = 3L;

        Department parent = createDepartment(parentId, "Parent", "/1/", null);
        Department child = createDepartment(childId, "Child", "/1/2/", parentId);
        Department grandChild = createDepartment(grandChildId, "Grand Child", "/1/2/", childId);
        Department circularDescendant = createDepartment(4L, "Circular", "/1/2/3/4/1/", grandChildId);

        // When: Deleting the parent department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteDepartment(parent.getId());
        });

        // Then: Verify the exception message
        assertEquals("Circular reference detected in department path: " + circularDescendant.getPath(), exception.getMessage());
    }

    /**
     * Integration test for retrieving all departments when there are no departments.
     */
    @Test
    public void testGetAllDepartments_noDepartments() {
        // Given: No departments in the repository

        // When: Retrieving all departments
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then: Verify that the result is an empty list
        assertTrue(result.isEmpty());
    }

    /**
     * Integration test for retrieving all departments when there are existing departments.
     */
    @Test
    public void testGetAllDepartments_withDepartments() {
        // Given: Departments in the repository
        Department parent = createDepartment(1L, "Parent", "/1/", null);
        Department child = createDepartment(2L, "Child", "/1/2/", parent.getId());

        // When: Retrieving all departments
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals("Parent", result.get(0).getName());
        assertEquals("Child", result.get(1).getName());
    }

    /**
     * Integration test for retrieving an existing department by ID.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() {
        // Given: An existing department
        Department parent = createDepartment(1L, "Parent", "/1/", null);

        // When: Retrieving the department by ID
        DepartmentResponseDto result = departmentService.getDepartmentById(parent.getId());

        // Then: Verify the result
        assertEquals(parent.getId(), result.getId());
        assertEquals("Parent", result.getName());
    }

    /**
     * Integration test for retrieving a non-existent department by ID.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given: A non-existing department ID
        Long departmentId = -1L;
        // When: Retrieving the department by ID
        DepartmentNotFoundException thrown = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getDepartmentById(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department not found with ID: " + departmentId, thrown.getMessage());
    }

    /**
     * Integration test for searching departments by an existing name.
     */
    @Test
    public void testSearchDepartmentsByName_ExistingName() {
        // Given: Departments with matching name
        Department parent = createDepartment(1L, "Parent", "/1/", null);
        Department child = createDepartment(2L, "Child", "/1/2/", parent.getId());

        // When: Searching departments by name
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName("Parent");

        // Then: Verify the result
        assertEquals(1, result.size());
        assertEquals("Parent", result.get(0).getName());
    }

    /**
     * Integration test for searching departments by a non-existing name.
     */
    @Test
    public void testSearchDepartmentsByName_NonExistingName() {
        // Given: No departments with matching name
        String departmentName = "NonExistingDepartment_" + UUID.randomUUID(); // Generate a unique non-existing name

        // When: Searching departments by name
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(departmentName);

        // Then: Verify the result is an empty list
        assertTrue(result.isEmpty());
    }

    /**
     * Integration test for {@link DepartmentService#getSubDepartments(Long)} when parent department exists.
     * Verifies that sub-departments are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetSubDepartments_existingParent() {
        // Given: Existing parent department
        Long parentId = 1L;
        Department parent = createDepartment(parentId, "Parent", "/1/", null);
        Department child1 = createDepartment(2L, "Child1", "/1/2/", parent.getId());
        Department child2 = createDepartment(3L, "Child2", "/1/3/", parent.getId());

        // When: Getting sub-departments
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(parent.getId());

        // Then: Verify the result
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> "Child1".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "Child2".equals(dto.getName())));
    }

    /**
     * Integration test for {@link DepartmentService#getSubDepartments(Long)} when parent department does not exist.
     * Verifies that {@link ParentDepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetSubDepartments_nonExistingParent() {
        // Given: Non-existing parent department ID
        Long parentId = -1L;

        // When: Getting sub-departments
        ParentDepartmentNotFoundException thrown = assertThrows(
                ParentDepartmentNotFoundException.class,
                () -> departmentService.getSubDepartments(parentId)
        );

        // Then: Verify the exception
        assertEquals("Parent department not found with id: " + parentId, thrown.getMessage());
    }

    /**
     * Integration test for {@link DepartmentService#getDescendants(Long)} when department exists.
     * Verifies that descendants are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetDescendants_existingDepartment() {
        // Given: Existing department
        Department parent = createDepartment(1L, "Parent", "/1/", null);
        Department department = createDepartment(2L, "Department", "/1/2/", parent.getId());
        Department child1 = createDepartment(3L, "Child1", "/1/2/3/", department.getId());
        Department child2 = createDepartment(4L, "Child2", "/1/2/4/", department.getId());

        String parentPath = "/" + parent.getId() + "/";
        parent.setPath(parentPath);
        departmentRepository.save(parent);

        String departmentPath = parent.getPath() + department.getId() + "/";
        department.setPath(departmentPath);
        departmentRepository.save(department);

        String child1Path = department.getPath() + child1.getId() + "/";
        child1.setPath(child1Path);
        departmentRepository.save(child1);

        String child2Path = department.getPath() + child2.getId() + "/";
        child2.setPath(child2Path);
        departmentRepository.save(child2);

        // When: Getting descendants
        List<DepartmentResponseDto> result = departmentService.getDescendants(parent.getId());

        // Then: Verify the result
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(dto -> "Department".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "Child1".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "Child2".equals(dto.getName())));
    }

    /**
     * Integration test for {@link DepartmentService#getDescendants(Long)} when department does not exist.
     * Verifies that {@link DepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetDescendants_nonExistingDepartment() {
        // Given: Non-existing department ID
        Long departmentId = -1L;

        // When: Getting descendants
        DepartmentNotFoundException thrown = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getDescendants(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department not found with ID: " + departmentId, thrown.getMessage());
    }

    /**
     * Integration test for {@link DepartmentService#getAncestors(Long)} when department exists.
     * Verifies that ancestors are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetAncestors_existingDepartment() {
        // Given: Existing department
        Department parent = createDepartment(1L, "Parent", "/1/", null);
        Department department = createDepartment(2L, "Department", "/1/2/", parent.getId());
        Department child1 = createDepartment(3L, "Child1", "/1/2/3/", department.getId());
        Department child2 = createDepartment(4L, "Child2", "/1/2/4/", department.getId());

        String parentPath = "/" + parent.getId() + "/";
        parent.setPath(parentPath);
        departmentRepository.save(parent);

        String departmentPath = parent.getPath() + department.getId() + "/";
        department.setPath(departmentPath);
        departmentRepository.save(department);

        String child1Path = department.getPath() + child1.getId() + "/";
        child1.setPath(child1Path);
        departmentRepository.save(child1);

        String child2Path = department.getPath() + child2.getId() + "/";
        child2.setPath(child2Path);
        departmentRepository.save(child2);

        // When: Getting ancestors
        List<DepartmentResponseDto> result = departmentService.getAncestors(child1.getId());

        // Then: Verify the result
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> "Parent".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "Department".equals(dto.getName())));
    }

    /**
     * Integration test for {@link DepartmentService#getAncestors(Long)} when department does not exist.
     * Verifies that {@link DepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetAncestors_nonExistingDepartment() {
        // Given: Non-existing department ID
        Long departmentId = -1L;

        // When: Getting ancestors
        DepartmentNotFoundException thrown = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getAncestors(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department not found with ID: " + departmentId, thrown.getMessage());
    }

    /**
     * Integration test for {@link DepartmentService#getParentDepartment(Long)} when parent department exists.
     * Verifies that parent department is correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetParentDepartment_existingParent() {
        // Given: Existing department with parent
        Long parentId = 1L;
        Long departmentId = 2L;
        Department parent = createDepartment(parentId, "Parent", "/1/", null);
        Department department = createDepartment(departmentId, "Department", "/1/2/", parent.getId());

        // When: Getting parent department
        DepartmentResponseDto result = departmentService.getParentDepartment(department.getId());

        // Then: Verify the result
        assertEquals(parent.getId(), result.getId());
        assertEquals(parent.getName(), result.getName());
    }

    /**
     * Integration test for {@link DepartmentService#getParentDepartment(Long)} when department does not exist.
     * Verifies that {@link DepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetParentDepartment_nonExistingDepartment() {
        // Given: Non-existing department ID
        Long departmentId = -1L;

        // When: Getting parent department
        DepartmentNotFoundException thrown = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getParentDepartment(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department not found with ID: " + departmentId, thrown.getMessage());
    }

    /**
     * Integration test for {@link DepartmentService#getParentDepartment(Long)} when department parent does not exist.
     * Verifies that {@link ParentDepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetParentDepartment_noParent() {
        // Given: Department with no parent
        Long departmentId = 1L;
        Department department = createDepartment(departmentId, "Department", "/1/2/", null);

        // When: Getting parent department
        ParentDepartmentNotFoundException thrown = assertThrows(
                ParentDepartmentNotFoundException.class,
                () -> departmentService.getParentDepartment(department.getId())
        );

        // Then: Verify the exception
        assertEquals("Department with id: " + department.getId() + " has no parent.", thrown.getMessage());
    }
}

