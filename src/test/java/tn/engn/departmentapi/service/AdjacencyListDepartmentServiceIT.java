package tn.engn.departmentapi.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for the AdjacencyListDepartmentService.
 */
@Slf4j // Lombok annotation to generate SLF4J logger field
@SpringBootTest
@Testcontainers
public class AdjacencyListDepartmentServiceIT {

    // Testcontainers MySQL container for database integration testing
    @Container
    public static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AdjacencyListDepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Value("${department.max-name-length}") // Assuming maxNameLength is configured in application properties
    private int maxNameLength;

    /**
     * Set up the database container before all tests.
     */
    @BeforeAll
    public static void setUp() {
        mysqlContainer.start();
        System.setProperty("DB_URL", mysqlContainer.getJdbcUrl());
        System.setProperty("DB_USERNAME", mysqlContainer.getUsername());
        System.setProperty("DB_PASSWORD", mysqlContainer.getPassword());
    }

    /**
     * Tear down the database container after all tests.
     */
    @AfterAll
    public static void tearDown() {
        mysqlContainer.stop();
    }

    /**
     * Test creating a department with a valid name.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithValidName() {
        String validName = "Valid Department Name";

        // Create a department with a valid name, expect no exceptions
        departmentService.createDepartment(validName, null);
    }

    /**
     * Test creating a department with a name of maximum length.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithMaxLengthName() {
        // Generate a name that exactly meets the max length
        String maxLengthName = "A".repeat(maxNameLength);

        // Create a department with a name of maximum length, expect no exceptions
        departmentService.createDepartment(maxLengthName, null);
    }

    /**
     * Test creating a department with a name that exceeds maximum length.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithExceededMaxLengthName() {
        // Generate a name that exceeds the max length by one character
        String exceededName = "A".repeat(maxNameLength + 1);

        // Create a department with a name that exceeds max length, expect IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(exceededName, null));
    }

    /**
     * Test creating a department with an empty name.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithEmptyName() {
        // Create a department with an empty name, expect IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment("", null));
    }

    /**
     * Test creating a department with a null name.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithNullName() {
        // Create a department with a null name, expect IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> departmentService.createDepartment(null, null));
    }

    /**
     * Test creating a department without parent.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithoutParent() {
        // Create a department without a parent
        Department department = departmentService.createDepartment("Department Without Parent", null);

        // Assertions
        Assertions.assertNotNull(department);
        Assertions.assertNull(department.getParentDepartment());
    }

    /**
     * Test creating a department with a parent.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithParent() {
        // Create parent department
        Department parent = departmentService.createDepartment("Parent Department", null);
        // Create child department with parent reference
        Department child = departmentService.createDepartment("Child Department", parent.getId());

        // Assertions
        Assertions.assertEquals(parent.getId(), child.getParentDepartment().getId());
        Assertions.assertTrue(parent.getSubDepartments().contains(child));
    }

    /**
     * Test attempting to create a department with a non-existing parent ID.
     */
    @Test
    @Transactional
    public void testNonExistingParentId() {
        // Attempt to create a department with a non-existing parent ID
        assertThrows(EntityNotFoundException.class, () -> departmentService.createDepartment("Department", -1L));
    }

    /**
     * Test updating a department.
     */
    @Test
    @Transactional
    public void testUpdateDepartment() {
        // Create parent department
        Department parent = departmentService.createDepartment("Parent Department", null);
        // Create department to update
        Department department = departmentService.createDepartment("Department", parent.getId());

        // Update department name and parent
        Department updatedDepartment = departmentService.updateDepartment(department.getId(), "Updated Department", parent.getId());

        // Assertions
        Assertions.assertEquals("Updated Department", updatedDepartment.getName());
        Assertions.assertEquals(parent.getId(), updatedDepartment.getParentDepartment().getId());
    }

    /**
     * Test updating a department's parent.
     */
    @Test
    @Transactional
    public void testUpdateDepartmentParent() {
        // Create parent departments
        Department parent1 = departmentService.createDepartment("Parent Department 1", null);
        Department parent2 = departmentService.createDepartment("Parent Department 2", null);

        // Create department to update
        Department department = departmentService.createDepartment("Department", parent1.getId());

        // Assert initial parent
        Assertions.assertEquals(parent1.getId(), department.getParentDepartment().getId());

        // Update parent to parent2
        department = departmentService.updateDepartment(department.getId(), "Department", parent2.getId());

        // Assertions
        Assertions.assertEquals(parent2.getId(), department.getParentDepartment().getId());

        // Verify parent1 no longer contains the department in its sub-departments
        Assertions.assertFalse(parent1.getSubDepartments().contains(department));

        // Verify parent2 now contains the department in its sub-departments
        Assertions.assertTrue(parent2.getSubDepartments().contains(department));
    }

