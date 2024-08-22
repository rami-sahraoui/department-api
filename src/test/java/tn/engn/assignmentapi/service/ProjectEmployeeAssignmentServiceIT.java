package tn.engn.assignmentapi.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tn.engn.HierarchicalEntityApiApplication;
import tn.engn.assignmentapi.dto.AssignmentMetadataRequestDto;
import tn.engn.assignmentapi.dto.AssignmentResponseDto;
import tn.engn.assignmentapi.exception.AssignmentAlreadyExistsException;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.AssignmentMetadata;
import tn.engn.assignmentapi.model.ProjectEmployeeAssignment;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.TestContainerSetup;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Project;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest(classes = HierarchicalEntityApiApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Reset context after each test
//@ActiveProfiles("test-real-db")
//public class ProjectEmployeeAssignmentServiceIT {
@ActiveProfiles("test-container")
public class ProjectEmployeeAssignmentServiceIT extends TestContainerSetup {
    @Autowired
    protected HierarchyBaseRepository<Project> hierarchicalEntityRepository;

    @Autowired
    protected EmployeeRepository assignableEntityRepository;

    @Autowired
    protected AssignmentRepository<Project, Employee, ProjectEmployeeAssignment> assignmentRepository;

    @Autowired
    protected AssignmentMetadataRepository assignmentMetadataRepository;

    @Autowired
    protected HierarchyMapper<Project, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper;

    @Autowired
    private AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper;

    @Autowired
    private AssignmentMapper<Project, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper;

    @Autowired
    private ProjectEmployeeAssignmentService projectEmployeeAssignmentService;
    @Autowired
    private Environment environment;

    /**
     * Clean up the database after each test to ensure isolation.
     */
    @AfterEach
    public void cleanUp() {
        assignmentMetadataRepository.deleteAll();
        assignmentRepository.deleteAll();
        assignableEntityRepository.deleteAll();
        hierarchicalEntityRepository.deleteAll();
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
     * Tests the successful assignment of an assignable entity to a hierarchical entity with metadata.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_Success() {
        // Given


        Project project = Project.builder()
                .name("API Development")
                .build();
        project = hierarchicalEntityRepository.save(project); // Persist the entity

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();

        employee = assignableEntityRepository.save(employee); // Persist the entity

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = projectEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(project.getId(), employee.getId(), metadataDtos);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity()).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(project.getId());
        assertThat(result.getAssignableEntity()).isNotNull();
        assertThat(result.getAssignableEntity().getId()).isEqualTo(employee.getId());
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).hasSize(1);
        assertThat(result.getMetadata().get(0).getKey()).isEqualTo("Role");
        assertThat(result.getMetadata().get(0).getValue()).isEqualTo("Manager");
    }

    /**
     * Tests the successful removal of an assignable entity from a hierarchical entity.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_Success() {
        // Given
        Project project = Project.builder()
                .name("API Development")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();

        project.setEmployee(employee);
        employee = assignableEntityRepository.save(employee);

        employee.setProject(project);
        project = hierarchicalEntityRepository.save(project);


        ProjectEmployeeAssignment assignment = ProjectEmployeeAssignment.builder()
                .hierarchicalEntity(project)
                .assignableEntity(employee)
                .build();

        assignment = assignmentRepository.save(assignment);

        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .key("Role")
                .value("Manager")
                .assignment(assignment)
                .build();

        assignment.setMetadata(Collections.singleton(metadata));
        assignmentRepository.save(assignment);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = projectEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(project.getId(), employee.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(project.getId());
        assertThat(result.getAssignableEntity().getId()).isEqualTo(employee.getId());

        assertThat(hierarchicalEntityRepository.findById(project.getId()).get().getEmployee()).isNull();
        assertThat(assignableEntityRepository.findById(employee.getId()).get().getProject()).isNull();
        assertThat(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(project, employee)).isEmpty();
    }

    /**
     * Tests the successful assignment of multiple assignable entities to a single hierarchical entity
     * with metadata for each assignment.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_AssignmentAlreadyExistsException() {
        // Given
        Project project = Project.builder()
                .name("API Development")
                .build();
        project = hierarchicalEntityRepository.save(project);
        Long hierarchicalEntityId = project.getId();

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Architect")
                .build();
        Employee employee3 = Employee.builder()
                .firstName("Emily")
                .lastName("Jones")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("emily.jones@email.com")
                .position("Developer")
                .build();
        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);
        assignableEntityRepository.saveAll(employees);

        List<Long> assignableEntityIds = employees.stream()
                .map(Employee::getId)
                .collect(Collectors.toList());

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        // When & Then
        assertThatThrownBy(() -> projectEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(hierarchicalEntityId, assignableEntityIds, metadataDtos))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Project is already assigned to an employee.");

        // Verify the assignments in the repository
        List<ProjectEmployeeAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).isEmpty();
    }

    /**
     * Test case for the successful bulk assignment of hierarchical entities to an assignable entity.
     * This test ensures that the method correctly creates assignments and returns a populated BulkAssignmentResponseDto.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_AssignmentAlreadyExistsException() {
        // Given
        Project project1 = Project.builder()
                .name("Java API Development")
                .build();

        Project project2 = Project.builder()
                .name("Python API Development")
                .build();

        Project project3 = Project.builder()
                .name("Javascript API Development")
                .build();

        project1 = hierarchicalEntityRepository.save(project1);
        project2 = hierarchicalEntityRepository.save(project2);
        project3 = hierarchicalEntityRepository.save(project3);

        List<Long> hierarchicalEntityIds = Arrays.asList(project1.getId(), project2.getId(), project3.getId());

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .build();

        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // When & Then
        assertThatThrownBy(() -> projectEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(assignableEntityId, hierarchicalEntityIds, null))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Employee is already assigned to a project.");

        // Verify the assignments in the repository
        List<ProjectEmployeeAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).isEmpty();
    }

    /**
     * Test case for the successful retrieval of all assignments for specific hierarchical and assignable entity classes.
     * This test ensures that the method retrieves assignments based on the given classes and converts them into a list of AssignmentResponseDto.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Success() {
        // Given
        Project project = Project.builder()
                .name("API Development")
                .build();
        project = hierarchicalEntityRepository.save(project);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        ProjectEmployeeAssignment assignment = ProjectEmployeeAssignment.builder()
                .hierarchicalEntity(project)
                .assignableEntity(employee)
                .build();
        assignment = assignmentRepository.save(assignment);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = projectEmployeeAssignmentService.getAssignmentsByEntityClasses();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHierarchicalEntity().getId()).isEqualTo(project.getId());
        assertThat(result.get(0).getAssignableEntity().getId()).isEqualTo(employee.getId());
    }

}
