package tn.engn.hierarchicalentityapi.service;

import com.querydsl.core.BooleanBuilder;
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
import tn.engn.hierarchicalentityapi.model.QDepartment;
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NestedSetDepartmentService.
 * Tests CRUD operations and business logic within the Nested Set Model.
 */
@SpringBootTest
@Slf4j
//@ActiveProfiles("test-real-db")
//public class NestedSetDepartmentServiceIT {
@ActiveProfiles("test-container")
public class NestedSetDepartmentServiceIT extends TestContainerSetup {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    @Autowired
    private NestedSetDepartmentService departmentService;

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
        // Clear the database before each test
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
     * Test creating a root department without a parent.
     */
    @Test
    public void testCreateRootDepartment_Success() {
        // Given
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("Engineering")
                .build();

        // When
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

        // Then
        assertNotNull(responseDto.getId());
        assertEquals("Engineering", responseDto.getName());
        assertNull(responseDto.getParentEntityId());
        assertNotNull(responseDto.getSubEntities());
        assertEquals(0, responseDto.getSubEntities().size());

        assertIndexes(responseDto, 1); // Assuming left and right indexes for Engineering

        Department department = departmentRepository.findById(responseDto.getId()).get();
        assertEquals(responseDto.getId(), department.getRootId());
        assertEquals(0, department.getLevel());
    }

    /**
     * Test creating a child department with a parent and assert indexes.
     */
    @Test
    public void testCreateChildDepartment_Success() {
        // Given
        HierarchyRequestDto parentRequestDto = HierarchyRequestDto.builder()
                .name("Engineering")
                .build();
        HierarchyResponseDto parentResponseDto = departmentService.createEntity(parentRequestDto);

        // When creating child department
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name("Software Development")
                .parentEntityId(parentResponseDto.getId())
                .build();
        HierarchyResponseDto childResponseDto = departmentService.createEntity(childRequestDto);

        // Then
        assertNotNull(childResponseDto.getId());
        assertEquals("Software Development", childResponseDto.getName());
        assertEquals(parentResponseDto.getId(), childResponseDto.getParentEntityId());
        assertNotNull(childResponseDto.getSubEntities());
        assertEquals(0, childResponseDto.getSubEntities().size());

        // Verify indexes
        assertNestedSetIndexes();
        assertNestedSetIndexes(parentResponseDto.getId());
        assertIndexes(parentResponseDto, 1); // Assuming left and right indexes for parent department
        assertIndexes(childResponseDto, 2); // Assuming left and right indexes for child department

        Department parent = departmentRepository.findById(parentResponseDto.getId()).get();
        assertEquals(parentResponseDto.getId(), parent.getRootId());
        assertEquals(0, parent.getLevel());

        Department child = departmentRepository.findById(childResponseDto.getId()).get();
        assertEquals(parentResponseDto.getId(), child.getRootId());
        assertEquals(1, child.getLevel());
    }

    /**
     * Test creating a real subtree of departments and assert indexes.
     */
    @Test
    public void testCreateRealSubtree_Success() {
        // Given
        // When creating root nodes
        HierarchyResponseDto engineeringResponseDto = createEntity("Engineering", null);
        HierarchyResponseDto hrDepartmentResponseDto = createEntity("HR Department", null);

        // Then
        assertIndexes(engineeringResponseDto, 1); // Assuming left and right indexes for Engineering
        assertIndexes(hrDepartmentResponseDto, 3); // Assuming left and right indexes for HR Department

        Department engineering = departmentRepository.findById(engineeringResponseDto.getId()).get();
        assertEquals(engineeringResponseDto.getId(), engineering.getRootId());
        assertEquals(0, engineering.getLevel());

        Department hrDepartment = departmentRepository.findById(hrDepartmentResponseDto.getId()).get();
        assertEquals(hrDepartmentResponseDto.getId(), hrDepartment.getRootId());
        assertEquals(0, hrDepartment.getLevel());

        // When creating middle nodes
        HierarchyResponseDto itDepartmentResponseDto = createEntity("IT Department", engineeringResponseDto.getId());
        HierarchyResponseDto recruitingTeamResponseDto = createEntity("Recruiting Team", hrDepartmentResponseDto.getId());

        // Then
        assertNotNull(itDepartmentResponseDto.getId());
        assertNotNull(hrDepartmentResponseDto.getId());

        assertEquals(itDepartmentResponseDto.getParentEntityId(), engineeringResponseDto.getId());
        assertEquals(recruitingTeamResponseDto.getParentEntityId(), hrDepartmentResponseDto.getId());

        assertIndexes(engineeringResponseDto, 1); // Assuming left and right indexes for Engineering
        assertIndexes(itDepartmentResponseDto, 2); // Assuming left and right indexes for IT department
        assertIndexes(hrDepartmentResponseDto, 5); // Assuming left and right indexes for HR Department
        assertIndexes(recruitingTeamResponseDto, 6); // Assuming left and right indexes for Frontend Team

        Department itDepartment = departmentRepository.findById(itDepartmentResponseDto.getId()).get();
        assertEquals(engineering.getRootId(), itDepartment.getRootId());
        assertEquals(1, itDepartment.getLevel());

        Department recruitingTeam = departmentRepository.findById(recruitingTeamResponseDto.getId()).get();
        assertEquals(hrDepartment.getRootId(), recruitingTeam.getRootId());
        assertEquals(1, recruitingTeam.getLevel());

        // When creating leaf nodes
        HierarchyResponseDto backendTeamResponseDto = createEntity("Backend Team", itDepartmentResponseDto.getId());
        HierarchyResponseDto frontendTeamResponseDto = createEntity("Frontend Team", itDepartmentResponseDto.getId());

        // Then
        assertNotNull(backendTeamResponseDto.getId());
        assertNotNull(frontendTeamResponseDto.getId());

        assertEquals(backendTeamResponseDto.getParentEntityId(), itDepartmentResponseDto.getId());
        assertEquals(frontendTeamResponseDto.getParentEntityId(), itDepartmentResponseDto.getId());

        // Verify indexes
        assertNestedSetIndexes();

        assertNestedSetIndexes(engineeringResponseDto.getId());
        assertNestedSetIndexes(hrDepartmentResponseDto.getId());

        assertIndexes(engineeringResponseDto, 1); // Assuming left and right indexes for Engineering
        assertIndexes(itDepartmentResponseDto, 2); // Assuming left and right indexes for IT department
        assertIndexes(backendTeamResponseDto, 3); // Assuming left and right indexes for Backend Team
        assertIndexes(frontendTeamResponseDto, 5); // Assuming left and right indexes for Frontend Team
        assertIndexes(hrDepartmentResponseDto, 9); // Assuming left and right indexes for HR Department
        assertIndexes(recruitingTeamResponseDto, 10); // Assuming left and right indexes for Frontend Team

        Department backendTeam = departmentRepository.findById(backendTeamResponseDto.getId()).get();
        assertEquals(itDepartment.getRootId(), backendTeam.getRootId());
        assertEquals(2, backendTeam.getLevel());

        Department frontendTeam = departmentRepository.findById(frontendTeamResponseDto.getId()).get();
        assertEquals(itDepartment.getRootId(), frontendTeam.getRootId());
        assertEquals(2, frontendTeam.getLevel());
    }

