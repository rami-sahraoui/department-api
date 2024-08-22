package tn.engn.hierarchicalentityapi.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
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
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

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
//@ActiveProfiles("test-real-db")
//public class AdjacencyListDepartmentServiceIT {
@ActiveProfiles("test-container")
public class AdjacencyListDepartmentServiceIT extends TestContainerSetup {

    @Autowired
    private AdjacencyListDepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private Environment environment;

    @Value("${entity.max-name-length}") // Assuming maxNameLength is configured in application properties
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
    public void testCreateEntityWithValidName() {
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("Valid Department Name")
                .build();

        // Create department with a valid name, expect no exceptions
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

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
    public void testCreateEntityWithMaxLengthName() {
        String maxLengthName = "A".repeat(maxNameLength);

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(maxLengthName)
                .build();

        // Create department with a name of maximum length, expect no exceptions
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

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
    public void testCreateEntityWithExceededMaxLengthName() {
        String exceededName = "A".repeat(maxNameLength + 1);

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(exceededName)
                .build();

        // Create department with a name that exceeds max length, expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Test creating a department with an empty name using DTO.
     */
    @Test
    @Transactional
    public void testCreateEntityWithEmptyName() {
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("")
                .build();

        // Create department with an empty name, expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Test creating a department with a null name using DTO.
     */
    @Test
    @Transactional
    public void testCreateEntityWithNullName() {
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(null)
                .build();

        // Create department with a null name, expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Test creating a department without parent using DTO.
     */
    @Test
    @Transactional
    public void testCreateEntityWithoutParent() {
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("Department Without Parent")
                .build();

        // Create department without a parent
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

        // Assertions
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getParentEntityId()).isNull();
    }

    /**
     * Test creating a department with a parent using DTO.
     */
    @Test
    @Transactional
    public void testCreateEntityWithParent() {
        // Create parent department
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name("Parent Department")
                .build();
        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);

        // Create child department with parent reference
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Child Department")
                .parentEntityId(parentResponseDto.getId())
                .build();
        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        Department parent = departmentRepository.findById(parentResponseDto.getId()).orElseThrow();
        Department child = departmentRepository.findById(childResponseDto.getId()).orElseThrow();

        // Assertions
        assertThat(childResponseDto).isNotNull();
        assertThat(childResponseDto.getParentEntityId()).isNotNull();
        assertThat(childResponseDto.getParentEntityId()).isEqualTo(parentResponseDto.getId());

        assertNotNull(parent.getSubEntities());
        assertFalse(parent.getSubEntities().isEmpty());
        assertEquals(1, parent.getSubEntities().size());
        assertTrue(parent.getSubEntities().contains(child));
        // No assertion for sub-departments in this test, as per provided details
    }

    /**
     * Test attempting to create a department with a non-existing parent ID using DTO.
     */
    @Test
    @Transactional
    public void testNonExistingParentId() {
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("Department")
                .parentEntityId(-1L)
                .build();

        // Attempt to create a department with a non-existing parent ID
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.createEntity(requestDto));
    }

    @Test
    /**
     * Integration test for updating a department's name.
     * Verifies that the department's name is updated correctly.
     */
    public void testUpdateEntity_updateName() {
        // Create a department
        Department department = new Department();
        department.setName("Original Department Name");
        department = departmentRepository.save(department);

        // Prepare the update request
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated Department Name");

        // Perform the update
        HierarchyResponseDto updatedDto = departmentService.updateEntity(department.getId(), requestDto);

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
    public void testUpdateEntity_departmentNotFound() {
        // Prepare the update request for a non-existent department
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated Department Name");

        // Perform the update and assert that EntityNotFoundException is thrown
        assertThrows(EntityNotFoundException.class, () -> departmentService.updateEntity(-1L, requestDto));
    }

    @Test
    /**
     * Integration test for updating a department with a parent that does not exist.
     * Expects EntityNotFoundException to be thrown.
     */
    public void testUpdateEntity_parentEntityNotFound() {
        // Create a department
        Department department = new Department();
        department.setName("Department");
        department = departmentRepository.save(department);

        // Prepare the update request with a non-existent parent department ID
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated Department Name");
        requestDto.setParentEntityId(-1L); // Non-existent parent ID

        // Perform the update and assert that EntityNotFoundException is thrown
        Department finalDepartment = department;
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.updateEntity(finalDepartment.getId(), requestDto));
    }

    @Test
    /**
     * Integration test for updating a department with an empty or null name.
     * Expects IllegalArgumentException to be thrown.
     */
    public void testUpdateEntity_emptyOrNullName() {
        // Create a department
        Department department = new Department();
        department.setName("Department");
        department = departmentRepository.save(department);

        // Prepare the update request with an empty or null name
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName(""); // Empty name

        // Perform the update and assert that IllegalArgumentException is thrown
        Department finalDepartment = department;
        assertThrows(ValidationException.class, () -> departmentService.updateEntity(finalDepartment.getId(), requestDto));
    }

    @Test
    /**
     * Integration test for updating a department with a name that exceeds maximum length.
     * Expects ValidationException to be thrown.
     */
    public void testUpdateEntity_ExceededMaxLengthName() {
        // Create a department
        Department department = new Department();
        department.setName("Department");
        department = departmentRepository.save(department);

        // Prepare the update request with name that exceeds max length
        String exceededName = "A".repeat(maxNameLength + 1);

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(exceededName)
                .build();

        // Perform the update and assert that ValidationException is thrown
        Department finalDepartment = department;
        assertThrows(ValidationException.class, () -> departmentService.updateEntity(finalDepartment.getId(), requestDto));
    }

    @Test
    @Transactional
    /**
     * Integration test for updating a department that would create a circular dependency.
     * Expects IllegalArgumentException to be thrown.
     */
    public void testUpdateEntity_circularDependency() {
        // Create departments
        HierarchyRequestDto department1RequestDto = HierarchyRequestDto.builder()
                .name("Department 1")
                .parentEntityId(null)
                .build();

        HierarchyResponseDto department1ResponseDto = departmentService.createEntity(department1RequestDto);

        HierarchyRequestDto department2RequestDto = HierarchyRequestDto.builder()
                .name("Department 2")
                .parentEntityId(department1ResponseDto.getId())
                .build();

        HierarchyResponseDto department2ResponseDto = departmentService.createEntity(department2RequestDto);

        Department department1 = departmentRepository.findById(department1ResponseDto.getId()).orElseThrow();
        Department department2 = departmentRepository.findById(department2ResponseDto.getId()).orElseThrow();

        // Attempt to set department1 as a child of department2, creating a circular dependency
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated Department 1 Name");
        requestDto.setParentEntityId(department2ResponseDto.getId());

        // Perform the update and assert that IllegalArgumentException is thrown
        assertThrows(DataIntegrityException.class, () -> departmentService.updateEntity(department1ResponseDto.getId(), requestDto));
    }

    @Test
    /**
     * Test case for updating the parent department of a department.
     * Verifies that the parent department is updated correctly.
     */
    public void testUpdateEntity_updateParent() {
        // Create and save parent and child departments
        Department oldParent = Department.builder().name("Old Parent Department").build();
        Department newParent = Department.builder().name("New Parent Department").build();
        Department child = Department.builder().name("Child Department").parentEntity(oldParent).build();
        departmentRepository.save(oldParent);
        departmentRepository.save(newParent);
        departmentRepository.save(child);

        // Create the request DTO with the new parent ID
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("Updated Child Department")
                .parentEntityId(newParent.getId())
                .build();

        // Call the method to test
        HierarchyResponseDto result = departmentService.updateEntity(child.getId(), requestDto);

        // Verify that the result is not null and has the expected parent department ID
        assertNotNull(result, "The result should not be null");
        assertEquals(newParent.getId(), result.getParentEntityId(), "The result parent ID should be the new parent's ID");
    }

    @Test
    @Transactional
    /**
     * Test case for removing the parent department of a department.
     * Verifies that the parent department is removed correctly.
     */
    public void testUpdateEntity_removeParent() {
        // Create and save parent and child departments
        HierarchyRequestDto parent = HierarchyRequestDto.builder().name("Parent Department").build();
        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parent);

        HierarchyRequestDto child = HierarchyRequestDto.builder().name("Child Department").parentEntityId(parentResponseDto.getId()).build();
        HierarchyResponseDto childResponseDto = departmentService.createEntity(child);


        // Create the request DTO with no parent ID
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("Updated Child Department")
                .parentEntityId(null)
                .build();

        // Call the method to test
        HierarchyResponseDto result = departmentService.updateEntity(childResponseDto.getId(), requestDto);

        // Verify that the result is not null and has no parent department ID
        assertNotNull(result, "The result should not be null");
        assertNull(result.getParentEntityId(), "The result should have no parent department ID");
    }

    @Test
    /**
     * Test case for updating the department without changing the parent.
     * Verifies that unnecessary updates are avoided.
     */
    @Transactional
    public void testUpdateEntity_avoidUnnecessaryUpdate() {
        // Create and save parent and child departments
        HierarchyRequestDto parentRequest = HierarchyRequestDto.builder()
                .name("Parent Department")
                .build();

        HierarchyResponseDto parent = departmentService.createEntity(parentRequest);
        Department savedParent = departmentRepository.findById(parent.getId()).get();

        HierarchyRequestDto childRequest = HierarchyRequestDto.builder()
                .name("Child Department")
                .parentEntityId(savedParent.getId())
                .build();

        HierarchyResponseDto child = departmentService.createEntity(childRequest);
        Department savedChild = departmentRepository.findById(child.getId()).get();

        // Create the request DTO with the same parent ID
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .id(savedChild.getId())
                .name("Updated Child Department")
                .parentEntityId(savedParent.getId())
                .build();

        // Call the method to test
        HierarchyResponseDto result = departmentService.updateEntity(savedChild.getId(), requestDto);

        // Verify that the result is not null and has the expected parent department ID
        assertNotNull(result, "The result should not be null");
        assertEquals(parent.getId(), result.getParentEntityId(), "The result parent ID should be the parent's ID");
    }

    @Test
    /**
     * Integration test for deleting an existing department.
     * Verifies that the department is deleted successfully.
     */
    public void testDeleteEntity_existingDepartment() {
        // Create a department
        Department department = new Department();
        department.setName("Department");
        department = departmentRepository.save(department);

        // Delete the department
        departmentService.deleteEntity(department.getId());

        // Verify that the department is deleted by checking repository
        assertFalse(departmentRepository.findById(department.getId()).isPresent(), "Department should be deleted");
    }

    @Test
    /**
     * Integration test for deleting a non-existing department.
     * Expects EntityNotFoundException to be thrown.
     */
    public void testDeleteEntity_nonExistingDepartment() {
        // Attempt to delete a department with a non-existing ID
        Long nonExistingId = Long.MAX_VALUE;

        // Perform the delete and assert that EntityNotFoundException is thrown
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                departmentService.deleteEntity(nonExistingId));

        assertEquals("Entity not found with id: " + nonExistingId, exception.getMessage());
    }

