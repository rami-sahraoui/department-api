package tn.engn.hierarchicalentityapi.service;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.DataIntegrityException;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ParentEntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.mapper.DepartmentMapper;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.model.QHierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AdjacencyListDepartmentService}.
 */
public class AdjacencyListDepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private AdjacencyListDepartmentService departmentService;

    private int maxNameLength = 50; // Mocked value for maxNameLength

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test
        MockitoAnnotations.openMocks(this);

        // Mock the value of maxNameLength directly
        departmentService.setMaxNameLength(maxNameLength);
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createEntity}.
     * Tests creation of a new department.
     */
    @Test
    public void testCreateDepartment() {
        // Mock input DTO
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Engineering");

        // Mock parent department ID (optional)
        Long parentId = 1L;
        requestDto.setParentEntityId(parentId);

        // Mock parent department entity
        Department parentEntity = new Department();
        parentEntity.setId(parentId);
        parentEntity.setName("Parent Department");

        // Mock department entity
        Department departmentEntity = new Department();
        departmentEntity.setId(1L);
        departmentEntity.setName(requestDto.getName());
        departmentEntity.setParentEntity(parentEntity);

        // Mock response DTO
        HierarchyResponseDto responseDto = new HierarchyResponseDto();
        responseDto.setId(departmentEntity.getId());
        responseDto.setName(departmentEntity.getName());
        responseDto.setParentEntityId(parentId);

        // Mock repository behavior
        when(departmentRepository.save(any(Department.class))).thenReturn(departmentEntity);
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentEntity));

        // Mock mapper behavior
        when(departmentMapper.toEntity(requestDto)).thenReturn(departmentEntity);
        when(departmentMapper.toDto(departmentEntity)).thenReturn(responseDto);

        // Call service method
        HierarchyResponseDto createdDto = departmentService.createEntity(requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).save(any(Department.class));

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(departmentEntity);

        // Assertions
        assertNotNull(createdDto);
        assertEquals(responseDto.getId(), createdDto.getId());
        assertEquals(responseDto.getName(), createdDto.getName());
        assertEquals(responseDto.getParentEntityId(), createdDto.getParentEntityId());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createEntity}.
     * Tests creation of a new department without a parent.
     */
    @Test
    public void testCreateDepartmentWithoutParent() {
        // Mock input DTO
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("HR");

        // Mock repository behavior
        when(departmentRepository.save(any(Department.class))).thenAnswer((Answer<Department>) invocation -> {
            Department department = invocation.getArgument(0);
            department.setId(2L); // Simulate auto-generated ID
            return department;
        });

        // Mock mapper behavior
        when(departmentMapper.toDto(any(Department.class))).thenAnswer((Answer<HierarchyResponseDto>) invocation -> {
            Department department = invocation.getArgument(0);
            HierarchyResponseDto responseDto = new HierarchyResponseDto();
            responseDto.setId(department.getId());
            responseDto.setName(department.getName());
            responseDto.setParentEntityId(null); // No parent
            return responseDto;
        });
        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenAnswer((Answer<Department>) invocation -> {
            HierarchyRequestDto request = invocation.getArgument(0);
            Department department = new Department();
            department.setName(request.getName());
            department.setParentId(null); // No parent
            department.setParentEntity(null); // No parent
            return department;
        });

        // Call service method
        HierarchyResponseDto createdDto = departmentService.createEntity(requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).save(any(Department.class));

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(any(Department.class));

        // Assertions
        assertNotNull(createdDto);
        assertEquals(2L, createdDto.getId()); // Assuming auto-generated ID is 2
        assertEquals(requestDto.getName(), createdDto.getName());
        assertNull(createdDto.getParentEntityId());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createEntity}.
     * Tests creation of a department with an invalid empty name.
     */
    @Test
    public void testCreateDepartmentWithEmptyName() {
        // Mock input DTO with empty name
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("");

        // Call service method and expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));

        // Verify repository method not called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createEntity}.
     * Tests handling of a department name at maximum allowed length.
     */
    @Test
    public void testCreateDepartmentMaxNameLength() {
        // Prepare input DTO with maximum length name
        String name = "DepartmentName123456789012345678901234567890123456";
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName(name);

        // Mock parent department ID (optional)
        Long parentId = 1L;
        requestDto.setParentEntityId(parentId);

        // Mock repository behavior (parent department found)
        Department parentEntity = new Department();
        parentEntity.setId(parentId);
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentEntity));

        // Mock created department
        Department createdDepartment = new Department();
        createdDepartment.setId(2L);
        createdDepartment.setName(name);

        // Mock save method of repository
        when(departmentRepository.save(any())).thenReturn(createdDepartment);

        // Mock mapper behavior (toDto)
        HierarchyResponseDto responseDto = new HierarchyResponseDto();
        responseDto.setId(createdDepartment.getId());
        responseDto.setName(createdDepartment.getName());
        when(departmentMapper.toEntity(requestDto)).thenReturn(createdDepartment);
        when(departmentMapper.toDto(createdDepartment)).thenReturn(responseDto);

        // Call service method
        HierarchyResponseDto resultDto = departmentService.createEntity(requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).save(any());

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(createdDepartment);

        // Assertions
        assertNotNull(resultDto);
        assertEquals(createdDepartment.getId(), resultDto.getId());
        assertEquals(createdDepartment.getName(), resultDto.getName());

        // Assertions
        assertNotNull(resultDto);
        assertEquals(name, resultDto.getName());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createEntity}.
     * Tests creation of a department with a name exceeding maximum allowed length.
     */
    @Test
    public void testCreateDepartmentWithNameTooLong() {
        // Mock input DTO with long name
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("This Department Name Is Way Too Long And Exceeds The Maximum Allowed Length");

        // Call service method and expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));

        // Verify repository method not called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createEntity}.
     * Tests creation of a department with a non-existent parent department ID.
     */
    @Test
    public void testCreateDepartmentWithNonExistentParentId() {
        // Mock input DTO with non-existent parent ID
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Finance");
        requestDto.setParentEntityId(100L); // Assuming ID 100 doesn't exist

        // Mock repository behavior (parent not found)
        when(departmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Call service method and expect EntityNotFoundException
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.createEntity(requestDto));

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(100L);
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateEntity}.
     * Tests updating a department's name.
     */
    @Test
    public void testUpdateDepartmentName() {
        // Existing department ID and new name
        Long departmentId = 1L;
        String newName = "Updated Department Name";

        // Mock input DTO with updated name
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName(newName);

        // Mock existing department entity
        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Old Department Name");

        // Mock repository behavior (find existing department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        // Mock repository behavior (save updated department)
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // Mock mapper behavior
        when(departmentMapper.toDto(existingDepartment)).thenReturn(new HierarchyResponseDto());

        // Call service method
        HierarchyResponseDto updatedDto = departmentService.updateEntity(departmentId, requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(existingDepartment);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(existingDepartment);

        // Assertions
        assertNotNull(updatedDto);
        assertEquals(newName, existingDepartment.getName());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateEntity}.
     * Tests updating a department's parent.
     */
    @Test
    public void testUpdateDepartmentParent() {
        // Existing department ID and new parent department ID
        Long departmentId = 1L;
        Long newParentId = 2L;

        // Mock input DTO with updated parent ID
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setParentEntityId(newParentId);
        requestDto.setName("Updated Department");

        // Mock existing department entity
        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Department");
        existingDepartment.setParentEntity(null); // No parent initially

        // Mock new parent department entity
        Department newParentEntity = new Department();
        newParentEntity.setId(newParentId);
        newParentEntity.setName("New Parent Department");

        // Mock repository behavior (find existing department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        // Mock repository behavior (find new parent department)
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentEntity));
        // Mock repository behavior (save updated department)
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // Mock mapper behavior
        when(departmentMapper.toDto(existingDepartment)).thenReturn(new HierarchyResponseDto());

        // Call service method
        HierarchyResponseDto updatedDto = departmentService.updateEntity(departmentId, requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(existingDepartment);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(existingDepartment);

        // Assertions
        assertNotNull(updatedDto);
        assertEquals(newParentId, existingDepartment.getParentEntity().getId());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateEntity}.
     * Tests updating a department to remove its parent.
     */
    @Test
    public void testUpdateDepartmentRemoveParent() {
        // Existing department ID with current parent
        Long departmentId = 1L;

        // Mock input DTO with no parent ID
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setParentEntityId(null);
        requestDto.setName("Updated Department");

        // Mock existing department entity with parent
        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Department");

        // Mock parent department entity
        Department parentEntity = new Department();
        parentEntity.setId(2L);
        parentEntity.setName("Parent Department");

        existingDepartment.setParentEntity(parentEntity);

        // Mock repository behavior (find existing department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        // Mock repository behavior (save updated department)
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // Mock mapper behavior
        when(departmentMapper.toDto(existingDepartment)).thenReturn(new HierarchyResponseDto());

        // Call service method
        HierarchyResponseDto updatedDto = departmentService.updateEntity(departmentId, requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(existingDepartment);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(existingDepartment);

        // Assertions
        assertNotNull(updatedDto);
        assertNull(updatedDto.getParentEntityId());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateEntity}.
     * Tests handling of circular dependency detection when updating department parent.
     */
    @Test
    public void testUpdateDepartmentCircularDependency() {
        // Prepare departments
        Department department = new Department();
        department.setId(1L);
        department.setName("Child Department");

        Department parentEntity = new Department();
        parentEntity.setId(2L);
        parentEntity.setName("Parent Department");

        department.setParentEntity(parentEntity);
        parentEntity.addSubEntity(department);

        // Mock repository behavior
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(parentEntity));

        // Prepare update request with circular dependency
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setParentEntityId(1L);
        requestDto.setName("Parent Department");

        // Call service method and assert circular dependency handling
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> departmentService.updateEntity(2L, requestDto));
        assertEquals("Circular dependency detected.", exception.getMessage());

        // Verify repository method not called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateEntity}.
     * Tests handling of updating department with a null ID.
     * Expected behavior is throwing an {@link EntityNotFoundException}.
     */
    @Test
    public void testUpdateDepartmentNullId() {
        // Prepare a request DTO with a valid parent department ID and name
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setParentEntityId(1L);
        requestDto.setName("Department1");

        // Call the service method with null department ID and assert EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> departmentService.updateEntity(null, requestDto));
        assertEquals("Entity not found with id: null", exception.getMessage());

        // Verify that findById method of departmentRepository was called once with any argument
        verify(departmentRepository, times(1)).findById(any());

        // Verify that save method of departmentRepository was never called
        verify(departmentRepository, never()).save(any());
    }


    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateEntity}.
     * Tests handling of updating department with a non-existent ID.
     * Expected behavior is throwing an {@link EntityNotFoundException}.
     */
    @Test
    public void testUpdateDepartmentNonExistentId() {
        // Prepare a request DTO with a valid parent department ID and name
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setParentEntityId(1L);
        requestDto.setName("Department1");

        // Mock repository behavior to return Optional.empty() for any department ID
        when(departmentRepository.findById(any())).thenReturn(Optional.empty());

        // Call the service method with a non-existent department ID and assert EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> departmentService.updateEntity(100L, requestDto));
        assertEquals("Entity not found with id: 100", exception.getMessage());

        // Verify that findById method of departmentRepository was called once with ID 100
        verify(departmentRepository, times(1)).findById(100L);

        // Verify that save method of departmentRepository was never called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#deleteEntity}.
     * Tests deletion of a department.
     */
    @Test
    public void testDeleteDepartment() {
        // Existing department ID
        Long departmentId = 1L;

        // Mock existing department entity
        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Department");

        // Mock repository behavior (find existing department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));

        // Call service method
        departmentService.deleteEntity(departmentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).delete(existingDepartment);
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#deleteEntity}.
     * Tests deletion of a department with a parent.
     */
    @Test
    public void testDeleteDepartmentWithParent() {
        // Existing department ID with parent
        Long departmentId = 1L;

        // Mock existing department entity with parent
        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Department");

        // Mock parent department entity
        Department parentEntity = new Department();
        parentEntity.setId(2L);
        parentEntity.setName("Parent Department");
        existingDepartment.setParentEntity(parentEntity);

        // Mock repository behavior (find existing department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));

        // Call service method
        departmentService.deleteEntity(departmentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).delete(existingDepartment);

        // Verify parent department's sub-departments list
        assertTrue(parentEntity.getSubEntities().isEmpty());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#deleteEntity}.
     * Tests deletion of a non-existent department.
     */
    @Test
    public void testDeleteNonExistentDepartment() {
        // Non-existent department ID
        Long departmentId = -1L;

        // Mock repository behavior (department not found)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // Call service method and expect EntityNotFoundException
        assertThrows(EntityNotFoundException.class, () -> departmentService.deleteEntity(departmentId));

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).delete(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getSubEntities}.
     * Tests retrieval of sub-departments.
     */
    @Test
    public void testgetSubEntities() {
        // Existing parent department ID
        Long parentId = 1L;

        // Mock parent department entity with sub-departments
        Department parentEntity = new Department();
        parentEntity.setId(parentId);
        parentEntity.setName("Parent Department");

        Department subDepartment1 = new Department();
        subDepartment1.setId(2L);
        subDepartment1.setName("Sub Department 1");

        Department subDepartment2 = new Department();
        subDepartment2.setId(3L);
        subDepartment2.setName("Sub Department 2");

        parentEntity.addSubEntity(subDepartment1);
        parentEntity.addSubEntity(subDepartment2);

        // Mock repository behavior (find parent department)
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentEntity));

        // Mock mapper behavior (toDtoList)
        when(departmentMapper.toDtoList(parentEntity.getSubEntities(), false)).thenReturn(Arrays.asList(
                HierarchyResponseDto.builder().id(subDepartment1.getId()).name(subDepartment1.getName()).build(),
                HierarchyResponseDto.builder().id(subDepartment2.getId()).name(subDepartment2.getName()).build())
        );

        // Call service method
        List<HierarchyResponseDto> subEntities = departmentService.getSubEntities(parentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(parentId);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDtoList(parentEntity.getSubEntities(), false);

        // Assertions
        assertNotNull(subEntities);
        assertEquals(2, subEntities.size());
        assertEquals("Sub Department 1", subEntities.get(0).getName());
        assertEquals("Sub Department 2", subEntities.get(1).getName());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getSubEntities}.
     * Tests retrieval of sub-departments for a parent department with no sub-departments.
     */
    @Test
    public void testGetSubEntitiesWithNoSubEntities() {
        // Existing parent department ID with no sub-departments
        Long parentId = 1L;

        // Mock parent department entity with no sub-departments
        Department parentEntity = new Department();
        parentEntity.setId(parentId);
        parentEntity.setName("Parent Department");

        // Mock repository behavior (find parent department)
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentEntity));

        // Mock mapper behavior (toDtoList)
        when(departmentMapper.toDtoList(parentEntity.getSubEntities(), false)).thenReturn(Collections.emptyList());

        // Call service method
        List<HierarchyResponseDto> subEntities = departmentService.getSubEntities(parentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(parentId);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDtoList(parentEntity.getSubEntities(),false);

        // Assertions
        assertNotNull(subEntities);
        assertTrue(subEntities.isEmpty());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getSubEntities}.
     * Tests retrieval of sub-departments for a non-existent parent department.
     */
    @Test
    public void testGetSubEntitiesWithNonExistentParent() {
        // Non-existent parent department ID
        Long parentId = 100L;

        // Mock repository behavior (parent department not found)
        when(departmentRepository.findById(parentId)).thenReturn(Optional.empty());

        // Call service method and expect EntityNotFoundException
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.getSubEntities(parentId));

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(parentId);

        // Verify mapper method not called
        verify(departmentMapper, never()).toDtoList(any());
    }

    /**
     * Test case for the getSubEntities method with pageable.
     * Verifies that the method retrieves sub-entities (children) of a given parent entity
     * with pagination, sorting, and optionally fetching sub-entities.
     */
    @Test
    public void testGetSubEntities_Pageable() {
        // Given: Mock pageable, parent entity ID, and sub-entities
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Long parentId = 1L;
        List<Department> subEntities = createMockEntities(); // Mock sub-entity list

        // Given: Mock parent entity
        Department parentEntity = Department.builder().id(parentId).name("Parent Department").build();

        // Given: Mock repository behavior for finding parent entity
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentEntity));

        // Given: Mock JPAQueryFactory behavior for fetching sub-entities
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryListPageable(subEntities);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getSubEntities(parentId, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(departmentRepository).findById(parentId);
        verify(jpaQueryFactory, times(2)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(2)).where(any(Predicate.class));
        verify(jpaQuery).orderBy(any(OrderSpecifier[].class));
        verify(jpaQuery).offset(anyLong());
        verify(jpaQuery).limit(anyLong());
        verify(jpaQuery).fetch();
        verify(jpaQuery).fetchCount();
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }


    /**
     * Unit test for {@link AdjacencyListDepartmentService#getAncestors}.
     * Tests retrieval of ancestors for a department.
     */
    @Test
    public void testGetAncestors() {
        // Existing department ID
        Long departmentId = 1L;

        // Mock department entity with ancestors
        Department department = new Department();
        department.setId(departmentId);
        department.setName("Department");

        Department parentEntity = new Department();
        parentEntity.setId(2L);
        parentEntity.setName("Parent Department");

        department.setParentEntity(parentEntity);

        // Mock repository behavior (find department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        // Mock mapper behavior (toDtoList)
        when(departmentMapper.toDtoList(any(), anyBoolean()))
                .thenReturn(
                        List.of(
                                HierarchyResponseDto.builder()
                                        .id(parentEntity.getId())
                                        .name(parentEntity.getName())
                                        .build()
                        )
                );

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(department);

        // Call service method
        List<HierarchyResponseDto> ancestors = departmentService.getAncestors(departmentId);

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDtoList(any(), anyBoolean());

        // Assertions
        assertNotNull(ancestors);
        assertEquals(1, ancestors.size());
        assertEquals("Parent Department", ancestors.get(0).getName());
    }

    HierarchyBaseEntity<?> toBaseEntity(Department department) {
        if (department == null) return null;
        HierarchyBaseEntity<?> baseEntity = HierarchyBaseEntity.builder().id(department.getId())
                .name(department.getName())
                .parentId(department.getParentId())
                .build();
        return baseEntity;
    }

    /**
     * Helper method to mock and stub a select department from query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<HierarchyBaseEntity<?>> mockSelectFromQuery(Department department) {
        HierarchyBaseEntity entity = (HierarchyBaseEntity) department;

        if (department != null) {
            entity.setParentEntity(department.getParentEntity());
            entity.setSubEntities(
                    department.getSubEntities().stream()
                            .map(d -> (HierarchyBaseEntity) d)
                            .collect(Collectors.toList())
            );
        }
        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetchOne()).thenReturn(entity);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QHierarchyBaseEntity.class))).thenReturn(mockQuery);
        return mockQuery;
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getAncestors}.
     * Tests retrieval of ancestors for a department with no parent.
     */
    @Test
    public void testGetAncestorsWithNoParent() {
        // Existing department ID with no parent
        Long departmentId = 1L;

        // Mock department entity with no parent
        Department department = new Department();
        department.setId(departmentId);
        department.setName("Department");

        // Mock repository behavior (find department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(department);

        // Call service method
        List<HierarchyResponseDto> ancestors = departmentService.getAncestors(departmentId);

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();

        // Verify mapper method not called (no ancestors)
        verify(departmentMapper, never()).toDto(any());

        // Assertions
        assertNotNull(ancestors);
        assertTrue(ancestors.isEmpty());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getAncestors}.
     * Tests retrieval of ancestors for a non-existent department.
     */
    @Test
    public void testGetAncestorsForNonExistentDepartment() {
        // Non-existent department ID
        Long departmentId = -1L;

        // Mock repository behavior (department not found)
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(null);

        // Call service method and expect EntityNotFoundException
        assertThrows(EntityNotFoundException.class, () -> departmentService.getAncestors(departmentId));

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();

        // Verify mapper method not called (no ancestors)
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Test case for the getAncestors method with pageable.
     * Verifies that the method retrieves all ancestors of a given entity with pagination, sorting, and optionally fetching sub-entities.
     */
    @Test
    public void testGetAncestors_Pageable() {
        // Given: Mock pageable and entity ID
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Long entityId = 1L;

        // Given: Mock entities and ancestors
        List<Department> entities = createMockEntities(); // Mock entity list
        List<Department> ancestors = createMockAncestors(); // Mock ancestors list

        // Given: Mock entity retrieval
        when(departmentRepository.findById(entityId))
                .thenReturn(Optional.of(entities.get(0))); // Return the root entity

        // Given: Mock JPAQueryFactory behavior for retrieving ancestors
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryList(ancestors);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getAncestors(entityId, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(departmentRepository).findById(entityId);
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Helper method to create mock ancestors.
     *
     * @return a list of mock ancestor entities.
     */
    private List<Department> createMockAncestors() {
        // Replace with your mock ancestor creation logic
        return Arrays.asList(
                Department.builder().id(1L).name("Department 1").parentEntity(null).build(),
                Department.builder().id(2L).name("Department 2").parentEntity(null).build(),
                Department.builder().id(3L).name("Department 3").parentEntity(null).build()
                // Add more ancestors as needed
        );
    }


    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateEntity}.
     * Tests the performance of updating a department with many sub-departments.
     * <p>
     * This test verifies that the service method performs efficiently when updating
     * a department that has a large number of sub-departments, even if the parent
     * department does not exist.
     * </p>
     */
    @Test
    public void testUpdateDepartmentPerformance() {
        // Prepare a large number of sub-departments
        int numsubEntities = 10000;
        Department parentEntity = new Department();
        parentEntity.setId(1L);
        parentEntity.setName("parentEntity");

        List<Department> subEntities = new ArrayList<>();
        for (int i = 0; i < numsubEntities; i++) {
            Department subDepartment = new Department();
            subDepartment.setId((long) (i + 2)); // Start IDs from 2 onwards
            subDepartment.setName("SubDepartment" + (i + 1));
            subDepartment.setParentEntity(parentEntity); // Set parent department
            subEntities.add(subDepartment);
        }
        parentEntity.setSubEntities(subEntities);

        // Mock repository behavior to return parent department and its sub-departments
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parentEntity));

        // Mock repository behavior to return empty optional for parent department with ID 2
        when(departmentRepository.findById(2L)).thenReturn(Optional.empty());

        // Prepare update request with circular dependency (setting one of the sub-departments as parent)
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setParentEntityId(2L); // Set one of the sub-departments as parent
        requestDto.setName("UpdatedparentEntity");

        // Call the service method and expect EntityNotFoundException due to missing parent department
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.updateEntity(1L, requestDto);
        });

        // Verify that the exception message indicates the parent department was not found
        assertEquals("Parent entity not found with id: 2", exception.getMessage());

        // Verify repository method not called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Test case for the getAllEntities method of AdjacencyListDepartmentService.
     * Verifies that the method retrieves all departments and maps them to DTOs correctly.
     */
    @Test
    public void testGetAllEntities() {
        // Mock behavior for jpaQuery
        List<Department> mockEntities = Arrays.asList(
                Department.builder().id(1L).name("Department 1").parentEntity(null).build(),
                Department.builder().id(2L).name("Department 2").parentEntity(null).build(),
                Department.builder().id(3L).name("Department 3").parentEntity(null).build()
                // Add more departments as needed
        );

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(mockEntities);

        // Mock behavior for departmentMapper.toDtoList()
        List<HierarchyResponseDto> mockDtoList = Arrays.asList(
                HierarchyResponseDto.builder().id(1L).name("Department 1").build(),
                HierarchyResponseDto.builder().id(2L).name("Department 2").build(),
                HierarchyResponseDto.builder().id(3L).name("Department 3").build()
                // Add corresponding DTOs as needed
        );
        when(departmentMapper.toDtoList(mockEntities, false)).thenReturn(mockDtoList);

        // Call the method under test
        List<HierarchyResponseDto> responseDtoList = departmentService.getAllEntities();

        // Assert the size of the response list
        assertEquals(3, responseDtoList.size(), "Expected 3 departments in the response");

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetch();
    }

    /**
     * Test case for an empty database scenario.
     */
    @Test
    public void testGetAllEntities_EmptyDatabase() {
        // Mock behavior for jpaQuery returning an empty list
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(Collections.emptyList());

        // Mock behavior for departmentMapper.toDtoList() returning an empty list
        when(departmentMapper.toDtoList(Collections.emptyList(), false)).thenReturn(Collections.emptyList());

        // Call the method under test
        List<HierarchyResponseDto> responseDtoList = departmentService.getAllEntities();

        // Assert the size of the response list is zero
        assertEquals(0, responseDtoList.size(), "Expected 0 departments in the response");
        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetch();
    }

    /**
     * Test case for a database failure scenario.
     */
    @Test
    public void testGetAllEntities_DatabaseFailure() {
        // Mock behavior for jpaQuery to throw an exception
        when(jpaQueryFactory.selectFrom(any())).thenThrow(new RuntimeException("Database connection error"));

        // Call the method under test and expect an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            departmentService.getAllEntities();
        });

        // Assert the exception message
        assertEquals("Database connection error", exception.getMessage());
    }

    /**
     * Test case for a mapping failure scenario.
     */
    @Test
    public void testGetAllEntities_MappingFailure() {
        // Mock behavior for jpaQuery returning a list
        List<Department> mockEntities = Arrays.asList(
                Department.builder().id(1L).name("Department 1").parentEntity(null).build()
        );

        mockSelectFromQueryList(mockEntities);

        // Mock behavior for departmentMapper.toDtoList() to throw an exception
        when(departmentMapper.toDtoList(mockEntities, false)).thenThrow(new RuntimeException("Mapping error"));

        // Call the method under test and expect an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            departmentService.getAllEntities();
        });

        // Assert the exception message
        assertEquals("Mapping error", exception.getMessage());
    }

    /**
     * Test case for a large number of departments scenario.
     */
    @Test
    public void testGetAllEntities_LargeNumberOfEntities() {
        // Create a large number of mock departments
        List<Department> mockEntities = new ArrayList<>();
        for (long i = 1; i <= 10000; i++) {
            mockEntities.add(Department.builder().id(i).name("Department " + i).parentEntity(null).build());
        }

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(mockEntities);

        // Create corresponding DTOs
        List<HierarchyResponseDto> mockDtoList = new ArrayList<>();
        for (long i = 1; i <= 10000; i++) {
            mockDtoList.add(HierarchyResponseDto.builder().id(i).name("Department " + i).build());
        }
        when(departmentMapper.toDtoList(mockEntities, false)).thenReturn(mockDtoList);

        // Call the method under test
        List<HierarchyResponseDto> responseDtoList = departmentService.getAllEntities();

        // Assert the size of the response list
        assertEquals(10000, responseDtoList.size(), "Expected 10000 departments in the response");

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetch();
    }

    /**
     * Test case for the getAllEntities method with pageable.
     * Verifies that the method retrieves all departments and maps them to DTOs correctly.
     */
    @Test
    public void testGetAllEntities_Pageable() {
        // Given: Mock pageable and entities
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        List<Department> entities = createMockEntities(); // Mock entity list

        // Given: Mock JPAQueryFactory behavior
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryListPageable(entities);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getAllEntities(pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(jpaQueryFactory, times(2)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(2)).where(any(Predicate.class));
        verify(jpaQuery).orderBy(any(OrderSpecifier[].class));
        verify(jpaQuery).offset(anyLong());
        verify(jpaQuery).limit(anyLong());
        verify(jpaQuery).fetch();
        verify(jpaQuery).fetchCount();
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Helper method to create mock entities with a deep hierarchy.
     *
     * @return a list of mock Department entities.
     */
    private List<Department> createMockEntities() {
        // Mock entity creation logic with a deep hierarchy
        Department dept1 = Department.builder().id(1L).name("Department 1").parentEntity(null).build();
        Department dept2 = Department.builder().id(2L).name("Department 2").parentEntity(dept1).build();
        Department dept3 = Department.builder().id(3L).name("Department 3").parentEntity(dept2).build();
        Department dept4 = Department.builder().id(4L).name("Department 4").parentEntity(dept2).build();
        Department dept5 = Department.builder().id(5L).name("Department 5").parentEntity(dept1).build();

        dept1.setSubEntities(Arrays.asList(dept2, dept5));
        dept2.setSubEntities(Arrays.asList(dept3, dept4));
        return Arrays.asList(dept1, dept2, dept3, dept4, dept5);
    }

    /**
     * Helper method to create a mock PaginatedResponseDto.
     *
     * @return a mock PaginatedResponseDto.
     */
    private PaginatedResponseDto<HierarchyResponseDto> createMockPaginatedResponseDto() {
        // Mock PaginatedResponseDto creation logic
        List<HierarchyResponseDto> mockDtoList = Arrays.asList(
                HierarchyResponseDto.builder().id(1L).name("Department 1").build(),
                HierarchyResponseDto.builder().id(2L).name("Department 2").build(),
                HierarchyResponseDto.builder().id(3L).name("Department 3").build()
                // Add corresponding DTOs as needed
        );

        return PaginatedResponseDto.<HierarchyResponseDto>builder()
                .content(mockDtoList)
                .page(0)
                .size(10)
                .totalElements(3)
                .totalPages(1)
                .build();
    }

    /**
     * Helper method to mock and stub a select pageable list of departments from query.
     *
     * @param departments the list of mock departments to return from the query.
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<HierarchyBaseEntity<?>> mockSelectFromQueryListPageable(List<Department> departments) {
        // Mocking the JPAQuery behavior
        JPAQuery jpaQuery = mock(JPAQuery.class);
        when(jpaQuery.where(any(Predicate.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(OrderSpecifier[].class))).thenReturn(jpaQuery); // Mock orderBy with the array
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(departments);
        when(jpaQuery.fetchCount()).thenReturn((long) departments.size());
        when(jpaQuery.fetchOne()).thenReturn(departments.get(0));

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QHierarchyBaseEntity.class))).thenReturn(jpaQuery);

        return jpaQuery;
    }


    /**
     * Test case for the getEntityById method of AdjacencyListDepartmentService.
     * Verifies that the method retrieves a department by its ID and maps it to a DTO correctly.
     */
    @Test
    public void testGetDepartmentById_ValidId() {
        // Mock behavior for departmentRepository.findById()
        Department mockDepartment = Department.builder().id(1L).name("Department 1").parentEntity(null).build();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));

        // Mock behavior for departmentMapper.toDto()
        HierarchyResponseDto mockDto = HierarchyResponseDto.builder().id(1L).name("Department 1").build();
        when(departmentMapper.toDto(mockDepartment, false)).thenReturn(mockDto);

        // Call the method under test
        HierarchyResponseDto responseDto = departmentService.getEntityById(1L);

        // Assert the response
        assertEquals(mockDto, responseDto, "Expected department DTO with ID 1");
    }

    /**
     * Test case for the getEntityById method when the department does not exist.
     * Verifies that the method throws an EntityNotFoundException.
     */
    @Test
    public void testGetDepartmentById_NonExistentId() {
        // Mock behavior for departmentRepository.findById() to return an empty Optional
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the method under test and expect an exception
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getEntityById(1L);
        });

        // Assert the exception message
        assertEquals("Entity not found with id: 1", exception.getMessage());
    }

    /**
     * Test case for the getEntityById method when the mapping to DTO fails.
     * Verifies that the method handles the mapping failure.
     */
    @Test
    public void testGetDepartmentById_MappingFailure() {
        // Mock behavior for departmentRepository.findById()
        Department mockDepartment = Department.builder().id(1L).name("Department 1").parentEntity(null).build();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));

        // Mock behavior for departmentMapper.toDto() to throw an exception
        when(departmentMapper.toDto(mockDepartment,false)).thenThrow(new RuntimeException("Mapping error"));

        // Call the method under test and expect an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            departmentService.getEntityById(1L);
        });

        // Assert the exception message
        assertEquals("Mapping error", exception.getMessage());
    }

    /**
     * Tests the {@link DepartmentService#searchEntitiesByName(String)} method.
     * Verifies that departments are correctly searched by name.
     */
    @Test
    public void testSearchEntitiesByName() {
        // Mock data
        String name = "Engineering";
        Department mockDepartment = Department.builder()
                .id(1L)
                .name(name)
                .parentEntity(null)
                .build();
        List<Department> mockEntities = new ArrayList<>();
        mockEntities.add(mockDepartment);


        // Mock jpaQuery
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(mockEntities);

        // Mock mapper behavior
        List<HierarchyResponseDto> expectedDtoList = mockEntities.stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());

        when(departmentMapper.toDtoList(any(), anyBoolean())).thenReturn(expectedDtoList);

        // Call the method under test
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(name);

        // Assertions
        assertThat(result).isNotNull();
        assertThat(result).hasSize(mockEntities.size());

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetch();
        // Add more assertions based on your expected behavior
    }

    /**
     * Tests the {@link DepartmentService#searchEntitiesByName(String)} method.
     * Verifies handling of an empty result set.
     */
    @Test
    public void testSearchEntitiesByName_EmptyResult() {
        // Mock data
        String name = "NonExistingDepartment";

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(Collections.emptyList());

        // Call the method under test
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(name);

        // Assertions
        assertThat(result).isEmpty();

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetch();
    }

    /**
     * Test case for the searchEntitiesByName method with pageable.
     * Verifies that the method searches entities by name with pagination, sorting, and optionally fetching sub-entities.
     */
    @Test
    public void testSearchEntitiesByName_Pageable() {
        // Given: Mock pageable and search criteria
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        String name = "Department";
        List<Department> entities = createMockEntities(); // Mock entity list

        // Given: Mock JPAQueryFactory behavior for searching entities by name
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryListPageable(entities);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.searchEntitiesByName(name, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(jpaQueryFactory, times(2)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(2)).where(any(Predicate.class));
        verify(jpaQuery).orderBy(any(OrderSpecifier[].class));
        verify(jpaQuery).offset(anyLong());
        verify(jpaQuery).limit(anyLong());
        verify(jpaQuery).fetch();
        verify(jpaQuery).fetchCount();
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Test case for the getParentEntity method of AdjacencyListDepartmentService.
     * Verifies that the method retrieves the parent department and maps it to a DTO correctly.
     */
    @Test
    public void testGetParentEntity_ValidIdWithParent() {
        // Mock behavior for jpaQuery
        Department parentEntity = Department.builder().id(1L).name("Parent Department").parentEntity(null).build();
        Department mockDepartment = Department.builder().id(2L).name("Child Department").parentEntity(parentEntity).build();

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(mockDepartment);

        // Mock behavior for departmentMapper.toDto()
        HierarchyResponseDto mockParentDto = HierarchyResponseDto.builder().id(1L).name("Parent Department").build();
        when(departmentMapper.toDto(any(), anyBoolean())).thenReturn(mockParentDto);

        // Call the method under test
        HierarchyResponseDto responseDto = departmentService.getParentEntity(2L);

        // Assert the response
        assertEquals(mockParentDto, responseDto, "Expected parent department DTO with ID 1");

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();
    }

    /**
     * Test case for the getParentEntity method when the department does not have a parent.
     * Verifies that the method throws an EntityNotFoundException.
     */
    @Test
    public void testGetParentEntity_ValidIdWithoutParent() {
        // Mock behavior for departmentRepository.findById()
        Department mockDepartment = Department.builder().id(2L).name("Child Department").parentEntity(null).build();

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(mockDepartment);

        // Call the method under test and expect an exception
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.getParentEntity(2L);
        });

        // Assert the exception message
        assertEquals("Parent entity not found for entity with id: 2", exception.getMessage());

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();
    }

    /**
     * Test case for the getParentEntity method when the department does not exist.
     * Verifies that the method throws an EntityNotFoundException.
     */
    @Test
    public void testGetParentEntity_NonExistentId() {
        // Mock behavior for jpaQuery
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(null);

        // Call the method under test and expect an exception
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getParentEntity(-1L);
        });

        // Assert the exception message
        assertEquals("Entity not found with id: -1", exception.getMessage());

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();
    }

    /**
     * Test case for the getDescendants method of AdjacencyListDepartmentService.
     * Verifies that the method retrieves all descendants and maps them to DTOs correctly.
     */
    @Test
    public void testGetDescendants_ValidIdWithDescendants() {
        // Mock behavior for departmentRepository.findById()
        Department department1 = Department.builder().id(1L).name("Department 1").build();
        Department department2 = Department.builder().id(2L).name("Department 2").parentEntity(department1).build();
        Department department3 = Department.builder().id(3L).name("Department 3").parentEntity(department1).build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department1));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department2));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department3));

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(department1);

        // Mock behavior for departmentMapper.toDtoList()
        List<HierarchyResponseDto> mockDtoList = Arrays.asList(
                HierarchyResponseDto.builder().id(2L).name("Department 2").build(),
                HierarchyResponseDto.builder().id(3L).name("Department 3").build()
                // Add corresponding DTOs as needed
        );
        when(departmentMapper.toDtoList(anyList(), anyBoolean())).thenReturn(mockDtoList);

        // Call the method under test
        List<HierarchyResponseDto> responseDtoList = departmentService.getDescendants(department1.getId());

        // Assert the size of the response list
        assertEquals(2, responseDtoList.size(), "Expected 2 departments in the response");

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();
    }

    /**
     * Test case for the getDescendants method of AdjacencyListDepartmentService.
     * Verifies that an exception is thrown if the department ID is not found.
     */
    @Test
    public void testGetDescendants_DepartmentNotFound() {
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(null);

        // Call the method under test and expect an exception
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getDescendants(1L);
        });

        // Assert the exception message
        assertEquals("Entity not found with id: 1", exception.getMessage());

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();

        // Verify mapper method called
        verify(departmentMapper, never()).toDtoList(any());
    }

    /**
     * Test case for the updateEntity method of AdjacencyListDepartmentService.
     * Verifies that the method updates the department's information and removes it from parent's sub-departments correctly.
     */
    @Test
    public void testUpdateDepartment_removeSubEntities() {
        // Create a parent department and a department to update
        Department parentEntity = Department.builder().id(1L).name("Parent Department").subEntities(new ArrayList<>()).build();
        Department departmentToUpdate = Department.builder().id(2L).name("Department to Update").parentEntity(parentEntity).build();

        // Add departmentToUpdate to parentEntity's subEntities
        parentEntity.addSubEntity(departmentToUpdate);

        // Mock behavior for departmentRepository.findById()
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(departmentToUpdate));

        // Update the department's name (this is done implicitly when calling updateEntity)
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated Department Name");

        // Mock behavior for departmentRepository.findById() to return parentEntity
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parentEntity));

        // Assert the parent department's subEntities list size before update
        assertEquals(1, parentEntity.getSubEntities().size(), "Expected 1 sub-department before update");

        // Call the method under test
        departmentService.updateEntity(2L, requestDto);

        // Assert that the department's name was updated (this is indirectly tested by checking the return value)
        assertEquals("Updated Department Name", departmentToUpdate.getName());

        // Assert the parent department's subEntities list size after update
        assertEquals(0, parentEntity.getSubEntities().size(), "Expected 0 sub-departments after removal");

        // Assert that departmentToUpdate is no longer in parentEntity's subEntities
        assertFalse(parentEntity.getSubEntities().contains(departmentToUpdate));
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#handleSubEntitiesOnDelete}.
     * Tests deletion of a department with sub-departments.
     */
    @Test
    public void testHandleSubEntitiesOnDelete_WithSubEntities() {
        // Create a parent department with sub-departments
        Department parentEntity = new Department();
        parentEntity.setId(1L);
        parentEntity.setName("Parent Department");

        Department subDepartment1 = new Department();
        subDepartment1.setId(2L);
        subDepartment1.setName("Sub Department 1");
        subDepartment1.setParentEntity(parentEntity);

        Department subDepartment2 = new Department();
        subDepartment2.setId(3L);
        subDepartment2.setName("Sub Department 2");
        subDepartment2.setParentEntity(parentEntity);

        parentEntity.addSubEntity(subDepartment1);
        parentEntity.addSubEntity(subDepartment2);

        // Mock behavior for departmentRepository.delete
        doNothing().when(departmentRepository).delete(any());

        // Call the method under test
        departmentService.handleSubEntitiesOnDelete(parentEntity);

        // Verify that each sub-department's parent reference is set to null
        assertEquals(null, subDepartment1.getParentEntity());
        assertEquals(null, subDepartment2.getParentEntity());

        // Verify that delete method was called for each sub-department
        verify(departmentRepository, times(1)).save(subDepartment1);
        verify(departmentRepository, times(1)).save(subDepartment2);
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#handleSubEntitiesOnDelete}.
     * Tests deletion of a department without sub-departments.
     */
    @Test
    public void testHandleSubEntitiesOnDelete_WithoutSubEntities() {
        // Create a parent department without any sub-departments
        Department parentEntity = new Department();
        parentEntity.setId(1L);
        parentEntity.setName("Parent Department");

        // Call the method under test
        departmentService.handleSubEntitiesOnDelete(parentEntity);

        // Verify that sub-departments list is cleared (should already be empty)
        assertNotNull( parentEntity.getSubEntities());
        assertEquals(0, parentEntity.getSubEntities().size());

        // Verify that delete method was not called because there were no sub-departments
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#handleSubEntitiesOnDelete}.
     * Tests deletion of a department with a large number of sub-departments.
     */
    @Test
    public void testHandleSubEntitiesOnDelete_LargeNumberOfSubEntities() {
        // Create a parent department with a large number of sub-departments
        Department parentEntity = new Department();
        parentEntity.setId(1L);
        parentEntity.setName("Parent Department");

        List<Department> subEntities = new ArrayList<>();
        int numSubEntities = 100;
        for (int i = 0; i < numSubEntities; i++) {
            Department subDepartment = new Department();
            subDepartment.setId((long) (i + 2));
            subDepartment.setName("Sub Department " + (i + 1));
            subDepartment.setParentEntity(parentEntity);
            subEntities.add(subDepartment);
            parentEntity.addSubEntity(subDepartment);
        }

        // Mock behavior for departmentRepository.delete
        doNothing().when(departmentRepository).delete(any());

        // Call the method under test
        departmentService.handleSubEntitiesOnDelete(parentEntity);

        // Verify that each sub-department's parent reference is set to null
        for (Department subDept : subEntities) {
            assertEquals(null, subDept.getParentEntity());
        }

        // Verify that delete method was called for each sub-department
        verify(departmentRepository, times(numSubEntities)).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getDescendants}.
     * Tests fetching descendants when sub-departments list is null.
     */
    @Test
    public void testGetDescendants_NullSubEntities() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Department 1");

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(department);

        List<HierarchyResponseDto> descendants = departmentService.getDescendants(department.getId());

        // Expecting an empty list of descendants when sub-departments list is null
        assertEquals(0, descendants.size());

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getDescendants}.
     * Tests fetching descendants when sub-departments list is empty.
     */
    @Test
    public void testGetDescendants_EmptySubEntities() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Department 1");
        department.setSubEntities(new ArrayList<>()); // Empty sub-departments list

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(department);

        List<HierarchyResponseDto> descendants = departmentService.getDescendants(department.getId());

        // Expecting an empty list of descendants when sub-departments list is empty
        assertNotNull(descendants);
        assertTrue(descendants.isEmpty());

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getDescendants}.
     * Tests fetching descendants when there are multiple levels of sub-departments.
     */
    @Test
    public void testGetDescendants_MultipleLevels() {
        // Create departments with multiple levels of sub-departments
        Department department1 = new Department();
        department1.setId(1L);
        department1.setName("Department 1");

        Department department2 = new Department();
        department2.setId(2L);
        department2.setName("Department 2");

        Department department3 = new Department();
        department3.setId(3L);
        department3.setName("Department 3");

        Department subDepartment1 = new Department();
        subDepartment1.setId(4L);
        subDepartment1.setName("Sub Department 1");

        Department subDepartment2 = new Department();
        subDepartment2.setId(5L);
        subDepartment2.setName("Sub Department 2");

        Department subSubDepartment1 = new Department();
        subSubDepartment1.setId(6L);
        subSubDepartment1.setName("Sub Sub Department 1");

        // Construct hierarchy
        department1.addSubEntity(department2);
        department1.addSubEntity(department3);
        department2.addSubEntity(subDepartment1);
        department2.addSubEntity(subDepartment2);
        subDepartment1.addSubEntity(subSubDepartment1);

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQuery(department1);

        // Mock behavior for departmentMapper.toDtoList()
        List<HierarchyResponseDto> mockDtoList = Arrays.asList(
                HierarchyResponseDto.builder().id(department2.getId()).name(department2.getName()).build(),
                HierarchyResponseDto.builder().id(department3.getId()).name(department3.getName()).build(),
                HierarchyResponseDto.builder().id(subDepartment1.getId()).name(subDepartment1.getName()).build(),
                HierarchyResponseDto.builder().id(subDepartment2.getId()).name(subDepartment2.getName()).build(),
                HierarchyResponseDto.builder().id(subSubDepartment1.getId()).name(subSubDepartment1.getName()).build()
        );
        when(departmentMapper.toDtoList(anyList(), anyBoolean())).thenReturn(mockDtoList);

        List<HierarchyResponseDto> descendants = departmentService.getDescendants(department1.getId());

        // Expecting all descendants to be fetched recursively
        assertEquals(5, descendants.size()); // Including department2, department3, subDepartment1, subDepartment2, subSubDepartment1

        // Verify jpaQuery method called
        verify(jpaQueryFactory, times(1)).selectFrom(any());
        verify(mockQuery, times(1)).where(any(Predicate.class));
        verify(mockQuery, times(1)).fetchOne();
    }

    /**
     * Helper method to mock and stub a select list of departments from query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<HierarchyBaseEntity<?>> mockSelectFromQueryList(List<Department> departments) {
        List<HierarchyBaseEntity<?>> entities = departments.stream()
                .map(d -> (HierarchyBaseEntity<?>) d)
                .collect(Collectors.toList());

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mock(JPAQuery.class);
        when(mockQuery.orderBy(any(OrderSpecifier.class))).thenReturn(mockQuery);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(entities);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QHierarchyBaseEntity.class))).thenReturn(mockQuery);

        return mockQuery;

    }

    /**
     * Test case for the getDescendants method with pageable.
     * Verifies that the method retrieves all descendants of a given entity with pagination, sorting, and optionally fetching sub-entities.
     */
    @Test
    public void testGetDescendants() {
        // Given: Mock pageable and entity ID
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Long entityId = 1L;

        // Given: Mock entities and descendants
        List<Department> entities = createMockEntities(); // Mock entity list
        List<Department> descendants = createMockDescendants(); // Mock descendants list

        // Given: Mock JPAQueryFactory behavior for retrieving entity and its descendants
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryListPageable(descendants);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getDescendants(entityId, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(jpaQueryFactory, times(1)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(1)).where(any(Predicate.class));
        verify(jpaQuery).fetchOne();
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Helper method to create mock descendants.
     *
     * @return a list of mock descendant entities.
     */
    private List<Department> createMockDescendants() {
        // Replace with your mock descendant creation logic
        return Arrays.asList(
                Department.builder().id(1L).name("Department 1").parentEntity(null).build(),
                Department.builder().id(2L).name("Department 2").parentEntity(null).build(),
                Department.builder().id(3L).name("Department 3").parentEntity(null).build()
                // Add more descendants as needed
        );
    }


}
