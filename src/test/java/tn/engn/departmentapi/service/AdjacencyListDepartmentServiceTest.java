package tn.engn.departmentapi.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AdjacencyListDepartmentService class.
 */

class AdjacencyListDepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private AdjacencyListDepartmentService departmentService;

    private int maxNameLength = 100; // Mocked value for maxNameLength

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test
        MockitoAnnotations.openMocks(this);

        // Mock the value of maxNameLength directly
        departmentService.setMaxNameLength(maxNameLength);
    }

    /**
     * Test for creating a department successfully.
     */
    @Test
    void testCreateDepartment_Successful() {
        // Mock repository response for parent department (if needed)
        Department parent = new Department();
        parent.setId(1L);
        parent.setName("Parent Department");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parent));

        // Mock repository save method
        Department createdDepartment = new Department();
        createdDepartment.setId(2L);
        createdDepartment.setName("New Department");
        createdDepartment.setParentDepartment(parent); // Assuming the parent-child relationship is set

        when(departmentRepository.save(any(Department.class))).thenReturn(createdDepartment);

        // Call the service method
        Department created = departmentService.createDepartment("New Department", 1L);

        // Assertions
        assertNotNull(created);
        assertEquals("New Department", created.getName());
        assertEquals(parent, created.getParentDepartment());

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    /**
     * Test for creating a department with an empty name.
     */
    @Test
    void testCreateDepartment_EmptyName() {
        // Call the service method with an empty name
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.createDepartment("", null));

        // Assert the exception message
        assertEquals("Department name must not be empty", exception.getMessage());

        // Verify interactions with the repository
        verify(departmentRepository, never()).findById(anyLong());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Test for creating a department with a name that exceeds the maximum length.
     */
    @Test
    void testCreateDepartment_NameTooLong() {
        // Call the service method with a name exceeding max length
        String longName = "DepartmentNameExceedingMaxLength_" + "x".repeat(maxNameLength);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> departmentService.createDepartment(longName, null));

        // Assert the exception message
        assertEquals("Department name exceeds maximum allowed length of " + maxNameLength + " characters",
                exception.getMessage());

        // Verify interactions with the repository
        verify(departmentRepository, never()).findById(anyLong());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Test for creating a department with a non-existing parent.
     */
    @Test
    void testCreateDepartment_NonExistingParent() {
        // Mock the repository response for findById method (parent not found)
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the service method and assert the exception
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> departmentService.createDepartment("New Department", 1L));

        // Assert the exception message
        assertEquals("Parent department not found with id: 1", exception.getMessage());

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, never()).save(any(Department.class));
    }


    /**
     * Test for updating an existing department with parent ID provided.
     */
    @Test
    void testUpdateDepartmentWithParentId() {
        // Create a mock existing department
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Existing Department");

        // Create a mock parent department
        Department parent = new Department();
        parent.setId(2L);
        parent.setName("Parent Department");

        // Mock the repository responses for findById and save methods
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(departmentRepository.save(any(Department.class))).thenReturn(existing);

        // Call the service method
        Department updated = departmentService.updateDepartment(1L, "Updated Department", 2L);

        // Assertions
        assertNotNull(updated);
        assertEquals("Updated Department", updated.getName());
        assertEquals(parent, updated.getParentDepartment());

        // Verify interactions with the repository
        verify(departmentRepository, times(2)).findById(1L);
        verify(departmentRepository, times(1)).findById(2L);
        verify(departmentRepository, times(1)).save(existing);
    }

    /**
     * Test for updating an existing department successfully.
     */
    @Test
    void testUpdateDepartment_Successful() {
        // Arrange
        Long departmentId = 1L;
        Long parentId = 2L;
        String newName = "Updated Department";

        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Existing Department");

        Department newParentDepartment = new Department();
        newParentDepartment.setId(parentId);
        newParentDepartment.setName("New Parent Department");

        existingDepartment.setParentDepartment(newParentDepartment);
        newParentDepartment.addSubDepartment(existingDepartment);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(newParentDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // Act
        Department updatedDepartment = departmentService.updateDepartment(departmentId, newName, parentId);

        // Assert
        assertEquals(newName, updatedDepartment.getName());
        assertEquals(parentId, updatedDepartment.getParentDepartment().getId());

        verify(departmentRepository, times(2)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Test for updating a department with circular dependency.
     */
    @Test
    void testUpdateDepartment_CircularDependency() {
        // Arrange
        Long departmentId = 1L;
        Long parentId = 2L;
        String newName = "Updated Department";

        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Existing Department");

        Department newParentDepartment = new Department();
        newParentDepartment.setId(parentId);
        newParentDepartment.setName("New Parent Department");

        existingDepartment.setParentDepartment(newParentDepartment);
        newParentDepartment.addSubDepartment(existingDepartment);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(newParentDepartment));

        // Create circular dependency
        // Act and Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                departmentService.updateDepartment(parentId, newName, departmentId));

        assertEquals("Circular dependency detected.", exception.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(2)).findById(parentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Test for updating a department and removing it from the current parent's subDepartments.
     */
    @Test
    public void testUpdateDepartment_RemoveFromCurrentParentsSubDepartments() {
        Long departmentId = 1L;
        Long currentParentId = 2L;
        Long newParentId = 3L;
        String newName = "Updated Department Name";

        // Create departments
        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Original Name");

        Department currentParent = new Department();
        currentParent.setId(currentParentId);
        currentParent.setName("Current Parent");
        existingDepartment.setParentDepartment(currentParent);
        currentParent.addSubDepartment(existingDepartment);

        Department newParent = new Department();
        newParent.setId(newParentId);
        newParent.setName("New Parent");

        // Mock repository behavior
       when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
       when(departmentRepository.findById(currentParentId)).thenReturn(Optional.of(currentParent));
       when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParent));

        // Mock save method behavior
       when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department departmentToSave = invocation.getArgument(0);
            // Simulate saving the department
            return departmentToSave;
        });

        // Call the service method with new parent
        Department updatedDepartment = departmentService.updateDepartment(departmentId, newName, newParentId);

        // Verify that department is removed from current parent's subDepartments
        assertFalse(currentParent.getSubDepartments().contains(updatedDepartment));
        // Verify that parent department is updated to newParent
        assertEquals(newParent, updatedDepartment.getParentDepartment());
        // Verify that name is updated
        assertEquals(newName, updatedDepartment.getName());
    }

    /**
     * Test for updating a department where parent ID is null.
     */
    @Test
    void testUpdateDepartment_NullParentId() {
        // Arrange
        Long departmentId = 1L;
        Long currentParentId = 2L;
        String newName = "Updated Department";

        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Existing Department");

        Department currentParent = new Department();
        currentParent.setId(currentParentId);
        currentParent.setName("Current Parent");
        existingDepartment.setParentDepartment(currentParent);
        currentParent.addSubDepartment(existingDepartment);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(currentParentId)).thenReturn(Optional.of(currentParent));
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // Act
        Department updatedDepartment = departmentService.updateDepartment(departmentId, newName, null);

        // Assert
        assertEquals(newName, updatedDepartment.getName());
        assertNull(updatedDepartment.getParentDepartment());
        assertFalse(currentParent.getSubDepartments().contains(updatedDepartment));

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Test for updating a department that does not exist (EntityNotFoundException).
     */
    @Test
    void testUpdateDepartment_DepartmentNotFound() {
        // Arrange
        Long departmentId = 1L;
        String newName = "Updated Department";

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // Act and Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                departmentService.updateDepartment(departmentId, newName, null));

        assertEquals("Department not found with id: " + departmentId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Test for updating a department with a parent that does not exist (EntityNotFoundException).
     */
    @Test
    void testUpdateDepartment_ParentDepartmentNotFound() {
        // Arrange
        Long departmentId = 1L;
        Long parentId = 2L;
        String newName = "Updated Department";

        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Existing Department");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.empty());

        // Act and Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                departmentService.updateDepartment(departmentId, newName, parentId));

        assertEquals("Parent department not found with id: " + parentId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Test for deleting an existing department.
     */
    @Test
    void testDeleteDepartment() {
        // Create a mock existing department
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Existing Department");

        // Create a mock parent department
        Department parent = new Department();
        parent.setId(2L);
        parent.setName("Parent Department");

        // Set the parent department for the existing department
        existing.setParentDepartment(parent);

        // Add the existing department to the parent's list of sub-departments
        parent.getSubDepartments().add(existing);

        // Mock the repository response for findById method
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Mock the repository response for delete method
        doNothing().when(departmentRepository).delete(existing);

        // Call the service method
        assertDoesNotThrow(() -> departmentService.deleteDepartment(1L));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, times(1)).delete(existing);

        // Verify that the existing department was removed from the parent's list of sub-departments
        assertFalse(parent.getSubDepartments().contains(existing));
    }


    /**
     * Test for deleting a department that does not exist.
     */
    @Test
    void testDeleteNonExistingDepartment() {
        // Mock the repository response for findById method (department not found)
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the service method and assert the exception
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> departmentService.deleteDepartment(1L));

        // Assert the exception message
        assertEquals("Department not found with id: 1", exception.getMessage());

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L); // Verify findById is called once
        verify(departmentRepository, never()).deleteById(anyLong()); // Verify deleteById is never called
        verify(departmentRepository, never()).delete(any(Department.class)); // Verify delete is never called
    }

    /**
     * Test for retrieving all departments.
     */
    @Test
    void testGetAllDepartments() {
        // Create mock departments
        Department dept1 = new Department();
        dept1.setId(1L);
        dept1.setName("Dept1");

        Department dept2 = new Department();
        dept2.setId(2L);
        dept2.setName("Dept2");

        // Mock the repository response for findAll method
        when(departmentRepository.findAll()).thenReturn(Arrays.asList(dept1, dept2));

        // Call the service method
        List<Department> departments = departmentService.getAllDepartments();

        // Assertions
        assertNotNull(departments);
        assertEquals(2, departments.size());
        assertTrue(departments.contains(dept1));
        assertTrue(departments.contains(dept2));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findAll();
    }

    /**
     * Test for retrieving sub-departments of a department.
     */
    @Test
    void testGetSubDepartments() {
        // Create a mock parent department
        Department parent = new Department();
        parent.setId(1L);
        parent.setName("Parent Department");

        // Create mock child departments
        Department child1 = new Department();
        child1.setId(2L);
        child1.setName("Child 1");
        child1.setParentDepartment(parent);

        Department child2 = new Department();
        child2.setId(3L);
        child2.setName("Child 2");
        child2.setParentDepartment(parent);

        parent.setSubDepartments(Arrays.asList(child1, child2));

        // Mock the repository response for findById method
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parent));

        // Call the service method
        List<Department> subDepartments = departmentService.getSubDepartments(1L);

        // Assertions
        assertNotNull(subDepartments);
        assertEquals(2, subDepartments.size());
        assertTrue(subDepartments.contains(child1));
        assertTrue(subDepartments.contains(child2));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
    }

    /**
     * Test for retrieving sub-departments of a non-existing department.
     */
    @Test
    void testGetSubDepartmentsForNonExistingDepartment() {
        // Mock the repository response for findById method (parent not found)
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the service method and assert the exception
        assertThrows(EntityNotFoundException.class,
                () -> departmentService.getSubDepartments(1L));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
    }

    /**
     * Test for retrieving a department by ID.
     */
    @Test
    void testGetDepartmentById() {
        // Create a mock department
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("Dept");

        // Mock the repository response for findById method
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        // Call the service method
        Department found = departmentService.getDepartmentById(1L);

        // Assertions
        assertNotNull(found);
        assertEquals("Dept", found.getName());

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
    }

    /**
     * Test for retrieving a non-existing department by ID.
     */
    @Test
    void testGetNonExistingDepartmentById() {
        // Mock the repository response for findById method (department not found)
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the service method and assert the exception
        assertThrows(EntityNotFoundException.class,
                () -> departmentService.getDepartmentById(1L));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
    }

    /**
     * Test for retrieving the parent department of a department.
     */
    @Test
    void testGetParentDepartment() {
        // Create a mock department
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("Dept");

        // Create a mock parent department
        Department parent = new Department();
        parent.setId(2L);
        parent.setName("Parent Department");

        // Set parent department for the department
        dept.setParentDepartment(parent);

        // Mock the repository response for findById method
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        // Call the service method
        Department foundParent = departmentService.getParentDepartment(1L);

        // Assertions
        assertNotNull(foundParent);
        assertEquals("Parent Department", foundParent.getName());

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
    }

    /**
     * Test for retrieving the parent department of a department that has no parent.
     */
    @Test
    void testGetParentDepartmentForDepartmentWithoutParent() {
        // Create a mock department with no parent
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("Dept");

        // Mock the repository response for findById method
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));

        // Call the service method and assert the exception
        assertThrows(EntityNotFoundException.class,
                () -> departmentService.getParentDepartment(1L));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
    }

    /**
     * Test for retrieving descendants of a department.
     */
    @Test
    void testGetDescendants() {
        // Create mock departments
        Department parent = new Department();
        parent.setId(1L);
        parent.setName("Parent Department");

        Department child1 = new Department();
        child1.setId(2L);
        child1.setName("Child 1");
        child1.setParentDepartment(parent);

        Department child2 = new Department();
        child2.setId(3L);
        child2.setName("Child 2");
        child2.setParentDepartment(parent);

        Department grandchild1 = new Department();
        grandchild1.setId(4L);
        grandchild1.setName("Grandchild 1");
        grandchild1.setParentDepartment(child1);

        parent.setSubDepartments(Arrays.asList(child1, child2));
        child1.setSubDepartments(Arrays.asList(grandchild1));

        // Mock the repository response for findById method
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(child1));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(child2));
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(grandchild1));

        // Call the service method
        List<Department> descendants = departmentService.getDescendants(1L);

        // Assertions
        assertNotNull(descendants);
        assertEquals(3, descendants.size());
        assertTrue(descendants.contains(child1));
        assertTrue(descendants.contains(child2));
        assertTrue(descendants.contains(grandchild1));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L); // Verify parent department lookup
        verify(departmentRepository, never()).findById(2L); // Verify no direct lookup for child1
        verify(departmentRepository, never()).findById(3L); // Verify no direct lookup for child2
        verify(departmentRepository, never()).findById(4L); // Verify no direct lookup for grandchild1
    }

    /**
     * Test for retrieving descendants of a department that does not exist.
     */
    @Test
    void testGetDescendantsForNonExistingDepartment() {
        // Mock the repository response for findById method (parent not found)
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the service method and assert the exception
        assertThrows(EntityNotFoundException.class,
                () -> departmentService.getDescendants(1L));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(1L);
    }

    /**
     * Test for retrieving ancestors of a department.
     */
    @Test
    void testGetAncestors() {
        // Create mock departments
        Department grandparent = new Department();
        grandparent.setId(1L);
        grandparent.setName("Grandparent Department");

        Department parent = new Department();
        parent.setId(2L);
        parent.setName("Parent Department");
        parent.setParentDepartment(grandparent);

        Department child = new Department();
        child.setId(3L);
        child.setName("Child Department");
        child.setParentDepartment(parent);

        // Mock the repository response for findById method
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(child));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(grandparent));

        // Call the service method
        List<Department> ancestors = departmentService.getAncestors(3L);

        // Assertions
        assertNotNull(ancestors);
        assertEquals(2, ancestors.size());
        assertTrue(ancestors.contains(parent));
        assertTrue(ancestors.contains(grandparent));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(3L);
        verify(departmentRepository, never()).findById(2L);
        verify(departmentRepository, never()).findById(1L);
    }

    /**
     * Test for retrieving ancestors of a department that does not exist.
     */
    @Test
    void testGetAncestorsForNonExistingDepartment() {
        // Mock the repository response for findById method (department not found)
        when(departmentRepository.findById(3L)).thenReturn(Optional.empty());

        // Call the service method and assert the exception
        assertThrows(EntityNotFoundException.class,
                () -> departmentService.getAncestors(3L));

        // Verify interactions with the repository
        verify(departmentRepository, times(1)).findById(3L);
    }
}
