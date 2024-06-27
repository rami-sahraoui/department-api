package tn.engn.departmentapi.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.departmentapi.TestContainerSetup;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.exception.DataIntegrityException;
import tn.engn.departmentapi.exception.DepartmentNotFoundException;
import tn.engn.departmentapi.exception.ParentDepartmentNotFoundException;
import tn.engn.departmentapi.exception.ValidationException;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the AdjacencyListDepartmentService using DTOs.
 */
@Slf4j
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Reset context after each test
public class AdjacencyListDepartmentServiceIT extends TestContainerSetup {

    @Autowired
    private AdjacencyListDepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private Environment environment;

    @Value("${department.max-name-length}") // Assuming maxNameLength is configured in application properties
    private int maxNameLength;

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
     * Test creating a department with a valid name using DTO.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithValidName() {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Valid Department Name")
                .build();

        // Create department with a valid name, expect no exceptions
        DepartmentResponseDto responseDto = departmentService.createDepartment(requestDto);

        // Assertions
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isNotNull();
        assertThat(responseDto.getName()).isEqualTo(requestDto.getName());
    }

    /**
     * Test creating a department with a name of maximum length using DTO.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithMaxLengthName() {
        String maxLengthName = "A".repeat(maxNameLength);

        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name(maxLengthName)
                .build();

        // Create department with a name of maximum length, expect no exceptions
        DepartmentResponseDto responseDto = departmentService.createDepartment(requestDto);

        // Assertions
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isNotNull();
        assertThat(responseDto.getName()).isEqualTo(requestDto.getName());
    }

    /**
     * Test creating a department with a name that exceeds maximum length using DTO.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithExceededMaxLengthName() {
        String exceededName = "A".repeat(maxNameLength + 1);

        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name(exceededName)
                .build();

        // Create department with a name that exceeds max length, expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));
    }

    /**
     * Test creating a department with an empty name using DTO.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithEmptyName() {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("")
                .build();

        // Create department with an empty name, expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));
    }

    /**
     * Test creating a department with a null name using DTO.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithNullName() {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name(null)
                .build();

        // Create department with a null name, expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));
    }

    /**
     * Test creating a department without parent using DTO.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithoutParent() {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Department Without Parent")
                .build();

        // Create department without a parent
        DepartmentResponseDto responseDto = departmentService.createDepartment(requestDto);

        // Assertions
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getParentDepartmentId()).isNull();
    }

    /**
     * Test creating a department with a parent using DTO.
     */
    @Test
    @Transactional
    public void testCreateDepartmentWithParent() {
        // Create parent department
        DepartmentRequestDto parentRequestDto = DepartmentRequestDto.builder()
                .name("Parent Department")
                .build();
        DepartmentResponseDto parentResponseDto = departmentService.createDepartment(parentRequestDto);

        // Create child department with parent reference
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name("Child Department")
                .parentDepartmentId(parentResponseDto.getId())
                .build();
        DepartmentResponseDto childResponseDto = departmentService.createDepartment(childRequestDto);

        // Assertions
        assertThat(childResponseDto).isNotNull();
        assertThat(childResponseDto.getParentDepartmentId()).isNotNull();
        assertThat(childResponseDto.getParentDepartmentId()).isEqualTo(parentResponseDto.getId());
        // No assertion for sub-departments in this test, as per provided details
    }

    /**
     * Test attempting to create a department with a non-existing parent ID using DTO.
     */
    @Test
    @Transactional
    public void testNonExistingParentId() {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Department")
                .parentDepartmentId(-1L)
                .build();

        // Attempt to create a department with a non-existing parent ID
        assertThrows(ParentDepartmentNotFoundException.class, () -> departmentService.createDepartment(requestDto));
    }

    @Test
    /**
     * Integration test for updating a department's name.
     * Verifies that the department's name is updated correctly.
     */
    public void testUpdateDepartment_updateName() {
        // Create a department
        Department department = new Department();
        department.setName("Original Department Name");
        department = departmentRepository.save(department);

        // Prepare the update request
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated Department Name");

        // Perform the update
        DepartmentResponseDto updatedDto = departmentService.updateDepartment(department.getId(), requestDto);

        // Retrieve the department from database
        Optional<Department> optionalDepartment = departmentRepository.findById(department.getId());
        assertTrue(optionalDepartment.isPresent(), "Department should exist in the database");

        // Verify the department's name is updated
        assertEquals(requestDto.getName(), optionalDepartment.get().getName());
    }

