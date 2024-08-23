package tn.engn.assignmentapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.engn.assignmentapi.dto.AssignmentMetadataRequestDto;
import tn.engn.assignmentapi.dto.AssignmentMetadataResponseDto;
import tn.engn.assignmentapi.dto.AssignmentResponseDto;
import tn.engn.assignmentapi.dto.BulkAssignmentResponseDto;
import tn.engn.assignmentapi.exception.AssignmentAlreadyExistsException;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.AssignmentMetadata;
import tn.engn.assignmentapi.model.JobEmployeeAssignment;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Job;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobEmployeeAssignmentServiceTest {
    @Mock
    private HierarchyBaseRepository<Job> hierarchicalEntityRepository;

    @Mock
    private EmployeeRepository assignableEntityRepository;

    @Mock
    private AssignmentRepository<Job, Employee, JobEmployeeAssignment> assignmentRepository;

    @Mock
    private AssignmentMetadataRepository assignmentMetadataRepository;

    @Mock
    private HierarchyMapper<Job, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper;

    @Mock
    private AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper;

    @Mock
    private AssignmentMapper<Job, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper;

    @InjectMocks
    private JobEmployeeAssignmentService jobEmployeeAssignmentService;

    /**
     * Initializes mock objects before each test execution.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Tests the successful assignment of an assignable entity to a hierarchical entity with metadata.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Job job = Job.builder()
                .id(hierarchicalEntityId)
                .name("Manager")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(job.getId())
                .name(job.getName())
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

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        JobEmployeeAssignment assignment = JobEmployeeAssignment.builder()
                .hierarchicalEntity(job)
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

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(job));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(job, employee)).thenReturn(false);
        when(assignmentRepository.save(any())).thenReturn(assignment);
        when(hierarchyMapper.toDto(job, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(employee)).thenReturn(employeeResponseDto);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = jobEmployeeAssignmentService
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
        verify(assignmentRepository).existsByHierarchicalEntityAndAssignableEntity(job, employee);
        verify(hierarchyMapper).toDto(any(), anyBoolean());
        verify(assignableEntityMapper).toDto(any());
        verify(assignmentRepository).save(any());
    }
    
    /**
     * Tests the successful removal of an assignable entity from a hierarchical entity.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_Success() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Job job = Job.builder()
                .id(hierarchicalEntityId)
                .name("Manager")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(job.getId())
                .name(job.getName())
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
        job.setEmployees(employees);

        employee.setJob(job);

        JobEmployeeAssignment assignment = JobEmployeeAssignment.builder()
                .hierarchicalEntity(job)
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

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(job));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(job, employee)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any())).thenReturn(job);
        doNothing().when(assignmentMetadataRepository).delete(any());
        when(hierarchyMapper.toDto(job, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(employee)).thenReturn(employeeResponseDto);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = jobEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(hierarchicalEntityId, assignableEntityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(hierarchicalEntityId);
        assertThat(result.getAssignableEntity().getId()).isEqualTo(assignableEntityId);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(job, employee);
        verify(assignmentMetadataRepository).delete(any());
        verify(assignmentRepository).delete(assignment);
        verify(hierarchicalEntityRepository).save(job);
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

        Job job = Job.builder()
                .id(hierarchicalEntityId)
                .name("Manager")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(job.getId())
                .name(job.getName())
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

        List<JobEmployeeAssignment> assignments = employees.stream()
                .map(emp -> JobEmployeeAssignment.builder()
                        .hierarchicalEntity(job)
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

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(job));
        when(assignableEntityRepository.findAllById(assignableEntityIds)).thenReturn(employees);
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(any(), any())).thenReturn(false);
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(hierarchyMapper.toDto(job, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(any())).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            return employeeResponseDtos.stream()
                    .filter(dto -> dto.getId().equals(emp.getId()))
                    .findFirst()
                    .orElse(null);
        });

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = jobEmployeeAssignmentService
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
     * Test case for the successful bulk assignment of hierarchical entities to an assignable entity.
     * This test ensures that the method correctly creates assignments and returns a populated BulkAssignmentResponseDto.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_AssignmentAlreadyExistsException() {
        // Given
        Long assignableEntityId = 1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        Employee assignableEntity = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(assignableEntityId)
                .firstName(assignableEntity.getFirstName())
                .lastName(assignableEntity.getLastName())
                .build();

        Job job1 = Job.builder()
                .id(hierarchicalEntityIds.get(0))
                .name("Manager")
                .employees(new HashSet<>())
                .build();

        Job job2 = Job.builder()
                .id(hierarchicalEntityIds.get(1))
                .name("Architect")
                .employees(new HashSet<>())
                .build();

        Job job3 = Job.builder()
                .id(hierarchicalEntityIds.get(2))
                .name("Developer")
                .employees(new HashSet<>())
                .build();

        List<Job> hierarchicalEntities = Arrays.asList(job1, job2, job3);

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
            Job emp = invocation.getArgument(0);
            return hierarchyResponseDtos.stream()
                    .filter(dto -> dto.getId().equals(emp.getId()))
                    .findFirst()
                    .orElse(null);
        });

        // When & Then
        assertThatThrownBy(() -> jobEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(assignableEntityId, hierarchicalEntityIds, metadataDtos))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Employee is already assigned to a job.");

        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(hierarchicalEntityRepository).findAllById(hierarchicalEntityIds);
        verify(assignmentRepository, times(2)).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(assignableEntityRepository, never()).save(assignableEntity);
        verify(assignableEntityMapper, never()).toDto(any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
    }

    /**
     * Test case for the successful retrieval of all assignments for specific hierarchical and assignable entity classes.
     * This test ensures that the method retrieves assignments based on the given classes and converts them into a list of AssignmentResponseDto.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Success() {
        // Given
        Job job = Job.builder()
                .id(1L)
                .name("Manager")
                .build();

        Employee employee = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .build();

        JobEmployeeAssignment assignment = JobEmployeeAssignment.builder()
                .id(3L)
                .hierarchicalEntity(job)
                .assignableEntity(employee)
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(job.getId())
                .name(job.getName())
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

        List<JobEmployeeAssignment> assignments = Collections.singletonList(assignment);
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> expectedDtos = Collections.singletonList(dto);

        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class))).thenReturn(assignments);
        when(assignmentMapper.toDto(any(JobEmployeeAssignment.class))).thenReturn(dto);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = jobEmployeeAssignmentService.getAssignmentsByEntityClasses();

        // Then
        assertThat(result).isEqualTo(expectedDtos);

        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class));
        verify(assignmentMapper, times(1)).toDto(assignment);
    }
}