    /**
     * Test creating a department with an invalid name.
     */
    @Test
    public void testCreateDepartment_InvalidName() {
        // Given
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("")
                .build();

        // When / Then
        ValidationException exception = assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
        assertEquals("Entity name cannot be null or empty.", exception.getMessage());
    }

    /**
     * Test creating a department with a non-existing parent.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("HR")
                .parentEntityId(-1L) // Assuming -1L is a non-existing parent ID
                .build();

        // When / Then
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> departmentService.createEntity(requestDto));
        assertEquals("Parent entity not found with ID: -1", exception.getMessage());
    }

    /**
     * Test creating a department with a name equal to the maximum length.
     */
    @Test
    void testCreateDepartment_NameMaxLength() {
        // Generate a name that matches the maximum length
        String maxLengthName = "A".repeat(maxNameLength);

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(maxLengthName)
                .build();

        // When
        HierarchyResponseDto createdDepartment = departmentService.createEntity(requestDto);

        // Then
        assertNotNull(createdDepartment);
        assertEquals(maxLengthName, createdDepartment.getName());
    }

    /**
     * Test creating a department with a name exceeding the maximum length.
     */
    @Test
    void testCreateDepartment_NameTooLong() {
        // Generate a name that exceeds the maximum length
        String exceededName = "A".repeat(maxNameLength + 1);

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(exceededName)
                .build();

        // When / Then
        ValidationException exception = assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
        assertEquals("Entity name cannot be longer than " + maxNameLength + " characters.", exception.getMessage());
    }

    /**
     * Test case for retrieving all departments when the repository is populated.
     * Verifies that the method returns a list of all departments.
     */
    @Test
    public void testGetAllDepartments_withDepartments() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering", null);
        HierarchyResponseDto hrDepartment = createEntity("HR Department", null);
        HierarchyResponseDto itDepartment = createEntity("IT Department", engineering.getId());

        // When
        List<HierarchyResponseDto> departments = departmentService.getAllEntities();

        // Then
        assertNotNull(departments, "The result should not be null");
        assertEquals(3, departments.size(), "The result should contain 3 departments"); // Assuming 3 departments were created

        // Extract department names from the result for verification
        List<String> departmentNames = departments.stream()
                .map(HierarchyResponseDto::getName)
                .collect(Collectors.toList());

        // Verify that the result contains the expected department names
        assertTrue(departmentNames.contains(engineering.getName()), "The result should contain 'Engineering'");
        assertTrue(departmentNames.contains(hrDepartment.getName()), "The result should contain 'HR Department'");
        assertTrue(departmentNames.contains(itDepartment.getName()), "The result should contain 'IT Department'");

        HierarchyResponseDto savedEngineeringDepartment = departments.stream().
                filter(d -> d.getName().contains(engineering.getName())).findFirst()
                .get();

        HierarchyResponseDto savedItDepartment = departments.stream().
                filter(d -> d.getName().contains(itDepartment.getName())).findFirst()
                .get();

        assertEquals(savedEngineeringDepartment.getId(), savedItDepartment.getParentEntityId());
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

    /**
     * Test case for retrieving sub-departments when the parent department exists.
     * Verifies that the method returns a list of direct sub-departments.
     */
    @Test
    public void testGetSubDepartments_ExistingParent() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering", null);
        HierarchyResponseDto itDepartment = createEntity("IT Department", engineering.getId());
        HierarchyResponseDto frontendTeam = createEntity("Frontend Team", itDepartment.getId());
        HierarchyResponseDto backendTeam = createEntity("Backend Team", itDepartment.getId());

        // When
        List<HierarchyResponseDto> subDepartments = departmentService.getSubEntities(itDepartment.getId());

        // Then
        assertNotNull(subDepartments, "The result should not be null");
        assertEquals(2, subDepartments.size(), "The result should contain 2 sub-departments"); // Assuming 2 sub-departments were created

        // Extract department names from the result for verification
        List<String> departmentNames = subDepartments.stream()
                .map(HierarchyResponseDto::getName)
                .collect(Collectors.toList());