    /**
     * Test handling circular dependencies when updating department parent.
     */
    @Test
    @Transactional
    public void testCircularDependencies() {
        // Create departments
        Department parent = departmentService.createDepartment("Parent Department", null);
        Department child = departmentService.createDepartment("Child Department", parent.getId());

        // Attempt to make the parent a child of the current child
        assertThrows(IllegalArgumentException.class, () -> departmentService.updateDepartment(parent.getId(), "Parent Department", child.getId()));
    }

    /**
     * Test updating a department's parent to null.
     */
    @Test
    @Transactional
    public void testUpdateDepartmentParentToNull() {
        // Create departments
        Department parent = departmentService.createDepartment("Parent Department", null);
        Department child = departmentService.createDepartment("Child Department", parent.getId());

        // Update child department to have no parent
        Department updatedChild = departmentService.updateDepartment(child.getId(), "Child Department", null);

        // Assertions
        Assertions.assertNull(updatedChild.getParentDepartment());
        Assertions.assertFalse(parent.getSubDepartments().contains(updatedChild));
    }

    /**
     * Test updating a non-existing department.
     */
    @Test
    @Transactional
    public void testUpdateNonExistingDepartment() {
        // Attempt to update a department with a non-existing ID
        assertThrows(EntityNotFoundException.class, () -> departmentService.updateDepartment(-1L, "Updated Department", null));
    }

    /**
     * Test deleting a department without parent.
     */
    @Test
    @Transactional
    public void testDeleteDepartmentWithoutParent() {
        // Create a department
        Department department = departmentService.createDepartment("Department Without Parent", null);
        Long departmentId = department.getId();

        // Delete the department
        departmentService.deleteDepartment(departmentId);

        // Verify department is deleted from repository
        assertThat(departmentRepository.findById(departmentId)).isEmpty();
    }

    /**
     * Test deleting a department with parent.
     */
    @Test
    @Transactional
    public void testDeleteDepartmentWithParent() {
        // Create parent department
        Department parent = departmentService.createDepartment("Parent Department", null);
        Long parentId = parent.getId();

        // Create child department
        Department child = departmentService.createDepartment("Child Department", parentId);
        Long childId = child.getId();

        // Delete the child department
        departmentService.deleteDepartment(childId);

        // Verify child department is deleted from repository
        assertThat(departmentRepository.findById(childId)).isEmpty();

        // Verify child department is removed from parent's sub-departments list
        Department updatedParent = departmentRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent department not found with id: " + parentId));
        assertThat(updatedParent.getSubDepartments()).doesNotContain(child);
    }

    /**
     * Test attempting to delete a non-existing department.
     */
    @Test
    @Transactional
    public void testDeleteNonExistingDepartment() {
        // Attempt to delete a non-existing department
        assertThrows(EntityNotFoundException.class, () -> departmentService.deleteDepartment(-1L));
    }

    /**
     * Test deleting a department with sub-departments.
     */
    @Test
    @Transactional
    public void testDeleteDepartmentWithSubDepartments() {
        // Create parent department
        Department parent = departmentService.createDepartment("Parent Department", null);

        // Create child departments
        Department child1 = departmentService.createDepartment("Child Department 1", parent.getId());
        Department child2 = departmentService.createDepartment("Child Department 2", parent.getId());

        // Delete the parent department (and its sub-departments recursively)
        departmentService.deleteDepartment(parent.getId());

        // Verify parent department is deleted from repository
        assertThat(departmentRepository.findById(parent.getId())).isEmpty();

        // Verify child departments are deleted from repository
        assertThat(departmentRepository.findById(child1.getId())).isEmpty();
        assertThat(departmentRepository.findById(child2.getId())).isEmpty();
    }

    /**
     * Test retrieving all departments.
     */
    @Test
    @Transactional
    public void testGetAllDepartments() {
        // Create departments
        Department department1 = departmentService.createDepartment("Department 1", null);
        Department department2 = departmentService.createDepartment("Department 2", null);

        // Retrieve all departments
        List<Department> departments = departmentService.getAllDepartments();

        // Assertions
        Assertions.assertEquals(2, departments.size());
        Assertions.assertTrue(departments.contains(department1));
        Assertions.assertTrue(departments.contains(department2));
    }

    /**
     * Test retrieving sub-departments.
     */
    @Test
    @Transactional
    public void testGetSubDepartments() {
        // Create parent department
        Department parent = departmentService.createDepartment("Parent Department", null);
        // Create child departments
        Department child1 = departmentService.createDepartment("Child Department 1", parent.getId());
        Department child2 = departmentService.createDepartment("Child Department 2", parent.getId());

        // Retrieve sub-departments of parent
        List<Department> subDepartments = departmentService.getSubDepartments(parent.getId());

        // Assertions
        Assertions.assertEquals(2, subDepartments.size());
        Assertions.assertTrue(subDepartments.contains(child1));
        Assertions.assertTrue(subDepartments.contains(child2));
    }