    /**
     * Test case for deleting a department with sub-departments.
     * Verifies that all sub-departments are also deleted.
     */
    @Test
    @Transactional
    public void testDeleteEntity_withSubEntities() {
        // Create a parent department
        HierarchyRequestDto parentRequest = HierarchyRequestDto.builder()
                .name("Parent Department")
                .build();

        HierarchyResponseDto parent = departmentService.createEntity(parentRequest);
        Department savedParent = departmentRepository.findById(parent.getId()).get();

        // Create sub-departments
        HierarchyRequestDto sub1Request = HierarchyRequestDto.builder()
                .name("Sub Department 1")
                .parentEntityId(savedParent.getId())
                .build();

        HierarchyResponseDto sub1 = departmentService.createEntity(sub1Request);
        Department savedSub1 = departmentRepository.findById(sub1.getId()).get();

        HierarchyRequestDto sub2Request = HierarchyRequestDto.builder()
                .name("Sub Department 2")
                .parentEntityId(savedParent.getId())
                .build();

        HierarchyResponseDto sub2 = departmentService.createEntity(sub2Request);
        Department savedSub2 = departmentRepository.findById(sub2.getId()).get();

        // Call deleteEntity for parent department
        departmentService.deleteEntity(savedParent.getId());

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
    @Transactional
    public void testDeleteEntity_circularDependency() {
        // Create departments
        HierarchyRequestDto department1RequestDto = HierarchyRequestDto.builder()
                .name("Department 1")
                .build();

        HierarchyRequestDto department2RequestDto = HierarchyRequestDto.builder()
                .name("Department 2")
                .build();

        HierarchyResponseDto department1ResponseDto = departmentService.createEntity(department1RequestDto);
        HierarchyResponseDto department2ResponseDto = departmentService.createEntity(department2RequestDto);

        Department department1 = departmentRepository.findById(department1ResponseDto.getId()).orElseThrow();
        Department department2 = departmentRepository.findById(department2ResponseDto.getId()).orElseThrow();
        department1.addSubEntity(department2);
        department2.addSubEntity(department1);

        // Save departments to the repository
        Department savedDepartment1 = departmentRepository.save(department1);
        Department savedDepartment2 = departmentRepository.save(department2);


        // Attempt to delete department1
        DataIntegrityException exception = assertThrows(
                DataIntegrityException.class,
                () -> departmentService.deleteEntity(savedDepartment1.getId())
        );

        // Verify the exception message
        assertTrue(exception.getMessage().contains("circular dependency"));
    }

    /**
     * Test case for retrieving all departments when the repository is populated.
     * Verifies that the method returns a list of all departments.
     */
    @Test
    @Transactional
    public void testGetAllEntities_withDepartments() {
        // Create some departments
        Department department1 = Department.builder().name("Department 1").subEntities(new ArrayList<>()).build();
        Department department2 = Department.builder().name("Department 2").subEntities(new ArrayList<>()).build();

        // Save departments to the repository
        departmentRepository.save(department1);
        departmentRepository.save(department2);

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify that the result contains the expected number of departments
        assertEquals(2, result.size(), "The result should contain 2 departments");

        // Extract department names from the result for verification
        List<String> departmentNames = result.stream()
                .map(HierarchyResponseDto::getName)
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
    public void testGetAllEntities_noDepartments() {
        // Ensure the repository is empty
        departmentRepository.deleteAll();

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify that the result is an empty list
        assertTrue(result.isEmpty(), "The result should be an empty list");
    }

    /**
     * Test for retrieving all departments paginated when the repository is populated.
     */
    @Test
    public void testGetAllEntities_Paginated() {
        // Given: Prepare test data
        List<Department> departments = createTestDepartments();
        departmentRepository.saveAll(departments);

        // Prepare pagination and sorting
        Pageable pageable = PageRequest.of(0, 5, Sort.by("name").descending()); // First page, 5 items per page, sorted by name
        boolean fetchSubEntities = true; // Example: Fetch sub-entities

        // When: Invoke the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getAllEntities(pageable, fetchSubEntities);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(5, result.getContent().size()); // Expecting 5 items on the first page
        assertEquals(20, result.getTotalElements()); // Total number of departments should be 10

        // Verify the order of the returned entities
        List<String> expectedNames = List.of("Department 9", "Department 8", "Department 7", "Department 6", "Department 5");
        List<String> actualNames = new ArrayList<>();
        for (HierarchyResponseDto dto : result.getContent()) {
            actualNames.add(dto.getName());
        }
        assertEquals(expectedNames, actualNames);

        // Verify sub-entities are fetched correctly
        for (HierarchyResponseDto dto : result.getContent()) {
            if (fetchSubEntities) {
                assertNotNull(dto.getSubEntities());
//                assertFalse(dto.getSubEntities().isEmpty());
            } else {
                assertNull(dto.getSubEntities());
            }
        }
    }

    // Helper method to create test departments
    private List<Department> createTestDepartments() {
        List<Department> departments = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Department parent = Department.builder().name("Department " + i).build();
            departments.add(parent);
            Department child = Department.builder().name("Child of Department " + i).parentEntity(parent).build();
            parent.getSubEntities().add(child);
        }
        return departments;
    }

    /**
     * Test case for retrieving sub-departments for a valid parent department.
     * Verifies that the method returns a list of sub-departments.
     */
    @Test
    @Transactional
    public void testGetSubEntities_validParentWithSubEntities() {
        // Create parent and sub-departments
        Department parent = Department.builder().name("Parent Department").subEntities(new ArrayList<>()).build();
        Department sub1 = Department.builder().name("Sub Department 1").parentEntity(parent).build();
        Department sub2 = Department.builder().name("Sub Department 2").parentEntity(parent).build();
        parent.getSubEntities().add(sub1);
        parent.getSubEntities().add(sub2);

        // Save parent and sub-departments to the repository
        departmentRepository.save(parent);
        departmentRepository.save(sub1);
        departmentRepository.save(sub2);

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getSubEntities(parent.getId());

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify that the result contains the expected number of sub-departments
        assertEquals(2, result.size(), "The result should contain 2 sub-departments");

        // Verify the sub-department names
        List<String> subNames = result.stream()
                .map(HierarchyResponseDto::getName)
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
    public void testGetSubEntities_validParentNoSubEntities() {
        // Create a parent department with no sub-departments
        Department parent = Department.builder().name("Parent Department").subEntities(new ArrayList<>()).build();
        departmentRepository.save(parent);

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getSubEntities(parent.getId());

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
    public void testGetSubEntities_nonExistentParent() {
        // Call the method to test with a non-existent parent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.getSubEntities(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Parent entity not found with id: " + nonExistentId, exception.getMessage());
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

        // Verify the entities are correctly created in the repository
        Department parentDepartment = departmentRepository.findById(parentResponseDto.getId()).orElseThrow();
        Department child1Department = departmentRepository.findById(child1ResponseDto.getId()).orElseThrow();
        Department child2Department = departmentRepository.findById(child2ResponseDto.getId()).orElseThrow();

        assertEquals(parentDepartment.getId(), child1Department.getParentId(), "Child 1 parent ID mismatch");
        assertEquals(parentDepartment.getId(), child2Department.getParentId(), "Child 2 parent ID mismatch");

        assertEquals(parentDepartment, child1Department.getParentEntity(), "Child 1 parent mismatch");
        assertEquals(parentDepartment, child2Department.getParentEntity(), "Child 2 parent mismatch");

        // Log the current state of the entities
        log.info("Parent Department ID: {}", parentDepartment.getId());
        log.info("Child 1 Department: {}, Parent ID: {}", child1Department.getId(), child1Department.getParentId());
        log.info("Child 2 Department: {}, Parent ID: {}", child2Department.getId(), child2Department.getParentId());

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
        response.getContent().forEach(dto -> {
            assertNotNull(dto.getSubEntities(), "Sub-entities first level should be fetched");
            dto.getSubEntities().forEach(sub -> {
                assertNull(sub.getSubEntities(), "Sub-entities should not be fetched");
            });
        });
    }


    /**
     * Test case for retrieving a department by a valid ID.
     * Verifies that the method returns the expected department.
     */
    @Test
    @Transactional
    public void testGetEntityById_validId() {
        // Create and save a department
        Department department = Department.builder().name("Test Department").build();
        Department savedDepartment = departmentRepository.save(department);

        // Call the method to test
        HierarchyResponseDto result = departmentService.getEntityById(savedDepartment.getId());

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
    public void testGetEntityById_nonExistentId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getEntityById(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Entity not found with id: " + nonExistentId, exception.getMessage());
    }

    /**
     * Integration test to verify behavior when searching departments by an existing name.
     * Expectation: The method should return a list containing the departments with the specified name.
     */
    @Test
    @Transactional
    public void testSearchEntitiesByName() {
        // Given
        String searchName = "Engineering";
        createEntity("Engineering"); // Create a department with the name "Engineering"

        // When
        List<HierarchyResponseDto> departments = departmentService.searchEntitiesByName(searchName);

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
    private void createEntity(String name) {
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(name)
                .build();
        departmentService.createEntity(requestDto);
    }

    /**
     * Integration test to verify behavior when searching departments by a non-existing name.
     * Expectation: The method should return an empty list.
     */
    @Test
    @Transactional
    public void testSearchEntitiesByNameEmptyList() {
        // Given
        String nonExistingDepartmentName = "NonExistingDepartment-" + UUID.randomUUID().toString();

        // When searching for a non-existing department
        List<HierarchyResponseDto> departments = departmentService.searchEntitiesByName(nonExistingDepartmentName);

        // Then the result should be an empty list
        assertThat(departments).isNotNull();
        assertThat(departments).isEmpty();
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

        HierarchyRequestDto parent2 = HierarchyRequestDto.builder().name("Parent 2 Department").build();
        HierarchyResponseDto parent2ResponseDto = departmentService.createEntity(parent2);

        HierarchyRequestDto child1 = HierarchyRequestDto.builder().name("Child 1 Department").parentEntityId(parent1ResponseDto.getId()).build();
        HierarchyResponseDto child1ResponseDto = departmentService.createEntity(child1);

        HierarchyRequestDto child2 = HierarchyRequestDto.builder().name("Child 2 Department").parentEntityId(parent2ResponseDto.getId()).build();
        HierarchyResponseDto child2ResponseDto = departmentService.createEntity(child2);

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
        response.getContent().forEach(dto -> {
            assertNotNull(dto.getSubEntities());
            dto.getSubEntities().forEach(sub -> {
                assertNull(sub.getSubEntities(), "Sub-entities should not be fetched");
            });
        });
    }


    /**
     * Test case for retrieving sub-departments with a valid parent ID.
     * Verifies that the method returns the expected sub-departments.
     */
    @Test
    @Transactional
    public void testGetSubEntities_validParentId_withSubEntities() {
        // Create and save a parent department
        Department parent = Department.builder().name("Parent Department").subEntities(new ArrayList<>()).build();
        Department savedParent = departmentRepository.save(parent);

        // Create and save sub-departments
        Department subDepartment1 = Department.builder().name("Sub Department 1").build();
        Department subDepartment2 = Department.builder().name("Sub Department 2").build();
        savedParent.addSubEntity(subDepartment1);
        savedParent.addSubEntity(subDepartment2);

        departmentRepository.save(subDepartment1);
        departmentRepository.save(subDepartment2);

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getSubEntities(savedParent.getId());

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
    public void testGetSubEntities_validParentId_nosubEntities() {
        // Create and save a parent department
        Department parent = Department.builder().name("Parent Department").build();
        Department savedParent = departmentRepository.save(parent);

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getSubEntities(savedParent.getId());

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
    public void testGetSubEntities_nonExistentParentId() {
        // Call the method to test with a non-existent parent ID
        Long nonExistentParentId = -1L;

        // Verify that the method throws EntityNotFoundException
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.getSubEntities(nonExistentParentId);
        });

        // Verify the exception message
        assertEquals("Parent entity not found with id: " + nonExistentParentId, exception.getMessage());
    }

    /**
     * Test case for retrieving the parent department of a department with a valid ID.
     * Verifies that the method returns the expected parent department.
     */
    @Test
    public void testGetParentEntity_validId() {
        // Create and save parent and child departments
        Department parent = Department.builder().name("Parent Department").build();
        Department child = Department.builder().name("Child Department").parentEntity(parent).build();
        departmentRepository.save(parent);
        departmentRepository.save(child);

        // Call the method to test
        HierarchyResponseDto result = departmentService.getParentEntity(child.getId());

        // Verify that the result is not null and has the expected name
        assertNotNull(result, "The result should not be null");
        assertEquals("Parent Department", result.getName(), "The result name should be 'Parent Department'");
    }

    /**
     * Test case for retrieving the parent department of a department with no parent.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetParentEntity_noParent() {
        // Create and save a department with no parent
        Department department = Department.builder().name("Orphan Department").build();
        Department savedDepartment = departmentRepository.save(department);

        // Verify that the method throws EntityNotFoundException
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.getParentEntity(savedDepartment.getId());
        });

        // Verify the exception message
        assertEquals("Parent entity not found for entity with id: " + savedDepartment.getId(), exception.getMessage());
    }

    /**
     * Test case for retrieving the parent department of a non-existent department ID.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetParentEntity_nonExistentId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getParentEntity(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Entity not found with id: " + nonExistentId, exception.getMessage());
    }

    /**
     * Test case for retrieving all descendants of a department with a valid ID.
     * Verifies that the method returns the expected descendants.
     */
    @Test
    @Transactional
    public void testGetDescendants_validId() {
        // Create and save departments
        HierarchyRequestDto parent = HierarchyRequestDto.builder().name("Parent Department").build();
        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parent);
        HierarchyRequestDto child1 = HierarchyRequestDto.builder().name("Child Department 1").parentEntityId(parentResponseDto.getId()).build();
        HierarchyResponseDto child1ResponseDto = departmentService.createEntity(child1);
        HierarchyRequestDto child2 = HierarchyRequestDto.builder().name("Child Department 2").parentEntityId(parentResponseDto.getId()).build();
        HierarchyResponseDto child2ResponseDto  = departmentService.createEntity(child2);

        Department parentEntity = departmentRepository.findById(parentResponseDto.getId()).orElseThrow();
        Department child1Entity = departmentRepository.findById(child1ResponseDto.getId()).orElseThrow();
        Department child2Entity = departmentRepository.findById(child2ResponseDto.getId()).orElseThrow();

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getDescendants(parentResponseDto.getId());

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
        List<HierarchyResponseDto> result = departmentService.getDescendants(savedDepartment.getId());

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
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Entity not found with id: " + nonExistentId, exception.getMessage());
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


    /**
     * Test case for retrieving all ancestors of a department with a valid ID.
     * Verifies that the method returns the expected ancestors.
     */
    @Test
    public void testGetAncestors_validId() {
        // Create and save departments
        Department grandParent = Department.builder().name("Grand Parent Department").build();
        Department parent = Department.builder().name("Parent Department").parentEntity(grandParent).build();
        Department child = Department.builder().name("Child Department").parentEntity(parent).build();
        departmentRepository.save(grandParent);
        departmentRepository.save(parent);
        departmentRepository.save(child);

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getAncestors(child.getId());

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
        List<HierarchyResponseDto> result = departmentService.getAncestors(savedDepartment.getId());

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
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getAncestors(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Entity not found with id: " + nonExistentId, exception.getMessage());
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

    // Other test methods like update, delete, retrieval, etc. using DTOs
}
