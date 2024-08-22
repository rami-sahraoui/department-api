package tn.engn.assignmentapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tn.engn.assignmentapi.dto.*;
import tn.engn.assignmentapi.exception.AssignmentAlreadyExistsException;
import tn.engn.assignmentapi.exception.AssignmentNotFoundException;
import tn.engn.assignmentapi.exception.MetadataNotFoundException;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.assignmentapi.model.AssignmentMetadata;
import tn.engn.assignmentapi.model.DepartmentEmployeeAssignment;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link DepartmentEmployeeAssignmentServiceIT} class.
 * <p>
 * These tests verify the behavior of the service's methods, including cases where
 * assignable entities exist, do not exist, or have no hierarchical entities assigned.
 * </p>
 */
public class DepartmentEmployeeAssignmentServiceTest {

    @Mock
    private HierarchyBaseRepository<Department> hierarchicalEntityRepository;

    @Mock
    private EmployeeRepository assignableEntityRepository;

    @Mock
    private AssignmentRepository<Department, Employee, DepartmentEmployeeAssignment> assignmentRepository;

    @Mock
    private AssignmentMetadataRepository assignmentMetadataRepository;

    @Mock
    private HierarchyMapper<Department, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper;

    @Mock
    private AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper;

    @Mock
    private AssignmentMapper<Department, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper;

    @InjectMocks
    private DepartmentEmployeeAssignmentService departmentEmployeeAssignmentService;