    @Test
    /**
     * Integration test for updating a department that does not exist.
     * Expects EntityNotFoundException to be thrown.
     */
    public void testUpdateDepartment_departmentNotFound() {
        // Prepare the update request for a non-existent department
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated Department Name");

        // Perform the update and assert that EntityNotFoundException is thrown
        assertThrows(DepartmentNotFoundException.class, () -> departmentService.updateDepartment(-1L, requestDto));
    }

    @Test
    /**
     * Integration test for updating a department with a parent that does not exist.
     * Expects EntityNotFoundException to be thrown.
     */
    public void testUpdateDepartment_parentDepartmentNotFound() {
        // Create a department
        Department department = new Department();
        department.setName("Department");
        department = departmentRepository.save(department);

        // Prepare the update request with a non-existent parent department ID
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated Department Name");
        requestDto.setParentDepartmentId(-1L); // Non-existent parent ID

        // Perform the update and assert that EntityNotFoundException is thrown
        Department finalDepartment = department;
        assertThrows(ParentDepartmentNotFoundException.class, () -> departmentService.updateDepartment(finalDepartment.getId(), requestDto));
    }

    @Test
    /**
     * Integration test for updating a department with an empty or null name.
     * Expects IllegalArgumentException to be thrown.
     */
    public void testUpdateDepartment_emptyOrNullName() {
        // Create a department
        Department department = new Department();
        department.setName("Department");
        department = departmentRepository.save(department);

        // Prepare the update request with an empty or null name
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName(""); // Empty name

        // Perform the update and assert that IllegalArgumentException is thrown
        Department finalDepartment = department;
        assertThrows(ValidationException.class, () -> departmentService.updateDepartment(finalDepartment.getId(), requestDto));
    }

    @Test
    /**
     * Integration test for updating a department with a name that exceeds maximum length.
     * Expects ValidationException to be thrown.
     */
    public void testUpdateDepartment_ExceededMaxLengthName() {
        // Create a department
        Department department = new Department();
        department.setName("Department");
        department = departmentRepository.save(department);

        // Prepare the update request with name that exceeds max length
        String exceededName = "A".repeat(maxNameLength + 1);

        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name(exceededName)
                .build();

        // Perform the update and assert that ValidationException is thrown
        Department finalDepartment = department;
        assertThrows(ValidationException.class, () -> departmentService.updateDepartment(finalDepartment.getId(), requestDto));
    }

    @Test
    /**
     * Integration test for updating a department that would create a circular dependency.
     * Expects IllegalArgumentException to be thrown.
     */
    public void testUpdateDepartment_circularDependency() {
        // Create departments
        Department department1 = new Department();
        department1.setName("Department 1");
        department1 = departmentRepository.save(department1);

        Department department2 = new Department();
        department2.setName("Department 2");
        department2.setParentDepartment(department1);
        department2 = departmentRepository.save(department2);

        // Attempt to set department1 as a child of department2, creating a circular dependency
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated Department 1 Name");
        requestDto.setParentDepartmentId(department2.getId());

        // Perform the update and assert that IllegalArgumentException is thrown
        Department finalDepartment = department1;
        assertThrows(DataIntegrityException.class, () -> departmentService.updateDepartment(finalDepartment.getId(), requestDto));
    }

    @Test
    /**
     * Test case for updating the parent department of a department.
     * Verifies that the parent department is updated correctly.
     */
    public void testUpdateDepartment_updateParent() {
        // Create and save parent and child departments
        Department oldParent = Department.builder().name("Old Parent Department").build();
        Department newParent = Department.builder().name("New Parent Department").build();
        Department child = Department.builder().name("Child Department").parentDepartment(oldParent).build();
        departmentRepository.save(oldParent);
        departmentRepository.save(newParent);
        departmentRepository.save(child);

        // Create the request DTO with the new parent ID
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Updated Child Department")
                .parentDepartmentId(newParent.getId())
                .build();

        // Call the method to test
        DepartmentResponseDto result = departmentService.updateDepartment(child.getId(), requestDto);

        // Verify that the result is not null and has the expected parent department ID
        assertNotNull(result, "The result should not be null");
        assertEquals(newParent.getId(), result.getParentDepartmentId(), "The result parent ID should be the new parent's ID");
    }

