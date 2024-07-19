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
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

import java.util.ArrayList;
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

    @Value("${entity.max-name-length}")
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
     * @param id        the unique identifier of the entity
     * @param name      the name of the entity
     * @param path      the path representing the hierarchical position of the entity
     * @param parentId  the unique identifier of the parent entity, or null if this is a root entity
     * @return the created Department entity
     *
     * This method constructs a new Department entity using the provided parameters and
     * saves it to the database. The method ensures that the entity is properly built
     * using the Department builder and then persisted using the departmentRepository.
     * It is a utility method intended for use in test cases to set up initial test data.
     */
    private Department createEntity(Long id, String name, String path, Long parentId) {
        Department entity = Department.builder()
                .id(id)
                .name(name)
                .path(path)
                .parentId(parentId)
                .build();
        return departmentRepository.save(entity);
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
     * Integration test for successfully creating a child entity.
     */
    @Test
    public void testCreateChildDepartment_Success() {
        // Given: A root entity
        Department rootDepartment = createEntity(1L, "Root", "/1/", null);

        // When: Creating a child entity under the root
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Child");
        requestDto.setParentEntityId(rootDepartment.getId());
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

        // Then: Verify the child entity is created successfully
        assertNotNull(responseDto.getId());
        assertEquals("Child", responseDto.getName());
        Department childDepartment = departmentRepository.findById(responseDto.getId()).orElse(null);
        assertNotNull(childDepartment);
        assertEquals("/1/" + childDepartment.getId() + "/", childDepartment.getPath());
    }

    /**
     * Integration test for creating a entity with an invalid name.
     */
    @Test
    public void testCreateDepartment_InvalidName() {
        // Given: An invalid entity name (null)
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName(null);

        // When: Attempting to create the entity
        // Then: A ValidationException should be thrown
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Integration test for creating a entity with a name that is too long.
     */
    @Test
    public void testCreateDepartment_NameTooLong() {
        // Given: A entity name exceeding the maximum length
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("A".repeat(maxNameLength + 1));

        // When: Attempting to create the entity
        // Then: A ValidationException should be thrown
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Integration test for creating a entity with a parent that does not exist.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given: A non-existent parent entity ID
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("New Department");
        requestDto.setParentEntityId(-1L); // Assuming -1L does not exist

        // When: Attempting to create the entity
        // Then: A ParentEntityNotFoundException should be thrown
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Integration test for successfully creating a root entity.
     */
    @Test
    public void testCreateRootDepartment_Success() {
        // Given: A request to create a root entity
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Root Department");

        // When: Creating the root entity
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

        // Then: Verify the root entity is created successfully
        assertNotNull(responseDto.getId());
        assertEquals("Root Department", responseDto.getName());
        Department rootDepartment = departmentRepository.findById(responseDto.getId()).orElse(null);
        assertNotNull(rootDepartment);
        assertEquals("/" + rootDepartment.getId() + "/", rootDepartment.getPath());
    }

    /**
     * Integration test for updating a entity's name only.
     */
    @Test
    public void testUpdateDepartment_NameOnly() {
        // Given: An existing entity with a specified ID and name "Engineering".
        Department existingDepartment = createEntity(1L, "Engineering", "/1/", null);

        // When: Updating the entity name to "IT".
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("IT");
        HierarchyResponseDto updatedResponse = departmentService.updateEntity(existingDepartment.getId(), updatedDto);

        // Then: Verify that the entity's name is updated correctly to "IT".
        assertEquals("IT", updatedResponse.getName());
        Department updatedDepartment = departmentRepository.findById(existingDepartment.getId()).orElse(null);
        assertNotNull(updatedDepartment);
        assertEquals("IT", updatedDepartment.getName());
    }

    /**
     * Integration test for updating a entity's name in a subtree.
     */
    @Test
    public void testUpdateDepartment_NameOnly_InSubtree() {
        // Given: A root entity and a child entity
        Department rootDepartment = createEntity(1L, "Root", "/1/", null);
        Department childDepartment = createEntity(2L, "Child", "/1/2/", rootDepartment.getId());

        // When: Updating the child entity name
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Updated Child");
        updatedDto.setParentEntityId(rootDepartment.getId());

        HierarchyResponseDto updatedResponse = departmentService.updateEntity(childDepartment.getId(), updatedDto);

        // Then: Verify that the child entity's name is updated correctly
        assertEquals("Updated Child", updatedResponse.getName());
        Department updatedChild = departmentRepository.findById(childDepartment.getId()).orElse(null);
        assertNotNull(updatedChild);
        assertEquals("Updated Child", updatedChild.getName());
    }

    /**
     * Integration test for updating a child entity to be a root entity.
     */
    @Test
    public void testUpdateDepartment_ChildToRoot() {
        // Given: A root entity and a child entity
        Department rootDepartment = createEntity(1L, "Root", "/1/", null);
        Department childDepartment = createEntity(2L, "Child", "/1/2/", rootDepartment.getId());

        // When: Updating the child entity's to be a root entity
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Updated Child");
        updatedDto.setParentEntityId(null);
        HierarchyResponseDto updatedResponse = departmentService.updateEntity(childDepartment.getId(), updatedDto);

        // Then: Verify that the child entity is updated correctly to be a root entity
        assertEquals("Updated Child", updatedResponse.getName());
        Department updatedChild = departmentRepository.findById(childDepartment.getId()).orElse(null);
        assertNotNull(updatedChild);
        assertEquals("Updated Child", updatedChild.getName());
        assertEquals("/" + childDepartment.getId() + "/", updatedChild.getPath());
    }

    /**
     * Integration test for updating a entity's parent.
     */
    @Test
    public void testUpdateDepartment_ChangeParent() {
        // Given: A root entity and two child departments
        Department rootDepartment = createEntity(1L, "Root", "/1/", null);
        Department child1 = createEntity(2L, "Child1", "/1/2/", rootDepartment.getId());
        Department child2 = createEntity(3L, "Child2", "/1/3/", rootDepartment.getId());

        // When: Changing the parent of Child2 to Child1
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Child2");
        updatedDto.setParentEntityId(child1.getId());

        HierarchyResponseDto updatedResponse = departmentService.updateEntity(child2.getId(), updatedDto);

        // Then: Verify that the parent of Child2 is changed to Child1
        assertEquals(child1.getId(), updatedResponse.getParentEntityId());
        Department updatedChild2 = departmentRepository.findById(child2.getId()).orElse(null);
        assertNotNull(updatedChild2);
        assertEquals(child1.getId(), updatedChild2.getParentId());
        assertEquals(child1.getPath() + child2.getId() + "/", updatedChild2.getPath());
    }

    /**
     * Integration test for updating a entity's parent in a subtree.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree() {
        // Given: A root entity and two child departments
        Department rootDepartment = createEntity(1L, "Root", "/1/", null);
        Department child1 = createEntity(2L, "Child1", "/1/2/", rootDepartment.getId());
        Department child2 = createEntity(3L, "Child2", "/1/3/", rootDepartment.getId());
        Department grandchild = createEntity(4L, "Grandchild", "/1/3/4/", child2.getId());

        // When: Changing the parent of Grandchild to Child1
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Grandchild");
        updatedDto.setParentEntityId(child1.getId());
        HierarchyResponseDto updatedResponse = departmentService.updateEntity(grandchild.getId(), updatedDto);

        // Then: Verify that the parent of Grandchild is changed to Child1
        assertEquals(child1.getId(), updatedResponse.getParentEntityId());
        Department updatedGrandchild = departmentRepository.findById(grandchild.getId()).orElse(null);
        assertNotNull(updatedGrandchild);
        assertEquals(child1.getId(), updatedGrandchild.getParentId());
        assertEquals(child1.getPath() + grandchild.getId() + "/", updatedGrandchild.getPath());
    }

    /**
     * Integration test for circular reference detection during entity update.
     */
    @Test
    public void testUpdateDepartment_CircularReferences() {
        // Given: A root entity and a child entity
        Department rootDepartment = createEntity(1L, "Root", "/1/", null);
        Department child = createEntity(2L, "Child", "/1/2/", rootDepartment.getId());

        // When: Attempting to make the root entity a child of its own child
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Root");
        updatedDto.setParentEntityId(child.getId());

        // Then: A DataIntegrityException should be thrown
        assertThrows(DataIntegrityException.class, () -> departmentService.updateEntity(rootDepartment.getId(), updatedDto));
    }

    /**
     * Integration test for data integrity violation during entity update.
     */
    @Test
    public void testUpdateDepartment_DataIntegrityException() {
        // Given: Two departments with the same parent
        Department parent = createEntity(1L, "Parent", "/1/", null);
        Department child1 = createEntity(2L, "Child1", "/1/2/", parent.getId());
        Department child2 = createEntity(3L, "Child2", "/1/3/", parent.getId());

        // When: Attempting to update Child1 to have the same path as Child2
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Parent");
        updatedDto.setParentEntityId(child1.getId());

        // Then: A DataIntegrityException should be thrown
        assertThrows(DataIntegrityException.class, () -> departmentService.updateEntity(parent.getId(), updatedDto));
    }

    /**
     * Integration test for updating a entity with a non-existent parent.
     */
    @Test
    public void testUpdateDepartment_ParentDepartmentNotFoundException() {
        // Given: An existing entity
        Department existingDepartment = createEntity(1L, "Existing", "/1/", null);

        // When: Attempting to set a non-existent parent entity
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Existing");
        updatedDto.setParentEntityId(-1L); // Assuming -1L does not exist

        // Then: A ParentEntityNotFoundException should be thrown
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.updateEntity(existingDepartment.getId(), updatedDto));
    }

    /**
     * Integration test for updating a entity with a validation exception.
     */
    @Test
    public void testUpdateDepartment_ValidationException() {
        // Given: An existing entity
        Department existingDepartment = createEntity(1L, "Existing", "/1/", null);

        // When: Attempting to update the entity with an invalid name
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName(""); // Invalid name

        // Then: A ValidationException should be thrown
        assertThrows(ValidationException.class, () -> departmentService.updateEntity(existingDepartment.getId(), updatedDto));
    }

    /**
     * Integration test for updating a entity with its sub-departments.
     */
    @Test
    public void testUpdateDepartment_ParentDepartment_SubDepartments() {
        // Given: A parent entity and its child departments
        Department parent = createEntity(1L, "Parent", "/1/", null);
        Department child1 = createEntity(2L, "Child1", "/1/2/", parent.getId());
        Department child2 = createEntity(3L, "Child2", "/1/3/", parent.getId());

        // When: Updating the parent entity's name
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Updated Parent");
        HierarchyResponseDto updatedResponse = departmentService.updateEntity(parent.getId(), updatedDto);

        // Then: Verify that the parent entity and its sub-departments are updated correctly
        assertEquals("Updated Parent", updatedResponse.getName());
        Department updatedParent = departmentRepository.findById(parent.getId()).orElse(null);
        assertNotNull(updatedParent);
        assertEquals("Updated Parent", updatedParent.getName());

        List<HierarchyResponseDto> subDepartments = departmentService.getSubEntities(updatedResponse.getId());
        assertEquals(2, subDepartments.size());
    }

    /**
     * Integration test for deleting a entity with an existent id and with descendants.
     */
    @Test
    public void testDeleteDepartment_ExistingId_WithDescendants() {
        // Given: An existing entity with descendants in the database
        Long departmentId = 1L;
        String departmentPath = "/1/";

        Department entity = createEntity(departmentId, "Software", departmentPath, null);
        Department child1 = createEntity(2L, "Backend", "/1/2/", departmentId);
        Department child2 = createEntity(3L, "Frontend", "/1/3/", departmentId);

        // When: Deleting the entity
        departmentService.deleteEntity(entity.getId());

        // Then: Verify the entity and its descendants are deleted from the database
        assertFalse(departmentRepository.findById(departmentId).isPresent());
        assertFalse(departmentRepository.findById(child1.getId()).isPresent());
        assertFalse(departmentRepository.findById(child2.getId()).isPresent());
    }

    /**
     * Integration test for deleting a entity with a non-existent id.
     */
    @Test
    public void testDeleteDepartment_NonExistingId_IT() {
        // Given: A non-existing entity ID
        Long nonExistingId = -1L;

        // When: Deleting the non-existing entity
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.deleteEntity(nonExistingId);
        });

        // Then: Verify the exception message
        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());
    }


    /**
     * Integration test for circular reference detection during entity delete.
     */
    @Test
    public void testDeleteDepartment_CircularReference_IT() {
        // Given: A entity with a circular reference in its path
        Long parentId = 1L;
        Long childId = 2L;
        Long grandChildId = 3L;

        Department parent = createEntity(parentId, "Parent", "/1/", null);
        Department child = createEntity(childId, "Child", "/1/2/", parentId);
        Department grandChild = createEntity(grandChildId, "Grand Child", "/1/2/", childId);
        Department circularDescendant = createEntity(4L, "Circular", "/1/2/3/4/1/", grandChildId);

        // When: Deleting the parent entity
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteEntity(parent.getId());
        });

        // Then: Verify the exception message
        assertEquals("Circular reference detected in entity path: " + circularDescendant.getPath(), exception.getMessage());
    }

    /**
     * Integration test for retrieving all departments when there are no departments.
     */
    @Test
    public void testGetAllDepartments_noDepartments() {
        // Given: No departments in the repository

        // When: Retrieving all departments
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then: Verify that the result is an empty list
        assertTrue(result.isEmpty());
    }

    /**
     * Integration test for retrieving all departments when there are existing departments.
     */
    @Test
    public void testGetAllDepartments_withDepartments() {
        // Given: Departments in the repository
        Department parent = createEntity(1L, "Parent", "/1/", null);
        Department child = createEntity(2L, "Child", "/1/2/", parent.getId());

        // When: Retrieving all departments
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals("Parent", result.get(0).getName());
        assertEquals("Child", result.get(1).getName());
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
     * Integration test for retrieving an existing entity by ID.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() {
        // Given: An existing entity
        Department parent = createEntity(1L, "Parent", "/1/", null);

        // When: Retrieving the entity by ID
        HierarchyResponseDto result = departmentService.getEntityById(parent.getId());

        // Then: Verify the result
        assertEquals(parent.getId(), result.getId());
        assertEquals("Parent", result.getName());
    }

    /**
     * Integration test for retrieving a non-existent entity by ID.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given: A non-existing entity ID
        Long departmentId = -1L;
        // When: Retrieving the entity by ID
        EntityNotFoundException thrown = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getEntityById(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity not found with ID: " + departmentId, thrown.getMessage());
    }

    /**
     * Integration test for searching departments by an existing name.
     */
    @Test
    public void testSearchDepartmentsByName_ExistingName() {
        // Given: Departments with matching name
        Department parent = createEntity(1L, "Parent", "/1/", null);
        Department child = createEntity(2L, "Child", "/1/2/", parent.getId());

        // When: Searching departments by name
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName("Parent");

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
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(departmentName);

        // Then: Verify the result is an empty list
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

    /**
     * Integration test for {@link DepartmentService#getSubEntities(Long)} when parent entity exists.
     * Verifies that sub-departments are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetSubDepartments_existingParent() {
        // Given: Existing parent entity
        Long parentId = 1L;
        Department parent = createEntity(parentId, "Parent", "/1/", null);
        Department child1 = createEntity(2L, "Child1", "/1/2/", parent.getId());
        Department child2 = createEntity(3L, "Child2", "/1/3/", parent.getId());

        // When: Getting sub-departments
        List<HierarchyResponseDto> result = departmentService.getSubEntities(parent.getId());

        // Then: Verify the result
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> "Child1".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "Child2".equals(dto.getName())));
    }

    /**
     * Integration test for {@link DepartmentService#getSubEntities(Long)} when parent entity does not exist.
     * Verifies that {@link ParentEntityNotFoundException} is thrown.
     */
    @Test
    public void testGetSubDepartments_nonExistingParent() {
        // Given: Non-existing parent entity ID
        Long parentId = -1L;

        // When: Getting sub-departments
        ParentEntityNotFoundException thrown = assertThrows(
                ParentEntityNotFoundException.class,
                () -> departmentService.getSubEntities(parentId)
        );

        // Then: Verify the exception
        assertEquals("Parent entity not found with id: " + parentId, thrown.getMessage());
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
     * Integration test for {@link DepartmentService#getDescendants(Long)} when entity exists.
     * Verifies that descendants are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetDescendants_existingDepartment() {
        // Given: Existing entity
        Department parent = createEntity(1L, "Parent", "/1/", null);
        Department entity = createEntity(2L, "Entity", "/1/2/", parent.getId());
        Department child1 = createEntity(3L, "Child1", "/1/2/3/", entity.getId());
        Department child2 = createEntity(4L, "Child2", "/1/2/4/", entity.getId());

        String parentPath = "/" + parent.getId() + "/";
        parent.setPath(parentPath);
        departmentRepository.save(parent);

        String departmentPath = parent.getPath() + entity.getId() + "/";
        entity.setPath(departmentPath);
        departmentRepository.save(entity);

        String child1Path = entity.getPath() + child1.getId() + "/";
        child1.setPath(child1Path);
        departmentRepository.save(child1);

        String child2Path = entity.getPath() + child2.getId() + "/";
        child2.setPath(child2Path);
        departmentRepository.save(child2);

        // When: Getting descendants
        List<HierarchyResponseDto> result = departmentService.getDescendants(parent.getId());

        // Then: Verify the result
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(dto -> "Entity".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "Child1".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "Child2".equals(dto.getName())));
    }

    /**
     * Integration test for {@link DepartmentService#getDescendants(Long)} when entity does not exist.
     * Verifies that {@link EntityNotFoundException} is thrown.
     */
    @Test
    public void testGetDescendants_nonExistingDepartment() {
        // Given: Non-existing entity ID
        Long departmentId = -1L;

        // When: Getting descendants
        EntityNotFoundException thrown = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getDescendants(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity not found with ID: " + departmentId, thrown.getMessage());
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
     * Integration test for {@link DepartmentService#getAncestors(Long)} when entity exists.
     * Verifies that ancestors are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetAncestors_existingDepartment() {
        departmentRepository.deleteAll();
        // Given: Existing entity
        Department parent = createEntity(1L, "Parent", "/1/", null);
        Department entity = createEntity(2L, "Entity", "/1/2/", parent.getId());
        Department child1 = createEntity(3L, "Child1", "/1/2/3/", entity.getId());
        Department child2 = createEntity(4L, "Child2", "/1/2/4/", entity.getId());

        String parentPath = "/" + parent.getId() + "/";
        parent.setPath(parentPath);
        departmentRepository.save(parent);

        String departmentPath = parent.getPath() + entity.getId() + "/";
        entity.setPath(departmentPath);
        departmentRepository.save(entity);

        String child1Path = entity.getPath() + child1.getId() + "/";
        child1.setPath(child1Path);
        departmentRepository.save(child1);

        String child2Path = entity.getPath() + child2.getId() + "/";
        child2.setPath(child2Path);
        departmentRepository.save(child2);

        // When: Getting ancestors
        List<HierarchyResponseDto> result = departmentService.getAncestors(child1.getId());

        // Then: Verify the result
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> "Parent".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "Entity".equals(dto.getName())));
    }

    /**
     * Integration test for {@link DepartmentService#getAncestors(Long)} when entity does not exist.
     * Verifies that {@link EntityNotFoundException} is thrown.
     */
    @Test
    public void testGetAncestors_nonExistingDepartment() {
        // Given: Non-existing entity ID
        Long departmentId = -1L;

        // When: Getting ancestors
        EntityNotFoundException thrown = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getAncestors(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity not found with ID: " + departmentId, thrown.getMessage());
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
     * Integration test for {@link DepartmentService#getParentEntity(Long)} when parent entity exists.
     * Verifies that parent entity is correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetParentDepartment_existingParent() {
        // Given: Existing entity with parent
        Long parentId = 1L;
        Long departmentId = 2L;
        Department parent = createEntity(parentId, "Parent", "/1/", null);
        Department entity = createEntity(departmentId, "Entity", "/1/2/", parent.getId());

        // When: Getting parent entity
        HierarchyResponseDto result = departmentService.getParentEntity(entity.getId());

        // Then: Verify the result
        assertEquals(parent.getId(), result.getId());
        assertEquals(parent.getName(), result.getName());
    }

    /**
     * Integration test for {@link DepartmentService#getParentEntity(Long)} when entity does not exist.
     * Verifies that {@link EntityNotFoundException} is thrown.
     */
    @Test
    public void testGetParentDepartment_nonExistingDepartment() {
        // Given: Non-existing entity ID
        Long departmentId = -1L;

        // When: Getting parent entity
        EntityNotFoundException thrown = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getParentEntity(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity not found with ID: " + departmentId, thrown.getMessage());
    }

    /**
     * Integration test for {@link DepartmentService#getParentEntity(Long)} when entity parent does not exist.
     * Verifies that {@link ParentEntityNotFoundException} is thrown.
     */
    @Test
    public void testGetParentDepartment_noParent() {
        // Given: Department with no parent
        Long departmentId = 1L;
        Department entity = createEntity(departmentId, "Entity", "/1/2/", null);

        // When: Getting parent entity
        ParentEntityNotFoundException thrown = assertThrows(
                ParentEntityNotFoundException.class,
                () -> departmentService.getParentEntity(entity.getId())
        );

        // Then: Verify the exception
        assertEquals("Entity with ID " + entity.getId() + " has no parent entity", thrown.getMessage());
    }
}

