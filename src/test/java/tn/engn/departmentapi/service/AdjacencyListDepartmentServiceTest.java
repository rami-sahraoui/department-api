package tn.engn.departmentapi.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.exception.DataIntegrityException;
import tn.engn.departmentapi.exception.DepartmentNotFoundException;
import tn.engn.departmentapi.exception.ParentDepartmentNotFoundException;
import tn.engn.departmentapi.exception.ValidationException;
import tn.engn.departmentapi.mapper.DepartmentMapper;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.*;

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
     * Unit test for {@link AdjacencyListDepartmentService#createDepartment}.
     * Tests creation of a new department.
     */
    @Test
    public void testCreateDepartment() {
        // Mock input DTO
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Engineering");

        // Mock parent department ID (optional)
        Long parentId = 1L;
        requestDto.setParentDepartmentId(parentId);

        // Mock parent department entity
        Department parentDepartment = new Department();
        parentDepartment.setId(parentId);
        parentDepartment.setName("Parent Department");

        // Mock department entity
        Department departmentEntity = new Department();
        departmentEntity.setId(1L);
        departmentEntity.setName(requestDto.getName());
        departmentEntity.setParentDepartment(parentDepartment);

        // Mock response DTO
        DepartmentResponseDto responseDto = new DepartmentResponseDto();
        responseDto.setId(departmentEntity.getId());
        responseDto.setName(departmentEntity.getName());
        responseDto.setParentDepartmentId(parentId);

        // Mock repository behavior
        when(departmentRepository.save(any(Department.class))).thenReturn(departmentEntity);
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));

        // Mock mapper behavior
        when(departmentMapper.toDto(departmentEntity)).thenReturn(responseDto);

        // Call service method
        DepartmentResponseDto createdDto = departmentService.createDepartment(requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).save(any(Department.class));

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(departmentEntity);

        // Assertions
        assertNotNull(createdDto);
        assertEquals(responseDto.getId(), createdDto.getId());
        assertEquals(responseDto.getName(), createdDto.getName());
        assertEquals(responseDto.getParentDepartmentId(), createdDto.getParentDepartmentId());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createDepartment}.
     * Tests creation of a new department without a parent.
     */
    @Test
    public void testCreateDepartmentWithoutParent() {
        // Mock input DTO
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("HR");

        // Mock repository behavior
        when(departmentRepository.save(any(Department.class))).thenAnswer((Answer<Department>) invocation -> {
            Department department = invocation.getArgument(0);
            department.setId(2L); // Simulate auto-generated ID
            return department;
        });

        // Mock mapper behavior
        when(departmentMapper.toDto(any(Department.class))).thenAnswer((Answer<DepartmentResponseDto>) invocation -> {
            Department department = invocation.getArgument(0);
            DepartmentResponseDto responseDto = new DepartmentResponseDto();
            responseDto.setId(department.getId());
            responseDto.setName(department.getName());
            responseDto.setParentDepartmentId(null); // No parent
            return responseDto;
        });

        // Call service method
        DepartmentResponseDto createdDto = departmentService.createDepartment(requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).save(any(Department.class));

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(any(Department.class));

        // Assertions
        assertNotNull(createdDto);
        assertEquals(2L, createdDto.getId()); // Assuming auto-generated ID is 2
        assertEquals(requestDto.getName(), createdDto.getName());
        assertNull(createdDto.getParentDepartmentId());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createDepartment}.
     * Tests creation of a department with an invalid empty name.
     */
    @Test
    public void testCreateDepartmentWithEmptyName() {
        // Mock input DTO with empty name
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("");

        // Call service method and expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));

        // Verify repository method not called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createDepartment}.
     * Tests handling of a department name at maximum allowed length.
     */
    @Test
    public void testCreateDepartmentMaxNameLength() {
        // Prepare input DTO with maximum length name
        String name = "DepartmentName123456789012345678901234567890123456";
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName(name);

        // Mock parent department ID (optional)
        Long parentId = 1L;
        requestDto.setParentDepartmentId(parentId);

        // Mock repository behavior (parent department found)
        Department parentDepartment = new Department();
        parentDepartment.setId(parentId);
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));

        // Mock created department
        Department createdDepartment = new Department();
        createdDepartment.setId(2L);
        createdDepartment.setName(name);

        // Mock save method of repository
        when(departmentRepository.save(any())).thenReturn(createdDepartment);

        // Mock mapper behavior (toDto)
        DepartmentResponseDto responseDto = new DepartmentResponseDto();
        responseDto.setId(createdDepartment.getId());
        responseDto.setName(createdDepartment.getName());
        when(departmentMapper.toDto(createdDepartment)).thenReturn(responseDto);

        // Call service method
        DepartmentResponseDto resultDto = departmentService.createDepartment(requestDto);

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
     * Unit test for {@link AdjacencyListDepartmentService#createDepartment}.
     * Tests creation of a department with a name exceeding maximum allowed length.
     */
    @Test
    public void testCreateDepartmentWithNameTooLong() {
        // Mock input DTO with long name
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("This Department Name Is Way Too Long And Exceeds The Maximum Allowed Length");

        // Call service method and expect IllegalArgumentException
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));

        // Verify repository method not called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#createDepartment}.
     * Tests creation of a department with a non-existent parent department ID.
     */
    @Test
    public void testCreateDepartmentWithNonExistentParentId() {
        // Mock input DTO with non-existent parent ID
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Finance");
        requestDto.setParentDepartmentId(100L); // Assuming ID 100 doesn't exist

        // Mock repository behavior (parent not found)
        when(departmentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Call service method and expect EntityNotFoundException
        assertThrows(ParentDepartmentNotFoundException.class, () -> departmentService.createDepartment(requestDto));

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(100L);
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateDepartment}.
     * Tests updating a department's name.
     */
    @Test
    public void testUpdateDepartmentName() {
        // Existing department ID and new name
        Long departmentId = 1L;
        String newName = "Updated Department Name";

        // Mock input DTO with updated name
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
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
        when(departmentMapper.toDto(existingDepartment)).thenReturn(new DepartmentResponseDto());

        // Call service method
        DepartmentResponseDto updatedDto = departmentService.updateDepartment(departmentId, requestDto);

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
     * Unit test for {@link AdjacencyListDepartmentService#updateDepartment}.
     * Tests updating a department's parent.
     */
    @Test
    public void testUpdateDepartmentParent() {
        // Existing department ID and new parent department ID
        Long departmentId = 1L;
        Long newParentId = 2L;

        // Mock input DTO with updated parent ID
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setParentDepartmentId(newParentId);
        requestDto.setName("Updated Department");

        // Mock existing department entity
        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Department");
        existingDepartment.setParentDepartment(null); // No parent initially

        // Mock new parent department entity
        Department newParentDepartment = new Department();
        newParentDepartment.setId(newParentId);
        newParentDepartment.setName("New Parent Department");

        // Mock repository behavior (find existing department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        // Mock repository behavior (find new parent department)
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));
        // Mock repository behavior (save updated department)
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // Mock mapper behavior
        when(departmentMapper.toDto(existingDepartment)).thenReturn(new DepartmentResponseDto());

        // Call service method
        DepartmentResponseDto updatedDto = departmentService.updateDepartment(departmentId, requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(existingDepartment);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(existingDepartment);

        // Assertions
        assertNotNull(updatedDto);
        assertEquals(newParentId, existingDepartment.getParentDepartment().getId());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateDepartment}.
     * Tests updating a department to remove its parent.
     */
    @Test
    public void testUpdateDepartmentRemoveParent() {
        // Existing department ID with current parent
        Long departmentId = 1L;

        // Mock input DTO with no parent ID
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setParentDepartmentId(null);
        requestDto.setName("Updated Department");

        // Mock existing department entity with parent
        Department existingDepartment = new Department();
        existingDepartment.setId(departmentId);
        existingDepartment.setName("Department");

        // Mock parent department entity
        Department parentDepartment = new Department();
        parentDepartment.setId(2L);
        parentDepartment.setName("Parent Department");

        existingDepartment.setParentDepartment(parentDepartment);

        // Mock repository behavior (find existing department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        // Mock repository behavior (save updated department)
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // Mock mapper behavior
        when(departmentMapper.toDto(existingDepartment)).thenReturn(new DepartmentResponseDto());

        // Call service method
        DepartmentResponseDto updatedDto = departmentService.updateDepartment(departmentId, requestDto);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(existingDepartment);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDto(existingDepartment);

        // Assertions
        assertNotNull(updatedDto);
        assertNull(existingDepartment.getParentDepartment());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateDepartment}.
     * Tests handling of circular dependency detection when updating department parent.
     */
    @Test
    public void testUpdateDepartmentCircularDependency() {
        // Prepare departments
        Department department = new Department();
        department.setId(1L);
        department.setName("Child Department");

        Department parentDepartment = new Department();
        parentDepartment.setId(2L);
        parentDepartment.setName("Parent Department");

        department.setParentDepartment(parentDepartment);
        parentDepartment.addSubDepartment(department);

        // Mock repository behavior
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(parentDepartment));

        // Prepare update request with circular dependency
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setParentDepartmentId(1L);
        requestDto.setName("Parent Department");

        // Call service method and assert circular dependency handling
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> departmentService.updateDepartment(2L, requestDto));
        assertEquals("Circular dependency detected.", exception.getMessage());

        // Verify repository method not called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateDepartment}.
     * Tests handling of updating department with a null ID.
     * Expected behavior is throwing an {@link EntityNotFoundException}.
     */
    @Test
    public void testUpdateDepartmentNullId() {
        // Prepare a request DTO with a valid parent department ID and name
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setParentDepartmentId(1L);
        requestDto.setName("Department1");

        // Call the service method with null department ID and assert EntityNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> departmentService.updateDepartment(null, requestDto));
        assertEquals("Department not found with id: null", exception.getMessage());

        // Verify that findById method of departmentRepository was called once with any argument
        verify(departmentRepository, times(1)).findById(any());

        // Verify that save method of departmentRepository was never called
        verify(departmentRepository, never()).save(any());
    }


    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateDepartment}.
     * Tests handling of updating department with a non-existent ID.
     * Expected behavior is throwing an {@link EntityNotFoundException}.
     */
    @Test
    public void testUpdateDepartmentNonExistentId() {
        // Prepare a request DTO with a valid parent department ID and name
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setParentDepartmentId(1L);
        requestDto.setName("Department1");

        // Mock repository behavior to return Optional.empty() for any department ID
        when(departmentRepository.findById(any())).thenReturn(Optional.empty());

        // Call the service method with a non-existent department ID and assert EntityNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> departmentService.updateDepartment(100L, requestDto));
        assertEquals("Department not found with id: 100", exception.getMessage());

        // Verify that findById method of departmentRepository was called once with ID 100
        verify(departmentRepository, times(1)).findById(100L);

        // Verify that save method of departmentRepository was never called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#deleteDepartment}.
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
        departmentService.deleteDepartment(departmentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).delete(existingDepartment);
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#deleteDepartment}.
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
        Department parentDepartment = new Department();
        parentDepartment.setId(2L);
        parentDepartment.setName("Parent Department");
        existingDepartment.setParentDepartment(parentDepartment);

        // Mock repository behavior (find existing department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));

        // Call service method
        departmentService.deleteDepartment(departmentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).delete(existingDepartment);

        // Verify parent department's sub-departments list
        assertTrue(parentDepartment.getSubDepartments().isEmpty());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#deleteDepartment}.
     * Tests deletion of a non-existent department.
     */
    @Test
    public void testDeleteNonExistentDepartment() {
        // Non-existent department ID
        Long departmentId = 100L;

        // Mock repository behavior (department not found)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // Call service method and expect EntityNotFoundException
        assertThrows(DepartmentNotFoundException.class, () -> departmentService.deleteDepartment(departmentId));

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).delete(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getSubDepartments}.
     * Tests retrieval of sub-departments.
     */
    @Test
    public void testGetSubDepartments() {
        // Existing parent department ID
        Long parentId = 1L;

        // Mock parent department entity with sub-departments
        Department parentDepartment = new Department();
        parentDepartment.setId(parentId);
        parentDepartment.setName("Parent Department");

        Department subDepartment1 = new Department();
        subDepartment1.setId(2L);
        subDepartment1.setName("Sub Department 1");

        Department subDepartment2 = new Department();
        subDepartment2.setId(3L);
        subDepartment2.setName("Sub Department 2");

        parentDepartment.addSubDepartment(subDepartment1);
        parentDepartment.addSubDepartment(subDepartment2);

        // Mock repository behavior (find parent department)
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));

        // Mock mapper behavior (toDtoList)
        when(departmentMapper.toDtoList(parentDepartment.getSubDepartments())).thenReturn(Arrays.asList(
                DepartmentResponseDto.builder().id(subDepartment1.getId()).name(subDepartment1.getName()).build(),
                DepartmentResponseDto.builder().id(subDepartment2.getId()).name(subDepartment2.getName()).build())
        );

        // Call service method
        List<DepartmentResponseDto> subDepartments = departmentService.getSubDepartments(parentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(parentId);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDtoList(parentDepartment.getSubDepartments());

        // Assertions
        assertNotNull(subDepartments);
        assertEquals(2, subDepartments.size());
        assertEquals("Sub Department 1", subDepartments.get(0).getName());
        assertEquals("Sub Department 2", subDepartments.get(1).getName());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getSubDepartments}.
     * Tests retrieval of sub-departments for a parent department with no sub-departments.
     */
    @Test
    public void testGetSubDepartmentsWithNoSubDepartments() {
        // Existing parent department ID with no sub-departments
        Long parentId = 1L;

        // Mock parent department entity with no sub-departments
        Department parentDepartment = new Department();
        parentDepartment.setId(parentId);
        parentDepartment.setName("Parent Department");

        // Mock repository behavior (find parent department)
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));

        // Mock mapper behavior (toDtoList)
        when(departmentMapper.toDtoList(parentDepartment.getSubDepartments())).thenReturn(Collections.emptyList());

        // Call service method
        List<DepartmentResponseDto> subDepartments = departmentService.getSubDepartments(parentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(parentId);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDtoList(parentDepartment.getSubDepartments());

        // Assertions
        assertNotNull(subDepartments);
        assertTrue(subDepartments.isEmpty());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getSubDepartments}.
     * Tests retrieval of sub-departments for a non-existent parent department.
     */
    @Test
    public void testGetSubDepartmentsWithNonExistentParent() {
        // Non-existent parent department ID
        Long parentId = 100L;

        // Mock repository behavior (parent department not found)
        when(departmentRepository.findById(parentId)).thenReturn(Optional.empty());

        // Call service method and expect EntityNotFoundException
        assertThrows(ParentDepartmentNotFoundException.class, () -> departmentService.getSubDepartments(parentId));

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(parentId);

        // Verify mapper method not called
        verify(departmentMapper, never()).toDtoList(any());
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

        Department parentDepartment = new Department();
        parentDepartment.setId(2L);
        parentDepartment.setName("Parent Department");

        department.setParentDepartment(parentDepartment);

        // Mock repository behavior (find department)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        // Mock mapper behavior (toDtoList)
        when(departmentMapper.toDtoList(any())).thenReturn(Collections.singletonList( DepartmentResponseDto.builder().id(parentDepartment.getId()).name(parentDepartment.getName()).build()));

        // Call service method
        List<DepartmentResponseDto> ancestors = departmentService.getAncestors(departmentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);

        // Verify mapper method called
        verify(departmentMapper, times(1)).toDtoList(any());

        // Assertions
        assertNotNull(ancestors);
        assertEquals(1, ancestors.size());
        assertEquals("Parent Department", ancestors.get(0).getName());
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

        // Call service method
        List<DepartmentResponseDto> ancestors = departmentService.getAncestors(departmentId);

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);

        // Verify mapper method not called (no ancestors)
        verify(departmentMapper, never()).toDtoList(any());

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
        Long departmentId = 100L;

        // Mock repository behavior (department not found)
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // Call service method and expect EntityNotFoundException
        assertThrows(DepartmentNotFoundException.class, () -> departmentService.getAncestors(departmentId));

        // Verify repository method called
        verify(departmentRepository, times(1)).findById(departmentId);

        // Verify mapper method not called (no ancestors)
        verify(departmentMapper, never()).toDtoList(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#updateDepartment}.
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
        int numSubDepartments = 10000;
        Department parentDepartment = new Department();
        parentDepartment.setId(1L);
        parentDepartment.setName("ParentDepartment");

        List<Department> subDepartments = new ArrayList<>();
        for (int i = 0; i < numSubDepartments; i++) {
            Department subDepartment = new Department();
            subDepartment.setId((long) (i + 2)); // Start IDs from 2 onwards
            subDepartment.setName("SubDepartment" + (i + 1));
            subDepartment.setParentDepartment(parentDepartment); // Set parent department
            subDepartments.add(subDepartment);
        }
        parentDepartment.setSubDepartments(subDepartments);

        // Mock repository behavior to return parent department and its sub-departments
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parentDepartment));

        // Mock repository behavior to return empty optional for parent department with ID 2
        when(departmentRepository.findById(2L)).thenReturn(Optional.empty());

        // Prepare update request with circular dependency (setting one of the sub-departments as parent)
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setParentDepartmentId(2L); // Set one of the sub-departments as parent
        requestDto.setName("UpdatedParentDepartment");

        // Call the service method and expect EntityNotFoundException due to missing parent department
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.updateDepartment(1L, requestDto);
        });

        // Verify that the exception message indicates the parent department was not found
        assertEquals("Parent department not found with id: 2", exception.getMessage());

        // Verify repository method not called
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Test case for the getAllDepartments method of AdjacencyListDepartmentService.
     * Verifies that the method retrieves all departments and maps them to DTOs correctly.
     */
    @Test
    public void testGetAllDepartments() {
        // Mock behavior for departmentRepository.findAll()
        List<Department> mockDepartments = Arrays.asList(
                Department.builder().id(1L).name("Department 1").parentDepartment(null).build(),
                Department.builder().id(2L).name("Department 2").parentDepartment(null).build(),
                Department.builder().id(3L).name("Department 3").parentDepartment(null).build()
                // Add more departments as needed
        );
        when(departmentRepository.findAll()).thenReturn(mockDepartments);

        // Mock behavior for departmentMapper.toDtoList()
        List<DepartmentResponseDto> mockDtoList = Arrays.asList(
                DepartmentResponseDto.builder().id(1L).name("Department 1").build(),
                DepartmentResponseDto.builder().id(2L).name("Department 2").build(),
                DepartmentResponseDto.builder().id(3L).name("Department 3").build()
                // Add corresponding DTOs as needed
        );
        when(departmentMapper.toDtoList(mockDepartments)).thenReturn(mockDtoList);

        // Call the method under test
        List<DepartmentResponseDto> responseDtoList = departmentService.getAllDepartments();

        // Assert the size of the response list
        assertEquals(3, responseDtoList.size(), "Expected 3 departments in the response");
    }

    /**
     * Test case for an empty database scenario.
     */
    @Test
    public void testGetAllDepartments_EmptyDatabase() {
        // Mock behavior for departmentRepository.findAll() returning an empty list
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());

        // Mock behavior for departmentMapper.toDtoList() returning an empty list
        when(departmentMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // Call the method under test
        List<DepartmentResponseDto> responseDtoList = departmentService.getAllDepartments();

        // Assert the size of the response list is zero
        assertEquals(0, responseDtoList.size(), "Expected 0 departments in the response");
    }

    /**
     * Test case for a database failure scenario.
     */
    @Test
    public void testGetAllDepartments_DatabaseFailure() {
        // Mock behavior for departmentRepository.findAll() to throw an exception
        when(departmentRepository.findAll()).thenThrow(new RuntimeException("Database connection error"));

        // Call the method under test and expect an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            departmentService.getAllDepartments();
        });

        // Assert the exception message
        assertEquals("Database connection error", exception.getMessage());
    }

    /**
     * Test case for a mapping failure scenario.
     */
    @Test
    public void testGetAllDepartments_MappingFailure() {
        // Mock behavior for departmentRepository.findAll() returning a list
        List<Department> mockDepartments = Arrays.asList(
                Department.builder().id(1L).name("Department 1").parentDepartment(null).build()
        );
        when(departmentRepository.findAll()).thenReturn(mockDepartments);

        // Mock behavior for departmentMapper.toDtoList() to throw an exception
        when(departmentMapper.toDtoList(mockDepartments)).thenThrow(new RuntimeException("Mapping error"));

        // Call the method under test and expect an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            departmentService.getAllDepartments();
        });

        // Assert the exception message
        assertEquals("Mapping error", exception.getMessage());
    }

    /**
     * Test case for a large number of departments scenario.
     */
    @Test
    public void testGetAllDepartments_LargeNumberOfDepartments() {
        // Create a large number of mock departments
        List<Department> mockDepartments = new ArrayList<>();
        for (long i = 1; i <= 10000; i++) {
            mockDepartments.add(Department.builder().id(i).name("Department " + i).parentDepartment(null).build());
        }
        when(departmentRepository.findAll()).thenReturn(mockDepartments);

        // Create corresponding DTOs
        List<DepartmentResponseDto> mockDtoList = new ArrayList<>();
        for (long i = 1; i <= 10000; i++) {
            mockDtoList.add(DepartmentResponseDto.builder().id(i).name("Department " + i).build());
        }
        when(departmentMapper.toDtoList(mockDepartments)).thenReturn(mockDtoList);

        // Call the method under test
        List<DepartmentResponseDto> responseDtoList = departmentService.getAllDepartments();

        // Assert the size of the response list
        assertEquals(10000, responseDtoList.size(), "Expected 10000 departments in the response");
    }

    /**
     * Test case for the getDepartmentById method of AdjacencyListDepartmentService.
     * Verifies that the method retrieves a department by its ID and maps it to a DTO correctly.
     */
    @Test
    public void testGetDepartmentById_ValidId() {
        // Mock behavior for departmentRepository.findById()
        Department mockDepartment = Department.builder().id(1L).name("Department 1").parentDepartment(null).build();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));

        // Mock behavior for departmentMapper.toDto()
        DepartmentResponseDto mockDto = DepartmentResponseDto.builder().id(1L).name("Department 1").build();
        when(departmentMapper.toDto(mockDepartment)).thenReturn(mockDto);

        // Call the method under test
        DepartmentResponseDto responseDto = departmentService.getDepartmentById(1L);

        // Assert the response
        assertEquals(mockDto, responseDto, "Expected department DTO with ID 1");
    }

    /**
     * Test case for the getDepartmentById method when the department does not exist.
     * Verifies that the method throws an EntityNotFoundException.
     */
    @Test
    public void testGetDepartmentById_NonExistentId() {
        // Mock behavior for departmentRepository.findById() to return an empty Optional
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the method under test and expect an exception
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDepartmentById(1L);
        });

        // Assert the exception message
        assertEquals("Department not found with id: 1", exception.getMessage());
    }

    /**
     * Test case for the getDepartmentById method when the mapping to DTO fails.
     * Verifies that the method handles the mapping failure.
     */
    @Test
    public void testGetDepartmentById_MappingFailure() {
        // Mock behavior for departmentRepository.findById()
        Department mockDepartment = Department.builder().id(1L).name("Department 1").parentDepartment(null).build();
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDepartment));

        // Mock behavior for departmentMapper.toDto() to throw an exception
        when(departmentMapper.toDto(mockDepartment)).thenThrow(new RuntimeException("Mapping error"));

        // Call the method under test and expect an exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            departmentService.getDepartmentById(1L);
        });

        // Assert the exception message
        assertEquals("Mapping error", exception.getMessage());
    }

    /**
     * Test case for the getParentDepartment method of AdjacencyListDepartmentService.
     * Verifies that the method retrieves the parent department and maps it to a DTO correctly.
     */
    @Test
    public void testGetParentDepartment_ValidIdWithParent() {
        // Mock behavior for departmentRepository.findById()
        Department parentDepartment = Department.builder().id(1L).name("Parent Department").parentDepartment(null).build();
        Department mockDepartment = Department.builder().id(2L).name("Child Department").parentDepartment(parentDepartment).build();
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(mockDepartment));

        // Mock behavior for departmentMapper.toDto()
        DepartmentResponseDto mockParentDto = DepartmentResponseDto.builder().id(1L).name("Parent Department").build();
        when(departmentMapper.toDto(parentDepartment)).thenReturn(mockParentDto);

        // Call the method under test
        DepartmentResponseDto responseDto = departmentService.getParentDepartment(2L);

        // Assert the response
        assertEquals(mockParentDto, responseDto, "Expected parent department DTO with ID 1");
    }

    /**
     * Test case for the getParentDepartment method when the department does not have a parent.
     * Verifies that the method throws an EntityNotFoundException.
     */
    @Test
    public void testGetParentDepartment_ValidIdWithoutParent() {
        // Mock behavior for departmentRepository.findById()
        Department mockDepartment = Department.builder().id(2L).name("Child Department").parentDepartment(null).build();
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(mockDepartment));

        // Call the method under test and expect an exception
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(2L);
        });

        // Assert the exception message
        assertEquals("Department with id: 2 has no parent department", exception.getMessage());
    }

    /**
     * Test case for the getParentDepartment method when the department does not exist.
     * Verifies that the method throws an EntityNotFoundException.
     */
    @Test
    public void testGetParentDepartment_NonExistentId() {
        // Mock behavior for departmentRepository.findById() to return an empty Optional
        when(departmentRepository.findById(2L)).thenReturn(Optional.empty());

        // Call the method under test and expect an exception
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(2L);
        });

        // Assert the exception message
        assertEquals("Department not found with id: 2", exception.getMessage());
    }

    /**
     * Test case for the getDescendants method of AdjacencyListDepartmentService.
     * Verifies that the method retrieves all descendants and maps them to DTOs correctly.
     */
    @Test
    public void testGetDescendants_ValidIdWithDescendants() {
        // Mock behavior for departmentRepository.findById()
        Department department1 = Department.builder().id(1L).name("Department 1").build();
        Department department2 = Department.builder().id(2L).name("Department 2").parentDepartment(department1).build();
        Department department3 = Department.builder().id(3L).name("Department 3").parentDepartment(department1).build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department1));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department2));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department3));

        // Mock behavior for departmentMapper.toDtoList()
        List<DepartmentResponseDto> mockDtoList = Arrays.asList(
                DepartmentResponseDto.builder().id(1L).name("Department 1").build(),
                DepartmentResponseDto.builder().id(2L).name("Department 2").build(),
                DepartmentResponseDto.builder().id(3L).name("Department 3").build()
                // Add corresponding DTOs as needed
        );
        when(departmentMapper.toDtoList(anyList())).thenReturn(mockDtoList);

        // Call the method under test
        List<DepartmentResponseDto> responseDtoList = departmentService.getDescendants(1L);

        // Assert the size of the response list
        assertEquals(3, responseDtoList.size(), "Expected 3 departments in the response");
    }

    /**
     * Test case for the getDescendants method of AdjacencyListDepartmentService.
     * Verifies that an exception is thrown if the department ID is not found.
     */
    @Test
    public void testGetDescendants_DepartmentNotFound() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // Call the method under test and expect an exception
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDescendants(1L);
        });

        // Assert the exception message
        assertEquals("Department not found with id: 1", exception.getMessage());
    }

    /**
     * Test case for the updateDepartment method of AdjacencyListDepartmentService.
     * Verifies that the method updates the department's information and removes it from parent's sub-departments correctly.
     */
    @Test
    public void testUpdateDepartment_removeSubDepartments() {
        // Create a parent department and a department to update
        Department parentDepartment = Department.builder().id(1L).name("Parent Department").subDepartments(new ArrayList<>()).build();
        Department departmentToUpdate = Department.builder().id(2L).name("Department to Update").parentDepartment(parentDepartment).build();

        // Add departmentToUpdate to parentDepartment's subDepartments
        parentDepartment.addSubDepartment(departmentToUpdate);

        // Mock behavior for departmentRepository.findById()
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(departmentToUpdate));

        // Update the department's name (this is done implicitly when calling updateDepartment)
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated Department Name");

        // Mock behavior for departmentRepository.findById() to return parentDepartment
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parentDepartment));

        // Assert the parent department's subDepartments list size before update
        assertEquals(1, parentDepartment.getSubDepartments().size(), "Expected 1 sub-department before update");

        // Call the method under test
        departmentService.updateDepartment(2L, requestDto);

        // Assert that the department's name was updated (this is indirectly tested by checking the return value)
        assertEquals("Updated Department Name", departmentToUpdate.getName());

        // Assert the parent department's subDepartments list size after update
        assertEquals(0, parentDepartment.getSubDepartments().size(), "Expected 0 sub-departments after removal");

        // Assert that departmentToUpdate is no longer in parentDepartment's subDepartments
        assertFalse(parentDepartment.getSubDepartments().contains(departmentToUpdate));
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#handleSubDepartmentsOnDelete}.
     * Tests deletion of a department with sub-departments.
     */
    @Test
    public void testHandleSubDepartmentsOnDelete_WithSubDepartments() {
        // Create a parent department with sub-departments
        Department parentDepartment = new Department();
        parentDepartment.setId(1L);
        parentDepartment.setName("Parent Department");

        Department subDepartment1 = new Department();
        subDepartment1.setId(2L);
        subDepartment1.setName("Sub Department 1");
        subDepartment1.setParentDepartment(parentDepartment);

        Department subDepartment2 = new Department();
        subDepartment2.setId(3L);
        subDepartment2.setName("Sub Department 2");
        subDepartment2.setParentDepartment(parentDepartment);

        parentDepartment.addSubDepartment(subDepartment1);
        parentDepartment.addSubDepartment(subDepartment2);

        // Mock behavior for departmentRepository.delete
        doNothing().when(departmentRepository).delete(any());

        // Call the method under test
        departmentService.handleSubDepartmentsOnDelete(parentDepartment);

        // Verify that each sub-department's parent reference is set to null
        assertEquals(null, subDepartment1.getParentDepartment());
        assertEquals(null, subDepartment2.getParentDepartment());

        // Verify that delete method was called for each sub-department
        verify(departmentRepository, times(1)).delete(subDepartment1);
        verify(departmentRepository, times(1)).delete(subDepartment2);

        // Verify that sub-departments list is cleared
        assertEquals(0, parentDepartment.getSubDepartments().size());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#handleSubDepartmentsOnDelete}.
     * Tests deletion of a department without sub-departments.
     */
    @Test
    public void testHandleSubDepartmentsOnDelete_WithoutSubDepartments() {
        // Create a parent department without any sub-departments
        Department parentDepartment = new Department();
        parentDepartment.setId(1L);
        parentDepartment.setName("Parent Department");

        // Mock behavior for departmentRepository.delete
        doNothing().when(departmentRepository).delete(any());

        // Call the method under test
        departmentService.handleSubDepartmentsOnDelete(parentDepartment);

        // Verify that sub-departments list is cleared (should already be empty)
        assertEquals(0, parentDepartment.getSubDepartments().size());

        // Verify that delete method was not called because there were no sub-departments
        verify(departmentRepository, never()).delete(any());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#handleSubDepartmentsOnDelete}.
     * Tests deletion of a department with a large number of sub-departments.
     */
    @Test
    public void testHandleSubDepartmentsOnDelete_LargeNumberOfSubDepartments() {
        // Create a parent department with a large number of sub-departments
        Department parentDepartment = new Department();
        parentDepartment.setId(1L);
        parentDepartment.setName("Parent Department");

        List<Department> subDepartments = new ArrayList<>();
        int numSubDepartments = 100;
        for (int i = 0; i < numSubDepartments; i++) {
            Department subDepartment = new Department();
            subDepartment.setId((long) (i + 2));
            subDepartment.setName("Sub Department " + (i + 1));
            subDepartment.setParentDepartment(parentDepartment);
            subDepartments.add(subDepartment);
            parentDepartment.addSubDepartment(subDepartment);
        }

        // Mock behavior for departmentRepository.delete
        doNothing().when(departmentRepository).delete(any());

        // Call the method under test
        departmentService.handleSubDepartmentsOnDelete(parentDepartment);

        // Verify that each sub-department's parent reference is set to null
        for (Department subDept : subDepartments) {
            assertEquals(null, subDept.getParentDepartment());
        }

        // Verify that delete method was called for each sub-department
        verify(departmentRepository, times(numSubDepartments)).delete(any());

        // Verify that sub-departments list is cleared
        assertEquals(0, parentDepartment.getSubDepartments().size());
    }
    /**
     * Unit test for {@link AdjacencyListDepartmentService#getAllDescendants}.
     * Tests fetching descendants when sub-departments list is null.
     */
    @Test
    public void testGetAllDescendants_NullSubDepartments() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Department 1");

        List<Department> descendants = departmentService.getAllDescendants(department);

        // Expecting an empty list of descendants when sub-departments list is null
        assertEquals(0, descendants.size());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getAllDescendants}.
     * Tests fetching descendants when sub-departments list is empty.
     */
    @Test
    public void testGetAllDescendants_EmptySubDepartments() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Department 1");
        department.setSubDepartments(new ArrayList<>()); // Empty sub-departments list

        List<Department> descendants = departmentService.getAllDescendants(department);

        // Expecting an empty list of descendants when sub-departments list is empty
        assertEquals(0, descendants.size());
    }

    /**
     * Unit test for {@link AdjacencyListDepartmentService#getAllDescendants}.
     * Tests fetching descendants when there are multiple levels of sub-departments.
     */
    @Test
    public void testGetAllDescendants_MultipleLevels() {
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
        department1.addSubDepartment(department2);
        department1.addSubDepartment(department3);
        department2.addSubDepartment(subDepartment1);
        department2.addSubDepartment(subDepartment2);
        subDepartment1.addSubDepartment(subSubDepartment1);

        List<Department> descendants = departmentService.getAllDescendants(department1);

        // Expecting all descendants to be fetched recursively
        assertEquals(5, descendants.size()); // Including department2, department3, subDepartment1, subDepartment2, subSubDepartment1
    }
}