    @Test
    /**
     * Test case for removing the parent department of a department.
     * Verifies that the parent department is removed correctly.
     */
    public void testUpdateDepartment_removeParent() {
        // Create and save parent and child departments
        Department parent = Department.builder().name("Parent Department").build();
        Department child = Department.builder().name("Child Department").parentDepartment(parent).build();
        departmentRepository.save(parent);
        departmentRepository.save(child);

        // Create the request DTO with no parent ID
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Updated Child Department")
                .parentDepartmentId(null)
                .build();

        // Call the method to test
        DepartmentResponseDto result = departmentService.updateDepartment(child.getId(), requestDto);

        // Verify that the result is not null and has no parent department ID
        assertNotNull(result, "The result should not be null");
        assertNull(result.getParentDepartmentId(), "The result should have no parent department ID");
    }

    @Test
    /**
     * Test case for updating the department without changing the parent.
     * Verifies that unnecessary updates are avoided.
     */
    public void testUpdateDepartment_avoidUnnecessaryUpdate() {
        // Create and save parent and child departments
        Department parent = Department.builder().name("Parent Department").build();
        Department child = Department.builder().name("Child Department").parentDepartment(parent).build();
        departmentRepository.save(parent);
        departmentRepository.save(child);

        // Create the request DTO with the same parent ID
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Updated Child Department")
                .parentDepartmentId(parent.getId())
                .build();

        // Call the method to test
        DepartmentResponseDto result = departmentService.updateDepartment(child.getId(), requestDto);

        // Verify that the result is not null and has the expected parent department ID
        assertNotNull(result, "The result should not be null");
        assertEquals(parent.getId(), result.getParentDepartmentId(), "The result parent ID should be the parent's ID");
    }

    @Test
    /**
     * Integration test for deleting an existing department.
     * Verifies that the department is deleted successfully.
     */
    public void testDeleteDepartment_existingDepartment() {
        // Create a department
        Department department = new Department();
        department.setName("Department");
        department = departmentRepository.save(department);

        // Delete the department
        departmentService.deleteDepartment(department.getId());

        // Verify that the department is deleted by checking repository
        assertFalse(departmentRepository.findById(department.getId()).isPresent(), "Department should be deleted");
    }