        // Verify that the result contains the expected sub-department names
        assertTrue(departmentNames.contains(frontendTeam.getName()), "The result should contain 'Frontend Team'");
        assertTrue(departmentNames.contains(backendTeam.getName()), "The result should contain 'Backend Team'");
    }

    /**
     * Test case for retrieving sub-departments when the parent department does not exist.
     * Verifies that ParentEntityNotFoundException is thrown.
     */
    @Test
    public void testGetSubDepartments_NonExistingParent() {
        // Given a non-existing parent department ID
        Long nonExistingParentId = -1L;

        // When / Then
        ParentEntityNotFoundException exception = assertThrows(
                ParentEntityNotFoundException.class,
                () -> departmentService.getSubEntities(nonExistingParentId)
        );

        assertEquals("Parent entity not found with ID: " + nonExistingParentId, exception.getMessage());
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

    /**
     * Test case for retrieving an existing department by ID.
     * Verifies that the method returns the correct department DTO.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering", null);

        // When
        HierarchyResponseDto result = departmentService.getEntityById(engineering.getId());

        // Then
        assertNotNull(result, "The result should not be null");
        assertEquals(engineering.getId(), result.getId(), "The returned department ID should match");
        assertEquals(engineering.getName(), result.getName(), "The returned department name should match");
        assertNull(result.getParentEntityId(), "The returned parent department ID should be null");
    }

    /**
     * Test case for retrieving a non-existing department by ID.
     * Verifies that EntityNotFoundException is thrown.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given a non-existing department ID
        Long nonExistingId = -1L;

        // When / Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getEntityById(nonExistingId)
        );

        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());
    }

    /**
     * Test case for searching departments by an existing name.
     * Verifies that the method returns departments matching the search name.
     */
    @Test
    public void testSearchDepartmentsByName_ExistingName() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto hrDepartment = createEntity("HR Department", null);
        HierarchyResponseDto itDepartment = createEntity("IT Department", engineering.getId());

        // When
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName("Department");

        // Then
        assertNotNull(result, "The result should not be null");
        assertEquals(3, result.size(), "The result should contain 3 departments"); // Assuming 3 departments match the search name

        // Extract department names from the result for verification
        List<String> departmentNames = result.stream()
                .map(HierarchyResponseDto::getName)
                .collect(Collectors.toList());

        // Verify that the result contains the expected department names
        assertTrue(departmentNames.contains(engineering.getName()), "The result should contain 'Engineering Department'");
        assertTrue(departmentNames.contains(hrDepartment.getName()), "The result should contain 'HR Department'");
        assertTrue(departmentNames.contains(itDepartment.getName()), "The result should contain 'IT Department'");
    }

    /**
     * Test case for searching departments by a non-existing name.
     * Verifies that the method returns an empty list.
     */
    @Test
    @Transactional
    public void testSearchDepartmentsByName_NonExistingName() {
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

    /**
     * Test case for retrieving the parent department of an existing department ID.
     * Verifies that the method returns the correct parent department.
     */
    @Test
    public void testGetParentDepartment_ExistingId() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto itDepartment = createEntity("IT Department", engineering.getId());

        // When
        HierarchyResponseDto parentDepartment = departmentService.getParentEntity(itDepartment.getId());

        // Then
        assertNotNull(parentDepartment, "The parent department should not be null");
        assertEquals(engineering.getName(), parentDepartment.getName(), "The parent department name should match");
    }

    /**
     * Test case for retrieving the parent department of a non-existent department ID.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetParentDepartment_NonExistingId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getParentEntity(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Entity not found with ID: " + nonExistentId, exception.getMessage());
    }

    /**
     * Test case for retrieving the parent department of a department without a parent.
     * Verifies that the method throws ParentEntityNotFoundException.
     */
    @Test
    public void testGetParentDepartment_NoParent() {
        // Given
        HierarchyResponseDto itDepartment = createEntity("IT Department", null);

        // When attempting to retrieve the parent department
        Throwable throwable = catchThrowable(() -> departmentService.getParentEntity(itDepartment.getId()));

        // Then ParentEntityNotFoundException should be thrown
        assertThat(throwable).isInstanceOf(ParentEntityNotFoundException.class)
                .hasMessageContaining("Entity with ID " + itDepartment.getId() + " has no parent entity");
    }

    /**
     * Test case for retrieving all root departments when there are root departments.
     * Verifies that the method returns a list of root departments.
     */
    @Test
    public void testGetAllRootDepartments_WithRootDepartments() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto hrDepartment = createEntity("HR Department", null);

        // When
        List<HierarchyResponseDto> rootDepartments = departmentService.getAllRootEntities();

        // Then
        assertNotNull(rootDepartments, "The result should not be null");
        assertEquals(2, rootDepartments.size(), "The result should contain 2 root departments");

        // Extract department names from the result for verification
        List<String> departmentNames = rootDepartments.stream()
                .map(HierarchyResponseDto::getName)
                .collect(Collectors.toList());

        // Verify that the result contains the expected department names
        assertTrue(departmentNames.contains(engineering.getName()), "The result should contain 'Engineering Department'");
        assertTrue(departmentNames.contains(hrDepartment.getName()), "The result should contain 'HR Department'");
    }

    /**
     * Test case for retrieving all root departments when there are no root departments.
     * Verifies that the method returns an empty list.
     */
    @Test
    @Transactional
    public void testGetAllRootDepartments_NoRootDepartments() {
        // Ensure there are no departments in the repository
        departmentRepository.deleteAll();

        // Call the method to test
        List<HierarchyResponseDto> result = departmentService.getAllRootEntities();

        // Verify that the result is not null
        assertNotNull(result, "The result should not be null");

        // Verify that the result is an empty list
        assertTrue(result.isEmpty(), "The result should be an empty list");
    }

    /**
     * Test case for retrieving all descendants of an existing department ID.
     * Verifies that the method returns all descendants of the given department.
     */
    @Test
    public void testGetDescendants_ExistingId() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto itDepartment = createEntity("IT Department", engineering.getId());
        HierarchyResponseDto frontendDepartment = createEntity("Frontend Department", itDepartment.getId());

        // When
        List<HierarchyResponseDto> descendants = departmentService.getDescendants(engineering.getId());

        // Then
        assertNotNull(descendants, "The result should not be null");
        assertEquals(2, descendants.size(), "The result should contain 2 descendants"); // Assuming 2 descendants were created

        // Extract department names from the result for verification
        List<String> departmentNames = descendants.stream()
                .map(HierarchyResponseDto::getName)
                .collect(Collectors.toList());

        // Verify that the result contains the expected department names
        assertTrue(departmentNames.contains(itDepartment.getName()), "The result should contain 'IT Department'");
        assertTrue(departmentNames.contains(frontendDepartment.getName()), "The result should contain 'Frontend Department'");
    }

    /**
     * Test case for retrieving descendants of a non-existent department ID.
     * Verifies that a EntityNotFoundException is thrown.
     */
    @Test
    public void testGetDescendants_NonExistingId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Entity not found with ID: " + nonExistentId, exception.getMessage());
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
     * Test case for retrieving all ancestors of an existing department ID.
     * Verifies that the method returns all ancestors of the given department.
     */
    @Test
    public void testGetAncestors_ExistingId() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto itDepartment = createEntity("IT Department", engineering.getId());
        HierarchyResponseDto frontendDepartment = createEntity("Frontend Department", itDepartment.getId());

        logDepartmentIndexes();

        // When
        List<HierarchyResponseDto> ancestors = departmentService.getAncestors(frontendDepartment.getId());

        // Then
        assertNotNull(ancestors, "The result should not be null");
        assertEquals(2, ancestors.size(), "The result should contain 2 ancestors"); // Assuming 2 ancestors were created

        // Extract department names from the result for verification
        List<String> departmentNames = ancestors.stream()
                .map(HierarchyResponseDto::getName)
                .collect(Collectors.toList());

        // Verify that the result contains the expected department names
        assertTrue(departmentNames.contains(engineering.getName()), "The result should contain 'Engineering Department'");
        assertTrue(departmentNames.contains(itDepartment.getName()), "The result should contain 'IT Department'");
    }

    /**
     * Test case for retrieving ancestors of a non-existent department ID.
     * Verifies that a EntityNotFoundException is thrown.
     */
    @Test
    public void testGetAncestors_NonExistingId() {
        // Call the method to test with a non-existent ID
        Long nonExistentId = -1L;

        // Verify that the method throws EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getAncestors(nonExistentId);
        });

        // Verify the exception message
        assertEquals("Entity not found with ID: " + nonExistentId, exception.getMessage());
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

    /**
     * Test case for deleting an existing department.
     * Verifies that the method deletes the department and its subtree correctly.
     */
    @Test
    @Transactional
    public void testDeleteDepartment_ExistingId() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);

        // When
        assertDoesNotThrow(() -> departmentService.deleteEntity(engineering.getId()));

        // Then
        assertThrows(EntityNotFoundException.class, () -> departmentService.getEntityById(engineering.getId()),
                "Department should not be found after deletion");

        // Additional assertions as needed for subtree deletion validation
    }

    /**
     * Test case for deleting a department with sub-departments (subtree deletion).
     * Verifies that the method deletes the department and all its descendants.
     */
    @Test
    @Transactional
    public void testDeleteDepartment_WithSubDepartments() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto software = createEntity("Software Department", engineering.getId());
        HierarchyResponseDto backend = createEntity("Backend Department", software.getId());

        // Log created departments for better traceability
        log.info("Created departments: Engineering (ID: {}), Software (ID: {}), Backend (ID: {})",
                engineering.getId(), software.getId(), backend.getId());

        // Act
        // Fetch and refresh the engineering department entity to ensure it is in sync with the database
        Department engineeringDepartment = departmentRepository.findById(engineering.getId()).get();
        entityManager.refresh(engineeringDepartment);

        // Retrieve sub-departments to verify they exist before deletion
        List<Department> subDepartments = departmentRepository.findSubDepartments(
                engineeringDepartment.getLeftIndex(), engineeringDepartment.getRightIndex());

        // Log the indexes of all departments before deletion for debugging purposes
        logDepartmentIndexes();

        // Assert that sub-departments are not empty before deletion
        assertFalse(subDepartments.isEmpty(), "Sub-departments should not be empty");

        // When
        // Perform the deletion and assert no exceptions are thrown
        assertDoesNotThrow(() -> departmentService.deleteEntity(engineering.getId()));

        // Then
        // Verify that the engineering department cannot be found after deletion
        assertThrows(EntityNotFoundException.class, () -> departmentService.getEntityById(engineering.getId()),
                "Engineering department should not be found after deletion");

        // Verify that the software department cannot be found after deletion of the parent department
        assertThrows(EntityNotFoundException.class, () -> departmentService.getEntityById(software.getId()),
                "Software department should not be found after deletion of parent");

        // Verify that the backend department cannot be found after deletion of the grandparent department
        assertThrows(EntityNotFoundException.class, () -> departmentService.getEntityById(backend.getId()),
                "Backend department should not be found after deletion of grandparent");
    }


    /**
     * Test case for attempting to delete a non-existent department.
     * Verifies that the method throws EntityNotFoundException.
     */
    @Test
    @Transactional
    public void testDeleteDepartment_NonExistentId() {
        // Given
        Long nonExistentId = -1L;

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> departmentService.deleteEntity(nonExistentId),
                "Expected EntityNotFoundException for non-existent ID");
    }

    /**
     * Tests deletion of a department with a non-existent parent.
     */
    @Test
    public void testDeleteDepartmentWithNonExistentParent() {
        // Given a department with a non-existent parent
        Department departmentWithNonExistentParent = Department.builder()
                .name("Department with Non-existent Parent")
                .parentId(-1L)
                .leftIndex(1)
                .rightIndex(2)
                .level(1)
                .build();
        departmentRepository.save(departmentWithNonExistentParent);

        // When deleting the department
        // Then a ParentEntityNotFoundException should be thrown
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.deleteEntity(departmentWithNonExistentParent.getId()));
    }

    /**
     * Tests deletion of a department with circular reference.
     */
    @Test
    public void testDeleteDepartmentWithCircularReference() {
        // Given a root department and a circular reference sub-department
        Department rootDepartment = Department.builder()
                .name("Root Department")
                .leftIndex(1)
                .rightIndex(4)
                .level(1)
                .build();
        rootDepartment = departmentRepository.save(rootDepartment);

        Department subDepartment = Department.builder()
                .name("Sub Department")
                .parentId(rootDepartment.getId())
                .leftIndex(2)
                .rightIndex(3)
                .level(2)
                .build();
        subDepartment = departmentRepository.save(subDepartment);

        // Create a circular reference
        rootDepartment.setParentId(subDepartment.getId());
        departmentRepository.save(rootDepartment);

        final Long subDepartmentId = subDepartment.getId();

        // When deleting the sub-department
        // Then a DataIntegrityException should be thrown
        assertThrows(DataIntegrityException.class, () -> departmentService.deleteEntity(subDepartmentId));
    }

    /**
     * Test for delete Department method to ensure it throws DataIntegrityException
     * when trying to delete a department involved in a circular dependency.
     */
    @Test
    @Transactional
    public void testDeleteDepartment_WithCircularDependency_ShouldThrowDataIntegrityException() {
        // Step 1: Create the department hierarchy
        Department rootDepartment = Department.builder()
                .name("Root Department")
                .leftIndex(1)
                .rightIndex(6)
                .level(0)
                .build();
        departmentRepository.save(rootDepartment);

        Department childDepartment = Department.builder()
                .name("Child Department")
                .parentId(rootDepartment.getId())
                .leftIndex(2)
                .rightIndex(5)
                .level(1)
                .build();
        departmentRepository.save(childDepartment);

        Department grandChildDepartment = Department.builder()
                .name("GrandChild Department")
                .parentId(childDepartment.getId())
                .leftIndex(3)
                .rightIndex(4)
                .level(2)
                .build();
        departmentRepository.save(grandChildDepartment);

        // Step 2: Attempt to create a circular dependency by updating grandchild to be the parent of root (Invalid scenario)
        rootDepartment.setParentId(grandChildDepartment.getId());
        departmentRepository.save(rootDepartment);

        // Step 3: Assert that attempting to delete the child department results in DataIntegrityException
        assertThatExceptionOfType(DataIntegrityException.class)
                .isThrownBy(() -> departmentService.deleteEntity(rootDepartment.getId()))
                .withMessageContaining("Cannot set an entity as its own descendant's parent.");
    }

    /**
     * Test case for deleting a leaf department (no sub-departments).
     * Verifies that the method deletes the department and reorders the nested set structure.
     */
    @Test
    @Transactional
    public void testDeleteDepartment_LeafNode() {
        // Given
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto software = createEntity("Software Department", engineering.getId());
        HierarchyResponseDto backend = createEntity("Backend Department", software.getId());

        // Log created departments
        log.info("Created departments: Engineering (ID: {}), Software (ID: {}), Backend (ID: {})",
                engineering.getId(), software.getId(), backend.getId());

        // When
        assertDoesNotThrow(() -> departmentService.deleteEntity(backend.getId()));

        // Then
        assertThrows(EntityNotFoundException.class, () -> departmentService.getEntityById(backend.getId()),
                "Backend department should not be found after deletion");
        // Verify other departments are still present
        assertDoesNotThrow(() -> departmentService.getEntityById(engineering.getId()));
        assertDoesNotThrow(() -> departmentService.getEntityById(software.getId()));
    }

    /**
     * Test case for updating a department's name only.
     * Verifies that the department's name is updated correctly.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_NameOnly() {
        // Given: Create an initial department
        String name = "Engineering Department";
        String updatedName = "Updated Engineering Department";
        HierarchyResponseDto engineering = createEntity(name, null);

        // When: Update the department's name

        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(updatedName)
                .parentEntityId(engineering.getParentEntityId())
                .build();

        HierarchyResponseDto updatedDepartment = departmentService.updateEntity(engineering.getId(), updatedDto);

        // Then: Verify the department's name is updated correctly
        assertNotNull(updatedDepartment);
        assertEquals(updatedName, updatedDepartment.getName());
        assertEquals(engineering.getParentEntityId(), updatedDepartment.getParentEntityId());
    }

    /**
     * Test case for updating a department's name only within a real subtree.
     * Verifies that the department's name is updated correctly.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_NameOnly_InSubtree() {
        // Given: Create departments to form a real subtree
        String name = "Backend Team";
        String updatedName = "Updated Backend Team";
        HierarchyResponseDto engineering = createEntity("Engineering", null);
        HierarchyResponseDto itDepartment = createEntity("IT Department", engineering.getId());
        HierarchyResponseDto backendTeam = createEntity(name, itDepartment.getId());

        // When: Update the department's name

        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(updatedName)
                .parentEntityId(backendTeam.getParentEntityId())
                .build();

        HierarchyResponseDto updatedDepartment = departmentService.updateEntity(backendTeam.getId(), updatedDto);

        // Then: Verify the department's name is updated correctly
        assertNotNull(updatedDepartment);
        assertEquals(updatedName, updatedDepartment.getName());
        assertEquals(backendTeam.getParentEntityId(), updatedDepartment.getParentEntityId());
    }

    /**
     * Test case for updating a department's parent.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_ChangeParent() {
        // Given: Create initial departments
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto software = createEntity("Software Department", engineering.getId());
        HierarchyResponseDto hr = createEntity("HR Department", null);

        // When: Change the parent of the Software Department
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(software.getName())
                .parentEntityId(hr.getId())
                .build();

        HierarchyResponseDto updatedDepartment = departmentService.updateEntity(software.getId(), updatedDto);

        // Then: Verify the department's parent is updated correctly
        assertEquals(software.getName(), updatedDepartment.getName());
        assertEquals(hr.getId(), updatedDepartment.getParentEntityId());

        // Fetch updated department from repository to verify indexes
        Department updatedDeptEntity = departmentRepository.findById(updatedDepartment.getId()).orElseThrow();
        Department hrDeptEntity = departmentRepository.findById(hr.getId()).orElseThrow();
        refreshEntities(List.of(updatedDeptEntity, hrDeptEntity));

        assertIndexes(updatedDeptEntity, hrDeptEntity.getLeftIndex() + 1);
    }

    /**
     * Test case for updating a department's parent within a real subtree.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_ChangeParent_InSubtree() {
        // Given: Create departments to form a real subtree
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto software = createEntity("Software Department", engineering.getId());
        HierarchyResponseDto backendTeam = createEntity("Backend Team", software.getId());
        HierarchyResponseDto frontendTeam = createEntity("Frontend Team", software.getId());

        HierarchyResponseDto hr = createEntity("HR Department", null);

        entityManager.flush();

        // Fetch updated department from repository to verify indexes
        Department engineeringDeptEntity = departmentRepository.findById(engineering.getId()).orElseThrow();
        Department hrDeptEntity = departmentRepository.findById(hr.getId()).orElseThrow();
        Department softwareDeptEntity = departmentRepository.findById(software.getId()).orElseThrow();
        Department backendTeamDeptEntity = departmentRepository.findById(backendTeam.getId()).orElseThrow();
        Department frontendTeamDeptEntity = departmentRepository.findById(frontendTeam.getId()).orElseThrow();

        refreshEntities(
                List.of(
                        engineeringDeptEntity,
                        hrDeptEntity,
                        softwareDeptEntity,
                        backendTeamDeptEntity,
                        frontendTeamDeptEntity
                )
        );

        // When: Change the parent of the Software Department
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(frontendTeam.getName())
                .parentEntityId(engineering.getId())
                .build();

        HierarchyResponseDto updatedDepartment = departmentService.updateEntity(frontendTeam.getId(), updatedDto);

        refreshEntities(
                List.of(
                        engineeringDeptEntity,
                        hrDeptEntity,
                        softwareDeptEntity,
                        backendTeamDeptEntity,
                        frontendTeamDeptEntity
                )
        );

        // Then: Verify the department's parent is updated correctly
        assertEquals(frontendTeam.getName(), updatedDepartment.getName());
        assertEquals(engineeringDeptEntity.getId(), updatedDepartment.getParentEntityId());

        // Verify indexes
        assertNestedSetIndexes();

        assertNestedSetIndexes(engineeringDeptEntity.getId());
        assertNestedSetIndexes(hrDeptEntity.getId());

        assertIndexes(softwareDeptEntity, engineeringDeptEntity.getLeftIndex() + 1);
        assertIndexes(backendTeamDeptEntity, softwareDeptEntity.getLeftIndex() + 1);
        assertIndexes(frontendTeamDeptEntity, softwareDeptEntity.getRightIndex() + 1);
        assertIndexes(hrDeptEntity, engineeringDeptEntity.getRightIndex() + 1);
        assertEquals(engineeringDeptEntity.getLevel() + 1, frontendTeamDeptEntity.getLevel());
    }

    /**
     * Test case for updating a department's parent within a real subtree (root to child of another root).
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_ChangeParent_InSubtree_RootToChildOfRoot() {
        // Given: Create departments to form a real subtree
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto software = createEntity("Software Department", engineering.getId());
        HierarchyResponseDto backendTeam = createEntity("Backend Team", software.getId());
        HierarchyResponseDto frontendTeam = createEntity("Frontend Team", software.getId());

        HierarchyResponseDto hr = createEntity("HR Department", null);

        entityManager.flush();

        // Fetch updated department from repository to verify indexes
        Department hrDeptEntity = departmentRepository.findById(hr.getId()).orElseThrow();
        Department engineeringDeptEntity = departmentRepository.findById(engineering.getId()).orElseThrow();
        Department softwareDeptEntity = departmentRepository.findById(software.getId()).orElseThrow();
        Department backendTeamDeptEntity = departmentRepository.findById(backendTeam.getId()).orElseThrow();
        Department frontendTeamDeptEntity = departmentRepository.findById(frontendTeam.getId()).orElseThrow();

        refreshEntities(
                List.of(
                        hrDeptEntity,
                        engineeringDeptEntity,
                        softwareDeptEntity,
                        backendTeamDeptEntity,
                        frontendTeamDeptEntity
                )
        );

        // When: Change the parent of the Software Department
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(engineering.getName())
                .parentEntityId(hr.getId())
                .build();

        HierarchyResponseDto updatedDepartment = departmentService.updateEntity(engineering.getId(), updatedDto);

        refreshEntities(
                List.of(
                        engineeringDeptEntity,
                        hrDeptEntity,
                        softwareDeptEntity,
                        backendTeamDeptEntity,
                        frontendTeamDeptEntity
                )
        );

        // Then:

        // Verify the name
        assertEquals(engineering.getName(), updatedDepartment.getName());

        // Verify the parent ID
        assertEquals(hr.getId(), updatedDepartment.getParentEntityId());

        // Verify the indexes
        assertNestedSetIndexes();
        assertNestedSetIndexes(hrDeptEntity.getId());

        assertIndexes(engineeringDeptEntity, hrDeptEntity.getLeftIndex() + 1);
        assertIndexes(softwareDeptEntity, engineeringDeptEntity.getLeftIndex() + 1);
        assertIndexes(frontendTeamDeptEntity, softwareDeptEntity.getLeftIndex() + 1);
        assertIndexes(backendTeamDeptEntity, frontendTeamDeptEntity.getRightIndex() + 1);

        // Verify the root ID
        assertEquals(hrDeptEntity.getId(), engineeringDeptEntity.getRootId());

        // Verify the level
        assertEquals(hrDeptEntity.getLevel() + 1, engineeringDeptEntity.getLevel());
    }

    /**
     * Test case for updating a department's parent.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_ParentDepartment_SubDepartments() {
        // Given: Create initial departments
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto hr = createEntity("HR Department", null);

        entityManager.flush();

        // Fetch updated department from repository to verify indexes and levels
        Department engineeringDeptEntity = departmentRepository.findById(engineering.getId()).orElseThrow();
        Department hrDeptEntity = departmentRepository.findById(hr.getId()).orElseThrow();

        refreshEntities(
                List.of(
                        hrDeptEntity,
                        engineeringDeptEntity
                )
        );

        // When: Update the parent of the Engineering Department to HR Department
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(engineering.getName())
                .parentEntityId(hr.getId())
                .build();

        HierarchyResponseDto updatedDepartment = departmentService.updateEntity(engineering.getId(), updatedDto);

        // Fetch updated department from repository to verify indexes
        refreshEntities(
                List.of(
                        hrDeptEntity,
                        engineeringDeptEntity
                )
        );

        assertIndexes(engineeringDeptEntity, hrDeptEntity.getLeftIndex() + 1);

        // Then: Verify the department's parent is updated correctly
        assertEquals(hr.getId(), engineeringDeptEntity.getParentId());

        // Verify sub-departments are populated correctly
        HierarchyResponseDto hrWithSubDepartments = departmentService.getEntityById(hr.getId());
        List<HierarchyResponseDto> subDepartments = (List<HierarchyResponseDto>) hrWithSubDepartments.getSubEntities();
        assertNotNull(subDepartments);
        assertEquals(1, subDepartments.size());
        assertEquals("Engineering Department", subDepartments.get(0).getName());
    }


    /**
     * Test case for updating a department with an invalid name.
     * Verifies that a ValidationException is thrown.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_ValidationException() {
        // Given: Create an initial department
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);

        // When: Attempt to update the department with an invalid name
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name("") // Invalid name
                .parentEntityId(engineering.getParentEntityId())
                .build();

        // Then: Verify that a ValidationException is thrown
        assertThrows(ValidationException.class, () -> departmentService.updateEntity(engineering.getId(), updatedDto));
    }

    /**
     * Test case for updating a department with a non-existent parent.
     * Verifies that a ParentEntityNotFoundException is thrown.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_ParentNotFoundException() {
        // Given: Create an initial department
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);

        // When: Attempt to update the department with a non-existent parent
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name("Updated Engineering Department")
                .parentEntityId(-1L) // Non-existent parent
                .build();

        // Then: Verify that a ParentEntityNotFoundException is thrown
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.updateEntity(engineering.getId(), updatedDto));
    }

    /**
     * Test case for updating a department that results in a self-circular reference.
     * Verifies that a DataIntegrityException is thrown.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_SelfCircularReferences() {
        // Given: Create initial departments
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto software = createEntity("Software Department", engineering.getId());

        // When: Attempt to update the department with a circular reference
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(software.getName())
                .parentEntityId(software.getId()) // Circular reference
                .build();

        // Then: Verify that a DataIntegrityException is thrown
        assertThrows(DataIntegrityException.class, () -> departmentService.updateEntity(software.getId(), updatedDto));
    }

    /**
     * Test case for updating a department that results in a circular reference.
     * Verifies that a DataIntegrityException is thrown.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_CircularReferences() {
        // Given: Create initial departments
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto software = createEntity("Software Department", engineering.getId());

        // When: Attempt to update the department with a circular reference
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(engineering.getName())
                .parentEntityId(software.getId()) // Circular reference
                .build();

        // Then: Verify that a DataIntegrityException is thrown
        assertThrows(DataIntegrityException.class, () -> departmentService.updateEntity(engineering.getId(), updatedDto));
    }

    /**
     * Test for updateDepartment method to ensure it throws DataIntegrityException
     * when trying to update a department to be its own descendant's parent, resulting in a circular dependency.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_WithCircularDependency_ShouldThrowDataIntegrityException() {
        // Step 1: Create the department hierarchy
        Department rootDepartment = Department.builder()
                .name("Root Department")
                .leftIndex(1)
                .rightIndex(6)
                .level(0)
                .build();
        departmentRepository.save(rootDepartment);

        Department childDepartment = Department.builder()
                .name("Child Department")
                .parentId(rootDepartment.getId())
                .leftIndex(2)
                .rightIndex(5)
                .level(1)
                .build();
        departmentRepository.save(childDepartment);

        Department grandChildDepartment = Department.builder()
                .name("GrandChild Department")
                .parentId(childDepartment.getId())
                .leftIndex(3)
                .rightIndex(4)
                .level(2)
                .build();
        departmentRepository.save(grandChildDepartment);

        // Step 2: Attempt to create a circular dependency by updating the root to be a child of its grandchild
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(rootDepartment.getName())
                .parentEntityId(grandChildDepartment.getId()) // Circular reference
                .build();

        // Step 3: Assert that attempting to update the root department results in DataIntegrityException
        assertThatExceptionOfType(DataIntegrityException.class)
                .isThrownBy(() -> departmentService.updateEntity(rootDepartment.getId(), updatedDto))
                .withMessageContaining("Cannot set an entity as its own descendant's parent.");

        assertNestedSetIndexes();

        assertNestedSetIndexes(rootDepartment.getId());
    }

    /**
     * Test case for updating a child department to be a root.
     * Verifies that the department's parent and indexes are adjusted correctly.
     */
    @Test
    @Transactional
    public void testUpdateDepartment_ChangeChildToRoot() {
        // Given: Create initial departments
        HierarchyResponseDto engineering = createEntity("Engineering Department", null);
        HierarchyResponseDto software = createEntity("Software Department", engineering.getId());
        HierarchyResponseDto backendTeam = createEntity("Backend Team", software.getId());
        HierarchyResponseDto frontendTeam = createEntity("Frontend Team", software.getId());
        HierarchyResponseDto hr = createEntity("HR Department", null);

        entityManager.flush();

        Department engineeringDepartment = departmentRepository.findById(engineering.getId()).orElseThrow();
        Department softwareDepartment = departmentRepository.findById(software.getId()).orElseThrow();
        Department hrDepartment = departmentRepository.findById(hr.getId()).orElseThrow();
        Department backendTeamDepartment = departmentRepository.findById(backendTeam.getId()).orElseThrow();
        Department frontendTeamDepartment = departmentRepository.findById(frontendTeam.getId()).orElseThrow();

        refreshEntities(
                List.of(
                        engineeringDepartment,
                        softwareDepartment,
                        hrDepartment,
                        backendTeamDepartment,
                        frontendTeamDepartment
                )
        );

        // When: Change the parent of the Software Department to root
        HierarchyRequestDto updatedDto = HierarchyRequestDto.builder()
                .name(software.getName())
                .parentEntityId(null)
                .build();

        HierarchyResponseDto updatedDepartment = departmentService.updateEntity(software.getId(), updatedDto);

        // Fetch updated department from repository to verify indexes
        Department updatedDeptEntity = departmentRepository.findById(updatedDepartment.getId()).orElseThrow();

        refreshEntities(
                List.of(
                        engineeringDepartment,
                        hrDepartment,
                        backendTeamDepartment,
                        frontendTeamDepartment,

                        updatedDeptEntity
                )
        );

        // Then: Verify the department's parent is updated to root correctly
        assertEquals(software.getName(), updatedDepartment.getName());
        assertNull(updatedDepartment.getParentEntityId());

        assertEquals(0, updatedDeptEntity.getLevel());
        assertIndexesForRoot(updatedDeptEntity);
        assertNestedSetIndexes();
    }

    /**
     * Verifies that the indexes for a root department are correct.
     *
     * @param department the department to verify
     */
    private void assertIndexesForRoot(Department department) {
        // Root department should have leftIndex = 1 and rightIndex = size of the subtree
        assertEquals(1, department.getLeftIndex());
        assertEquals(department.getRightIndex() - department.getLeftIndex() + 1, department.getRightIndex());
    }


    /**
     * Helper method to assert the nested set indexes of a department.
     *
     * @param department        The department to check.
     * @param expectedLeftIndex The expected left index.
     */
    private void assertIndexes(Department department, int expectedLeftIndex) {
        assertEquals(expectedLeftIndex, department.getLeftIndex());
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

    /**
     * Helper method to assert expected indexes after creating or updating a department.
     *
     * @param departmentDto     the department DTO after create or update
     * @param expectedLeftIndex the expected left index after create or update
     */
    @Transactional(readOnly = true)
    protected void assertIndexes(HierarchyResponseDto departmentDto, int expectedLeftIndex) {
        // Fetch the updated department entity from the database
        Department updatedDepartment = departmentRepository.findById(departmentDto.getId())
                .orElseThrow(() -> new RuntimeException("Department not found: " + departmentDto.getId()));
        // Check if the department is the root of a subtree
        if (isRootOfSubtreeDepartments(updatedDepartment)) {
            int rootLeftIndex = updatedDepartment.getLeftIndex();
            int rootRightIndex = updatedDepartment.getRightIndex();

            // Query for max right index within the subtree of the root department excluding the root department
            Integer maxRightIndex = jpaQueryFactory.select(QDepartment.department.rightIndex.max())
                    .from(QDepartment.department)
                    .where(QDepartment.department.parentId.eq(departmentDto.getId()))
                    .fetchOne();

            // Calculate expected right index
            int expectedRightIndex = (maxRightIndex != null) ? maxRightIndex + 1 : rootLeftIndex + 1;

            // Assert left and right indexes
            assertEquals(rootLeftIndex, updatedDepartment.getLeftIndex());
            assertEquals(expectedRightIndex, rootRightIndex);
        } else {
            // Department is part of a subtree
            assertEquals(expectedLeftIndex, updatedDepartment.getLeftIndex());
            assertEquals(expectedLeftIndex + 1, updatedDepartment.getRightIndex());
        }
    }

    /**
     * Checks if a department is the root of a subtree.
     *
     * @param department the department entity to check
     * @return true if the department is the root of a subtree, false otherwise
     */
    @Transactional(readOnly = true)
    protected boolean isRootOfSubtreeDepartments(Department department) {
        return department.getRightIndex() - department.getLeftIndex() != 1;
    }

    /**
     * Checks if a department is the has a sub-departments.
     *
     * @param department the department entity to check
     * @return true if the department is a parent of other departments, false otherwise
     */
    @Transactional(readOnly = true)
    protected boolean hasSubDepartments(Department department) {
        BooleanBuilder isRootPredicate = new BooleanBuilder();
        isRootPredicate.and(QDepartment.department.parentId.isNotNull());
        isRootPredicate.and(QDepartment.department.parentId.eq(department.getId()));

        return jpaQueryFactory.selectFrom(QDepartment.department)
                .where(isRootPredicate)
                .fetchCount() != 0;
    }

    /**
     * Helper method to assert nested set indexes within a specified range.
     *
     * @param leftIndex  left index of the range to check
     * @param rightIndex right index of the range to check
     */
    @Transactional(readOnly = true)
    protected void assertNestedSetIndexes(int leftIndex, int rightIndex) {
        List<Department> departments = jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.leftIndex.goe(leftIndex)
                        .and(QDepartment.department.rightIndex.loe(rightIndex)))
                .fetch();

        Set<Integer> expectedLeftIndexes = new HashSet<>();
        Set<Integer> expectedRightIndexes = new HashSet<>();

        int expectedIndex = leftIndex;
        for (Department department : departments) {
            expectedLeftIndexes.add(expectedIndex);
            expectedRightIndexes.add(expectedIndex + (department.getRightIndex() - department.getLeftIndex()) - 1);

            expectedIndex += 2; // Move to the next node's left index in nested set structure
        }

        assertEquals(departments.size(), expectedLeftIndexes.size());
        assertEquals(departments.size(), expectedRightIndexes.size());

        // Assert that all retrieved departments have correct nested set indexes
        for (Department department : departments) {
            assertTrue(expectedLeftIndexes.contains(department.getLeftIndex()));
            assertTrue(expectedRightIndexes.contains(department.getRightIndex()));
        }
    }

    /**
     * Helper method to assert nested set indexes for the entire department tree.
     */
    @Transactional(readOnly = true)
    protected void assertNestedSetIndexes() {
        List<Department> departments = jpaQueryFactory.selectFrom(QDepartment.department)
                .orderBy(QDepartment.department.leftIndex.asc())
                .fetch();

        // Check if the department list is null or empty
        if (departments == null || departments.isEmpty()) {
            // An empty department list is correctly indexed by definition
            return;
        }

        // Verify the correctness of nested set indexes
        for (Department department : departments) {
            // Check that leftIndex is less than rightIndex
            assertTrue(department.getLeftIndex() < department.getRightIndex());

            // Ensure the difference between leftIndex and rightIndex is even
            assertTrue((department.getRightIndex() - department.getLeftIndex()) % 2 == 1);

            // Check that the department is within its parent's bounds if it has a parent
            if (department.getParentId() != null) {
                Department parentDepartment = getEntityById(department.getParentId());
                assertNotNull(parentDepartment);
                assertTrue(department.getLeftIndex() > parentDepartment.getLeftIndex());
                assertTrue(department.getRightIndex() < parentDepartment.getRightIndex());
            }

            // Calculate the size (number of nodes) of the subtree
            int subtreeSize = (department.getRightIndex() - department.getLeftIndex() - 1) / 2;
            assertTrue(subtreeSize >= 0);
        }

//        // Additional checks for overall consistency in the hierarchy
//        for (int i = 0; i < departments.size() - 1; i++) {
//            assertTrue(departments.get(i).getRightIndex() < departments.get(i + 1).getLeftIndex());
//        }
        // Filter the list to identify siblings
        Map<Long, List<Department>> siblingsMap = departments.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(Department::getParentId));

        // Check the consistency among siblings
        for (List<Department> siblings : siblingsMap.values()) {
            for (int i = 0; i < siblings.size() - 1; i++) {
                assertTrue(siblings.get(i).getRightIndex() < siblings.get(i + 1).getLeftIndex());
            }
        }
    }

    /**
     * Retrieves a department by its ID.
     *
     * @param id the ID of the department
     * @return the department entity
     */
    @Transactional(readOnly = true)
    protected Department getEntityById(Long id) {
        return jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.id.eq(id))
                .fetchOne();
    }

    private void assertNestedSetIndexes(Long departmentId) {
        Department department = departmentRepository.findById(departmentId).orElse(null);
        assertNotNull(department, "Department should not be null");

        // Fetch all departments within the subtree of the given department
        List<Department> subtreeDepartments = jpaQueryFactory.selectFrom(QDepartment.department)
                .where(QDepartment.department.leftIndex.goe(department.getLeftIndex())
                        .and(QDepartment.department.rightIndex.loe(department.getRightIndex())))
                .orderBy(QDepartment.department.leftIndex.asc())
                .fetch();

        // Verify that the subtree is not empty
        assertFalse(subtreeDepartments.isEmpty(), "Subtree departments should not be empty");

        // Verify the correctness of nested set indexes for the subtree
        for (Department dep : subtreeDepartments) {
            // Check that leftIndex is less than rightIndex
            assertTrue(dep.getLeftIndex() < dep.getRightIndex(),
                    String.format("Department %s has leftIndex >= rightIndex", dep.getName()));

            // Ensure the difference between leftIndex and rightIndex is odd
            assertTrue((dep.getRightIndex() - dep.getLeftIndex()) % 2 == 1,
                    String.format("Department %s has an invalid index difference", dep.getName()));

            // Check that the department is within its parent's bounds if it has a parent
            if (dep.getParentId() != null) {
                Department parentDepartment = subtreeDepartments.stream()
                        .filter(d -> d.getId().equals(dep.getParentId()))
                        .findFirst()
                        .orElse(null);

                assertNotNull(parentDepartment,
                        String.format("Parent department of %s should not be null", dep.getName()));
                assertTrue(dep.getLeftIndex() > parentDepartment.getLeftIndex(),
                        String.format("Department %s has leftIndex <= parent's leftIndex", dep.getName()));
                assertTrue(dep.getRightIndex() < parentDepartment.getRightIndex(),
                        String.format("Department %s has rightIndex >= parent's rightIndex", dep.getName()));
            }

            // Calculate the size (number of nodes) of the subtree
            int subtreeSize = (dep.getRightIndex() - dep.getLeftIndex() - 1) / 2;
            assertTrue(subtreeSize >= 0,
                    String.format("Department %s has an invalid subtree size", dep.getName()));
        }

        // Additional checks for overall consistency in the hierarchy

        // Filter the list to identify siblings
        Map<Long, List<Department>> siblingsMap = subtreeDepartments.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(Department::getParentId));

        // Check the consistency among siblings
        for (List<Department> siblings : siblingsMap.values()) {
            for (int i = 0; i < siblings.size() - 1; i++) {
                assertTrue(siblings.get(i).getRightIndex() < siblings.get(i + 1).getLeftIndex(),
                        String.format("Department %s's rightIndex is not less than the next department's leftIndex",
                                siblings.get(i).getName())
                );
            }
        }
    }

    /**
     * Log all repository's departments indexes.
     */
    @Transactional(readOnly = true)
    protected void logDepartmentIndexes() {
        List<Department> allDepartments = departmentRepository.findAllByOrderByLeftIndexAsc();
        for (Department department : allDepartments) {
            log.info("Department {}: leftIndex={}, rightIndex={}\n", department.getName(), department.getLeftIndex(), department.getRightIndex());
        }
    }

    /**
     * Update departments entities for verification
     *
     * @param departments a fetched departments list from the repository
     */
    @Transactional
    protected void refreshEntities(List<Department> departments) {
        departments.forEach(entityManager::refresh);
    }
}
