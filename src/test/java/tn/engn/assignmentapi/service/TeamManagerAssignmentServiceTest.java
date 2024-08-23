package tn.engn.assignmentapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.engn.assignmentapi.dto.AssignmentMetadataRequestDto;
import tn.engn.assignmentapi.dto.AssignmentResponseDto;
import tn.engn.assignmentapi.dto.BulkAssignmentResponseDto;
import tn.engn.assignmentapi.exception.AssignmentAlreadyExistsException;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.AssignmentMetadata;
import tn.engn.assignmentapi.model.TeamManagerAssignment;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Team;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TeamManagerAssignmentServiceTest {
    @Mock
    private HierarchyBaseRepository<Team> hierarchicalEntityRepository;

    @Mock
    private EmployeeRepository assignableEntityRepository;

    @Mock
    private AssignmentRepository<Team, Employee, TeamManagerAssignment> assignmentRepository;

    @Mock
    private AssignmentMetadataRepository assignmentMetadataRepository;

    @Mock
    private HierarchyMapper<Team, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper;

    @Mock
    private AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper;

    @Mock
    private AssignmentMapper<Team, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper;

    @InjectMocks
    private TeamManagerAssignmentService teamEmployeeAssignmentService;

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

        Team team = Team.builder()
                .id(hierarchicalEntityId)
                .name("API Development")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(team.getId())
                .name(team.getName())
                .build();

        Employee employee = Employee.builder()
                .id(assignableEntityId)
                .firstName("John")
                .lastName("Doe")
                .teams(new HashSet<>())
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

        TeamManagerAssignment assignment = TeamManagerAssignment.builder()
                .hierarchicalEntity(team)
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

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(team));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(team, employee)).thenReturn(false);
        when(assignmentRepository.save(any())).thenReturn(assignment);
        when(hierarchyMapper.toDto(team, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(employee)).thenReturn(employeeResponseDto);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = teamEmployeeAssignmentService
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
        verify(assignmentRepository).existsByHierarchicalEntityAndAssignableEntity(team, employee);
        verify(hierarchyMapper).toDto(any(), anyBoolean());
        verify(assignableEntityMapper).toDto(any());
        verify(assignmentRepository).save(any());
    }

    /**
     * Tests the successful removal of an assignable entity from a hierarchical entity.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_AssignmentAlreadyExistsException() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        Team team = Team.builder()
                .id(hierarchicalEntityId)
                .name("API Development")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(team.getId())
                .name(team.getName())
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

        team.setManager(employee);

        Set<Team> teams = new HashSet<>();
        teams.add(team);
        employee.setTeams(teams);

        TeamManagerAssignment assignment = TeamManagerAssignment.builder()
                .hierarchicalEntity(team)
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

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(team));
        when(assignableEntityRepository.findById(assignableEntityId)).thenReturn(Optional.of(employee));
        when(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(team, employee)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any())).thenReturn(team);
        doNothing().when(assignmentMetadataRepository).delete(any());
        when(hierarchyMapper.toDto(team, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(employee)).thenReturn(employeeResponseDto);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = teamEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(hierarchicalEntityId, assignableEntityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(hierarchicalEntityId);
        assertThat(result.getAssignableEntity().getId()).isEqualTo(assignableEntityId);

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findById(assignableEntityId);
        verify(assignmentRepository).findByHierarchicalEntityAndAssignableEntity(team, employee);
        verify(assignmentMetadataRepository).delete(any());
        verify(assignmentRepository).delete(assignment);
        verify(hierarchicalEntityRepository).save(team);
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

        Team team = Team.builder()
                .id(hierarchicalEntityId)
                .name("API Development")
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(team.getId())
                .name(team.getName())
                .build();

        Employee employee1 = Employee.builder()
                .id(assignableEntityIds.get(0))
                .firstName("John")
                .lastName("Doe")
                .teams(new HashSet<>())
                .build();

        Employee employee2 = Employee.builder()
                .id(assignableEntityIds.get(1))
                .firstName("Jane")
                .lastName("Smith")
                .teams(new HashSet<>())
                .build();

        Employee employee3 = Employee.builder()
                .id(assignableEntityIds.get(2))
                .firstName("Emily")
                .lastName("Jones")
                .teams(new HashSet<>())
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

        List<TeamManagerAssignment> assignments = employees.stream()
                .map(emp -> TeamManagerAssignment.builder()
                        .hierarchicalEntity(team)
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

        when(hierarchicalEntityRepository.findById(hierarchicalEntityId)).thenReturn(Optional.of(team));
        when(assignableEntityRepository.findAllById(assignableEntityIds)).thenReturn(employees);
        when(assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(any(), any())).thenReturn(false);
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(hierarchyMapper.toDto(team, false)).thenReturn(hierarchyResponseDto);
        when(assignableEntityMapper.toDto(any())).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            return employeeResponseDtos.stream()
                    .filter(dto -> dto.getId().equals(emp.getId()))
                    .findFirst()
                    .orElse(null);
        });

        // When & Then
        assertThatThrownBy(() -> teamEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(hierarchicalEntityId, assignableEntityIds, metadataDtos))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Team is already assigned to a manager.");

        verify(hierarchicalEntityRepository).findById(hierarchicalEntityId);
        verify(assignableEntityRepository).findAllById(assignableEntityIds);
        verify(assignmentRepository, times(2)).existsByHierarchicalEntityAndAssignableEntity(any(), any());
        verify(hierarchyMapper, never()).toDto(any(), anyBoolean());
        verify(assignableEntityMapper, never()).toDto(any());
        verify(hierarchicalEntityRepository, never()).save(any());
        verify(assignmentRepository, never()).saveAll(any());
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
                .teams(new HashSet<>())
                .build();

        EmployeeResponseDto employeeResponseDto = EmployeeResponseDto.builder()
                .id(assignableEntityId)
                .firstName(assignableEntity.getFirstName())
                .lastName(assignableEntity.getLastName())
                .build();

        Team team1 = Team.builder()
                .id(hierarchicalEntityIds.get(0))
                .name("Software Team")
                .build();

        Team team2 = Team.builder()
                .id(hierarchicalEntityIds.get(1))
                .name("Backend Team")
                .build();

        Team team3 = Team.builder()
                .id(hierarchicalEntityIds.get(2))
                .name("Frontend Team")
                .build();

        List<Team> hierarchicalEntities = Arrays.asList(team1, team2, team3);

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
            Team emp = invocation.getArgument(0);
            return hierarchyResponseDtos.stream()
                    .filter(dto -> dto.getId().equals(emp.getId()))
                    .findFirst()
                    .orElse(null);
        });

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = teamEmployeeAssignmentService
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
     * Test case for the successful retrieval of all assignments for specific hierarchical and assignable entity classes.
     * This test ensures that the method retrieves assignments based on the given classes and converts them into a list of AssignmentResponseDto.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Success() {
        // Given
        Team team = Team.builder()
                .id(1L)
                .name("API Development")
                .build();

        Employee employee = Employee.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .build();

        TeamManagerAssignment assignment = TeamManagerAssignment.builder()
                .id(3L)
                .hierarchicalEntity(team)
                .assignableEntity(employee)
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(team.getId())
                .name(team.getName())
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

        List<TeamManagerAssignment> assignments = Collections.singletonList(assignment);
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> expectedDtos = Collections.singletonList(dto);

        when(assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class))).thenReturn(assignments);
        when(assignmentMapper.toDto(any(TeamManagerAssignment.class))).thenReturn(dto);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = teamEmployeeAssignmentService.getAssignmentsByEntityClasses();

        // Then
        assertThat(result).isEqualTo(expectedDtos);

        verify(assignmentRepository).findByHierarchicalEntityClassAndAssignableEntityClass(any(Class.class), any(Class.class));
        verify(assignmentMapper, times(1)).toDto(assignment);
    }
}