    @Test
    /**
     * Integration test for deleting a non-existing department.
     * Expects EntityNotFoundException to be thrown.
     */
    public void testDeleteDepartment_nonExistingDepartment() {
        // Attempt to delete a department with a non-existing ID
        Long nonExistingId = Long.MAX_VALUE;

        // Perform the delete and assert that EntityNotFoundException is thrown
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () ->
                departmentService.deleteDepartment(nonExistingId));

        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());
    }

    /**
     * Test case for deleting a department with sub-departments.
     * Verifies that all sub-departments are also deleted.
     */
    @Test
    public void testDeleteDepartment_withSubDepartments() {
        // Create a parent department
        Department parent = Department.builder().name("Parent Department").build();
        Department savedParent = departmentRepository.save(parent);

        // Create sub-departments
        Department sub1 = Department.builder().name("Sub Department 1").parentDepartment(savedParent).build();
        Department sub2 = Department.builder().name("Sub Department 2").parentDepartment(savedParent).build();
        departmentRepository.saveAllAndFlush(new ArrayList<>(List.of(sub1, sub2)));

        // Call deleteDepartment for parent department
        departmentService.deleteDepartment(savedParent.getId());

        // Verify that parent and all sub-departments are deleted
        assertFalse(departmentRepository.existsById(savedParent.getId()));
        assertFalse(departmentRepository.existsById(sub1.getId()));
        assertFalse(departmentRepository.existsById(sub2.getId()));
    }

    /**
     * Test case for deleting a department that would create a circular dependency.
     * Verifies that an IllegalArgumentException is thrown.
     */
    @Test
    public void testDeleteDepartment_circularDependency() {
        // Create departments
        Department department1 = Department.builder().name("Department 1").subDepartments(new ArrayList<>()).build();
        Department department2 = Department.builder().name("Department 2").subDepartments(new ArrayList<>()).build();
        department1.addSubDepartment(department2);
        department2.addSubDepartment(department1);

        // Save departments to the repository
        Department savedDepartment1 = departmentRepository.save(department1);
        Department savedDepartment2 = departmentRepository.save(department2);


        // Attempt to delete department1
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> departmentService.deleteDepartment(savedDepartment1.getId()));

        // Verify the exception message
        assertTrue(exception.getMessage().contains("circular dependency"));
    }

    /**
     * Test case for retrieving all departments when the repository is populated.
     * Verifies that the method returns a list of all departments.
     */
    @Test
    @Transactional
    public void testGetAllDepartments_withDepartments() {
        // Create some departments
        Department department1 = Department.builder().name("Department 1").subDepartments(new ArrayList<>()).build();
        Department department2 = Department.builder().name("Department 2").subDepartments(new ArrayList<>()).build();

        // Save departments to the repository
        departmentRepository.save(department1);
        departmentRepository.save(department2);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify that the result contains the expected number of departments
        assertEquals(2, result.size(), "The result should contain 2 departments");

        // Extract department names from the result for verification
        List<String> departmentNames = result.stream()
                .map(DepartmentResponseDto::getName)
                .collect(Collectors.toList());

        // Verify that the result contains the expected department names
        assertTrue(departmentNames.contains("Department 1"), "The result should contain 'Department 1'");
        assertTrue(departmentNames.contains("Department 2"), "The result should contain 'Department 2'");
    }

    /**
     * Test case for retrieving all departments when the repository is empty.
     * Verifies that the method returns an empty list.
     */
    @Test
    @Transactional
    public void testGetAllDepartments_noDepartments() {
        // Ensure the repository is empty
        departmentRepository.deleteAll();

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify that the result is an empty list
        assertTrue(result.isEmpty(), "The result should be an empty list");
    }

    /**
     * Test case for retrieving sub-departments for a valid parent department.
     * Verifies that the method returns a list of sub-departments.
     */
    @Test
    @Transactional
    public void testGetSubDepartments_validParentWithSubDepartments() {
        // Create parent and sub-departments
        Department parent = Department.builder().name("Parent Department").subDepartments(new ArrayList<>()).build();
        Department sub1 = Department.builder().name("Sub Department 1").parentDepartment(parent).build();
        Department sub2 = Department.builder().name("Sub Department 2").parentDepartment(parent).build();
        parent.getSubDepartments().add(sub1);
        parent.getSubDepartments().add(sub2);

        // Save parent and sub-departments to the repository
        departmentRepository.save(parent);
        departmentRepository.save(sub1);
        departmentRepository.save(sub2);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(parent.getId());

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify that the result contains the expected number of sub-departments
        assertEquals(2, result.size(), "The result should contain 2 sub-departments");

        // Verify the sub-department names
        List<String> subNames = result.stream()
                .map(DepartmentResponseDto::getName)
                .collect(Collectors.toList());
        assertTrue(subNames.contains("Sub Department 1"), "The result should contain 'Sub Department 1'");
        assertTrue(subNames.contains("Sub Department 2"), "The result should contain 'Sub Department 2'");
    }

    /**
     * Test case for retrieving sub-departments for a parent department with no sub-departments.
     * Verifies that the method returns an empty list.
     */
    @Test
    @Transactional
    public void testGetSubDepartments_validParentNoSubDepartments() {
        // Create a parent department with no sub-departments
        Department parent = Department.builder().name("Parent Department").subDepartments(new ArrayList<>()).build();
        departmentRepository.save(parent);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(parent.getId());

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify that the result is an empty list
        assertTrue(result.isEmpty(), "The result should be an empty list");
    }

    /**
     * Test case for retrieving sub-departments for a non-existent parent department.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    @Transactional
    public void testGetSubDepartments_nonExistentParent() {
        // Call the method to test with a non-existent parent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.getSubDepartments(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Parent department not found with id: " + nonExistentId, exception.getMessage());
    }

    /**
     * Test case for retrieving a department by a valid ID.
     * Verifies that the method returns the expected department.
     */
    @Test
    @Transactional
    public void testGetDepartmentById_validId() {
        // Create and save a department
        Department department = Department.builder().name("Test Department").build();
        Department savedDepartment = departmentRepository.save(department);

        // Call the method to test
        DepartmentResponseDto result = departmentService.getDepartmentById(savedDepartment.getId());

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify the department name
        assertEquals("Test Department", result.getName(), "The department name should match");
    }

    /**
     * Test case for retrieving a department by a non-existent ID.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    @Transactional
    public void testGetDepartmentById_nonExistentId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDepartmentById(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Department not found with id: " + nonExistentId, exception.getMessage());
    }

    /**
     * Integration test to verify behavior when searching departments by an existing name.
     * Expectation: The method should return a list containing the departments with the specified name.
     */
    @Test
    @Transactional
    public void testSearchDepartmentsByName() {
        // Given
        String searchName = "Engineering";
        createDepartment("Engineering"); // Create a department with the name "Engineering"

        // When
        List<DepartmentResponseDto> departments = departmentService.searchDepartmentsByName(searchName);

        // Then
        assertThat(departments).isNotNull();
        assertThat(departments).hasSizeGreaterThanOrEqualTo(1); // Assuming only one department is created with the name "Engineering"
        assertThat(departments.get(0).getName()).isEqualTo("Engineering");
    }

    /**
     * Helper method to create a department with the specified name.
     *
     * @param name Name of the department to create.
     */
    private void createDepartment(String name) {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name(name)
                .build();
        departmentService.createDepartment(requestDto);
    }

    /**
     * Integration test to verify behavior when searching departments by a non-existing name.
     * Expectation: The method should return an empty list.
     */
    @Test
    @Transactional
    public void testSearchDepartmentsByNameEmptyList() {
        // Given
        String nonExistingDepartmentName = "NonExistingDepartment-" + UUID.randomUUID().toString();

        // When searching for a non-existing department
        List<DepartmentResponseDto> departments = departmentService.searchDepartmentsByName(nonExistingDepartmentName);

        // Then the result should be an empty list
        assertThat(departments).isNotNull();
        assertThat(departments).isEmpty();
    }

    /**
     * Test case for retrieving sub-departments with a valid parent ID.
     * Verifies that the method returns the expected sub-departments.
     */
    @Test
    @Transactional
    public void testGetSubDepartments_validParentId_withSubDepartments() {
        // Create and save a parent department
        Department parent = Department.builder().name("Parent Department").subDepartments(new ArrayList<>()).build();
        Department savedParent = departmentRepository.save(parent);

        // Create and save sub-departments
        Department subDepartment1 = Department.builder().name("Sub Department 1").build();
        Department subDepartment2 = Department.builder().name("Sub Department 2").build();
        savedParent.addSubDepartment(subDepartment1);
        savedParent.addSubDepartment(subDepartment2);

        departmentRepository.save(subDepartment1);
        departmentRepository.save(subDepartment2);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(savedParent.getId());

        // Verify that the result is not null and has the expected size
        assertNotNull(result, "The result should not be null");
        assertEquals(2, result.size(), "The result size should be 2");

        // Verify the names of the sub-departments
        assertTrue(result.stream().anyMatch(dto -> dto.getName().equals("Sub Department 1")), "The result should contain Sub Department 1");
        assertTrue(result.stream().anyMatch(dto -> dto.getName().equals("Sub Department 2")), "The result should contain Sub Department 2");
    }

    /**
     * Test case for retrieving sub-departments with a valid parent ID and no sub-departments.
     * Verifies that the method returns an empty list.
     */
    @Test
    @Transactional
    public void testGetSubDepartments_validParentId_noSubDepartments() {
        // Create and save a parent department
        Department parent = Department.builder().name("Parent Department").build();
        Department savedParent = departmentRepository.save(parent);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(savedParent.getId());

        // Verify that the result is not null and is empty
        assertNotNull(result, "The result should not be null");
        assertTrue(result.isEmpty(), "The result should be empty");
    }

    /**
     * Test case for retrieving sub-departments with a non-existent parent ID.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    @Transactional
    public void testGetSubDepartments_nonExistentParentId() {
        // Call the method to test with a non-existent parent ID
        Long nonExistentParentId = -1L;

        // Verify that the method throws EntityNotFoundException
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.getSubDepartments(nonExistentParentId);
        });

        // Verify the exception message
        assertEquals("Parent department not found with id: " + nonExistentParentId, exception.getMessage());
    }

    /**
     * Test case for retrieving the parent department of a department with a valid ID.
     * Verifies that the method returns the expected parent department.
     */
    @Test
    public void testGetParentDepartment_validId() {
        // Create and save parent and child departments
        Department parent = Department.builder().name("Parent Department").build();
        Department child = Department.builder().name("Child Department").parentDepartment(parent).build();
        departmentRepository.save(parent);
        departmentRepository.save(child);

        // Call the method to test
        DepartmentResponseDto result = departmentService.getParentDepartment(child.getId());

        // Verify that the result is not null and has the expected name
        assertNotNull(result, "The result should not be null");
        assertEquals("Parent Department", result.getName(), "The result name should be 'Parent Department'");
    }

    /**
     * Test case for retrieving the parent department of a department with no parent.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetParentDepartment_noParent() {
        // Create and save a department with no parent
        Department department = Department.builder().name("Orphan Department").build();
        Department savedDepartment = departmentRepository.save(department);

        // Verify that the method throws EntityNotFoundException
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(savedDepartment.getId());
        });

        // Verify the exception message
        assertEquals("Department with id: " + savedDepartment.getId() + " has no parent department", exception.getMessage());
    }

    /**
     * Test case for retrieving the parent department of a non-existent department ID.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetParentDepartment_nonExistentId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Department not found with id: " + nonExistentId, exception.getMessage());
    }

    /**
     * Test case for retrieving all descendants of a department with a valid ID.
     * Verifies that the method returns the expected descendants.
     */
    @Test
    public void testGetDescendants_validId() {
        // Create and save departments
        Department parent = Department.builder().name("Parent Department").build();
        Department child1 = Department.builder().name("Child Department 1").parentDepartment(parent).build();
        Department child2 = Department.builder().name("Child Department 2").parentDepartment(parent).build();
        departmentRepository.save(parent);
        departmentRepository.save(child1);
        departmentRepository.save(child2);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getDescendants(parent.getId());

        // Verify that the result is not null and has the expected size
        assertNotNull(result, "The result should not be null");
        assertEquals(2, result.size(), "The result size should be 2");

        // Verify the names of the descendants
        assertTrue(result.stream().anyMatch(dto -> dto.getName().equals("Child Department 1")), "The result should contain Child Department 1");
        assertTrue(result.stream().anyMatch(dto -> dto.getName().equals("Child Department 2")), "The result should contain Child Department 2");
    }

    /**
     * Test case for retrieving all descendants of a department with no descendants.
     * Verifies that the method returns an empty list.
     */
    @Test
    public void testGetDescendants_noDescendants() {
        // Create and save a department with no descendants
        Department department = Department.builder().name("Orphan Department").build();
        Department savedDepartment = departmentRepository.save(department);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getDescendants(savedDepartment.getId());

        // Verify that the result is not null and is empty
        assertNotNull(result, "The result should not be null");
        assertTrue(result.isEmpty(), "The result should be empty");
    }

    /**
     * Test case for retrieving all descendants of a non-existent department ID.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetDescendants_nonExistentId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Department not found with id: " + nonExistentId, exception.getMessage());
    }

    /**
     * Test case for retrieving all ancestors of a department with a valid ID.
     * Verifies that the method returns the expected ancestors.
     */
    @Test
    public void testGetAncestors_validId() {
        // Create and save departments
        Department grandParent = Department.builder().name("Grand Parent Department").build();
        Department parent = Department.builder().name("Parent Department").parentDepartment(grandParent).build();
        Department child = Department.builder().name("Child Department").parentDepartment(parent).build();
        departmentRepository.save(grandParent);
        departmentRepository.save(parent);
        departmentRepository.save(child);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getAncestors(child.getId());

        // Verify that the result is not null and has the expected size
        assertNotNull(result, "The result should not be null");
        assertEquals(2, result.size(), "The result size should be 2");

        // Verify the names of the ancestors
        assertTrue(result.stream().anyMatch(dto -> dto.getName().equals("Parent Department")), "The result should contain Parent Department");
        assertTrue(result.stream().anyMatch(dto -> dto.getName().equals("Grand Parent Department")), "The result should contain Grand Parent Department");
    }

    /**
     * Test case for retrieving all ancestors of a department with no ancestors.
     * Verifies that the method returns an empty list.
     */
    @Test
    public void testGetAncestors_noAncestors() {
        // Create and save a department with no ancestors
        Department department = Department.builder().name("Orphan Department").build();
        Department savedDepartment = departmentRepository.save(department);

        // Call the method to test
        List<DepartmentResponseDto> result = departmentService.getAncestors(savedDepartment.getId());

        // Verify that the result is not null and is empty
        assertNotNull(result, "The result should not be null");
        assertTrue(result.isEmpty(), "The result should be empty");
    }

    /**
     * Test case for retrieving all ancestors of a non-existent department ID.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetAncestors_nonExistentId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getAncestors(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Department not found with id: " + nonExistentId, exception.getMessage());
    }
    // Other test methods like update, delete, retrieval, etc. using DTOs
}
