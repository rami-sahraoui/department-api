package tn.engn.assignmentapi.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tn.engn.HierarchicalEntityApiApplication;
import tn.engn.assignmentapi.dto.*;
import tn.engn.assignmentapi.exception.AssignmentAlreadyExistsException;
import tn.engn.assignmentapi.exception.AssignmentNotFoundException;
import tn.engn.assignmentapi.exception.MetadataNotFoundException;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.AssignmentMetadata;
import tn.engn.assignmentapi.model.DepartmentEmployeeAssignment;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.TestContainerSetup;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest(classes = HierarchicalEntityApiApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Reset context after each test
//@ActiveProfiles("test-real-db")
//public class DepartmentEmployeeAssignmentServiceIT {
@ActiveProfiles("test-container")
public class DepartmentEmployeeAssignmentServiceIT extends TestContainerSetup {

    @Autowired
    protected HierarchyBaseRepository<Department> hierarchicalEntityRepository;

    @Autowired
    protected EmployeeRepository assignableEntityRepository;

    @Autowired
    protected AssignmentRepository<Department, Employee, DepartmentEmployeeAssignment> assignmentRepository;

    @Autowired
    protected AssignmentMetadataRepository assignmentMetadataRepository;

    @Autowired
    protected HierarchyMapper<Department, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper;

    @Autowired
    private AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper;

    @Autowired
    private AssignmentMapper<Department, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper;

    @Autowired
    private DepartmentEmployeeAssignmentService departmentEmployeeAssignmentService;
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
     * Tests the successful retrieval of hierarchical entities for a given assignable entity.
     * <p>
     * Given an assignable entity ID that exists, when hierarchical entities are requested,
     * then the service should return the corresponding hierarchical entities as DTOs.
     * </p>
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_Success() {
        // Given
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();

        employee = assignableEntityRepository.save(employee); // Persist the entity

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        // Create an assignment and persist it
        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .assignableEntity(employee)
                .hierarchicalEntity(department)
                .build();
        assignmentRepository.save(assignment);

        // When
        List<HierarchyResponseDto> result = departmentEmployeeAssignmentService.getHierarchicalEntitiesForAssignableEntity(employee.getId());

        // Then
        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(department.getId());
        assertThat(result.get(0).getName()).isEqualTo(department.getName());
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
        Long assignableEntityId = -1L; // Using a non-existent ID

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getHierarchicalEntitiesForAssignableEntity(assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
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

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();

        employee = assignableEntityRepository.save(employee); // Persist the entity

        // When
        List<HierarchyResponseDto> result = departmentEmployeeAssignmentService.getHierarchicalEntitiesForAssignableEntity(employee.getId());

        // Then
        assertThat(result).isNotNull().isEmpty();
    }

    /**
     * Tests the successful retrieval of hierarchical entities associated with a specified assignable entity
     * with pagination.
     * <p>
     * Given an assignable entity ID that exists, when hierarchical entities are requested with pagination,
     * then the service should return the corresponding hierarchical entities as paginated DTOs.
     * </p>
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_WithPagination_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();

        employee = assignableEntityRepository.save(employee); // Persist the entity

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        // Create an assignment and persist it
        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .assignableEntity(employee)
                .hierarchicalEntity(department)
                .build();
        assignmentRepository.save(assignment);

        // When
        PaginatedResponseDto<HierarchyResponseDto> result = departmentEmployeeAssignmentService
                .getHierarchicalEntitiesForAssignableEntity(employee.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(department.getId());
        assertThat(result.getContent().get(0).getName()).isEqualTo(department.getName());
    }

    /**
     * Tests the scenario where the assignable entity is not found, and an EntityNotFoundException is thrown.
     * <p>
     * Given a non-existent assignable entity ID, when hierarchical entities are requested with pagination,
     * then the service should throw an {@link EntityNotFoundException}.
     * </p>
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_WithPagination_EntityNotFound() {
        // Given
        Long assignableEntityId = -1L; // Using a non-existent ID
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .getHierarchicalEntitiesForAssignableEntity(assignableEntityId, pageable))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Tests the scenario where no hierarchical entities are associated with the specified assignable entity.
     * <p>
     * Given an assignable entity ID with no assigned hierarchical entities, when hierarchical entities are requested
     * with pagination, then the service should return an empty paginated result.
     * </p>
     */
    @Test
    public void testGetHierarchicalEntitiesForAssignableEntity_WithPagination_NoHierarchicalEntities() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();

        employee = assignableEntityRepository.save(employee); // Persist the entity

        // When
        PaginatedResponseDto<HierarchyResponseDto> result = departmentEmployeeAssignmentService
                .getHierarchicalEntitiesForAssignableEntity(employee.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    /**
     * Tests the successful assignment of an assignable entity to a hierarchical entity with metadata.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_Success() {
        // Given


        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();

        employee = assignableEntityRepository.save(employee); // Persist the entity

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(department.getId(), employee.getId(), metadataDtos);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity()).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertThat(result.getAssignableEntity()).isNotNull();
        assertThat(result.getAssignableEntity().getId()).isEqualTo(employee.getId());
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).hasSize(1);
        assertThat(result.getMetadata().get(0).getKey()).isEqualTo("Role");
        assertThat(result.getMetadata().get(0).getValue()).isEqualTo("Manager");
    }

    /**
     * Tests the successful assignment of an assignable entity to a hierarchical entity without metadata.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_Success_withoutMetadata() {
        // Given

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee); // Persist the entity

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(department.getId(), employee.getId(), null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity()).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertThat(result.getAssignableEntity()).isNotNull();
        assertThat(result.getAssignableEntity().getId()).isEqualTo(employee.getId());
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).isEmpty();
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

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId, metadataDtos))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Tests the scenario where the assignable entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_AssignableEntityNotFound() {
        // Given
        Long assignableEntityId = 1L;

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        // When & Then
        Department finalDepartment = department;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(finalDepartment.getId(), assignableEntityId, metadataDtos))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Tests the scenario where an assignment already exists between the specified hierarchical entity
     * and assignable entity, and an AssignmentAlreadyExistsException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_AssignmentAlreadyExists() {
        // Given

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee); // Persist the entity

        DepartmentEmployeeAssignment existingAssignment = DepartmentEmployeeAssignment.builder()
                .assignableEntity(employee)
                .hierarchicalEntity(department)
                .build();
        assignmentRepository.save(existingAssignment); // Persist the assignment

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        // When & Then
        Department finalDepartment = department;
        Employee finalEmployee = employee;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(finalDepartment.getId(), finalEmployee.getId(), metadataDtos))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Assignment already exists between the specified hierarchical entity and assignable entity");
    }

    /**
     * Tests the scenario where metadata with a null key is provided, and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_NullKey() {
        // Given

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee); // Persist the entity

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key(null)
                        .value("Manager")
                        .build()
        );

        // When & Then
        Department finalDepartment = department;
        Employee finalEmployee = employee;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(finalDepartment.getId(), finalEmployee.getId(), metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Metadata key cannot be null or empty.");
    }

    /**
     * Tests the scenario where metadata with an empty key is provided, and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_EmptyKey() {
        // Given

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee); // Persist the entity

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("")
                        .value("Manager")
                        .build()
        );

        // When & Then
        Department finalDepartment = department;
        Employee finalEmployee = employee;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(finalDepartment.getId(), finalEmployee.getId(), metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Metadata key cannot be null or empty.");
    }

    /**
     * Tests the scenario where metadata with a null value is provided, and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_NullValue() {
        // Given

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        department = hierarchicalEntityRepository.save(department); // Persist the entity

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee); // Persist the entity

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value(null)
                        .build()
        );

        // When & Then
        Department finalDepartment = department;
        Employee finalEmployee = employee;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(finalDepartment.getId(), finalEmployee.getId(), metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Metadata value cannot be null or empty.");
    }

    /**
     * Tests the scenario where the metadata list contains duplicate keys, and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_DuplicateMetadataKeys() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        assignableEntityRepository.save(employee);

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

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(department.getId(), employee.getId(), metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata list contains duplicate keys.");

        // Ensure no assignment was saved
        assertThat(assignmentRepository.findAll()).isEmpty();
    }

    /**
     * Tests the scenario where a metadata key exceeds the length limit of 255 characters,
     * and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_KeyLengthExceedsLimit() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        assignableEntityRepository.save(employee);

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("a".repeat(256)) // Key length exceeds the 255-character limit
                        .value("Manager")
                        .build()
        );

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(department.getId(), employee.getId(), metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata key length cannot exceed 255 characters.");

        // Ensure no assignment was saved
        assertThat(assignmentRepository.findAll()).isEmpty();
    }

    /**
     * Tests the scenario where a metadata key contains invalid characters,
     * and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_InvalidKeyCharacters() {
        // Given

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        assignableEntityRepository.save(employee);

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Invalid Key!") // Contains space and exclamation mark, which are invalid
                        .value("Manager")
                        .build()
        );

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(department.getId(), employee.getId(), metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata key contains invalid characters.");

        // Ensure no assignment was saved
        assertThat(assignmentRepository.findAll()).isEmpty();
    }

    /**
     * Tests the scenario where a metadata value exceeds the length limit of 1024 characters,
     * and a ValidationException is thrown.
     */
    @Test
    public void testAssignEntityToHierarchicalEntity_InvalidMetadata_ValueLengthExceedsLimit() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        assignableEntityRepository.save(employee);

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("a".repeat(1025)) // Value length exceeds the 1024-character limit
                        .build()
        );

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .assignEntityToHierarchicalEntity(department.getId(), employee.getId(), metadataDtos))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Metadata value length cannot exceed 1024 characters.");

        // Ensure no assignment was saved
        assertThat(assignmentRepository.findAll()).isEmpty();
    }

    /**
     * Tests the successful removal of an assignable entity from a hierarchical entity.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
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
                .build();

        department = hierarchicalEntityRepository.save(department);
        employee = assignableEntityRepository.save(employee);
        assignment = assignmentRepository.save(assignment);

        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .key("Role")
                .value("Manager")
                .assignment(assignment)
                .build();

        assignment.setMetadata(Collections.singleton(metadata));
        assignmentRepository.save(assignment);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(department.getId(), employee.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertThat(result.getAssignableEntity().getId()).isEqualTo(employee.getId());

        assertThat(hierarchicalEntityRepository.findById(department.getId()).get().getEmployees()).doesNotContain(employee);
        assertThat(assignableEntityRepository.findById(employee.getId()).get().getDepartments()).doesNotContain(department);
        assertThat(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(department, employee)).isEmpty();
    }

    /**
     * Tests the scenario where the hierarchical entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        Long assignableEntityId = 2L;

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Tests the scenario where the assignable entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_AssignableEntityNotFound() {
        // Given
        Long assignableEntityId = 2L;

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();

        department = hierarchicalEntityRepository.save(department);

        // When & Then
        Department finalDepartment = department;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(finalDepartment.getId(), assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Tests the scenario where the assignment does not exist between the hierarchical entity and the assignable entity.
     */
    @Test
    public void testRemoveEntityFromHierarchicalEntity_AssignmentNotFound() {
        // Given

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();

        hierarchicalEntityRepository.save(department);
        assignableEntityRepository.save(employee);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .removeEntityFromHierarchicalEntity(department.getId(), employee.getId()))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    /**
     * Tests the successful assignment of multiple assignable entities to a single hierarchical entity
     * with metadata for each assignment.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Architect")
                .departments(new HashSet<>())
                .build();
        Employee employee3 = Employee.builder()
                .firstName("Emily")
                .lastName("Jones")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("emily.jones@email.com")
                .position("Developer")
                .departments(new HashSet<>())
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

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(department.getId(), assignableEntityIds, metadataDtos);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntities()).isNotNull();
        assertThat(result.getHierarchicalEntities().get(0).getId()).isEqualTo(department.getId());

        List<EmployeeResponseDto> resultEmployees = result.getAssignableEntities();
        assertThat(resultEmployees).isNotNull();
        assertThat(resultEmployees).hasSize(3);
        assertThat(resultEmployees).extracting(EmployeeResponseDto::getId).containsExactlyInAnyOrderElementsOf(assignableEntityIds);

        List<AssignmentMetadataResponseDto> resultMetadata = result.getMetadata();
        assertThat(resultMetadata).isNotNull();
        assertThat(resultMetadata).hasSize(1);
        assertThat(resultMetadata.get(0).getKey()).isEqualTo("Role");
        assertThat(resultMetadata.get(0).getValue()).isEqualTo("Manager");

        // Verify the assignments in the repository
        List<DepartmentEmployeeAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).hasSize(3); // One assignment for each employee
    }

    /**
     * Tests the scenario where the hierarchical entity is not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = 1L;
        List<Long> assignableEntityIds = Arrays.asList(2L, 3L, 4L);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(hierarchicalEntityId, assignableEntityIds, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");

        // Verify that no assignments were made
        List<DepartmentEmployeeAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).isEmpty();
    }


    /**
     * Tests the scenario where one or more assignable entities are not found, and an EntityNotFoundException is thrown.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_OneOrMoreAssignableEntitiesNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        Employee employee3 = Employee.builder()
                .firstName("Emily")
                .lastName("Jones")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("emily.jones@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build();
        List<Employee> employees = Arrays.asList(employee1, employee3);
        assignableEntityRepository.saveAll(employees);

        List<Long> assignableEntityIds = Arrays.asList(employee1.getId(), -1L, employee3.getId()); // Missing employee2

        // When & Then
        Department finalDepartment = department;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(finalDepartment.getId(), assignableEntityIds, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("One or more assignable entities not found");

        // Verify that no assignments were made
        List<DepartmentEmployeeAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).isEmpty();
    }

    /**
     * Tests the scenario where an assignment already exists for one or more of the assignable entities,
     * and an AssignmentAlreadyExistsException is thrown.
     */
    @Test
    public void testBulkAssignAssignableEntitiesToHierarchicalEntity_AssignmentAlreadyExists() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Architect")
                .departments(new HashSet<>())
                .build();
        Employee employee3 = Employee.builder()
                .firstName("Emily")
                .lastName("Jones")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("emily.jones@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build();
        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);
        assignableEntityRepository.saveAll(employees);

        List<Long> assignableEntityIds = employees.stream()
                .map(Employee::getId)
                .collect(Collectors.toList());

        // Prepopulate the assignment to trigger the "already exists" exception
        DepartmentEmployeeAssignment existingAssignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee1)
                .build();
        existingAssignment = assignmentRepository.save(existingAssignment);

        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .key("Role")
                .value("Manager")
                .assignment(existingAssignment)
                .build();

        existingAssignment.setMetadata(Collections.singleton(metadata));
        assignmentRepository.save(existingAssignment);

        // When & Then
        Department finalDepartment = department;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignAssignableEntitiesToHierarchicalEntity(finalDepartment.getId(), assignableEntityIds, null))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Assignment already exists for hierarchical entity ID: " + department.getId()
                        + " and assignable entity ID: " + employee1.getId());

        // Verify that the existing assignment is still there
        List<DepartmentEmployeeAssignment> assignments = assignmentRepository.findAll();
        assertThat(assignments).hasSize(1);
    }

    /**
     * Test case for the successful removal of multiple assignable entities from a hierarchical entity.
     * This test ensures that the method correctly removes the assignable entities, deletes associated
     * assignments and metadata, and returns a populated BulkAssignmentResponseDto.
     */
    @Test
    public void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Architect")
                .departments(new HashSet<>())
                .build();

        Employee employee3 = Employee.builder()
                .firstName("Emily")
                .lastName("Jones")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("emily.jones@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);

        Set<Department> departments = new HashSet<>();
        departments.add(department);
        employee1.setDepartments(departments);
        employee2.setDepartments(departments);
        employee3.setDepartments(departments);

        department.setEmployees(new HashSet<>(employees));

        // Save data to the database
        hierarchicalEntityRepository.save(department);
        employees = assignableEntityRepository.saveAll(employees);

        List<Long> assignableEntityIds = employees.stream()
                .map(Employee::getId)
                .collect(Collectors.toList());

        List<DepartmentEmployeeAssignment> assignments = employees.stream()
                .map(emp -> DepartmentEmployeeAssignment.builder()
                        .hierarchicalEntity(department)
                        .assignableEntity(emp)
                        .build())
                .collect(Collectors.toList());

        assignments = assignmentRepository.saveAll(assignments);

        Set<AssignmentMetadata> metadataSet = assignments.stream()
                .map(assignment -> {
                    AssignmentMetadata metadata = AssignmentMetadata.builder()
                            .key("Role")
                            .value("Manager")
                            .assignment(assignment)
                            .build();
                    assignment.setMetadata(Collections.singleton(metadata));
                    assignmentRepository.save(assignment);
                    return metadata;
                })
                .collect(Collectors.toSet());

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .bulkRemoveAssignableEntitiesFromHierarchicalEntity(department.getId(), assignableEntityIds);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntities()).hasSize(1);
        assertThat(result.getHierarchicalEntities().get(0).getId()).isEqualTo(department.getId());
        assertThat(result.getAssignableEntities()).hasSize(3);
        assertThat(result.getAssignableEntities().stream().map(EmployeeResponseDto::getId))
                .containsExactlyInAnyOrderElementsOf(assignableEntityIds);

        assertThat(hierarchicalEntityRepository.findById(department.getId())).isPresent();
        assertThat(assignableEntityRepository.findAllById(assignableEntityIds)).hasSize(3);
        assertThat(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(department, employee1)).isEmpty();
        assertThat(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(department, employee2)).isEmpty();
        assertThat(assignmentRepository.findByHierarchicalEntityAndAssignableEntity(department, employee3)).isEmpty();
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

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveAssignableEntitiesFromHierarchicalEntity(hierarchicalEntityId, assignableEntityIds))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Test case for handling the scenario where one or more assignable entities are not found.
     * This test ensures that the method throws an EntityNotFoundException when one or more of
     * the provided assignable entity IDs do not correspond to existing entities in the repository.
     */
    @Test
    public void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_OneOrMoreAssignableEntitiesNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Architect")
                .departments(new HashSet<>())
                .build();

        // Missing employee3
        List<Employee> employees = Arrays.asList(employee1, employee2);

        department = hierarchicalEntityRepository.save(department);
        employees = assignableEntityRepository.saveAll(employees);

        List<Long> assignableEntityIds = employees.stream()
                .map(Employee::getId)
                .collect(Collectors.toList());
        assignableEntityIds.add(-1L);

        // When & Then
        Department finalDepartment = department;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveAssignableEntitiesFromHierarchicalEntity(finalDepartment.getId(), assignableEntityIds))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("One or more assignable entities not found");
    }

    /**
     * Test case for handling the scenario where an assignment between the hierarchical entity
     * and an assignable entity is not found. This test ensures that the method skips removal
     * actions when no valid assignment is found and returns an appropriate response.
     */
    @Test
    public void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_AssignmentNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();

        HierarchyResponseDto hierarchyResponseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .build();

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Architect")
                .departments(new HashSet<>())
                .build();

        Employee employee3 = Employee.builder()
                .firstName("Emily")
                .lastName("Jones")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("emily.jones@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);

        department = hierarchicalEntityRepository.save(department);
        employees = assignableEntityRepository.saveAll(employees);

        List<Long> assignableEntityIds = employees.stream()
                .map(Employee::getId)
                .collect(Collectors.toList());

        // When
        Department finalDepartment = department;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveAssignableEntitiesFromHierarchicalEntity(finalDepartment.getId(), assignableEntityIds))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    /**
     * Test case for the successful bulk assignment of hierarchical entities to an assignable entity.
     * This test ensures that the method correctly creates assignments and returns a populated BulkAssignmentResponseDto.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_Success() {
        // Given
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();

        Department department2 = Department.builder()
                .name("HR")
                .path("/2/")
                .employees(new HashSet<>())
                .build();

        Department department3 = Department.builder()
                .name("Finance")
                .path("/3/")
                .employees(new HashSet<>())
                .build();

        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);
        department3 = hierarchicalEntityRepository.save(department3);

        List<Long> hierarchicalEntityIds = Arrays.asList(department1.getId(), department2.getId(), department3.getId());

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();

        employee = assignableEntityRepository.save(employee);

        // When
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(employee.getId(), hierarchicalEntityIds, new ArrayList<>());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAssignableEntities()).hasSize(1);
        assertThat(result.getAssignableEntities().get(0).getId()).isEqualTo(employee.getId());
        assertThat(result.getHierarchicalEntities()).hasSize(3);
        assertThat(result.getHierarchicalEntities().stream().map(HierarchyResponseDto::getId))
                .containsExactlyInAnyOrderElementsOf(hierarchicalEntityIds);
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found in the repository.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_AssignableEntityNotFound() {
        // Given
        Long assignableEntityId = -1L; // Non-existent ID
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(assignableEntityId, hierarchicalEntityIds, new ArrayList<>()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Test case for handling the scenario where one or more hierarchical entities are not found.
     * This test ensures that the method throws an EntityNotFoundException when one or more of
     * the provided hierarchical entity IDs do not correspond to existing entities in the repository.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_OneOrMoreHierarchicalEntitiesNotFound() {
        // Given
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();

        Department department2 = Department.builder()
                .name("HR")
                .path("/2/")
                .build();

        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);

        List<Long> hierarchicalEntityIds = Arrays.asList(department1.getId(), department2.getId(), -1L); // One invalid ID

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();

        employee = assignableEntityRepository.save(employee);

        // When & Then
        Employee finalEmployee = employee;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(finalEmployee.getId(), hierarchicalEntityIds, new ArrayList<>()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("One or more hierarchical entities not found");
    }

    /**
     * Test case for handling the scenario where an assignment between the hierarchical entity
     * and an assignable entity already exists. This test ensures that the method throws an
     * AssignmentAlreadyExistsException.
     */
    @Test
    public void testBulkAssignHierarchicalEntitiesToAssignableEntity_AssignmentAlreadyExists() {
        // Given
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build();

        department1 = hierarchicalEntityRepository.save(department1);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();

        employee = assignableEntityRepository.save(employee);

        // Create initial assignment
        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department1)
                .assignableEntity(employee)
                .build();

        assignmentRepository.save(assignment);

        List<Long> hierarchicalEntityIds = Collections.singletonList(department1.getId());

        // When & Then
        Employee finalEmployee = employee;
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkAssignHierarchicalEntitiesToAssignableEntity(finalEmployee.getId(), hierarchicalEntityIds, new ArrayList<>()))
                .isInstanceOf(AssignmentAlreadyExistsException.class)
                .hasMessage("Assignment already exists for hierarchical entity ID: " + department1.getId() + " and assignable entity ID: " + employee.getId());
    }

    /**
     * Test case for the successful bulk removal of hierarchical entities from an assignable entity.
     * This test ensures that the method correctly removes assignments and returns a populated
     * BulkAssignmentResponseDto with the remaining assignments.
     */
    @Test
    public void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_Success() {
        // Given: An employee assigned to multiple departments
        Employee employee = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        Department department1 = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Department department2 = hierarchicalEntityRepository.save(Department.builder()
                .name("HR")
                .path("/2/")
                .employees(new HashSet<>())
                .build());

        Department department3 = hierarchicalEntityRepository.save(Department.builder()
                .name("Finance")
                .path("/3/")
                .employees(new HashSet<>())
                .build());

        List<Department> departments =  Arrays.asList(department1, department2, department3);

        Set<Employee> employees = new HashSet<>();
        employees.add(employee);
        department1.setEmployees(employees);
        department2.setEmployees(employees);
        department3.setEmployees(employees);

        employee.setDepartments(new HashSet<>(departments));

        departments = hierarchicalEntityRepository.saveAll(departments);
        employee = assignableEntityRepository.save(employee);

        Employee finalEmployee = employee;
        List<DepartmentEmployeeAssignment> assignments = departments.stream()
                .map(department -> DepartmentEmployeeAssignment.builder()
                        .hierarchicalEntity(department)
                        .assignableEntity(finalEmployee)
                        .build())
                .collect(Collectors.toList());

        assignments = assignmentRepository.saveAll(assignments);

        Set<AssignmentMetadata> metadataSet = assignments.stream()
                .map(assignment -> {
                    AssignmentMetadata metadata = AssignmentMetadata.builder()
                            .key("Role")
                            .value("Manager")
                            .assignment(assignment)
                            .build();
                    assignment.setMetadata(Collections.singleton(metadata));
                    assignmentRepository.save(assignment);
                    return metadata;
                })
                .collect(Collectors.toSet());

        // When: Removing departments from the employee
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> removedDepartments = departmentEmployeeAssignmentService
                .bulkRemoveHierarchicalEntitiesFromAssignableEntity(
                        employee.getId(),
                        List.of(department1.getId(), department2.getId()));

        // Then: The removed departments should be the ones specified, and only the remaining ones should be in the employee's departments
        assertThat(removedDepartments.getHierarchicalEntities()).hasSize(2);
        assertThat(removedDepartments.getHierarchicalEntities()).extracting("id")
                .containsExactlyInAnyOrder(department1.getId(), department2.getId());

        Employee updatedEmployee = assignableEntityRepository.findById(employee.getId()).orElseThrow();
        assertThat(updatedEmployee.getDepartments()).hasSize(1);
        assertThat(updatedEmployee.getDepartments()).contains(department3);
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found during the bulk removal operation.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found
     * in the repository.
     */
    @Test
    public void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_AssignableEntityNotFound() {
        // Given: A non-existent employee ID
        Long assignableEntityId = -1L;
        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        // When/Then: Attempting to remove departments should throw a DepartmentNotFoundException
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveHierarchicalEntitiesFromAssignableEntity(assignableEntityId, hierarchicalEntityIds))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Test case for handling the scenario where one or more hierarchical entities are not found during
     * the bulk removal operation. This test ensures that the method throws an EntityNotFoundException
     * when one or more of the provided hierarchical entity IDs do not correspond to existing entities
     * in the repository.
     */
    @Test
    public void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_OneOrMoreHierarchicalEntitiesNotFound() {
        // Given: An employee and a non-existent department ID
        Employee employee = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        List<Long> hierarchicalEntityIds = Arrays.asList(2L, 3L, 4L);

        // When/Then: Attempting to remove a non-existent department should throw a DepartmentNotFoundException
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .bulkRemoveHierarchicalEntitiesFromAssignableEntity(employee.getId(), hierarchicalEntityIds))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("One or more hierarchical entities not found");
    }

    /**
     * Test case for the successful retrieval of assignable entities associated with a hierarchical entity.
     * This test ensures that the method correctly retrieves and converts the assignable entities to DTOs
     * when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignableEntitiesByHierarchicalEntity_Success() {
        // Given: A hierarchical entity with employees
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee1 = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        Employee employee2 = assignableEntityRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build());

        List<Employee> employees = Arrays.asList(employee1, employee2);
        List<Long> employeeIds = employees.stream().map(e -> e.getId()).collect(Collectors.toList());

        // Assign employees to department
        departmentEmployeeAssignmentService.bulkAssignAssignableEntitiesToHierarchicalEntity(department.getId(), employeeIds, null);

        // When: Retrieving assignable entities by hierarchical entity ID
        List<EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .getAssignableEntitiesByHierarchicalEntity(department.getId());

        // Then: The result should contain the correct assignable entities
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting("id")
                .containsExactlyInAnyOrder(employee1.getId(), employee2.getId());
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found during the retrieval of
     * assignable entities. This test ensures that the method throws an EntityNotFoundException when the
     * hierarchical entity is not found in the repository.
     */
    @Test
    public void testGetAssignableEntitiesByHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given: An ID for a non-existent hierarchical entity
        Long nonExistentId = -1L;

        // When & Then: Retrieving assignable entities should throw an EntityNotFoundException
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .getAssignableEntitiesByHierarchicalEntity(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Test case for the successful retrieval of a paginated list of assignable entities associated with a hierarchical entity.
     * This test ensures that the method correctly retrieves and converts the assignable entities to a paginated response DTO
     * when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignableEntitiesByHierarchicalEntity_Paginated_Success() {
        // Given: A hierarchical entity and paginated assignable entities
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee1 = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        Employee employee2 = assignableEntityRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build());

        Pageable pageable = PageRequest.of(0, 2); // Define the pagination parameters

        // Assign employees to department
        departmentEmployeeAssignmentService.bulkAssignAssignableEntitiesToHierarchicalEntity(department.getId(),
                Arrays.asList(employee1.getId(), employee2.getId()), null);

        // When: Retrieving assignable entities by hierarchical entity ID
        PaginatedResponseDto<EmployeeResponseDto> result = departmentEmployeeAssignmentService
                .getAssignableEntitiesByHierarchicalEntity(department.getId(), pageable);

        // Then: The result should contain the correct paginated assignable entities
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting("id").containsExactlyInAnyOrder(employee1.getId(), employee2.getId());
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found during the paginated retrieval of
     * assignable entities. This test ensures that the method throws an EntityNotFoundException when the hierarchical entity
     * is not found in the repository.
     */
    @Test
    public void testGetAssignableEntitiesByHierarchicalEntity_Paginated_HierarchicalEntityNotFound() {
        // Given: An ID for a non-existent hierarchical entity
        Long nonExistentId = -1L;
        Pageable pageable = PageRequest.of(0, 2);

        // When & Then: Retrieving assignable entities should throw an EntityNotFoundException
        assertThatThrownBy(() -> departmentEmployeeAssignmentService
                .getAssignableEntitiesByHierarchicalEntity(nonExistentId, pageable))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Test case for the successful retrieval of the count of assignable entities associated with a hierarchical entity.
     * This test ensures that the method correctly returns the count of assignable entities when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignableEntityCountByHierarchicalEntity_Success() {
        // Given: A hierarchical entity with assigned employees
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee1 = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        Employee employee2 = assignableEntityRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build());

        // Assign employees to department
        departmentEmployeeAssignmentService.bulkAssignAssignableEntitiesToHierarchicalEntity(department.getId(),
                Arrays.asList(employee1.getId(), employee2.getId()), null);

        // When: Retrieving the count of assignable entities by hierarchical entity ID
        int result = departmentEmployeeAssignmentService.getAssignableEntityCountByHierarchicalEntity(department.getId());

        // Then: The result should be equal to the number of assigned employees
        assertThat(result).isEqualTo(2);
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found during the retrieval of
     * the count of assignable entities. This test ensures that the method throws an EntityNotFoundException when the hierarchical entity
     * is not found in the repository.
     */
    @Test
    public void testGetAssignableEntityCountByHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given: An ID for a non-existent hierarchical entity
        Long nonExistentId = -1L;

        // When & Then: Retrieving the count of assignable entities should throw an EntityNotFoundException
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getAssignableEntityCountByHierarchicalEntity(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Test case for the successful retrieval of the count of hierarchical entities associated with an assignable entity.
     * This test ensures that the method correctly returns the count of hierarchical entities when the assignable entity exists.
     */
    @Test
    public void testGetHierarchicalEntityCountByAssignableEntity_Success() {
        // Given: An assignable entity (Employee) with associated hierarchical entities (Departments)
        Employee employee = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        Department department1 = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Department department2 = hierarchicalEntityRepository.save(Department.builder()
                .name("HR")
                .path("/2/")
                .employees(new HashSet<>())
                .build());

        // Assign departments to the employee
        departmentEmployeeAssignmentService.bulkAssignHierarchicalEntitiesToAssignableEntity(employee.getId(),
                Arrays.asList(department1.getId(), department2.getId()), null);

        // When: Retrieving the count of hierarchical entities by assignable entity ID
        int result = departmentEmployeeAssignmentService.getHierarchicalEntityCountByAssignableEntity(employee.getId());

        // Then: The result should be equal to the number of associated departments
        assertThat(result).isEqualTo(2);
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found during the retrieval of
     * the count of hierarchical entities. This test ensures that the method throws an EntityNotFoundException when the assignable entity
     * is not found in the repository.
     */
    @Test
    public void testGetHierarchicalEntityCountByAssignableEntity_EntityNotFound() {
        // Given: An ID for a non-existent assignable entity
        Long nonExistentId = -1L;

        // When & Then: Retrieving the count of hierarchical entities should throw an EntityNotFoundException
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getHierarchicalEntityCountByAssignableEntity(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Test case for the successful retrieval of all assignments.
     * This test ensures that the method retrieves all assignments from the repository and converts them into a list of AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> objects.
     */
    @Test
    public void testGetAllAssignments_Success() {
        // Given: A department, an employee, and an assignment between them exist in the database
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        DepartmentEmployeeAssignment assignment = assignmentRepository.save(DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build());

        // When: Retrieving all assignments
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAllAssignments();

        // Then: The result should contain the correct assignment details
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertThat(result.get(0).getAssignableEntity().getId()).isEqualTo(employee.getId());
    }

    /**
     * Test case for handling the scenario where no assignments are found in the repository.
     * This test ensures that the method returns an empty list when there are no assignments.
     */
    @Test
    public void testGetAllAssignments_EmptyList() {
        // Given: No assignments exist in the database
        // Ensure the repository is empty
        assignmentRepository.deleteAll();

        // When: Retrieving all assignments
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAllAssignments();

        // Then: The result should be an empty list
        assertThat(result).isEmpty();
    }

    /**
     * Test case for the successful retrieval of all assignments with pagination.
     * This test ensures that the method retrieves paginated assignments from the repository and converts them into a PaginatedResponseDto<AssignmentResponseDto<H, D>>.
     */
    @Test
    public void testGetAllAssignments_WithPagination_Success() {
        // Given: A department, an employee, and an assignment between them exist in the database
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        DepartmentEmployeeAssignment assignment = assignmentRepository.save(DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build());

        // Pagination setup
        Pageable pageable = PageRequest.of(0, 10);

        // When: Retrieving all assignments with pagination
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAllAssignments(pageable);

        // Then: The result should contain the correct paginated assignment details
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertThat(result.getContent().get(0).getAssignableEntity().getId()).isEqualTo(employee.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    /**
     * Test case for handling the scenario where no assignments are found with pagination.
     * This test ensures that the method returns an empty PaginatedResponseDto when there are no assignments.
     */
    @Test
    public void testGetAllAssignments_WithPagination_EmptyPage() {
        // Given: No assignments exist in the database
        assignmentRepository.deleteAll();

        // Pagination setup
        Pageable pageable = PageRequest.of(0, 10);

        // When: Retrieving all assignments with pagination
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAllAssignments(pageable);

        // Then: The result should be an empty PaginatedResponseDto
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    /**
     * Test case for the successful retrieval of all assignments for specific hierarchical and assignable entity classes.
     * This test ensures that the method retrieves assignments based on the given classes and converts them into a list of AssignmentResponseDto.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();
        assignment = assignmentRepository.save(assignment);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByEntityClasses();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertThat(result.get(0).getAssignableEntity().getId()).isEqualTo(employee.getId());
    }

    /**
     * Test case for handling the scenario where no assignments are found for the specified hierarchical and assignable entity classes.
     * This test ensures that the method returns an empty list of AssignmentResponseDto when there are no assignments for the given classes.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_NoAssignments() {
        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result = departmentEmployeeAssignmentService.getAssignmentsByEntityClasses();

        // Then
        assertThat(result).isEmpty();
    }

    /**
     * Test case for the successful retrieval of paginated assignments for specific hierarchical and assignable entity classes.
     * This test ensures that the method retrieves assignments with pagination and converts them into a PaginatedResponseDto.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Pagination_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();
        assignment = assignmentRepository.save(assignment);

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result =
                departmentEmployeeAssignmentService.getAssignmentsByEntityClasses(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertThat(result.getContent().get(0).getAssignableEntity().getId()).isEqualTo(employee.getId());
    }

    /**
     * Test case for handling the scenario where no assignments are found for the specified hierarchical and assignable entity classes with pagination.
     * This test ensures that the method returns an empty PaginatedResponseDto when there are no assignments for the given classes.
     */
    @Test
    public void testGetAssignmentsByEntityClasses_Pagination_NoAssignments() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result =
                departmentEmployeeAssignmentService.getAssignmentsByEntityClasses(pageable);

        // Then
        assertThat(result.getContent()).isEmpty();
    }

    /**
     * Test case for the successful update of an assignment based on the provided request DTO.
     * This test ensures that the method correctly retrieves, updates, and saves the assignment,
     * and returns the updated AssignmentResponseDto.
     */
    @Test
    public void testUpdateAssignment_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);

        AssignmentMetadataRequestDto existingMetadata = AssignmentMetadataRequestDto.builder()
                .key("key1")
                .value("oldValue1")
                .build();

        // Assign department to the employee
        departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), Collections.singletonList(existingMetadata));

        List<AssignmentMetadataRequestDto> metadataRequests = Arrays.asList(
                new AssignmentMetadataRequestDto("key1", "newValue1"),
                new AssignmentMetadataRequestDto("key2", "newValue2")
        );

        AssignmentRequestDto requestDto = new AssignmentRequestDto(department.getId(), employee.getId(), metadataRequests);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result = departmentEmployeeAssignmentService.updateAssignment(requestDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertThat(result.getAssignableEntity().getId()).isEqualTo(employee.getId());

        assertThat(result.getMetadata()).hasSize(2);
        assertThat(result.getMetadata()).extracting("key").containsExactlyInAnyOrder("key1", "key2");
        assertThat(result.getMetadata()).extracting("value").containsExactlyInAnyOrder("newValue1", "newValue2");
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical entity is not found.
     */
    @Test
    public void testUpdateAssignment_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = -1L;  // Non-existent ID
        Long assignableEntityId = 2L;
        List<AssignmentMetadataRequestDto> metadataRequests = Collections.emptyList();
        AssignmentRequestDto requestDto = new AssignmentRequestDto(hierarchicalEntityId, assignableEntityId, metadataRequests);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.updateAssignment(requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found.
     */
    @Test
    public void testUpdateAssignment_AssignableEntityNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Long assignableEntityId = -1L;  // Non-existent ID
        List<AssignmentMetadataRequestDto> metadataRequests = Collections.emptyList();
        AssignmentRequestDto requestDto = new AssignmentRequestDto(department.getId(), assignableEntityId, metadataRequests);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.updateAssignment(requestDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignment is not found.
     */
    @Test
    public void testUpdateAssignment_AssignmentNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);

        List<AssignmentMetadataRequestDto> metadataRequests = Collections.emptyList();
        AssignmentRequestDto requestDto = new AssignmentRequestDto(department.getId(), employee.getId(), metadataRequests);

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.updateAssignment(requestDto))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    /**
     * Test case for the successful removal of metadata from an assignment.
     * This test ensures that the method correctly removes the metadata and updates the assignment.
     */
    @Test
    public void testRemoveMetadataById_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);

        AssignmentMetadataRequestDto existingMetadata = AssignmentMetadataRequestDto.builder()
                .key("key1")
                .value("value1")
                .build();

        // Assign department to the employee
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignmentResponseDto
                = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                Collections.singletonList(existingMetadata));

        Long assignmentId = assignmentResponseDto.getId();
        Long metadataId = assignmentResponseDto.getMetadata().get(0).getId();

        // When
        departmentEmployeeAssignmentService.removeMetadataById(assignmentId, metadataId);

        // Then
        DepartmentEmployeeAssignment updatedAssignment = assignmentRepository.findById(assignmentId).orElse(null);
        assertThat(updatedAssignment).isNotNull();
        assertThat(updatedAssignment.getMetadata()).isEmpty();

        // Verify that metadata is also deleted from the metadata repository
        AssignmentMetadata deletedMetadata = assignmentMetadataRepository.findById(metadataId).orElse(null);
        assertThat(deletedMetadata).isNull();
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an AssignmentNotFoundException when the assignment is not found.
     */
    @Test
    public void testRemoveMetadataById_AssignmentNotFound() {
        // Given
        Long assignmentId = -1L;  // Non-existent ID
        Long metadataId = 2L;

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataById(assignmentId, metadataId))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    /**
     * Test case for handling the scenario where the metadata is not found.
     * This test ensures that the method throws a MetadataNotFoundException when the metadata is not found.
     */
    @Test
    public void testRemoveMetadataById_MetadataNotFound() {
        // Given
        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .key("key1")
                .value("value1")
                .build();
        metadata = assignmentMetadataRepository.save(metadata);

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .metadata(new HashSet<>())
                .build();
        assignment = assignmentRepository.save(assignment);

        Long assignmentId = assignment.getId();
        Long metadataId = metadata.getId() + 1L;  // Non-existent metadata ID

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataById(assignmentId, metadataId))
                .isInstanceOf(MetadataNotFoundException.class)
                .hasMessage("Metadata not found");
    }

    /**
     * Test case for the successful removal of metadata from an assignment by metadata key.
     * This test ensures that the method correctly removes the metadata with the specified key and updates the assignment.
     */
    @Test
    public void testRemoveMetadataByKey_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);

        String metadataKey = "key1";

        AssignmentMetadataRequestDto metadataRequestDto = AssignmentMetadataRequestDto.builder()
                .key(metadataKey)
                .value("value1")
                .build();

        // Assign department to the employee
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignmentResponseDto
                = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                Collections.singletonList(metadataRequestDto));

        Long assignmentId = assignmentResponseDto.getId();
        Long metadataId = assignmentResponseDto.getMetadata().get(0).getId();

        // When
        departmentEmployeeAssignmentService.removeMetadataByKey(assignmentId, metadataKey);

        // Then
        DepartmentEmployeeAssignment updatedAssignment = assignmentRepository.findById(assignmentId).orElse(null);
        assertThat(updatedAssignment).isNotNull();
        assertThat(updatedAssignment.getMetadata()).isEmpty();

        // Verify that metadata is also deleted from the metadata repository
        AssignmentMetadata deletedMetadata = assignmentMetadataRepository.findById(metadataId).orElse(null);
        assertThat(deletedMetadata).isNull();
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an AssignmentNotFoundException when the assignment is not found.
     */
    @Test
    public void testRemoveMetadataByKey_AssignmentNotFound() {
        // Given
        Long assignmentId = -1L;  // Non-existent ID
        String metadataKey = "key1";

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByKey(assignmentId, metadataKey))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    /**
     * Test case for handling the scenario where metadata with the specified key is not found.
     * This test ensures that the method throws a MetadataNotFoundException when no metadata with the key is found.
     */
    @Test
    public void testRemoveMetadataByKey_MetadataNotFound() {
        // Given
        String metadataKey = "key1";

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .metadata(new HashSet<>())
                .build();
        assignment = assignmentRepository.save(assignment);
        Long assignmentId = assignment.getId();
        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByKey(assignmentId, metadataKey))
                .isInstanceOf(MetadataNotFoundException.class)
                .hasMessage("Metadata with key '" + metadataKey + "' not found");
    }

    /**
     * Test case for the successful removal of metadata from an assignment by hierarchical entity ID,
     * assignable entity ID, and metadata ID. This test ensures that the method correctly performs the removal
     * when all entities and the assignment are present.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        AssignmentMetadataRequestDto metadataRequestDto = AssignmentMetadataRequestDto.builder()
                .key("key1")
                .value("value1")
                .build();
        // Assign department to the employee
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignmentResponseDto
                = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                Collections.singletonList(metadataRequestDto));

        Long assignmentId = assignmentResponseDto.getId();
        Long metadataId = assignmentResponseDto.getMetadata().get(0).getId();

        // When
        departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId);

        // Then
        DepartmentEmployeeAssignment updatedAssignment = assignmentRepository.findById(assignmentId).orElse(null);
        assertThat(updatedAssignment).isNotNull();
        assertThat(updatedAssignment.getMetadata()).isEmpty();

        // Verify that metadata is also deleted from the metadata repository
        AssignmentMetadata deletedMetadata = assignmentMetadataRepository.findById(metadataId).orElse(null);
        assertThat(deletedMetadata).isNull();
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical entity is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = -1L;  // Non-existent ID
        Long assignableEntityId = 2L;
        Long metadataId = 3L;

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_AssignableEntityNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Long assignableEntityId = -1L;  // Non-existent ID
        Long metadataId = 3L;

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an AssignmentNotFoundException when the assignment is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_AssignmentNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        Long metadataId = 3L;

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    /**
     * Test case for handling the scenario where the metadata with the specified id is not found.
     * This test ensures that the method throws a MetadataNotFoundException when the metadata with the id is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByID_MetadataKeyNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // Assign department to the employee
        departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), null);

        Long metadataId = 3L;

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataId))
                .isInstanceOf(MetadataNotFoundException.class)
                .hasMessage("Metadata not found");
    }

    /**
     * Test case for the successful removal of metadata from an assignment by hierarchical entity ID,
     * assignable entity ID, and metadata key. This test ensures that the method correctly performs the removal
     * when all entities and the assignment are present and the metadata with the key exists.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        String metadataKey = "key1";
        AssignmentMetadataRequestDto metadataRequestDto = AssignmentMetadataRequestDto.builder()
                .key(metadataKey)
                .value("value1")
                .build();

        // Assign department to the employee
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignmentResponseDto
                = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                Collections.singletonList(metadataRequestDto));

        Long assignmentId = assignmentResponseDto.getId();
        Long metadataId = assignmentResponseDto.getMetadata().get(0).getId();

        // When
        departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey);

        // Then
        DepartmentEmployeeAssignment updatedAssignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assertThat(updatedAssignment.getMetadata()).isEmpty();

        // Verify that metadata is also deleted from the metadata repository
        AssignmentMetadata deletedMetadata = assignmentMetadataRepository.findById(metadataId).orElse(null);
        assertThat(deletedMetadata).isNull();
    }

    /**
     * Test case for handling the scenario where the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical entity is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = -1L;  // Non-existent ID
        Long assignableEntityId = 2L;
        String metadataKey = "key1";

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hierarchical entity not found");
    }

    /**
     * Test case for handling the scenario where the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_AssignableEntityNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Long assignableEntityId = -1L;  // Non-existent ID
        String metadataKey = "key1";

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Assignable entity not found");
    }

    /**
     * Test case for handling the scenario where the assignment is not found.
     * This test ensures that the method throws an AssignmentNotFoundException when the assignment is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_AssignmentNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        String metadataKey = "key1";

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey))
                .isInstanceOf(AssignmentNotFoundException.class)
                .hasMessage("Assignment not found");
    }

    /**
     * Test case for handling the scenario where the metadata with the specified key is not found.
     * This test ensures that the method throws a MetadataNotFoundException when the metadata with the key is not found.
     */
    @Test
    public void testRemoveMetadataByHierarchicalAndAssignableEntity_ByKey_MetadataKeyNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // Assign department to the employee
        departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), null);

        String metadataKey = "key1";

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.removeMetadataByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId, metadataKey))
                .isInstanceOf(MetadataNotFoundException.class)
                .hasMessage("Metadata with key '" + metadataKey + "' not found");
    }

    /**
     * Test case for successfully retrieving assignments by hierarchical entity ID.
     * This test ensures that the method correctly retrieves and maps assignments to DTOs when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignmentsByHierarchicalEntity_Success() {
        // Given
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee1 = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        Employee employee2 = assignableEntityRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build());

        // Assign department to the employee
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignment1ResponseDto = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee1.getId(), null);
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignment2ResponseDto = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee2.getId(), null);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result =
                departmentEmployeeAssignmentService.getAssignmentsByHierarchicalEntity(department.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(assignment1ResponseDto, assignment2ResponseDto);
    }

    /**
     * Test case for successfully retrieving a paginated list of assignments by hierarchical entity ID.
     * This test ensures that the method correctly retrieves and maps a paginated list of assignments to DTOs when the hierarchical entity exists.
     */
    @Test
    public void testGetAssignmentsByHierarchicalEntity_Paginated_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10); // Pagination: first page, 10 items per page
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee1 = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        Employee employee2 = assignableEntityRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("jane.smith@email.com")
                .position("Developer")
                .departments(new HashSet<>())
                .build());

        // Assign department to the employee
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignment1ResponseDto = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee1.getId(), null);
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignment2ResponseDto = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee2.getId(), null);

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result =
                departmentEmployeeAssignmentService.getAssignmentsByHierarchicalEntity(department.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).contains(assignment1ResponseDto, assignment2ResponseDto);
        assertThat(result.getTotalPages()).isEqualTo(1); // Adjust according to the actual data
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    /**
     * Test case for successfully retrieving assignments by assignable entity ID.
     * This test ensures that the method correctly retrieves and maps assignments to DTOs when the assignable entity exists.
     */
    @Test
    public void testGetAssignmentsByAssignableEntity_Success() {
        // Given
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        // Assign department to the employee
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignmentResponseDto = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), null);

        // When
        List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result =
                departmentEmployeeAssignmentService.getAssignmentsByAssignableEntity(employee.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).contains(assignmentResponseDto);
    }

    /**
     * Test case for successfully retrieving a paginated list of assignments by assignable entity ID.
     * This test ensures that the method correctly retrieves and maps paginated assignments to DTOs when the assignable entity exists.
     */
    @Test
    public void testGetAssignmentsByAssignableEntityWithPagination_Success() {
        // Given
        Department department = hierarchicalEntityRepository.save(Department.builder()
                .name("Engineering")
                .path("/1/")
                .employees(new HashSet<>())
                .build());

        Employee employee = assignableEntityRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build());

        // Assign department to the employee
        departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), null);

        // Create pageable
        Pageable pageable = PageRequest.of(0, 2); // Pagination: first page, 2 items per page

        // When
        PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>> result =
                departmentEmployeeAssignmentService.getAssignmentsByAssignableEntity(employee.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1); // Adjust according to the actual data
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(1); // Adjust according to the actual data
        assertThat(result.getTotalPages()).isEqualTo(1); // Adjust according to the actual data
    }

    /**
     * Test case for successfully retrieving an assignment by hierarchical entity ID and assignable entity ID.
     * This test ensures that the method correctly retrieves and maps the assignment to a DTO when both entities exist.
     */
    @Test
    public void testGetAssignmentByHierarchicalAndAssignableEntity_Success() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // Assign department to the employee
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> assignmentResponseDto = departmentEmployeeAssignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), null);

        // When
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> result =
                departmentEmployeeAssignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(assignmentResponseDto);
    }

    /**
     * Test case for when the hierarchical entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the hierarchical entity does not exist.
     */
    @Test
    public void testGetAssignmentByHierarchicalAndAssignableEntity_HierarchicalEntityNotFound() {
        // Given
        Long hierarchicalEntityId = -1L;  // Non-existent ID
        Long assignableEntityId = 2L;

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Hierarchical entity not found");
    }

    /**
     * Test case for when the assignable entity is not found.
     * This test ensures that the method throws an EntityNotFoundException when the assignable entity does not exist.
     */
    @Test
    public void testGetAssignmentByHierarchicalAndAssignableEntity_AssignableEntityNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Long assignableEntityId = -1L;  // Non-existent ID

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Assignable entity not found");
    }

    /**
     * Test case for when the assignment is not found.
     * This test ensures that the method throws an EntityNotFoundException when no assignment exists for the given hierarchical and assignable entities.
     */
    @Test
    public void testGetAssignmentByHierarchicalAndAssignableEntity_AssignmentNotFound() {
        // Given
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("john.doe@email.com")
                .position("Manager")
                .departments(new HashSet<>())
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // When & Then
        assertThatThrownBy(() -> departmentEmployeeAssignmentService.getAssignmentByHierarchicalAndAssignableEntity(hierarchicalEntityId, assignableEntityId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Assignment not found for the given hierarchical entity ID: " + hierarchicalEntityId + " and assignable entity ID: " + assignableEntityId);
    }
}