    /**
     * Initializes mock objects before each test execution.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Tests the successful retrieval of hierarchical entities for a given assignable entity.
     * <p>
     * Given an assignable entity ID that exists, when hierarchical entities are requested,
     * then the service should return the corresponding hierarchical entities as DTOs.
     * </p>
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_Success() {
        // Given
        Long assignableEntityId = 1L;
        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        Department department = Department.builder()
                .id(2L)
                .name("Engineering")
                .build();

        List<Department> hierarchicalEntities = Arrays.asList(department);
        HierarchyResponseDto responseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any())).thenReturn(hierarchicalEntities);
        when(hierarchyMapper.toDto(department, false)).thenReturn(responseDto);

        // When
        List<HierarchyResponseDto> result = departmentEmployeeAssignmentService.getHierarchicalEntitiesForAssignableEntity(assignableEntityId);

        // Then
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(department.getId());
        assertThat(result.get(0).getName()).isEqualTo(department.getName());
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any());
        verify(hierarchyMapper).toDto(department, false);
    }

    /**
     * Tests the case where the assignable entity does not exist.
     * <p>
     * Given a non-existent assignable entity ID, when hierarchical entities are requested,
     * then the service should throw an {@link EntityNotFoundException}.
     * </p>
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_EntityNotFound() {
        // Given
        Long assignableEntityId = 1L;

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getHierarchicalEntitiesForAssignableEntity(assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository, never()).findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Tests the case where the assignable entity exists but has no hierarchical entities assigned.
     * <p>
     * Given an assignable entity ID with no assigned hierarchical entities, when hierarchical entities are requested,
     * then the service should return an empty list.
     * </p>
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_NoHierarchicalEntities() {
        // Given
        Long assignableEntityId = 1L;
        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        List<Department> hierarchicalEntities = Collections.emptyList();

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any())).thenReturn(hierarchicalEntities);

        // When
        List<HierarchyResponseDto> result = departmentEmployeeAssignmentService.getHierarchicalEntitiesForAssignableEntity(assignableEntityId);

        // Then
        assertThat(result).isNotNull().isEmpty();
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Tests the successful retrieval of hierarchical entities associated with a specified assignable entity
     * with pagination.
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_WithPagination_Success() {
        // Given
        Long assignableEntityId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        Department department = Department.builder()
                .id(2L)
                .name("Engineering")
                .build();

        Page<Department> hierarchicalEntitiesPage = new PageImpl<>(Collections.singletonList(department));

        HierarchyResponseDto responseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        PaginatedResponseDto paginatedResponseDto = PaginatedResponseDto.builder()
                .content(Arrays.asList(responseDto))
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(hierarchicalEntitiesPage.getTotalElements())
                .totalPages(hierarchicalEntitiesPage.getTotalPages())
                .build();

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any(), eq(pageable)))
                .thenReturn(hierarchicalEntitiesPage);
        when(hierarchyMapper.toDtoPage(hierarchicalEntitiesPage)).thenReturn(paginatedResponseDto);

        // When
        PaginatedResponseDto<HierarchyResponseDto> result = departmentEmployeeAssignmentService
                .getHierarchicalEntitiesForAssignableEntity(assignableEntityId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(department.getId());
        assertThat(result.getContent().get(0).getName()).isEqualTo(department.getName());
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any(), eq(pageable));
        verify(hierarchyMapper).toDtoPage(hierarchicalEntitiesPage);
    }

    /**
     * Tests the scenario where the assignable entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_WithPagination_EntityNotFound() {
        // Given
        Long assignableEntityId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .getHierarchicalEntitiesForAssignableEntity(assignableEntityId, pageable))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository, never()).findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any(), eq(pageable));
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Tests the scenario where no hierarchical entities are associated with the specified assignable entity.
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_WithPagination_NoHierarchicalEntities() {
        // Given
        Long assignableEntityId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        Page<Department> emptyPage = Page.empty();

        PaginatedResponseDto paginatedResponseDto = PaginatedResponseDto.builder()
                .content(Collections.emptyList())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(emptyPage.getTotalElements())
                .totalPages(emptyPage.getTotalPages())
                .build();

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any(), eq(pageable)))
                .thenReturn(emptyPage);
        when(hierarchyMapper.toDtoPage(emptyPage)).thenReturn(paginatedResponseDto);

        // When
        PaginatedResponseDto<HierarchyResponseDto> result = departmentEmployeeAssignmentService
                .getHierarchicalEntitiesForAssignableEntity(assignableEntityId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any(), eq(pageable));
        verify(hierarchyMapper).toDtoPage(any());
    }

    /**
     * Tests the successful assignment of an assignable entity to a hierarchical entity with metadata.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .metadata(
                        Collections.singleton(
                                AssignmentMetadata.builder()
                                        .key("Role")
                                        .value("Manager")
                                        .build()
                        )
                )
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(department, employee)).thenReturn(false);
        when(assignmentRepository.save(any())).thenReturn(assignment);
        when(hierarchyMapper.toDto(department, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(employee)).thenReturn(employeeResponseDto);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos);

        // Then
        assertThat(result).isNotNull();

        assertThat(result.getHierarchicalEntity()).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(hierarchicalEntityId);

        assertThat(result.getAssignableEntity()).isNotNull();
        assertThat(result.getAssignableEntity().getId()).isEqualTo(assignableEntityId);

        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).hasSize(1);
        assertThat(result.getMetadata().get(0).getKey()).isEqualTo("Role");
        assertThat(result.getMetadata().get(0).getValue()).isEqualTo("Manager");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).existsByHierarchicalEntityAndAssignableEntity(department, employee);
        verify(hierarchyMapper).toDto(any(), anyBoolean());
        verify(assignableEntityMapper).toDto(any());
        verify(assignmentRepository).save(any());
    }

    /**
     * Tests the successful assignment of an assignable entity to a hierarchical entity without metadata.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_Success_withoutMetadata() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(department, employee)).thenReturn(false);
        when(assignmentRepository.save(any())).thenReturn(assignment);
        when(hierarchyMapper.toDto(department, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(employee)).thenReturn(employeeResponseDto);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, null);

        // Then
        assertThat(result).isNotNull();

        assertThat(result.getHierarchicalEntity()).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(hierarchicalEntityId);

        assertThat(result.getAssignableEntity()).isNotNull();
        assertThat(result.getAssignableEntity().getId()).isEqualTo(assignableEntityId);

        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).isEmpty();

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).existsByHierarchicalEntityAndAssignableEntity(department, employee);
        verify(hierarchyMapper).toDto(any(), anyBoolean());
        verify(assignableEntityMapper).toDto(any());
        verify(assignmentRepository).save(any());
    }

    /**
     * Tests the scenario where the hierarchical entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository, never()).findById(assignableEntityId);
        verify(assignmentRepository, never()).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where the assignable entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_AssignableEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository, never()).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where an assignment already exists between the specified hierarchical entity
     * and assignable entity, and an AssignmentAlreadyExistsException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_AssignmentAlreadyExists() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(department, employee)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Assignment already exists between the specified hierarchical entity and assignable entity");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).existsByHierarchicalEntityAndAssignableEntity(department, employee);
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where metadata with a null key is provided, and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_NullKey() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key(null)
                        .value("Manager")
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata key cannot be null or empty.");

        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where metadata with an empty key is provided, and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_EmptyKey() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("")
                        .value("Manager")
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata key cannot be null or empty.");

        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where metadata with a null value is provided, and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_NullValue() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value(null)
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata value cannot be null or empty.");

        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where the metadata list contains duplicate keys, and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_DuplicateMetadataKeys() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Arrays.asList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build(),
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Lead")
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata list contains duplicate keys.");

        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where a metadata key exceeds the length limit of 255 characters,
     * and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_KeyLengthExceedsLimit() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("a".repeat(256)) // Key length exceeds the 255-character limit
                        .value("Manager")
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata key length cannot exceed 255 characters.");

        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where a metadata key contains invalid characters,
     * and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_InvalidKeyCharacters() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Invalid Key!") // Contains space and exclamation mark, which are invalid
                        .value("Manager")
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata key contains invalid characters.");

        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where a metadata value exceeds the length limit of 1024 characters,
     * and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_ValueLengthExceedsLimit() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("a".repeat(1025)) // Value length exceeds the 1024-character limit
                        .build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata value length cannot exceed 1024 characters.");

        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the successful removal of an assignable entity from a hierarchical entity.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();

        Set<Employee> employees = new HashSet<>();
        employees.add(employee);
        department.setEmployees(employees);

        Set<Department> departments = new HashSet<>();
        departments.add(department);
        employee.setDepartments(departments);

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .metadata(
                        Collections.singleton(
                                AssignmentMetadata.builder()
                                        .key("Role")
                                        .value("Manager")
                                        .build()
                        )
                )
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(department, employee)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any())).thenReturn(department);
        doNothing().when(assignmentMetadataRepository).delete(any());
        when(hierarchyMapper.toDto(department, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(employee)).thenReturn(employeeResponseDto);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(hierarchicalEntityId, assignableEntityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(hierarchicalEntityId);
        assertThat(result.getAssignableEntity().getId()).isEqualTo(assignableEntityId);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(department, employee);
        verify(assignmentMetadataRepository).delete(any());
        verify(assignmentRepository).delete(assignment);
        verify(hierarchicalEntityRepository).save(department);
    }

    /**
     * Tests the scenario where the hierarchical entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository, never()).findById(assignableEntityId);
        verify(assignmentRepository, never()).findByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignmentMetadataRepository, never()).delete(any());
        verify(assignmentRepository, never()).delete(any());
        verify(hierarchicalEntityRepository, never()).save(any());
    }

    /**
     * Tests the scenario where the assignable entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_AssignableEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository, never()).findByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignmentMetadataRepository, never()).delete(any());
        verify(assignmentRepository, never()).delete(any());
        verify(hierarchicalEntityRepository, never()).save(any());
    }

    /**
     * Tests the scenario where the assignment does not exist between the hierarchical entity and the assignable entity.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_AssignmentNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(department, employee)).thenReturn(Optional.empty());

        // When
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");

        // Then
        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(department, employee);
        verify(assignmentMetadataRepository, never()).delete(any());
        verify(assignmentRepository, never()).delete(any());
        verify(hierarchicalEntityRepository, never()).save(department);
    }

    /**
     * Tests the successful assignment of multiple assignable entities to a single hierarchical entity
     * with metadata for each assignment.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        Employee employee1 = Employee.builder()
                .id(assignableEntityIds.get(0))
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        Employee employee2 = Employee.builder()
                .id(assignableEntityIds.get(1))
                .firstName("Jane")
                .lastName("Smith")
                .departments(new HashSet<>())
                .build();

        Employee employee3 = Employee.builder()
                .id(assignableEntityIds.get(2))
                .firstName("Emily")
                .lastName("Jones")
                .departments(new HashSet<>())
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);

        List<EmployeeResponseDto> employeeResponseDtos = employees.stream()
                .map(emp -> EmployeeResponseDto.builder()
                        .id(emp.getId())
                        .firstName(emp.getFirstName())
                        .lastName(emp.getLastName())
                        .build())
                .collect(Collectors.toList());

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        List<DepartmentEmployeeAssignment> assignments = employees.stream()
                .map(emp -> DepartmentEmployeeAssignment.builder()
                        .hierarchicalEntity(department)
                        .assignableEntity(emp)
                        .metadata(
                                Collections.singleton(
                                        AssignmentMetadata.builder()
                                                .key("Role")
                                                .value("Manager")
                                                .build()
                                )
                        )
                        .build())
                .collect(Collectors.toList());

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findAllById(assignableEntityIds)).thenReturn(employees);
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(any(), any())).thenReturn(false);
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(hierarchyMapper.toDto(department, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(any())).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            return employeeResponseDtos.stream()
                    .filter(dto -> dto.getId().equals(emp.getId()))
                    .findFirst()
                    .orElse(null);
        });

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(hierarchicalEntityId, assignableEntityIds, metadataDtos);

        // Then
        assertThat(result).isNotNull();

        assertThat(result.getHierarchicalEntities()).isNotNull();
        assertThat(result.getHierarchicalEntities().get(0).getId()).isEqualTo(hierarchicalEntityId);

        List<EmployeeResponseDto> resultEmployees = result.getAssignableEntities();
        assertThat(resultEmployees).isNotNull();
        assertThat(resultEmployees).hasSize(3);
        assertThat(resultEmployees).extracting(EmployeeResponseDto::getId).containsExactlyInAnyOrderElementsOf(assignableEntityIds);

        List<AssignmentMetadataResponseDto> resultMetadata = result.getMetadata();
        assertThat(resultMetadata).isNotNull();
        assertThat(result.getMetadata()).hasSize(1);
        assertThat(result.getMetadata().get(0).getKey()).isEqualTo("Role");
        assertThat(result.getMetadata().get(0).getValue()).isEqualTo("Manager");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findAllById(assignableEntityIds);
        verify(assignmentRepository, times(3)).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(hierarchyMapper).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, times(3)).toDto(any());
        verify(hierarchicalEntityRepository).save(any());
        verify(assignmentRepository).saveAll(any());
    }

    /**
     * Tests the scenario where the hierarchical entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(hierarchicalEntityId, assignableEntityIds, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository, never()).findById(any());
        verify(assignmentRepository, never()).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where one or more assignable entities are not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_OneOrMoreAssignableEntitiesNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee employee1 = Employee.builder()
                .id(assignableEntityIds.get(0))
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        Employee employee2 = Employee.builder()
                .id(assignableEntityIds.get(1))
                .firstName("Jane")
                .lastName("Smith")
                .departments(new HashSet<>())
                .build();

        Employee employee3 = Employee.builder()
                .id(assignableEntityIds.get(2))
                .firstName("Emily")
                .lastName("Jones")
                .departments(new HashSet<>())
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee3);

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findAllById(assignableEntityIds)).thenReturn(employees); // missing employee2

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(hierarchicalEntityId, assignableEntityIds, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("One or more assignable entities not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findAllById(assignableEntityIds);
        verify(assignmentRepository, never()).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Tests the scenario where an assignment already exists for one or more of the assignable entities,
     * and an AssignmentAlreadyExistsException is thrown.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_AssignmentAlreadyExists() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee employee1 = Employee.builder()
                .id(assignableEntityIds.get(0))
                .firstName("John")
                .lastName("Doe")
                .build();

        Employee employee2 = Employee.builder()
                .id(assignableEntityIds.get(1))
                .firstName("Jane")
                .lastName("Smith")
                .build();

        Employee employee3 = Employee.builder()
                .id(assignableEntityIds.get(2))
                .firstName("Emily")
                .lastName("Jones")
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findAllById(assignableEntityIds)).thenReturn(employees);
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(any(), any())).thenReturn(true); // Already exists

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(hierarchicalEntityId, assignableEntityIds, null))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Assignment already exists for hierarchical entity ID: " + hierarchicalEntityId
                        + " and assignable entity ID: " + assignableEntityIds.get(0));

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findAllById(assignableEntityIds);
        verify(assignmentRepository).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(assignmentRepository, never()).save(any());
    }

    /**
     * Test case for the successful removal of multiple assignable entities from a hierarchical entity.
     * This test ensures that the method correctly removes the assignable entities, deletes associated
     * assignments and metadata, and returns a populated BulkAssignmentResponseDto.
     */
    @Test
    public void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        Employee employee1 = Employee.builder()
                .id(assignableEntityIds.get(0))
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        Employee employee2 = Employee.builder()
                .id(assignableEntityIds.get(1))
                .firstName("Jane")
                .lastName("Smith")
                .departments(new HashSet<>())
                .build();