    /**
     * Test retrieving a department by ID.
     */
    @Test
    @Transactional
    public void testGetDepartmentById() {
        // Create a department
        Department department = departmentService.createDepartment("Test Department", null);

        // Retrieve the department by ID
        Department foundDepartment = departmentService.getDepartmentById(department.getId());

        // Assertions
        Assertions.assertNotNull(foundDepartment);
        Assertions.assertEquals(department.getId(), foundDepartment.getId());
    }

    /**
     * Test retrieving the parent department.
     */
    @Test
    @Transactional
    public void testGetParentDepartment() {
        // Create parent department
        Department parent = departmentService.createDepartment("Parent Department", null);
        // Create child department
        Department child = departmentService.createDepartment("Child Department", parent.getId());

        // Retrieve parent of child department
        Department foundParent = departmentService.getParentDepartment(child.getId());

        // Assertions
        Assertions.assertEquals(parent, foundParent);
    }

    /**
     * Test getting parent department of a department that has no parent.
     */
    @Test
    @Transactional
    public void testGetParentDepartmentNoParent() {
        // Create a department with no parent
        Department department = departmentService.createDepartment("No Parent Department", null);

        // Attempt to retrieve parent department, expect EntityNotFoundException
        assertThrows(EntityNotFoundException.class, () -> departmentService.getParentDepartment(department.getId()));
    }

    /**
     * Test retrieving descendants of a department.
     */
    @Test
    @Transactional
    public void testGetDescendants() {
        // Create parent department
        Department parent = new Department();
        parent.setName("Parent Department");
        Department savedParent = departmentRepository.save(parent);

        // Create child department
        Department child = new Department();
        child.setName("Child Department");
        savedParent.addSubDepartment(child); // Establish bidirectional relationship
        Department savedChild = departmentRepository.save(child);

        // Create grandchild department
        Department grandChild = new Department();
        grandChild.setName("GrandChild Department");
        savedChild.addSubDepartment(grandChild); // Establish bidirectional relationship
        departmentRepository.save(grandChild);

        // Retrieve all descendants of the parent department
        List<Department> descendants = departmentService.getDescendants(savedParent.getId());

        // Assertions
        Assertions.assertEquals(2, descendants.size()); // Expecting Child and GrandChild departments
        Assertions.assertTrue(descendants.contains(savedChild));
        Assertions.assertTrue(descendants.contains(grandChild));
    }

    /**
     * Test retrieving ancestors of a department.
     */
    @Test
    @Transactional
    public void testGetAncestors() {
        // Create departments
        Department parent = departmentService.createDepartment("Parent Department", null);
        Department child = departmentService.createDepartment("Child Department", parent.getId());
        Department grandChild = departmentService.createDepartment("GrandChild Department", child.getId());

        // Retrieve ancestors of grandchild department
        List<Department> ancestors = departmentService.getAncestors(grandChild.getId());

        // Assertions
        Assertions.assertEquals(2, ancestors.size()); // Expecting Parent and Child departments
        Assertions.assertTrue(ancestors.contains(parent));
        Assertions.assertTrue(ancestors.contains(child));
    }

    /**
     * Test simulating concurrent access to departments.
     * Temporarily disabled due to concurrent access issue.
     */
    @Test
    @Transactional
    @Disabled("Temporarily disabled due to concurrent access issue")
    public void testConcurrentAccess() {
        // Simulate concurrent access to departments
        Department parent = departmentService.createDepartment("Parent Department", null);

        // Create child departments in parallel
        CompletableFuture<Department> child1Future = CompletableFuture.supplyAsync(() -> departmentService.createDepartment("Child 1", parent.getId()));
        CompletableFuture<Department> child2Future = CompletableFuture.supplyAsync(() -> departmentService.createDepartment("Child 2", parent.getId()));

        // Wait for completion
        Department child1 = child1Future.join();
        Department child2 = child2Future.join();

        // Assertions
        Assertions.assertEquals(2, parent.getSubDepartments().size());
    }

    /**
     * Test performance of retrieving descendants of a department.
     */
    @Test
    @Transactional
    public void testPerformanceGetDescendants() {
        // Create a large hierarchy of departments
        Department parent = new Department();
        parent.setName("Parent Department");
        parent = departmentRepository.save(parent);

        Department currentParent = parent;
        int numChildren = 1000; // Adjust based on your performance testing needs

        for (int i = 0; i < numChildren; i++) {
            Department child = new Department();
            child.setName("Child Department " + i);
            currentParent.addSubDepartment(child);
            child.setParentDepartment(currentParent);
            currentParent = departmentRepository.save(child);
        }

        // Retrieve all descendants of the parent department
        List<Department> descendants = departmentService.getDescendants(parent.getId());

        // Assertions
        Assertions.assertEquals(numChildren, descendants.size());
    }
}