        Employee employee3 = Employee.builder()
                .id(assignableEntityIds.get(2))
                .firstName("Emily")
                .lastName("Jones")
                .departments(new HashSet<>())
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);

        List<EmployeeResponseDto> employeeResponseDtos = employees.stream()
                .map(emp -> EmployeeResponseDto.builder()
                        .id(emp.getId())
                        .firstName(emp.getFirstName())
                        .lastName(emp.getLastName())
                        .build())
                .collect(Collectors.toList());

        department.setEmployees(new HashSet<>(employees));

        Set<Department> departments = new HashSet<>();
        departments.add(department);
        employee1.setDepartments(departments);
        employee2.setDepartments(departments);
        employee3.setDepartments(departments);

        // Assuming assignment entities exist for each employee
        List<DepartmentEmployeeAssignment> assignments = employees.stream()
                .map(emp -> DepartmentEmployeeAssignment.builder()
                        .hierarchicalEntity(department)
                        .assignableEntity(emp)
                        .metadata(
                                Collections.singleton(
                                        AssignmentMetadata.builder()
                                                .key("Role")
                                                .value("Manager")
                                                .build()
                                )
                        )
                        .build())
                .collect(Collectors.toList());

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findAllById(assignableEntityIds)).thenReturn(employees);
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(any(), any()))
                .thenReturn(Optional.of(assignments.get(0)))
                .thenReturn(Optional.of(assignments.get(1)))
                .thenReturn(Optional.of(assignments.get(2)));
        when(hierarchyMapper.toDto(department, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(any())).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            return employeeResponseDtos.stream()
                    .filter(dto -> dto.getId().equals(emp.getId()))
                    .findFirst()
                    .orElse(null);
        });

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .bulkRemoveAssignableEntitiesFromHierarchicalEntity(hierarchicalEntityId, assignableEntityIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntities()).hasSize(1);
        assertThat(result.getHierarchicalEntities().get(0).getId()).isEqualTo(hierarchicalEntityId);
        assertThat(result.getAssignableEntities()).hasSize(3);
        assertThat(result.getAssignableEntities().stream().map(EmployeeResponseDto::getId))
                .containsExactlyInAnyOrderElementsOf(assignableEntityIds);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findAllById(assignableEntityIds);
        verify(assignmentRepository, times(3)).delete(any());
        verify(hierarchicalEntityRepository).save(department);
        verify(hierarchyMapper).toDto(department, false);
        verify(assignableEntityMapper, times(3)).toDto(any());
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical
     * entity is not found in the repository.
     */
    @Test
    public void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveAssignableEntitiesFromHierarchicalEntity(hierarchicalEntityId, assignableEntityIds))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository, never()).findAllById(assignableEntityIds);
        verify(assignmentRepository, never()).delete(any());
        verify(hierarchicalEntityRepository, never()).save(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
    }

    /**
     * Test case for handling the scenario where one or more assignable entities are not found.
     * This test ensures that the method throws an EntityNotFoundException when one or more of
     * the provided assignable entity IDs do not correspond to existing entities in the repository.
     */
    @Test
    public void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_OneOrMoreAssignableEntitiesNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee employee1 = Employee.builder()
                .id(assignableEntityIds.get(0))
                .firstName("John")
                .lastName("Doe")
                .build();

        Employee employee2 = Employee.builder()
                .id(assignableEntityIds.get(1))
                .firstName("Jane")
                .lastName("Smith")
                .build();

        // Missing employee3
        List<Employee> employees = Arrays.asList(employee1, employee2);

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findAllById(assignableEntityIds)).thenReturn(employees);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveAssignableEntitiesFromHierarchicalEntity(hierarchicalEntityId, assignableEntityIds))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("One or more assignable entities not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findAllById(assignableEntityIds);
        verify(assignmentRepository, never()).delete(any());
        verify(hierarchicalEntityRepository, never()).save(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
    }

    /**
     * Test case for handling the scenario where an assignment between the hierarchical entity
     * and an assignable entity is not found. This test ensures that the method skips removal
     * actions when no valid assignment is found and returns an appropriate response.
     */
    @Test
    public void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_AssignmentNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        Employee employee1 = Employee.builder()
                .id(assignableEntityIds.get(0))
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        Employee employee2 = Employee.builder()
                .id(assignableEntityIds.get(1))
                .firstName("Jane")
                .lastName("Smith")
                .departments(new HashSet<>())
                .build();

        Employee employee3 = Employee.builder()
                .id(assignableEntityIds.get(2))
                .firstName("Emily")
                .lastName("Jones")
                .departments(new HashSet<>())
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);

        List<EmployeeResponseDto> employeeResponseDtos = employees.stream()
                .map(emp -> EmployeeResponseDto.builder()
                        .id(emp.getId())
                        .firstName(emp.getFirstName())
                        .lastName(emp.getLastName())
                        .build())
                .collect(Collectors.toList());

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findAllById(assignableEntityIds)).thenReturn(employees);
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(any(), any())).thenReturn(Optional.empty()); // No assignment found

        // When
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveAssignableEntitiesFromHierarchicalEntity(hierarchicalEntityId, assignableEntityIds))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
        // Then
        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findAllById(assignableEntityIds);
        verify(assignmentRepository, never()).delete(any()); // No deletion since no assignment was found
        verify(hierarchicalEntityRepository, never()).save(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
    }

    /**
     * Test case for the successful bulk assignment of hierarchical entities to an assignable entity.
     * This test ensures that the method correctly creates assignments and returns a populated BulkAssignmentResponseDto.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_Success() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(assignableEntityId)
                .firstName(assignableEntity.getFirstName())
                .lastName(assignableEntity.getLastName())
                .build();

        Department department1 = Department.builder()
                .id(hierarchicalEntityIds.get(0))
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Department department2 = Department.builder()
                .id(hierarchicalEntityIds.get(1))
                .name("HR")
                .employees(new HashSet<>())
                .build();

        Department department3 = Department.builder()
                .id(hierarchicalEntityIds.get(2))
                .name("Finance")
                .employees(new HashSet<>())
                .build();

        List<Department> hierarchicalEntities = Arrays.asList(department1, department2, department3);

        List<HierarchyResponseDto> hierarchyResponseDtos = hierarchicalEntities.stream()
                .map(emp -> HierarchyResponseDto.builder()
                        .id(emp.getId())
                        .name(emp.getName())
                        .build())
                .collect(Collectors.toList());

        // Metadata (could be empty or contain some values depending on the test)
        List<AssignmentMetadataRequestDto> metadataDtos = new ArrayList<>();

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(hierarchicalEntityRepository.findAllById(hierarchicalEntityIds)).thenReturn(hierarchicalEntities);
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(any(), any())).thenReturn(false);
        when(assignableEntityRepository.save(assignableEntity)).thenReturn(assignableEntity);
        when(assignableEntityMapper.toDto(assignableEntity)).thenReturn(employeeResponseDto);
        when(hierarchyMapper.toDto(any(), eq(false))).thenAnswer(invocation -> {
            Department emp = invocation.getArgument(0);
            return hierarchyResponseDtos.stream()
                    .filter(dto -> dto.getId().equals(emp.getId()))
                    .findFirst()
                    .orElse(null);
        });

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(assignableEntityId, hierarchicalEntityIds, metadataDtos);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAssignableEntities()).hasSize(1);
        assertThat(result.getAssignableEntities().get(0).getId()).isEqualTo(assignableEntityId);
        assertThat(result.getHierarchicalEntities()).hasSize(3);
        assertThat(result.getHierarchicalEntities().stream().map(HierarchyResponseDto::getId))
                .containsExactlyInAnyOrderElementsOf(hierarchicalEntityIds);

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository, times(3)).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignableEntityRepository).save(assignableEntity);
        verify(assignableEntityMapper).toDto(any());
        verify(hierarchyMapper, times(3)).toDto(any(), anyBoolean());
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found in the repository.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_AssignableEntityNotFound() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(assignableEntityId, hierarchicalEntityIds, new ArrayList<>()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository, never()).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository, never()).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignableEntityRepository, never()).save(any());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Test case for handling the scenario where one or more hierarchical entities are not found.
     * This test ensures that the method throws an EntityNotFoundException when one or more of
     * the provided hierarchical entity IDs do not correspond to existing entities in the repository.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_OneOrMoreHierarchicalEntitiesNotFound() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        Department department1 = Department.builder()
                .id(hierarchicalEntityIds.get(0))
                .name("Engineering")
                .build();

        Department department2 = Department.builder()
                .id(hierarchicalEntityIds.get(1))
                .name("HR")
                .build();

        // Missing department3
        List<Department> hierarchicalEntities = Arrays.asList(department1, department2);

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(hierarchicalEntityRepository.findAllById(hierarchicalEntityIds)).thenReturn(hierarchicalEntities);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(assignableEntityId, hierarchicalEntityIds, new ArrayList<>()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("One or more hierarchical entities not found");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository, never()).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignableEntityRepository, never()).save(any());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Test case for handling the scenario where an assignment between the hierarchical entity
     * and an assignable entity already exists. This test ensures that the method throws an
     * AssignmentAlreadyExistsException.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_AssignmentAlreadyExists() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        Department department1 = Department.builder()
                .id(hierarchicalEntityIds.get(0))
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Department department2 = Department.builder()
                .id(hierarchicalEntityIds.get(1))
                .name("HR")
                .employees(new HashSet<>())
                .build();

        Department department3 = Department.builder()
                .id(hierarchicalEntityIds.get(2))
                .name("Finance")
                .employees(new HashSet<>())
                .build();

        List<Department> hierarchicalEntities = Arrays.asList(department1, department2, department3);

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(hierarchicalEntityRepository.findAllById(hierarchicalEntityIds)).thenReturn(hierarchicalEntities);
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(department1, assignableEntity)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(assignableEntityId, hierarchicalEntityIds, new ArrayList<>()))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Assignment already exists for hierarchical entity ID: " + department1.getId() + " and assignable entity ID: " + assignableEntityId);

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository).existsByHierarchicalEntityAndAssignableEntity(department1, assignableEntity);
        verify(assignableEntityRepository, never()).save(any());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Test case for the successful bulk removal of hierarchical entities from an assignable entity.
     * This test ensures that the method correctly removes assignments and returns a populated
     * BulkAssignmentResponseDto with the remaining assignments.
     */
    @Test
    public void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_Success() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(assignableEntityId)
                .firstName(assignableEntity.getFirstName())
                .lastName(assignableEntity.getLastName())
                .build();

        Department department1 = Department.builder()
                .id(hierarchicalEntityIds.get(0))
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Department department2 = Department.builder()
                .id(hierarchicalEntityIds.get(1))
                .name("HR")
                .employees(new HashSet<>())
                .build();

        Department department3 = Department.builder()
                .id(hierarchicalEntityIds.get(2))
                .name("Finance")
                .employees(new HashSet<>())
                .build();

        // Assuming these departments were previously assigned to the employee
        assignableEntity.getDepartments().add(department1);
        assignableEntity.getDepartments().add(department2);
        assignableEntity.getDepartments().add(department3);

        department1.getEmployees().add(assignableEntity);
        department2.getEmployees().add(assignableEntity);
        department3.getEmployees().add(assignableEntity);

        List<Department> hierarchicalEntities = Arrays.asList(department1, department2, department3);

        List<HierarchyResponseDto> hierarchyResponseDtos = hierarchicalEntities.stream()
                .map(emp -> HierarchyResponseDto.builder()
                        .id(emp.getId())
                        .name(emp.getName())
                        .build())
                .collect(Collectors.toList());

        List<DepartmentEmployeeAssignment> assignments = hierarchicalEntities.stream()
                .map(department -> DepartmentEmployeeAssignment.builder()
                        .hierarchicalEntity(department)
                        .assignableEntity(assignableEntity)
                        .metadata(
                                Collections.singleton(
                                        AssignmentMetadata.builder()
                                                .key("Role")
                                                .value("Manager")
                                                .build()
                                )
                        )
                        .build())
                .collect(Collectors.toList());

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(hierarchicalEntityRepository.findAllById(hierarchicalEntityIds)).thenReturn(hierarchicalEntities);
        when(assignableEntityRepository.save(assignableEntity)).thenReturn(assignableEntity);
        when(assignableEntityMapper.toDto(assignableEntity)).thenReturn(employeeResponseDto);

        when(hierarchyMapper.toDto(any(), eq(false))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            return hierarchyResponseDtos.stream()
                    .filter(dto -> dto.getId().equals(dept.getId()))
                    .findFirst()
                    .orElse(null);
        });

        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(any(), any())).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            return Optional.of(
                    assignments.stream()
                            .filter(assignment -> assignment.getHierarchicalEntity().getId().equals(dept.getId()))
                            .findFirst()
                            .orElse(null)
            );
        });

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .bulkRemoveHierarchicalEntitiesFromAssignableEntity(assignableEntityId, hierarchicalEntityIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAssignableEntities()).hasSize(1);
        assertThat(result.getAssignableEntities().get(0).getId()).isEqualTo(assignableEntityId);
        assertThat(result.getHierarchicalEntities()).hasSize(3);
        assertThat(result.getHierarchicalEntities().stream().map(HierarchyResponseDto::getId))
                .containsExactlyInAnyOrderElementsOf(hierarchicalEntityIds);

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository, times(3)).findByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignableEntityRepository).save(assignableEntity);
        verify(assignableEntityMapper).toDto(any());
        verify(hierarchyMapper, times(3)).toDto(any(), anyBoolean());
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found during the bulk removal operation.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found
     * in the repository.
     */
    @Test
    public void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_AssignableEntityNotFound() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveHierarchicalEntitiesFromAssignableEntity(assignableEntityId, hierarchicalEntityIds))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository, never()).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository, never()).findByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignableEntityRepository, never()).save(any());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());

    }

    /**
     * Test case for handling the scenario where one or more hierarchical entities are not found during
     * the bulk removal operation. This test ensures that the method throws an EntityNotFoundException
     * when one or more of the provided hierarchical entity IDs do not correspond to existing entities
     * in the repository.
     */
    @Test
    public void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_OneOrMoreHierarchicalEntitiesNotFound() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        Department department1 = Department.builder()
                .id(hierarchicalEntityIds.get(0))
                .name("Engineering")
                .build();

        Department department2 = Department.builder()
                .id(hierarchicalEntityIds.get(1))
                .name("HR")
                .build();

        // Missing department3
        List<Department> hierarchicalEntities = Arrays.asList(department1, department2);

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(hierarchicalEntityRepository.findAllById(hierarchicalEntityIds)).thenReturn(hierarchicalEntities);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveHierarchicalEntitiesFromAssignableEntity(assignableEntityId, hierarchicalEntityIds))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("One or more hierarchical entities not found");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository, never()).findByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignableEntityRepository, never()).save(any());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Test case for handling the scenario where an assignment between the hierarchical entity
     * and an assignable entity is not found during the bulk removal operation. This test ensures
     * that the method successfully handles the removal even if the assignment does not exist.
     * In this case, it should silently succeed without throwing an exception.
     */
    @Test
    public void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_AssignmentNotFound() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .departments(new HashSet<>())
                .build();

        Department department1 = Department.builder()
                .id(hierarchicalEntityIds.get(0))
                .name("Engineering")
                .employees(new HashSet<>())
                .build();

        Department department2 = Department.builder()
                .id(hierarchicalEntityIds.get(1))
                .name("HR")
                .employees(new HashSet<>())
                .build();

        Department department3 = Department.builder()
                .id(hierarchicalEntityIds.get(2))
                .name("Finance")
                .employees(new HashSet<>())
                .build();

        // These departments are not assigned to the employee, so removal should be handled gracefully
        List<Department> hierarchicalEntities = Arrays.asList(department1, department2, department3);

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(hierarchicalEntityRepository.findAllById(hierarchicalEntityIds)).thenReturn(hierarchicalEntities);
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(any(), any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveHierarchicalEntitiesFromAssignableEntity(assignableEntityId, hierarchicalEntityIds))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignableEntityRepository, never()).save(assignableEntity);
        verify(assignableEntityMapper, never()).toDto(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Test case for the successful retrieval of assignable entities associated with a hierarchical entity.
     * This test ensures that the method correctly retrieves and converts the assignable entities to DTOs
     * when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignableEntitiesByHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Department hierarchicalEntity = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee assignableEntity1 = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .build();

        Employee assignableEntity2 = Employee.builder()
                .id(3L)
                .firstName("Jane")
                .lastName("Smith")
                .build();

        List<Employee> assignableEntities = Arrays.asList(assignableEntity1, assignableEntity2);

        EmployeeResponseDto dto1 = EmployeeResponseDto.builder()
                .id(assignableEntity1.getId())
                .firstName(assignableEntity1.getFirstName())
                .lastName(assignableEntity1.getLastName())
                .build();

        EmployeeResponseDto dto2 = EmployeeResponseDto.builder()
                .id(assignableEntity2.getId())
                .firstName(assignableEntity2.getFirstName())
                .lastName(assignableEntity2.getLastName())
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(hierarchicalEntity));
        when(assignmentRepository.findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(Employee.class, hierarchicalEntity))
                .thenReturn(assignableEntities);
        when(assignableEntityMapper.toDto(assignableEntity1)).thenReturn(dto1);
        when(assignableEntityMapper.toDto(assignableEntity2)).thenReturn(dto2);

        // When
        List<EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .getAssignableEntitiesByHierarchicalEntity(hierarchicalEntityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(dto1, dto2);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignmentRepository).findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(Employee.class, hierarchicalEntity);
        verify(assignableEntityMapper).toDto(assignableEntity1);
        verify(assignableEntityMapper).toDto(assignableEntity2);
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found during the retrieval of
     * assignable entities. This test ensures that the method throws an EntityNotFoundException when the
     * hierarchical entity is not found in the repository.
     */
    @Test
    public void testGetAssignableEntitiesByHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .getAssignableEntitiesByHierarchicalEntity(hierarchicalEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignmentRepository, never())
                .findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(any(), any());
        verify(assignableEntityMapper, never()).toDto(any());
    }

    /**
     * Test case for the successful retrieval of a paginated list of assignable entities associated with a hierarchical entity.
     * This test ensures that the method correctly retrieves and converts the assignable entities to a paginated response DTO
     * when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignableEntitiesByHierarchicalEntity_Paginated_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Department hierarchicalEntity = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Pageable pageable = PageRequest.of(0, 2); // Define the pagination parameters
        Employee assignableEntity1 = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .build();

        Employee assignableEntity2 = Employee.builder()
                .id(3L)
                .firstName("Jane")
                .lastName("Smith")
                .build();

        Page<Employee> assignableEntitiesPage = new PageImpl<>(Arrays.asList(assignableEntity1, assignableEntity2), pageable, 2);

        PaginatedResponseDto<EmployeeResponseDto> paginatedResponseDto = PaginatedResponseDto.<EmployeeResponseDto>builder()
                .content(
                        Arrays.asList(
                                EmployeeResponseDto.builder()
                                        .id(assignableEntity1.getId())
                                        .firstName(assignableEntity1.getFirstName())
                                        .lastName(assignableEntity1.getLastName())
                                        .build(),
                                EmployeeResponseDto.builder()
                                        .id(assignableEntity2.getId())
                                        .firstName(assignableEntity2.getFirstName())
                                        .lastName(assignableEntity2.getLastName())
                                        .build()
                        )
                )
                .page(pageable.getPageNumber())
                .totalElements(assignableEntitiesPage.getTotalElements())
                .totalPages(assignableEntitiesPage.getTotalPages())
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(hierarchicalEntity));
        when(assignmentRepository.findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(Employee.class, hierarchicalEntity, pageable))
                .thenReturn(assignableEntitiesPage);
        when(assignableEntityMapper.toDtoPage(assignableEntitiesPage)).thenReturn(paginatedResponseDto);

        // When
        PaginatedResponseDto<EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .getAssignableEntitiesByHierarchicalEntity(hierarchicalEntityId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting("id").containsExactlyInAnyOrder(assignableEntity1.getId(), assignableEntity2.getId());
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignmentRepository).findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(Employee.class, hierarchicalEntity, pageable);
        verify(assignableEntityMapper).toDtoPage(assignableEntitiesPage);
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found during the paginated retrieval of
     * assignable entities. This test ensures that the method throws an EntityNotFoundException when the hierarchical entity
     * is not found in the repository.
     */
    @Test
    public void testGetAssignableEntitiesByHierarchicalEntity_Paginated_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Pageable pageable = PageRequest.of(0, 2);

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .getAssignableEntitiesByHierarchicalEntity(hierarchicalEntityId, pageable))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignmentRepository, never())
                .findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(any(), any(), any());
        verify(assignableEntityMapper, never()).toDtoPage(any());
    }

    /**
     * Test case for the successful retrieval of the count of assignable entities associated with a hierarchical entity.
     * This test ensures that the method correctly returns the count of assignable entities when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignableEntityCountByHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Department hierarchicalEntity = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        List<Employee> assignableEntities = Arrays.asList(
                Employee.builder().id(2L).firstName("John").lastName("Doe").build(),
                Employee.builder().id(3L).firstName("Jane").lastName("Smith").build()
        );

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(hierarchicalEntity));
        when(assignmentRepository.findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(Employee.class, hierarchicalEntity))
                .thenReturn(assignableEntities);

        // When
        int result = departmentEmployeeAssignmentService.getAssignableEntityCountByHierarchicalEntity(hierarchicalEntityId);

        // Then
        assertThat(result).isEqualTo(assignableEntities.size());

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignmentRepository).findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(Employee.class, hierarchicalEntity);
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found during the retrieval of
     * the count of assignable entities. This test ensures that the method throws an EntityNotFoundException when the hierarchical entity
     * is not found in the repository.
     */
    @Test
    public void testGetAssignableEntityCountByHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getAssignableEntityCountByHierarchicalEntity(hierarchicalEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignmentRepository, never()).findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(any(), any());
    }

    /**
     * Test case for the successful retrieval of the count of hierarchical entities associated with an assignable entity.
     * This test ensures that the method correctly returns the count of hierarchical entities when the assignable entity exists.
     */
    @Test
    public void testGetHierarchicalEntityCountByAssignableEntity_Success() {
        // Given
        Long assignableEntityId = 1L;
        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        List<Department> hierarchicalEntities = Arrays.asList(
                Department.builder().id(2L).name("Engineering").build(),
                Department.builder().id(3L).name("HR").build()
        );

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(assignmentRepository.findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(Department.class, assignableEntity))
                .thenReturn(hierarchicalEntities);

        // When
        int result = departmentEmployeeAssignmentService.getHierarchicalEntityCountByAssignableEntity(assignableEntityId);

        // Then
        assertThat(result).isEqualTo(hierarchicalEntities.size());

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(Department.class, assignableEntity);
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found during the retrieval of
     * the count of hierarchical entities. This test ensures that the method throws an EntityNotFoundException when the assignable entity
     * is not found in the repository.
     */
    @Test
    public void testGetHierarchicalEntityCountByAssignableEntity_EntityNotFound() {
        // Given
        Long assignableEntityId = 1L;

        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getHierarchicalEntityCountByAssignableEntity(assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository, never()).findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(any(), any());
    }

    /**
     * Test case for the successful retrieval of all assignments.
     * This test ensures that the method retrieves all assignments from the repository and converts them into a list of AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> objects.
     */
    @Test
    public void testGetAllAssignments_Success() {
        // Given
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        Employee employee = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(3L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();

        List<DepartmentEmployeeAssignment> assignments = Collections.singletonList(assignment);

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> expectedDto = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employeeResponseDto)
                .build();

        when(assignmentRepository.findAll()).thenReturn(assignments);
        when(assignmentMapper.toDto(assignment)).thenReturn(expectedDto);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAllAssignments();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expectedDto);

        verify(assignmentRepository).findAll();
        verify(assignmentMapper).toDto(assignment);
    }

    /**
     * Test case for handling the scenario where no assignments are found in the repository.
     * This test ensures that the method returns an empty list when there are no assignments.
     */
    @Test
    public void testGetAllAssignments_EmptyList() {
        // Given
        when(assignmentRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAllAssignments();

        // Then
        assertThat(result).isEmpty();

        verify(assignmentRepository).findAll();
        verify(assignmentMapper, never()).toDto(any());
    }

    /**
     * Test case for the successful retrieval of all assignments with pagination.
     * This test ensures that the method retrieves paginated assignments from the repository and converts them into a PaginatedResponseDto<AssignmentResponseDto<H, D>>.
     */
    @Test
    public void testGetAllAssignments_WithPagination_Success() {
        // Given
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        Employee employee = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(3L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();

        Page<DepartmentEmployeeAssignment> assignmentPage = new PageImpl<>(Collections.singletonList(assignment), PageRequest.of(0, 10), 1);

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employeeResponseDto)
                .build();

        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> expectedPaginatedDto = PaginatedResponseDto.<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>builder()
                .content(Collections.singletonList(dto))
                .page(0)
                .size(10)
                .totalElements(assignmentPage.getTotalElements())
                .totalPages(assignmentPage.getTotalPages())
                .build();

        when(assignmentRepository.findAll(any(Pageable.class))).thenReturn(assignmentPage);
        when(assignmentMapper.toDtoPage(any(Page.class))).thenReturn(expectedPaginatedDto);

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAllAssignments(PageRequest.of(0, 10));

        // Then
        assertThat(result).isEqualTo(expectedPaginatedDto);

        verify(assignmentRepository).findAll(any(Pageable.class));
        verify(assignmentMapper).toDtoPage(any(Page.class));
    }

    /**
     * Test case for handling the scenario where no assignments are found with pagination.
     * This test ensures that the method returns an empty PaginatedResponseDto when there are no assignments.
     */
    @Test
    public void testGetAllAssignments_WithPagination_EmptyPage() {
        // Given
        Page<DepartmentEmployeeAssignment> emptyAssignmentPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> expectedPaginatedDto = PaginatedResponseDto.<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>builder()
                .content(Collections.emptyList())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(assignmentRepository.findAll(any(Pageable.class))).thenReturn(emptyAssignmentPage);
        when(assignmentMapper.toDtoPage(any(Page.class))).thenReturn(expectedPaginatedDto);

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAllAssignments(PageRequest.of(0, 10));

        // Then
        assertThat(result).isEqualTo(expectedPaginatedDto);

        verify(assignmentRepository).findAll(any(Pageable.class));
        verify(assignmentMapper).toDtoPage(any(Page.class));
    }

    /**
     * Test case for the successful retrieval of all assignments for specific hierarchical and assignable entity classes.
     * This test ensures that the method retrieves assignments based on the given classes and converts them into a list of AssignmentResponseDto.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Success() {
        // Given
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        Employee employee = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(3L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employeeResponseDto)
                .build();

        List<DepartmentEmployeeAssignment> assignments = Collections.singletonList(assignment);
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> expectedDtos = Collections.singletonList(dto);

        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class))).thenReturn(assignments);
        when(assignmentMapper.toDto(any(DepartmentEmployeeAssignment.class))).thenReturn(dto);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByEntityClasses();

        // Then
        assertThat(result).isEqualTo(expectedDtos);

        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class));
        verify(assignmentMapper, times(1)).toDto(assignment);
    }

    /**
     * Test case for handling the scenario where no assignments are found for the specified hierarchical and assignable entity classes.
     * This test ensures that the method returns an empty list of AssignmentResponseDto when there are no assignments for the given classes.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_NoAssignments() {
        // Given
        List<DepartmentEmployeeAssignment> assignments = Collections.emptyList();
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> expectedDtos = Collections.emptyList();

        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class))).thenReturn(assignments);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByEntityClasses();

        // Then
        assertThat(result).isEqualTo(expectedDtos);

        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class));
        verify(assignmentMapper, never()).toDto(any(DepartmentEmployeeAssignment.class));
    }

    /**
     * Test case for the successful retrieval of paginated assignments for specific hierarchical and assignable entity classes.
     * This test ensures that the method retrieves assignments with pagination and converts them into a PaginatedResponseDto.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Pagination_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10); // First page, size 10
        Department department = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        Employee employee = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(3L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employeeResponseDto)
                .build();

        Page<DepartmentEmployeeAssignment> assignmentsPage = new PageImpl<>(Collections.singletonList(assignment), pageable, 1);
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> expectedPageDto
                = PaginatedResponseDto.<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>builder()
                .content(Collections.singletonList(dto))
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(assignmentsPage.getTotalElements())
                .totalPages(assignmentsPage.getTotalPages())
                .build();

        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class), eq(pageable)))
                .thenReturn(assignmentsPage);
        when(assignmentMapper.toDtoPage(any(Page.class))).thenReturn(expectedPageDto);

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByEntityClasses(pageable);

        // Then
        assertThat(result).isEqualTo(expectedPageDto);

        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class), eq(pageable));
        verify(assignmentMapper).toDtoPage(any(Page.class));
    }

    /**
     * Test case for handling the scenario where no assignments are found for the specified hierarchical and assignable entity classes with pagination.
     * This test ensures that the method returns an empty PaginatedResponseDto when there are no assignments for the given classes.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Pagination_NoAssignments() {
        // Given
        Pageable pageable = PageRequest.of(0, 10); // First page, size 10
        Page<DepartmentEmployeeAssignment> assignmentsPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> expectedPageDto
                = PaginatedResponseDto.<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>builder()
                .content(Collections.emptyList())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(assignmentsPage.getTotalElements())
                .totalPages(assignmentsPage.getTotalPages())
                .build();

        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class), eq(pageable)))
                .thenReturn(assignmentsPage);
        when(assignmentMapper.toDtoPage(any(Page.class))).thenReturn(expectedPageDto);


        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByEntityClasses(pageable);

        // Then
        assertThat(result).isEqualTo(expectedPageDto);

        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class), eq(pageable));
        verify(assignmentMapper).toDtoPage(any(Page.class));
    }

    /**
     * Test case for the successful update of an assignment based on the provided request DTO.
     * This test ensures that the method correctly retrieves, updates, and saves the assignment,
     * and returns the updated AssignmentResponseDto.
     */
    @Test
    public void testUpdateAssignment_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        List<AssignmentMetadataRequestDto> metadataRequests = Arrays.asList(
                new AssignmentMetadataRequestDto("key1", "newValue1"),
                new AssignmentMetadataRequestDto("key2", "newValue2")
        );

        AssignmentRequestDto requestDto = new AssignmentRequestDto(hierarchicalEntityId, assignableEntityId, metadataRequests);

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(hierarchicalEntityId)
                .name(department.getName())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(assignableEntityId)
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .build();

        AssignmentMetadata existingMetadata = AssignmentMetadata.builder()
                .assignment(null) // Will be set later
                .key("key1")
                .value("oldValue1")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(3L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .metadata(new HashSet<>(Collections.singletonList(existingMetadata)))
                .build();

        AssignmentMetadataResponseDto updatedMetadataResponseDto = AssignmentMetadataResponseDto.builder()
                .key("key1")
                .value("newValue1")
                .build();

        AssignmentMetadataResponseDto addedMetadataResponseDto = AssignmentMetadataResponseDto.builder()
                .key("key2")
                .value("newValue2")
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> updatedDto
                = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employeeResponseDto)
                .metadata(List.of(updatedMetadataResponseDto, addedMetadataResponseDto))
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(department, employee)).thenReturn(Optional.of(assignment));
        when(assignmentMapper.toDto(any())).thenReturn(updatedDto);
        when(assignmentRepository.save(any(DepartmentEmployeeAssignment.class))).thenReturn(assignment);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService.updateAssignment(requestDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(updatedDto);

        assertThat(result.getHierarchicalEntity()).isEqualTo(hierarchyResponseDto);
        assertThat(result.getAssignableEntity()).isEqualTo(employeeResponseDto);

        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).size().isEqualTo(2);
        assertThat(result.getMetadata()).contains(updatedMetadataResponseDto);
        assertThat(result.getMetadata()).contains(addedMetadataResponseDto);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(department, employee);
        verify(assignmentRepository).save(assignment);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository);
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical entity is not found.
     */
    @Test
    public void testUpdateAssignment_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        List<AssignmentMetadataRequestDto> metadataRequests = Collections.emptyList();
        AssignmentRequestDto requestDto = new AssignmentRequestDto(hierarchicalEntityId, assignableEntityId, metadataRequests);

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.updateAssignment(requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository);
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found.
     */
    @Test
    public void testUpdateAssignment_AssignableEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        List<AssignmentMetadataRequestDto> metadataRequests = Collections.emptyList();
        AssignmentRequestDto requestDto = new AssignmentRequestDto(hierarchicalEntityId, assignableEntityId, metadataRequests);

        Department department = Department.builder().id(hierarchicalEntityId).build();
        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.updateAssignment(requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository);
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignment is not found.
     */
    @Test
    public void testUpdateAssignment_AssignmentNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        List<AssignmentMetadataRequestDto> metadataRequests = Collections.emptyList();
        AssignmentRequestDto requestDto = new AssignmentRequestDto(hierarchicalEntityId, assignableEntityId, metadataRequests);

        Department department = Department.builder().id(hierarchicalEntityId).build();
        Employee employee = Employee.builder().id(assignableEntityId).build();
        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(department, employee)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.updateAssignment(requestDto))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(department, employee);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository);
    }

    /**
     * Test case for the successful removal of metadata from an assignment.
     * This test ensures that the method correctly removes the metadata and updates the assignment.
     */
    @Test
    public void testRemoveMetadataById_Success() {
        // Given
        Long assignmentId = 1L;
        Long metadataId = 2L;

        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .id(metadataId)
                .key("key1")
                .value("value1")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(assignmentId)
                .metadata(new HashSet<>(Collections.singletonList(metadata)))
                .build();

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentMetadataRepository.findById(metadataId)).thenReturn(Optional.of(metadata));

        // When
        departmentEmployeeAssignmentService.removeMetadataById(assignmentId, metadataId);

        // Then
        assertThat(assignment.getMetadata()).isEmpty();

        verify(assignmentRepository).findById(assignmentId);
        verify(assignmentMetadataRepository).findById(metadataId);
        verify(assignmentMetadataRepository).delete(metadata);
        verify(assignmentRepository).save(assignment);
        verifyNoMoreInteractions(assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an AssignmentNotFoundException when the assignment is not found.
     */
    @Test
    public void testRemoveMetadataById_AssignmentNotFound() {
        // Given
        Long assignmentId = 1L;
        Long metadataId = 2L;

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataById(assignmentId, metadataId))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");

        verify(assignmentRepository).findById(assignmentId);
        verifyNoMoreInteractions(assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the metadata is not found.
     * This test ensures that the method throws a MetadataNotFoundException when the metadata is not found.
     */
    @Test
    public void testRemoveMetadataById_MetadataNotFound() {
        // Given
        Long assignmentId = 1L;
        Long metadataId = 2L;

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(assignmentId)
                .metadata(new HashSet<>())
                .build();

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentMetadataRepository.findById(metadataId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataById(assignmentId, metadataId))
                .isInstanceOf(MetadataNotFoundException.class)
                .hasMessage("Metadata not found");

        verify(assignmentRepository).findById(assignmentId);
        verify(assignmentMetadataRepository).findById(metadataId);
        verifyNoMoreInteractions(assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for the successful removal of metadata from an assignment by metadata key.
     * This test ensures that the method correctly removes the metadata with the specified key and updates the assignment.
     */
    @Test
    public void testRemoveMetadataByKey_Success() {
        // Given
        Long assignmentId = 1L;
        String metadataKey = "key1";

        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .id(2L)
                .key(metadataKey)
                .value("value1")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(assignmentId)
                .metadata(new HashSet<>(Collections.singletonList(metadata)))
                .build();

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignmentMetadataRepository.findById(metadata.getId())).thenReturn(Optional.of(metadata));

        // When
        departmentEmployeeAssignmentService.removeMetadataByKey(assignmentId, metadataKey);

        // Then
        assertThat(assignment.getMetadata()).isEmpty();

        verify(assignmentRepository).findById(assignmentId);
        verify(assignmentMetadataRepository).delete(metadata);
        verify(assignmentRepository).save(assignment);
        verifyNoMoreInteractions(assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an AssignmentNotFoundException when the assignment is not found.
     */
    @Test
    public void testRemoveMetadataByKey_AssignmentNotFound() {
        // Given
        Long assignmentId = 1L;
        String metadataKey = "key1";

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByKey(assignmentId, metadataKey))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");

        verify(assignmentRepository).findById(assignmentId);
        verifyNoMoreInteractions(assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where metadata with the specified key is not found.
     * This test ensures that the method throws a MetadataNotFoundException when no metadata with the key is found.
     */
    @Test
    public void testRemoveMetadataByKey_MetadataNotFound() {
        // Given
        Long assignmentId = 1L;
        String metadataKey = "key1";

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(assignmentId)
                .metadata(new HashSet<>())
                .build();

        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByKey(assignmentId, metadataKey))
                .isInstanceOf(MetadataNotFoundException.class)
                .hasMessage("Metadata with key '" + metadataKey + "' not found");

        verify(assignmentRepository).findById(assignmentId);
        verifyNoMoreInteractions(assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for the successful removal of metadata from an assignment by hierarchical entity ID,
     * assignable entity ID, and metadata ID. This test ensures that the method correctly performs the removal
     * when all entities and the assignment are present.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        Long metadataId = 3L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .id(metadataId)
                .key("key1")
                .value("value1")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(4L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .metadata(new HashSet<>(Collections.singletonList(metadata)))
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(assignmentMetadataRepository.findById(metadataId)).thenReturn(Optional.of(metadata));

        // When
        departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId);

        // Then
        assertThat(assignment.getMetadata()).isEmpty();

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId);
        verify(assignmentRepository).findById(assignment.getId());
        verify(assignmentMetadataRepository).findById(metadataId);
        verify(assignmentMetadataRepository).delete(metadata);
        verify(assignmentRepository).save(assignment);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical entity is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        Long metadataId = 3L;

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_AssignableEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        Long metadataId = 3L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an AssignmentNotFoundException when the assignment is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_AssignmentNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        Long metadataId = 3L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the metadata with the specified id is not found.
     * This test ensures that the method throws a MetadataNotFoundException when the metadata with the id is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_MetadataKeyNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        Long metadataId = 3L;

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(3L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .metadata(new HashSet<>())
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId))
                .isInstanceOf(MetadataNotFoundException.class)
                .hasMessage("Metadata not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId);
        verify(assignmentRepository).findById(assignment.getId());
        verify(assignmentMetadataRepository).findById(metadataId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for the successful removal of metadata from an assignment by hierarchical entity ID,
     * assignable entity ID, and metadata key. This test ensures that the method correctly performs the removal
     * when all entities and the assignment are present and the metadata with the key exists.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        String metadataKey = "key1";

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .id(3L)
                .key(metadataKey)
                .value("value1")
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(4L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .metadata(new HashSet<>(Collections.singletonList(metadata)))
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));

        // When
        departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey);

        // Then
        assertThat(assignment.getMetadata()).isEmpty();

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId);
        verify(assignmentRepository).findById(assignment.getId());
        verify(assignmentMetadataRepository).delete(metadata);
        verify(assignmentRepository).save(assignment);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical entity is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        String metadataKey = "key1";

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_AssignableEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        String metadataKey = "key1";

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an AssignmentNotFoundException when the assignment is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_AssignmentNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        String metadataKey = "key1";

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for handling the scenario where the metadata with the specified key is not found.
     * This test ensures that the method throws a MetadataNotFoundException when the metadata with the key is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_MetadataKeyNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;
        String metadataKey = "key1";

        Department department = Department.builder()
                .id(hierarchicalEntityId)
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .build();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(3L)
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .metadata(new HashSet<>())
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(department));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey))
                .isInstanceOf(MetadataNotFoundException.class)
                .hasMessage("Metadata with key '" + metadataKey + "' not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId);
        verify(assignmentRepository).findById(assignment.getId());
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMetadataRepository);
    }

    /**
     * Test case for successfully retrieving assignments by hierarchical entity ID.
     * This test ensures that the method correctly retrieves and maps assignments to DTOs when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignmentsByHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;

        // Create the hierarchical entity
        Department hierarchicalEntity = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        // Create assignments
        Employee employee1 = Employee.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .build();

        EmployeeResponseDto employee1ResponseDto = EmployeeResponseDto.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .build();

        DepartmentEmployeeAssignment assignment1 = DepartmentEmployeeAssignment.builder()
                .id(1L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(employee1)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        Employee employee2 = Employee.builder()
                .id(20L)
                .firstName("Jane")
                .lastName("Smith")
                .build();

        EmployeeResponseDto employee2ResponseDto = EmployeeResponseDto.builder()
                .id(20L)
                .firstName("Jane")
                .lastName("Smith")
                .build();

        DepartmentEmployeeAssignment assignment2 = DepartmentEmployeeAssignment.builder()
                .id(2L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(employee2)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        // Create DTOs
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto1 = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employee1ResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();


        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto2 = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employee2ResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();

        // Mock the repository and mapper methods
        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(hierarchicalEntity));
        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClassAndHierarchicalEntity(
                Department.class,
                Employee.class,
                hierarchicalEntity))
                .thenReturn(Arrays.asList(assignment1, assignment2));
        when(assignmentMapper.toDto(assignment1)).thenReturn(dto1);
        when(assignmentMapper.toDto(assignment2)).thenReturn(dto2);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByHierarchicalEntity(hierarchicalEntityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(dto1, dto2);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClassAndHierarchicalEntity(
                Department.class, Employee.class, hierarchicalEntity);
        verify(assignmentMapper).toDto(assignment1);
        verify(assignmentMapper).toDto(assignment2);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignmentRepository, assignmentMapper);
    }

    /**
     * Test case for successfully retrieving a paginated list of assignments by hierarchical entity ID.
     * This test ensures that the method correctly retrieves and maps a paginated list of assignments to DTOs when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignmentsByHierarchicalEntity_Paginated_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Pageable pageable = PageRequest.of(0, 10); // Pagination: first page, 10 items per page

        // Create the hierarchical entity
        Department hierarchicalEntity = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        // Create assignments
        Employee employee1 = Employee.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .build();

        EmployeeResponseDto employee1ResponseDto = EmployeeResponseDto.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .build();

        DepartmentEmployeeAssignment assignment1 = DepartmentEmployeeAssignment.builder()
                .id(1L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(employee1)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        Employee employee2 = Employee.builder()
                .id(20L)
                .firstName("Jane")
                .lastName("Smith")
                .build();

        EmployeeResponseDto employee2ResponseDto = EmployeeResponseDto.builder()
                .id(20L)
                .firstName("Jane")
                .lastName("Smith")
                .build();

        DepartmentEmployeeAssignment assignment2 = DepartmentEmployeeAssignment.builder()
                .id(2L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(employee2)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        // Create DTOs
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto1 = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employee1ResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto2 = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(employee2ResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();

        // Mock the repository and mapper methods
        Page<Assignment<Department, Employee>> assignmentPage = new PageImpl<>(Arrays.asList(assignment1, assignment2), pageable, 2);

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(hierarchicalEntity));
        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClassAndHierarchicalEntity(
                Department.class,
                Employee.class,
                hierarchicalEntity,
                pageable
        )).thenReturn(assignmentPage);
        when(assignmentMapper.toDtoPage(assignmentPage)).thenReturn(
                PaginatedResponseDto.<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>builder()
                        .content(Arrays.asList(dto1, dto2))
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .totalElements(assignmentPage.getTotalElements())
                        .totalPages(assignmentPage.getTotalPages())
                        .build()
        );

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByHierarchicalEntity(hierarchicalEntityId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).contains(dto1, dto2);
        assertThat(result.getTotalPages()).isEqualTo(assignmentPage.getTotalPages());
        assertThat(result.getTotalElements()).isEqualTo(assignmentPage.getTotalElements());

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClassAndHierarchicalEntity(
                Department.class, Employee.class, hierarchicalEntity, pageable);
        verify(assignmentMapper).toDtoPage(assignmentPage);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignmentRepository, assignmentMapper);
    }

    /**
     * Test case for successfully retrieving assignments by assignable entity ID.
     * This test ensures that the method correctly retrieves and maps assignments to DTOs when the assignable entity exists.
     */
    @Test
    public void testGetAssignmentsByAssignableEntity_Success() {
        // Given
        Long assignableEntityId = 10L;

        // Create the assignable entity (e.g., Employee)
        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        EmployeeResponseDto assignableEntityResponseDto = EmployeeResponseDto.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        // Create the hierarchical entity (e.g., Department)
        Department hierarchicalEntity = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(1L)
                .name("Engineering")
                .build();

        // Create assignments
        DepartmentEmployeeAssignment assignment1 = DepartmentEmployeeAssignment.builder()
                .id(1L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(assignableEntity)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        DepartmentEmployeeAssignment assignment2 = DepartmentEmployeeAssignment.builder()
                .id(2L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(assignableEntity)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        // Create DTOs
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto1 = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(assignableEntityResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto2 = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(assignableEntityResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();

        // Mock the repository and mapper methods
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClassAndAssignableEntity(
                Department.class,
                Employee.class,
                assignableEntity
        )).thenReturn(Arrays.asList(assignment1, assignment2));
        when(assignmentMapper.toDto(assignment1)).thenReturn(dto1);
        when(assignmentMapper.toDto(assignment2)).thenReturn(dto2);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByAssignableEntity(assignableEntityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(dto1, dto2);

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClassAndAssignableEntity(
                Department.class, Employee.class, assignableEntity);
        verify(assignmentMapper).toDto(assignment1);
        verify(assignmentMapper).toDto(assignment2);
        verifyNoMoreInteractions(assignableEntityRepository, assignmentRepository, assignmentMapper);
    }

    /**
     * Test case for successfully retrieving a paginated list of assignments by assignable entity ID.
     * This test ensures that the method correctly retrieves and maps paginated assignments to DTOs when the assignable entity exists.
     */
    @Test
    public void testGetAssignmentsByAssignableEntityWithPagination_Success() {
        // Given
        Long assignableEntityId = 10L;
        Pageable pageable = PageRequest.of(0, 2);

        // Create the assignable entity (e.g., Employee)
        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        EmployeeResponseDto assignableEntityResponseDto = EmployeeResponseDto.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        // Create the hierarchical entity (e.g., Department)
        Department hierarchicalEntity = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(1L)
                .name("Engineering")
                .build();

        // Create assignments
        DepartmentEmployeeAssignment assignment1 = DepartmentEmployeeAssignment.builder()
                .id(1L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(assignableEntity)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        DepartmentEmployeeAssignment assignment2 = DepartmentEmployeeAssignment.builder()
                .id(2L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(assignableEntity)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        // Create DTOs
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto1 = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(assignableEntityResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> dto2 = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(assignableEntityResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();

        // Mock the repository and mapper methods
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));

        Page<Assignment<Department, Employee>> pagedAssignments = new PageImpl<>(Arrays.asList(assignment1, assignment2), pageable, 2);
        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClassAndAssignableEntity(
                Department.class,
                Employee.class,
                assignableEntity,
                pageable
        )).thenReturn(pagedAssignments);

        when(assignmentMapper.toDtoPage(pagedAssignments)).thenReturn(
                PaginatedResponseDto.<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>builder()
                        .content(Arrays.asList(dto1, dto2))
                        .page(0)
                        .size(2)
                        .totalElements(2)
                        .totalPages(1)
                        .build()
        );

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByAssignableEntity(assignableEntityId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).contains(dto1, dto2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClassAndAssignableEntity(
                Department.class, Employee.class, assignableEntity, pageable);
        verify(assignmentMapper).toDtoPage(pagedAssignments);
        verifyNoMoreInteractions(assignableEntityRepository, assignmentRepository, assignmentMapper);
    }

    /**
     * Test case for successfully retrieving an assignment by hierarchical entity ID and assignable entity ID.
     * This test ensures that the method correctly retrieves and maps the assignment to a DTO when both entities exist.
     */
    @Test
    public void testGetAssignmentByHierarchicalAndAssignableEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 10L;

        // Create the hierarchical entity (e.g., Department)
        Department hierarchicalEntity = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        // Create the assignable entity (e.g., Employee)
        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        // Create the assignment
        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .id(100L)
                .hierarchicalEntity(hierarchicalEntity)
                .assignableEntity(assignableEntity)
                .metadata(new HashSet<>())  // Assume empty metadata for simplicity
                .build();

        // Create the expected DTO
        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        EmployeeResponseDto assignableEntityResponseDto = EmployeeResponseDto.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> expectedDto = AssignmentResponseDto.<HierarchyResponseDto, EmployeeResponseDto>builder()
                .hierarchicalEntity(hierarchyResponseDto)
                .assignableEntity(assignableEntityResponseDto)
                .metadata(new ArrayList<>())  // Assume empty metadata for simplicity
                .build();

        // Mock the repository and mapper methods
        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(hierarchicalEntity));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity))
                .thenReturn(Optional.of(assignment));
        when(assignmentMapper.toDto(assignment)).thenReturn(expectedDto);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedDto);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity);
        verify(assignmentMapper).toDto(assignment);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMapper);
    }

    /**
     * Test case for when the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical entity does not exist.
     */
    @Test
    public void testGetAssignmentByHierarchicalAndAssignableEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 10L;

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Hierarchical entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMapper);
    }

    /**
     * Test case for when the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity does not exist.
     */
    @Test
    public void testGetAssignmentByHierarchicalAndAssignableEntity_AssignableEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 10L;

        Department hierarchicalEntity = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(hierarchicalEntity));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Assignable entity not found");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMapper);
    }

    /**
     * Test case for when the assignment is not found.
     * This test ensures that the method throws an EntityNotFoundException when no assignment exists for the given hierarchical and assignable entities.
     */
    @Test
    public void testGetAssignmentByHierarchicalAndAssignableEntity_AssignmentNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 10L;

        Department hierarchicalEntity = Department.builder()
                .id(hierarchicalEntityId)
                .name("Engineering")
                .build();

        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(hierarchicalEntity));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(assignableEntity));
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Assignment not found for the given hierarchical entity ID: " + hierarchicalEntityId + " and assignable entity ID: " + assignableEntityId);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity);
        verifyNoMoreInteractions(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository, assignmentMapper);
    }
}
