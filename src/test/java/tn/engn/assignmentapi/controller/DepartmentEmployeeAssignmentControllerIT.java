package tn.engn.assignmentapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import tn.engn.assignmentapi.dto.*;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.assignmentapi.model.AssignmentMetadata;
import tn.engn.assignmentapi.model.DepartmentEmployeeAssignment;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.assignmentapi.service.DepartmentEmployeeAssignmentService;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.ErrorResponse;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class})
@ActiveProfiles("test-real-db")
@Slf4j
class DepartmentEmployeeAssignmentControllerIT {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    protected HierarchyBaseRepository<Department> hierarchicalEntityRepository;

    @Autowired
    protected EmployeeRepository assignableEntityRepository;

    @Autowired
    protected AssignmentRepository<Department, Employee, DepartmentEmployeeAssignment> assignmentRepository;

    @Autowired
    protected AssignmentMetadataRepository assignmentMetadataRepository;

    @Autowired
    protected DepartmentEmployeeAssignmentService assignmentService;

    private static final String BASE_URL = "/api/v1/department-employee-assignments";

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
     * Test assigning an entity to a hierarchical entity successfully.
     * <p>
     * Given: A valid hierarchical entity ID and a valid assignable entity ID
     * When: A POST request is made to assign the entity with metadata
     * Then: The response should contain the assignment details with status 200
     */
    @Test
    void testAssignEntityToHierarchicalEntity_Success() {
        // Given: Prepare valid hierarchical entity and assignable entity using builders
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

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        // When: Send POST request to assign entity
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> response = webTestClient.post()
                .uri(BASE_URL + "/assign?hierarchicalEntityId=" + department.getId() + "&assignableEntityId=" + employee.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(metadataDtos)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertNotNull(response.getHierarchicalEntity());
        assertThat(response.getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertNotNull(response.getAssignableEntity());
        assertThat(response.getAssignableEntity().getId()).isEqualTo(employee.getId());
        assertThat(response.getMetadata()).isNotNull();
        assertThat(response.getMetadata()).hasSize(1);
        assertThat(response.getMetadata().get(0).getKey()).isEqualTo("Role");
        assertThat(response.getMetadata().get(0).getValue()).isEqualTo("Manager");
    }

    /**
     * Test assigning an entity to a hierarchical entity with an invalid hierarchical entity ID.
     * <p>
     * Given: A valid assignable entity ID and metadata.
     * When: A POST request is made with an invalid hierarchical entity ID.
     * Then: The response should indicate that the hierarchical entity was not found with status 404.
     */
    @Test
    void testAssignEntityToHierarchicalEntity_InvalidHierarchicalEntityId() {
        // Given: Prepare valid assignable entity and metadata
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

        // When: Send POST request with invalid hierarchical entity ID
        webTestClient.post()
                .uri(BASE_URL + "/assign?hierarchicalEntityId=-1&assignableEntityId=" + employee.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(metadataDtos)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Hierarchical entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test assigning an entity to a hierarchical entity with an invalid assignable entity ID.
     * <p>
     * Given: A valid hierarchical entity ID and metadata.
     * When: A POST request is made with an invalid assignable entity ID.
     * Then: The response should indicate that the assignable entity was not found with status 404.
     */
    @Test
    void testAssignEntityToHierarchicalEntity_InvalidAssignableEntityId() {
        // Given: Prepare valid hierarchical entity and metadata
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

        // When: Send POST request with invalid assignable entity ID
        webTestClient.post()
                .uri(BASE_URL + "/assign?hierarchicalEntityId=" + department.getId() + "&assignableEntityId=-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(metadataDtos)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignable entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test assigning an entity to a hierarchical entity when an assignment already exists.
     * <p>
     * Given: A valid hierarchical entity ID, a valid assignable entity ID, and metadata.
     * When: An assignment is created between the entities.
     * When: A POST request is made for the same assignment.
     * Then: The response should indicate that the assignment already exists with status 409.
     */
    @Test
    void testAssignEntityToHierarchicalEntity_AssignmentAlreadyExists() {
        // Given: Prepare and persist hierarchical and assignable entities
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        // First assignment
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), metadataDtos);

        // When: Send POST request for the same assignment
        webTestClient.post()
                .uri(BASE_URL + "/assign?hierarchicalEntityId=" + department.getId() + "&assignableEntityId=" + employee.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(metadataDtos)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)  // Assuming Conflict (409) for already existing assignment
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment already exists between the specified hierarchical entity and assignable entity", errorResponse.getMessage());
                    assertEquals(HttpStatus.CONFLICT.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test assigning an entity to a hierarchical entity with invalid metadata.
     * <p>
     * Given: A valid hierarchical entity ID and a valid assignable entity ID.
     * When: A POST request is made with invalid metadata (null key).
     * Then: The response should indicate that the metadata is invalid with status 400.
     */
    @Test
    void testAssignEntityToHierarchicalEntity_InvalidMetadata() {
        // Given: Prepare valid hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key(null)
                        .value("Manager")
                        .build()
        );

        // When: Send POST request with invalid metadata
        webTestClient.post()
                .uri(BASE_URL + "/assign?hierarchicalEntityId=" + department.getId() + "&assignableEntityId=" + employee.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(metadataDtos)
                .exchange()
                .expectStatus().isBadRequest()  // Assuming Bad Request (400) for invalid input
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Metadata key cannot be null or empty.", errorResponse.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing an assignable entity from a hierarchical entity successfully.
     * <p>
     * Given: A valid hierarchical entity ID and a valid assignable entity ID
     * When: A DELETE request is made to remove the entity
     * Then: The response should indicate successful removal with status 200 and correct details in the response body
     */
    @Test
    void testRemoveEntityFromHierarchicalEntity_Success() {
        // Given: Prepare and persist hierarchical and assignable entities
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Create an assignment between the hierarchical entity and the assignable entity
        List<AssignmentMetadataRequestDto> metadataDtos = Collections.singletonList(
                AssignmentMetadataRequestDto.builder()
                        .key("Role")
                        .value("Manager")
                        .build()
        );

        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), metadataDtos);

        // When: Send DELETE request to remove the entity
        AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> response = webTestClient.delete()
                .uri(BASE_URL + "/remove?hierarchicalEntityId=" + department.getId() + "&assignableEntityId=" + employee.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertNotNull(response.getHierarchicalEntity());
        assertThat(response.getHierarchicalEntity().getId()).isEqualTo(department.getId());
        assertNotNull(response.getAssignableEntity());
        assertThat(response.getAssignableEntity().getId()).isEqualTo(employee.getId());
    }

    /**
     * Test removing an assignable entity from a hierarchical entity with an invalid hierarchical entity ID.
     * <p>
     * Given: A valid assignable entity ID and metadata
     * When: A DELETE request is made with an invalid hierarchical entity ID
     * Then: The response should indicate that the hierarchical entity was not found with status 404
     */
    @Test
    void testRemoveEntityFromHierarchicalEntity_InvalidHierarchicalEntityId() {
        // Given: Prepare valid assignable entity
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // When: Send DELETE request with invalid hierarchical entity ID
        webTestClient.delete()
                .uri(BASE_URL + "/remove?hierarchicalEntityId=-1&assignableEntityId=" + employee.getId())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Hierarchical entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing an assignable entity from a hierarchical entity with an invalid assignable entity ID.
     * <p>
     * Given: A valid hierarchical entity ID
     * When: A DELETE request is made with an invalid assignable entity ID
     * Then: The response should indicate that the assignable entity was not found with status 404
     */
    @Test
    void testRemoveEntityFromHierarchicalEntity_InvalidAssignableEntityId() {
        // Given: Prepare valid hierarchical entity
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        // When: Send DELETE request with invalid assignable entity ID
        webTestClient.delete()
                .uri(BASE_URL + "/remove?hierarchicalEntityId=" + department.getId() + "&assignableEntityId=-1")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignable entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing an assignable entity from a hierarchical entity when no assignment exists.
     * <p>
     * Given: A valid hierarchical entity ID and a valid assignable entity ID with no existing assignment
     * When: A DELETE request is made to remove the entity
     * Then: The response should indicate that the assignment was not found with status 404
     */
    @Test
    void testRemoveEntityFromHierarchicalEntity_NoExistingAssignment() {
        // Given: Prepare and persist hierarchical and assignable entities
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // When: Send DELETE request to remove the entity without prior assignment
        webTestClient.delete()
                .uri(BASE_URL + "/remove?hierarchicalEntityId=" + department.getId() + "&assignableEntityId=" + employee.getId())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test bulk assigning multiple assignable entities to a single hierarchical entity successfully.
     * <p>
     * Given: A valid hierarchical entity ID and a list of assignable entity IDs with valid metadata
     * When: A POST request is made to bulk assign the entities
     * Then: The response should indicate successful bulk assignment with status 200
     */
    @Test
    void testBulkAssignAssignableEntitiesToHierarchicalEntity_Success() {
        // Given: Prepare and persist hierarchical entity and assignable entities
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();
        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee1 = assignableEntityRepository.save(employee1);

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("jane.smith@email.com")
                .position("Developer")
                .build();
        employee2 = assignableEntityRepository.save(employee2);

        List<Long> assignableEntityIds = List.of(employee1.getId(), employee2.getId());
        List<AssignmentMetadataRequestDto> metadataDtos = List.of(
                new AssignmentMetadataRequestDto("Key1", "Value1"),
                new AssignmentMetadataRequestDto("Key2", "Value2")
        );

        BulkAssignmentToHierarchicalEntityRequestDto request = new BulkAssignmentToHierarchicalEntityRequestDto();
        request.setHierarchicalEntityId(department.getId());
        request.setAssignableEntityIds(assignableEntityIds);
        request.setMetadata(metadataDtos);

        // When: Send POST request to bulk assign entities
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> response = webTestClient.post()
                .uri(BASE_URL + "/bulk-assign-to-hierarchical-entity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertNotNull(response.getHierarchicalEntities());
        assertEquals(1, response.getHierarchicalEntities().size());
        assertEquals(hierarchicalEntityId, response.getHierarchicalEntities().get(0).getId());
        assertNotNull(response.getAssignableEntities());
        assertEquals(assignableEntityIds.size(), response.getAssignableEntities().size());
        assertNotNull(response.getMetadata());
        assertTrue(response.getMetadata().size() > 0);
        assertEquals(metadataDtos.size(), response.getMetadata().size());
    }

    /**
     * Test bulk assigning entities when one or more assignable entities are not found.
     * <p>
     * Given: A valid hierarchical entity ID and a list of assignable entity IDs where some IDs do not exist
     * When: A POST request is made to bulk assign the entities
     * Then: The response should indicate that one or more assignable entities were not found with status 404
     */
    @Test
    void testBulkAssignAssignableEntitiesToHierarchicalEntity_EntityNotFound() {
        // Given: Prepare and persist hierarchical entity
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        List<Long> assignableEntityIds = List.of(999L, 1000L); // Non-existent IDs
        List<AssignmentMetadataRequestDto> metadataDtos = List.of(
                new AssignmentMetadataRequestDto("Key1", "Value1")
        );

        BulkAssignmentToHierarchicalEntityRequestDto request = new BulkAssignmentToHierarchicalEntityRequestDto();
        request.setHierarchicalEntityId(department.getId());
        request.setAssignableEntityIds(assignableEntityIds);
        request.setMetadata(metadataDtos);

        // When: Send POST request to bulk assign entities
        webTestClient.post()
                .uri(BASE_URL + "/bulk-assign-to-hierarchical-entity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("One or more assignable entities not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test bulk assigning entities where an assignment already exists.
     * <p>
     * Given: A valid hierarchical entity ID and a list of assignable entity IDs where one or more assignments already exist
     * When: A POST request is made to bulk assign the entities
     * Then: The response should indicate that one or more assignments already exist with status 400
     */
    @Test
    void testBulkAssignAssignableEntitiesToHierarchicalEntity_AssignmentAlreadyExists() {
        // Given: Prepare and persist hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Pre-create an assignment
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), List.of((new AssignmentMetadataRequestDto("Key1", "Value1"))));

        List<Long> assignableEntityIds = List.of(employee.getId()); // Existing assignment
        List<AssignmentMetadataRequestDto> metadataDtos = List.of(
                new AssignmentMetadataRequestDto("Key2", "Value2")
        );
        Long hierarchicalEntityId = department.getId();
        BulkAssignmentToHierarchicalEntityRequestDto request = new BulkAssignmentToHierarchicalEntityRequestDto();
        request.setHierarchicalEntityId(department.getId());
        request.setAssignableEntityIds(assignableEntityIds);
        request.setMetadata(metadataDtos);

        // When: Send POST request to bulk assign entities
        webTestClient.post()
                .uri(BASE_URL + "/bulk-assign-to-hierarchical-entity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment already exists for hierarchical entity ID: " + hierarchicalEntityId + " and assignable entity ID: " + assignableEntityIds.get(0), errorResponse.getMessage());
                    assertEquals(HttpStatus.CONFLICT.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test bulk assigning entities with invalid request data.
     * <p>
     * Given: An invalid request payload (e.g., missing hierarchical entity ID)
     * When: A POST request is made with the invalid payload
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testBulkAssignAssignableEntitiesToHierarchicalEntity_InvalidData() {
        // Given: An invalid request with missing hierarchical entity ID
        BulkAssignmentToHierarchicalEntityRequestDto request = new BulkAssignmentToHierarchicalEntityRequestDto();
        request.setAssignableEntityIds(List.of(1L, 2L));
        request.setMetadata(List.of(new AssignmentMetadataRequestDto("Key", "Value")));

        // When: Send POST request to bulk assign entities
        webTestClient.post()
                .uri(BASE_URL + "/bulk-assign-to-hierarchical-entity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successfully bulk removing assignable entities from a hierarchical entity.
     * <p>
     * Given: A valid hierarchical entity ID and assignable entity IDs
     * When: A DELETE request is made to bulk remove the entities
     * Then: The response should indicate successful removal with status 200
     */
    @Test
    void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_Success() {
        // Given: A valid hierarchical entity ID and assignable entity IDs
        // Given: Prepare and persist hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Pre-create an assignment
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(), List.of((new AssignmentMetadataRequestDto("Key1", "Value1"))));

        List<Long> assignableEntityIds = List.of(employee.getId()); // Existing assignment
        Long hierarchicalEntityId = department.getId();


        // When: Send DELETE request to bulk remove entities
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> response = webTestClient.method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/bulk-remove-from-hierarchical-entity")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)  // Set content type for the body
                .body(BodyInserters.fromValue(assignableEntityIds))  // Provide the body of the request
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertNotNull(response.getHierarchicalEntities());
        assertEquals(1, response.getHierarchicalEntities().size());
        assertEquals(hierarchicalEntityId, response.getHierarchicalEntities().get(0).getId());
        assertNotNull(response.getAssignableEntities());
        assertEquals(assignableEntityIds.size(), response.getAssignableEntities().size());
        assertEquals(assignableEntityIds.get(0), response.getAssignableEntities().get(0).getId());
    }

    /**
     * Test bulk removing assignable entities when entities are not found.
     * <p>
     * Given: An invalid hierarchical entity ID or assignable entity IDs
     * When: A DELETE request is made with the invalid IDs
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_EntityNotFound() {
        // Given: An invalid hierarchical entity ID or assignable entity IDs
        Long hierarchicalEntityId = -1L;
        List<Long> assignableEntityIds = List.of(2L, 3L);

        // When: Send DELETE request to bulk remove entities
        webTestClient.method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/bulk-remove-from-hierarchical-entity")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)  // Set content type for the body
                .body(BodyInserters.fromValue(assignableEntityIds))  // Provide the body of the request
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Hierarchical entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test bulk removing assignable entities with invalid request data.
     * <p>
     * Given: An invalid request payload (e.g., missing hierarchical entity ID)
     * When: A DELETE request is made with the invalid payload
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testBulkRemoveAssignableEntitiesFromHierarchicalEntity_InvalidData() {
        // Given: An invalid request with missing hierarchical entity ID
        List<Long> assignableEntityIds = List.of(1L, 2L);

        // When: Send DELETE request to bulk remove entities with a null hierarchical entity ID
        webTestClient.method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/bulk-remove-from-hierarchical-entity")
                        .queryParam("hierarchicalEntityId", (Long) null) // Invalid hierarchical entity ID
                        .build())
                .body(BodyInserters.fromValue(assignableEntityIds))  // Provide the body of the request
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successfully bulk assigning hierarchical entities to a single assignable entity.
     * <p>
     * Given: Valid request data with hierarchical entity IDs and metadata
     * When: A POST request is made to bulk assign the entities
     * Then: The response should indicate successful assignment with status 200 and include correct data
     */
    @Test
    void testBulkAssignHierarchicalEntitiesToAssignableEntity_Success() {
        // Given: Prepare and persist hierarchical entities and assignable entity
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Valid request data
        List<Long> hierarchicalEntityIds = List.of(department1.getId(), department2.getId());
        BulkAssignmentToAssignableEntityRequestDto request = BulkAssignmentToAssignableEntityRequestDto.builder()
                .assignableEntityId(employee.getId())
                .hierarchicalEntityIds(hierarchicalEntityIds)
                .metadata(List.of(new AssignmentMetadataRequestDto("Key1", "Value1"), new AssignmentMetadataRequestDto("Key2", "Value2")))
                .build();

        // When: Send POST request to bulk assign entities
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> response = webTestClient.post()
                .uri(BASE_URL + "/bulk-assign-to-assignable-entity")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertNotNull(response.getAssignableEntities());
        assertEquals(employee.getId(), response.getAssignableEntities().get(0).getId());
        assertNotNull(response.getHierarchicalEntities());
        assertEquals(2, response.getHierarchicalEntities().size());
        assertTrue(response.getHierarchicalEntities().stream().anyMatch(e -> e.getId().equals(hierarchicalEntityIds.get(0))));
        assertTrue(response.getHierarchicalEntities().stream().anyMatch(e -> e.getId().equals(hierarchicalEntityIds.get(1))));
    }

    /**
     * Test bulk assigning hierarchical entities when entities are not found.
     * <p>
     * Given: An invalid assignable entity ID or hierarchical entity IDs
     * When: A POST request is made with invalid IDs
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testBulkAssignHierarchicalEntitiesToAssignableEntity_EntityNotFound() {
        // Given: Invalid assignable entity ID and hierarchical entity IDs
        Long invalidAssignableEntityId = -1L;
        List<Long> hierarchicalEntityIds = List.of(1L, 2L);

        BulkAssignmentToAssignableEntityRequestDto request = BulkAssignmentToAssignableEntityRequestDto.builder()
                .assignableEntityId(invalidAssignableEntityId)
                .hierarchicalEntityIds(hierarchicalEntityIds)
                .metadata(List.of(new AssignmentMetadataRequestDto("Key1", "Value1")))
                .build();

        // When: Send POST request to bulk assign entities
        webTestClient.post()
                .uri(BASE_URL + "/bulk-assign-to-assignable-entity")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Assignable entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test bulk assigning hierarchical entities where an assignment already exists.
     * <p>
     * Given: A valid assignable entity ID and a list of hierarchical entity IDs where one or more assignments already exist
     * When: A POST request is made to bulk assign the entities
     * Then: The response should indicate that one or more assignments already exist with status 400
     */
    @Test
    void testBulkAssignHierarchicalEntitiesToAssignableEntity_AssignmentAlreadyExists() {
        // Given: Prepare and persist hierarchical entities and assignable entity
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Pre-create an assignment
        assignmentService.assignEntityToHierarchicalEntity(department1.getId(), employee.getId(), List.of((new AssignmentMetadataRequestDto("Key1", "Value1"))));

        // Given: Valid request data
        Long assignableEntityId = employee.getId();
        List<Long> hierarchicalEntityIds = List.of(department1.getId(), department2.getId());
        BulkAssignmentToAssignableEntityRequestDto request = BulkAssignmentToAssignableEntityRequestDto.builder()
                .assignableEntityId(employee.getId())
                .hierarchicalEntityIds(hierarchicalEntityIds)
                .metadata(List.of(new AssignmentMetadataRequestDto("Key1", "Value1"), new AssignmentMetadataRequestDto("Key2", "Value2")))
                .build();

        // When: Send POST request to bulk assign hierarchical entities
        webTestClient.post()
                .uri(BASE_URL + "/bulk-assign-to-assignable-entity")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment already exists for hierarchical entity ID: " + hierarchicalEntityIds.get(0) + " and assignable entity ID: " + assignableEntityId, errorResponse.getMessage());
                    assertEquals(HttpStatus.CONFLICT.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test bulk assigning hierarchical entities with invalid request data.
     * <p>
     * Given: An invalid request payload (e.g., missing assignable entity ID)
     * When: A POST request is made with the invalid payload
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testBulkAssignHierarchicalEntitiesToAssignableEntity_InvalidData() {
        // Given: An invalid request with missing assignable entity ID
        BulkAssignmentToAssignableEntityRequestDto request = BulkAssignmentToAssignableEntityRequestDto.builder()
                .assignableEntityId(null) // Invalid assignable entity ID
                .hierarchicalEntityIds(List.of(1L, 2L))
                .metadata(List.of(new AssignmentMetadataRequestDto("Key1", "Value1")))
                .build();

        // When: Send POST request with invalid data
        webTestClient.post()
                .uri(BASE_URL + "/bulk-assign-to-assignable-entity")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successfully bulk removing hierarchical entities from a single assignable entity.
     * <p>
     * Given: Valid assignable entity ID and hierarchical entity IDs to be removed
     * When: A DELETE request is made to bulk remove the entities
     * Then: The response should indicate successful removal with status 200 and include correct data
     */
    @Test
    void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_Success() {
        // Given: Prepare and persist hierarchical entities and assignable entity
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Pre-create assignments
        assignmentService.assignEntityToHierarchicalEntity(department1.getId(), employee.getId(), List.of(new AssignmentMetadataRequestDto("Key1", "Value1")));
        assignmentService.assignEntityToHierarchicalEntity(department2.getId(), employee.getId(), List.of(new AssignmentMetadataRequestDto("Key2", "Value2")));

        // Given: Valid request data
        List<Long> hierarchicalEntityIdsToRemove = List.of(department1.getId(), department2.getId());
        Long assignableEntityId = employee.getId();

        // When: Send DELETE request to bulk remove entities
        BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto> response = webTestClient.method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/bulk-remove-from-assignable-entity")
                        .queryParam("assignableEntityId", assignableEntityId)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(hierarchicalEntityIdsToRemove))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<BulkAssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertNotNull(response.getAssignableEntities());
        assertEquals(assignableEntityId, response.getAssignableEntities().get(0).getId());
        assertNotNull(response.getHierarchicalEntities());
        assertEquals(2, response.getHierarchicalEntities().size());
    }

    /**
     * Test bulk removing hierarchical entities when entities are not found.
     * <p>
     * Given: An invalid assignable entity ID or hierarchical entity IDs
     * When: A DELETE request is made with invalid IDs
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_EntityNotFound() {
        // Given: Invalid assignable entity ID and hierarchical entity IDs
        Long invalidAssignableEntityId = -1L;
        List<Long> hierarchicalEntityIds = List.of(1L, 2L);

        // When: Send DELETE request to bulk remove entities
        webTestClient.method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/bulk-remove-from-assignable-entity")
                        .queryParam("assignableEntityId", invalidAssignableEntityId)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(hierarchicalEntityIds))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Assignable entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test bulk removing hierarchical entities with invalid request data.
     * <p>
     * Given: An invalid request payload (e.g., missing assignable entity ID)
     * When: A DELETE request is made with the invalid payload
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testBulkRemoveHierarchicalEntitiesFromAssignableEntity_InvalidData() {
        // Given: An invalid request with missing assignable entity ID
        Long assignableEntityId = null; // Invalid assignable entity ID
        List<Long> hierarchicalEntityIds = List.of(1L, 2L);

        // When: Send DELETE request to bulk remove entities with null assignable entity ID
        webTestClient.method(HttpMethod.DELETE)
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/bulk-remove-from-assignable-entity")
                        .queryParam("assignableEntityId", (Long) null) // Invalid assignable entity ID
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(hierarchicalEntityIds))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successful retrieval of assignable entities by hierarchical entity ID.
     * <p>
     * Given: A hierarchical entity with associated assignable entities
     * When: A GET request is made with the hierarchical entity ID
     * Then: The response should contain the list of assignable entities associated with the given hierarchical entity
     */
    @Test
    void testGetAssignableEntitiesByHierarchicalEntity_Success() {
        // Given: Prepare and persist a hierarchical entity and some assignable entities
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("john.doe@email.com")
                .position("Manager")
                .build();
        employee1 = assignableEntityRepository.save(employee1);

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(20))
                .email("jane.smith@email.com")
                .position("Developer")
                .build();
        employee2 = assignableEntityRepository.save(employee2);

        // Create assignments
        Long assignableEntity1Id = employee1.getId();
        assignmentService.assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntity1Id, List.of(new AssignmentMetadataRequestDto("Key1", "Value1")));
        Long assignableEntity2Id = employee2.getId();
        assignmentService.assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntity2Id, List.of(new AssignmentMetadataRequestDto("Key2", "Value2")));

        // When: Send GET request to retrieve assignable entities
        List<EmployeeResponseDto> response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entities")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<EmployeeResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertEquals(2, response.size());
        assertTrue(response.stream().anyMatch(dto -> dto.getId().equals(assignableEntity1Id)));
        assertTrue(response.stream().anyMatch(dto -> dto.getId().equals(assignableEntity2Id)));
    }

    /**
     * Test retrieval of assignable entities when the hierarchical entity is not found.
     * <p>
     * Given: An invalid hierarchical entity ID
     * When: A GET request is made with the invalid hierarchical entity ID
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testGetAssignableEntitiesByHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given: An invalid hierarchical entity ID
        Long invalidHierarchicalEntityId = -1L;

        // When: Send GET request to retrieve assignable entities with the invalid ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entities")
                        .queryParam("hierarchicalEntityId", invalidHierarchicalEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Hierarchical entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test retrieval of assignable entities with invalid input data.
     * <p>
     * Given: An invalid request payload (e.g., missing hierarchical entity ID)
     * When: A GET request is made with the invalid payload
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testGetAssignableEntitiesByHierarchicalEntity_InvalidData() {
        // Given: A null hierarchical entity ID
        Long nullHierarchicalEntityId = null;

        // When: Send GET request with the null hierarchical entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entities")
                        .queryParam("hierarchicalEntityId", nullHierarchicalEntityId) // Invalid hierarchical entity ID
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successful retrieval of paginated assignable entities by hierarchical entity ID.
     * <p>
     * Given: A hierarchical entity with associated assignable entities
     * When: A GET request is made with the hierarchical entity ID and pagination parameters
     * Then: The response should contain a paginated list of assignable entities associated with the given hierarchical entity
     */
    @Test
    void testGetAssignableEntitiesByHierarchicalEntity_Paginated_Success() {
        // Given: Prepare and persist a hierarchical entity and some assignable entities
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Long hierarchicalEntityId = department.getId();
        IntStream.range(1, 21).forEach(i -> {
            Employee employee = Employee.builder()
                    .firstName("Employee")
                    .lastName("LastName" + i)
                    .dateOfBirth(LocalDate.now().minusYears(30))
                    .email("employee" + i + "@email.com")
                    .position("Position" + i)
                    .build();
            employee = assignableEntityRepository.save(employee);
            assignmentService.assignEntityToHierarchicalEntity(hierarchicalEntityId, employee.getId(), List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        });

        Pageable pageable = PageRequest.of(0, 10); // Page 1 with 10 items

        // When: Send GET request to retrieve paginated assignable entities
        PaginatedResponseDto<EmployeeResponseDto> response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entities/paginated")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", "assignableEntity.lastName,asc")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PaginatedResponseDto<EmployeeResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertEquals(10, response.getContent().size());
        assertTrue(response.getContent().get(0).getLastName().compareTo(response.getContent().get(9).getLastName()) <= 0);
        assertTrue(response.getTotalElements() > 0);
        assertEquals(20, response.getTotalElements()); // Total entities should be 20
    }

    /**
     * Test retrieval of paginated assignable entities when the hierarchical entity is not found.
     * <p>
     * Given: An invalid hierarchical entity ID
     * When: A GET request is made with the invalid hierarchical entity ID
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testGetAssignableEntitiesByHierarchicalEntity_Paginated_HierarchicalEntityNotFound() {
        // Given: An invalid hierarchical entity ID
        Long invalidHierarchicalEntityId = -1L;

        Pageable pageable = PageRequest.of(0, 10);

        // When: Send GET request to retrieve paginated assignable entities with the invalid ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entities/paginated")
                        .queryParam("hierarchicalEntityId", invalidHierarchicalEntityId)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", pageable.getSort().toString())
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Hierarchical entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test retrieval of paginated assignable entities with invalid pagination parameters.
     * <p>
     * Given: Invalid pagination parameters
     * When: A GET request is made with invalid pagination parameters
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testGetAssignableEntitiesByHierarchicalEntity_Paginated_InvalidData() {
        // Given: Valid hierarchical entity ID but invalid pagination parameter

        Pageable pageable = PageRequest.of(0, 10); // Invalid page number

        // When: Send GET request with invalid pagination parameters
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entities/paginated")
                        .queryParam("hierarchicalEntityId", (Long) null)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", pageable.getSort().toString())
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successful retrieval of assignable entity count by hierarchical entity ID.
     * <p>
     * Given: A hierarchical entity with associated assignable entities
     * When: A GET request is made with the hierarchical entity ID
     * Then: The response should contain the count of assignable entities associated with the given hierarchical entity
     */
    @Test
    void testGetAssignableEntityCountByHierarchicalEntity_Success() {
        // Given: Prepare and persist a hierarchical entity and some assignable entities
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Long hierarchicalEntityId = department.getId();
        IntStream.range(1, 11).forEach(i -> {
            Employee employee = Employee.builder()
                    .firstName("Employee")
                    .lastName("LastName" + i)
                    .dateOfBirth(LocalDate.now().minusYears(30))
                    .email("employee" + i + "@email.com")
                    .position("Position" + i)
                    .build();
            employee = assignableEntityRepository.save(employee);
            assignmentService.assignEntityToHierarchicalEntity(hierarchicalEntityId, employee.getId(), List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        });

        // When: Send GET request to retrieve the count of assignable entities
        Integer response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entity-count")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertEquals(10, response); // Ensure the count matches the number of entities assigned
    }

    /**
     * Test retrieval of assignable entity count when the hierarchical entity is not found.
     * <p>
     * Given: An invalid hierarchical entity ID
     * When: A GET request is made with the invalid hierarchical entity ID
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testGetAssignableEntityCountByHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given: An invalid hierarchical entity ID
        Long invalidHierarchicalEntityId = -1L;

        // When: Send GET request to retrieve the count of assignable entities with the invalid ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entity-count")
                        .queryParam("hierarchicalEntityId", invalidHierarchicalEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Hierarchical entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test retrieval of assignable entity count with invalid input data.
     * <p>
     * Given: Invalid hierarchical entity ID parameter
     * When: A GET request is made with invalid hierarchical entity ID
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testGetAssignableEntityCountByHierarchicalEntity_InvalidData() {
        // Given: Valid hierarchical entity ID but invalid parameter

        // When: Send GET request with invalid parameter (null)
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/assignable-entity-count")
                        .queryParam("hierarchicalEntityId", (Long) null) // Invalid input (null)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successful retrieval of hierarchical entity count by assignable entity ID.
     * <p>
     * Given: An assignable entity with associated hierarchical entities
     * When: A GET request is made with the assignable entity ID
     * Then: The response should contain the count of hierarchical entities associated with the given assignable entity
     */
    @Test
    void testGetHierarchicalEntityCountByAssignableEntity_Success() {
        // Given: Prepare and persist an assignable entity and some hierarchical entities
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);

        Long assignableEntityId;
        Employee employee = Employee.builder()
                .firstName("Employee")
                .lastName("LastName")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("employee@email.com")
                .position("Position")
                .build();
        employee = assignableEntityRepository.save(employee);
        assignableEntityId = employee.getId();

        assignmentService.assignEntityToHierarchicalEntity(department1.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        assignmentService.assignEntityToHierarchicalEntity(department2.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));

        // When: Send GET request to retrieve the count of hierarchical entities
        Integer response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entity-count")
                        .queryParam("assignableEntityId", assignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Integer.class)
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertEquals(2, response); // Ensure the count matches the number of hierarchical entities assigned
    }

    /**
     * Test retrieval of hierarchical entity count when the assignable entity is not found.
     * <p>
     * Given: An invalid assignable entity ID
     * When: A GET request is made with the invalid assignable entity ID
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testGetHierarchicalEntityCountByAssignableEntity_AssignableEntityNotFound() {
        // Given: An invalid assignable entity ID
        Long invalidAssignableEntityId = -1L;

        // When: Send GET request to retrieve the count of hierarchical entities with the invalid ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entity-count")
                        .queryParam("assignableEntityId", invalidAssignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Assignable entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test retrieval of hierarchical entity count with invalid input data.
     * <p>
     * Given: Invalid assignable entity ID parameter
     * When: A GET request is made with invalid assignable entity ID
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testGetHierarchicalEntityCountByAssignableEntity_InvalidData() {
        // Given: Valid assignable entity ID but invalid parameter

        // When: Send GET request with invalid parameter (null)
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entity-count")
                        .queryParam("assignableEntityId", (Long) null) // Invalid input (null)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successful retrieval of hierarchical entities by assignable entity ID.
     * <p>
     * Given: An assignable entity with associated hierarchical entities
     * When: A GET request is made with the assignable entity ID
     * Then: The response should contain a list of hierarchical entities associated with the given assignable entity
     */
    @Test
    void testGetHierarchicalEntitiesForAssignableEntity_Success() {
        // Given: Prepare and persist an assignable entity and some hierarchical entities
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);

        Long assignableEntityId;
        Employee employee = Employee.builder()
                .firstName("Employee")
                .lastName("LastName")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("employee@email.com")
                .position("Position")
                .build();
        employee = assignableEntityRepository.save(employee);
        assignableEntityId = employee.getId();

        assignmentService.assignEntityToHierarchicalEntity(department1.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        assignmentService.assignEntityToHierarchicalEntity(department2.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));

        // When: Send GET request to retrieve the list of hierarchical entities
        List<HierarchyResponseDto> response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entities")
                        .queryParam("assignableEntityId", assignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<HierarchyResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertEquals(2, response.size()); // Ensure the response size matches the number of hierarchical entities assigned
        assertTrue(response.stream().anyMatch(h -> "Engineering".equals(h.getName())));
        assertTrue(response.stream().anyMatch(h -> "Marketing".equals(h.getName())));
    }

    /**
     * Test retrieval of hierarchical entities when the assignable entity is not found.
     * <p>
     * Given: An invalid assignable entity ID
     * When: A GET request is made with the invalid assignable entity ID
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testGetHierarchicalEntitiesForAssignableEntity_AssignableEntityNotFound() {
        // Given: An invalid assignable entity ID
        Long invalidAssignableEntityId = -1L;

        // When: Send GET request to retrieve the list of hierarchical entities with the invalid ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entities")
                        .queryParam("assignableEntityId", invalidAssignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Assignable entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test retrieval of hierarchical entities with invalid input data.
     * <p>
     * Given: Invalid assignable entity ID parameter
     * When: A GET request is made with invalid assignable entity ID
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testGetHierarchicalEntitiesForAssignableEntity_InvalidData() {
        // Given: Valid assignable entity ID but invalid parameter

        // When: Send GET request with invalid parameter (null)
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entities")
                        .queryParam("assignableEntityId", (Long) null) // Invalid input (null)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test successful retrieval of paginated hierarchical entities by assignable entity ID.
     * <p>
     * Given: An assignable entity with multiple associated hierarchical entities
     * When: A GET request is made with the assignable entity ID and pagination parameters
     * Then: The response should contain a paginated list of hierarchical entities
     */
    @Test
    void testGetPaginatedHierarchicalEntitiesForAssignableEntity_Success() {
        // Given: Prepare and persist an assignable entity and several hierarchical entities
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        Department department3 = Department.builder()
                .name("Sales")
                .path("/3/")
                .build();
        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);
        department3 = hierarchicalEntityRepository.save(department3);

        Long assignableEntityId;
        Employee employee = Employee.builder()
                .firstName("Employee")
                .lastName("LastName")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("employee@email.com")
                .position("Position")
                .build();
        employee = assignableEntityRepository.save(employee);
        assignableEntityId = employee.getId();

        assignmentService.assignEntityToHierarchicalEntity(department1.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        assignmentService.assignEntityToHierarchicalEntity(department2.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        assignmentService.assignEntityToHierarchicalEntity(department3.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));

        Pageable pageable = PageRequest.of(0, 2);

        // When: Send GET request to retrieve the paginated list of hierarchical entities
        PaginatedResponseDto<HierarchyResponseDto> response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entities/paginated")
                        .queryParam("assignableEntityId", assignableEntityId)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", "hierarchicalEntity.name,asc") // 'name,asc'
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>>() {
                })
                .returnResult()
                .getResponseBody();

        // Then: Validate the response
        assertNotNull(response);
        assertEquals(2, response.getContent().size()); // Ensure the response size matches the page size
        assertTrue(response.getContent().stream().anyMatch(h -> "Engineering".equals(h.getName())));
        assertTrue(response.getContent().stream().anyMatch(h -> "Marketing".equals(h.getName())));
        assertEquals(3, response.getTotalElements()); // Ensure total elements count reflects all entities
        assertEquals(2, response.getTotalPages()); // Ensure total pages count
    }

    /**
     * Test retrieval of paginated hierarchical entities when the assignable entity is not found.
     * <p>
     * Given: An invalid assignable entity ID
     * When: A GET request is made with the invalid assignable entity ID and pagination parameters
     * Then: The response should indicate not found with status 404
     */
    @Test
    void testGetPaginatedHierarchicalEntitiesForAssignableEntity_AssignableEntityNotFound() {
        // Given: An invalid assignable entity ID
        Long invalidAssignableEntityId = -1L;
        Pageable pageable = PageRequest.of(0, 2);

        // When: Send GET request to retrieve the paginated list of hierarchical entities with the invalid ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entities/paginated")
                        .queryParam("assignableEntityId", invalidAssignableEntityId)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", pageable.getSort().toString())
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Assignable entity not found");
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test retrieval of paginated hierarchical entities with invalid input data.
     * <p>
     * Given: Valid pagination parameters
     * When: A GET request is made with invalid input data
     * Then: The response should indicate bad request with status 400
     */
    @Test
    void testGetPaginatedHierarchicalEntitiesForAssignableEntity_InvalidData() {
        // Given: Valid pagination parameters
        Pageable pageable = PageRequest.of(0, 2);

        // When: Send GET request with invalid input data (null assignable entity ID)
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical-entities/paginated")
                        .queryParam("assignableEntityId", (Long) null) // Invalid input: null ID
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", "hierarchicalEntity.name,asc")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test retrieval of all assignments.
     * <p>
     * Given: There are existing assignments in the repository
     * When: A GET request is made to retrieve all assignments
     * Then: The response should contain a list of assignments or an empty list if no assignments are found
     */
    @Test
    void testGetAllAssignments() {
        // Given: Prepare and persist an assignable entity and several hierarchical entities
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        Department department3 = Department.builder()
                .name("Sales")
                .path("/3/")
                .build();
        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);
        department3 = hierarchicalEntityRepository.save(department3);

        Long assignableEntityId;
        Employee employee = Employee.builder()
                .firstName("Employee")
                .lastName("LastName")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("employee@email.com")
                .position("Position")
                .build();
        employee = assignableEntityRepository.save(employee);
        assignableEntityId = employee.getId();

        assignmentService.assignEntityToHierarchicalEntity(department1.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        assignmentService.assignEntityToHierarchicalEntity(department2.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        assignmentService.assignEntityToHierarchicalEntity(department3.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        // When: Send GET request to retrieve all assignments
        webTestClient.get()
                .uri(BASE_URL)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>>() {
                })
                .value(assignments -> {
                    // Assert that the response is not null and contains the expected assignments
                    assertNotNull(assignments);
                    assertFalse(assignments.isEmpty(), "Assignments list should not be empty");
                    assertEquals(3, assignments.size());
                    assertTrue(assignments.stream().anyMatch(a -> "Engineering".equals(a.getHierarchicalEntity().getName())));
                    assertTrue(assignments.stream().anyMatch(a -> "Marketing".equals(a.getHierarchicalEntity().getName())));
                    assertTrue(assignments.stream().anyMatch(a -> "Sales".equals(a.getHierarchicalEntity().getName())));
                    assertTrue(assignments.stream().anyMatch(a -> "Employee".equals(a.getAssignableEntity().getFirstName())));
                });
    }

    /**
     * Test retrieval of paginated assignments.
     * <p>
     * Given: Existing assignments in the repository
     * When: A GET request is made to retrieve paginated assignments
     * Then: The response should contain a paginated list of assignments
     */
    @Test
    void testGetPaginatedAssignments() {
        // Given: Prepare and persist an assignable entity and several hierarchical entities
        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        Department department3 = Department.builder()
                .name("Sales")
                .path("/3/")
                .build();
        department1 = hierarchicalEntityRepository.save(department1);
        department2 = hierarchicalEntityRepository.save(department2);
        department3 = hierarchicalEntityRepository.save(department3);

        Long assignableEntityId;
        Employee employee = Employee.builder()
                .firstName("Employee")
                .lastName("LastName")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("employee@email.com")
                .position("Position")
                .build();
        employee = assignableEntityRepository.save(employee);
        assignableEntityId = employee.getId();

        assignmentService.assignEntityToHierarchicalEntity(department1.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        assignmentService.assignEntityToHierarchicalEntity(department2.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));
        assignmentService.assignEntityToHierarchicalEntity(department3.getId(), assignableEntityId, List.of(new AssignmentMetadataRequestDto("Key", "Value")));

        // Define pagination parameters
        Pageable pageable = PageRequest.of(0, 2, Sort.by("hierarchicalEntity.name").ascending());

        // When: Send GET request to retrieve paginated assignments
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/paginated")
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", pageable.getSort().toString().replace(": ", ","))
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>>() {
                })
                .value(response -> {
                    // Assert that the response contains the expected paginated assignments
                    assertNotNull(response);
                    assertFalse(response.getContent().isEmpty(), "Paginated assignments list should not be empty");
                    assertEquals(2, response.getContent().size());
                    assertTrue(response.getContent().stream().anyMatch(a -> "Engineering".equals(a.getHierarchicalEntity().getName())));
                    assertTrue(response.getContent().stream().anyMatch(a -> "Marketing".equals(a.getHierarchicalEntity().getName())));
                    assertTrue(response.getContent().stream().anyMatch(a -> "Employee".equals(a.getAssignableEntity().getFirstName())));
                    assertEquals(3, response.getTotalElements());
                    assertEquals(2, response.getTotalPages());
                });
    }

    /**
     * Test updating an existing assignment successfully.
     * <p>
     * Given: An existing hierarchical entity, assignable entity, and assignment in the repository
     * When: A PUT request is made with valid update details
     * Then: The response should contain the updated assignment details
     */
    @Test
    void testUpdateAssignment_Success() {
        // Given: Prepare and persist an assignable entity and a hierarchical entity
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("john.doe@email.com")
                .position("Engineer")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // Given: Create and persist an assignment
        assignmentService.assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId,
                List.of(new AssignmentMetadataRequestDto("role", "engineer")));

        // Prepare the update request with new metadata
        AssignmentRequestDto updateRequest = AssignmentRequestDto.builder()
                .hierarchicalEntityId(hierarchicalEntityId)
                .assignableEntityId(assignableEntityId)
                .metadata(List.of(new AssignmentMetadataRequestDto("role", "senior engineer")))
                .build();

        // When: Send PUT request to update the assignment
        webTestClient.put()
                .uri(BASE_URL + "/update")
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .value(response -> {
                    // Then: The response should contain the updated assignment details
                    assertNotNull(response);
                    assertEquals(hierarchicalEntityId, response.getHierarchicalEntity().getId());
                    assertEquals(assignableEntityId, response.getAssignableEntity().getId());
                    assertEquals(1, response.getMetadata().size());
                    assertEquals("role", response.getMetadata().get(0).getKey());
                    assertEquals("senior engineer", response.getMetadata().get(0).getValue());
                });
    }

    /**
     * Test updating a non-existent assignment.
     * <p>
     * Given: A hierarchical entity and assignable entity exist, but no assignment between them
     * When: A PUT request is made with valid details for a non-existent assignment
     * Then: The response should be 404 Not Found
     */
    @Test
    void testUpdateAssignment_NotFound() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Marketing")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(28))
                .email("jane.smith@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Prepare the update request for a non-existent assignment
        AssignmentRequestDto updateRequest = AssignmentRequestDto.builder()
                .hierarchicalEntityId(department.getId())
                .assignableEntityId(employee.getId())
                .metadata(List.of(new AssignmentMetadataRequestDto("role", "manager")))
                .build();

        // When: Send PUT request to update the non-existent assignment
        webTestClient.put()
                .uri(BASE_URL + "/update")
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test updating an assignment with invalid metadata.
     * <p>
     * Given: A hierarchical entity and assignable entity exist, and a valid assignment exists
     * When: A PUT request is made with invalid input metadata (e.g., missing metadata key)
     * Then: The response should be 400 Bad Request
     */
    @Test
    void testUpdateAssignment_InvalidMetadata() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Sales")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("Alice")
                .lastName("Brown")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("alice.brown@email.com")
                .position("Sales Rep")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Create and persist a valid assignment
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                List.of(new AssignmentMetadataRequestDto("role", "sales rep")));

        // Prepare the update request with invalid metadata (e.g., missing key)
        AssignmentRequestDto updateRequest = AssignmentRequestDto.builder()
                .hierarchicalEntityId(department.getId())
                .assignableEntityId(employee.getId())
                .metadata(List.of(new AssignmentMetadataRequestDto(null, "senior sales rep"))) // Invalid key
                .build();

        // When: Send PUT request with invalid data
        webTestClient.put()
                .uri(BASE_URL + "/update")
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Metadata key cannot be null or empty.", errorResponse.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing metadata from an assignment successfully.
     * <p>
     * Given: An existing assignment with metadata
     * When: A DELETE request is made to remove a specific metadata entry by key
     * Then: The metadata should be removed, and the response should be 204 No Content
     */
    @Test
    void testRemoveMetadataByKey_Success() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("john.doe@email.com")
                .position("Engineer")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Create and persist an assignment with metadata
        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .key("role")
                .value("engineer")
                .build();
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                List.of(new AssignmentMetadataRequestDto(metadata.getKey(), metadata.getValue())));

        Assignment assignment = assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(
                department.getId(), employee.getId()).orElseThrow();

        // When: Send DELETE request to remove the metadata by key
        webTestClient.delete()
                .uri(BASE_URL + "/{assignmentId}/metadata?metadataKey={key}",
                        assignment.getId(), "role")
                .exchange()
                .expectStatus().isNoContent();

        // Then: Verify that the metadata is removed from the assignment
        Assignment updatedAssignment = assignmentRepository.findById(assignment.getId())
                .orElseThrow();
        assertTrue(updatedAssignment.getMetadata().isEmpty());
    }

    /**
     * Test removing metadata from a non-existent assignment.
     * <p>
     * Given: No assignment exists with the provided ID
     * When: A DELETE request is made to remove metadata
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataByKey_AssignmentNotFound() {
        // Given: Prepare a non-existent assignment ID
        Long nonExistentAssignmentId = -1L;

        // When: Send DELETE request to remove metadata from a non-existent assignment
        webTestClient.delete()
                .uri(BASE_URL + "/{assignmentId}/metadata?metadataKey={key}",
                        nonExistentAssignmentId, "role")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing non-existent metadata from an assignment.
     * <p>
     * Given: An existing assignment without the specified metadata key
     * When: A DELETE request is made to remove non-existent metadata by key
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataByKeyMetadataNotFound() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Sales")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("Alice")
                .lastName("Brown")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("alice.brown@email.com")
                .position("Sales Rep")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Create and persist an assignment with metadata
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                List.of(new AssignmentMetadataRequestDto("role", "sales rep")));

        Assignment assignment = assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(
                department.getId(), employee.getId()).orElseThrow();

        // When: Send DELETE request to remove non-existent metadata by key
        String metadataKey = "invalid-key";
        webTestClient.delete()
                .uri(BASE_URL + "/{assignmentId}/metadata?metadataKey={key}",
                        assignment.getId(), metadataKey)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Metadata with key '" + metadataKey + "' not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing metadata with invalid input.
     * <p>
     * Given: A valid assignment exists
     * When: A DELETE request is made with an invalid key (e.g., empty key)
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataByKey_InvalidInput() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Marketing")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("Bob")
                .lastName("White")
                .dateOfBirth(LocalDate.now().minusYears(32))
                .email("bob.white@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Create and persist an assignment with metadata
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                List.of(new AssignmentMetadataRequestDto("role", "manager")));

        Assignment assignment = assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(
                department.getId(), employee.getId()).orElseThrow();

        // When: Send DELETE request with invalid metadata key (empty string)
        webTestClient.delete()
                .uri(BASE_URL + "/{assignmentId}/metadata?metadataKey={key}",
                        assignment.getId(), "")
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * Test removing metadata from an assignment by metadata ID successfully.
     * <p>
     * Given: An existing assignment with metadata
     * When: A DELETE request is made to remove a specific metadata entry by ID
     * Then: The metadata should be removed, and the response should be 204 No Content
     */
    @Test
    void testRemoveMetadataById_Success() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("john.doe@email.com")
                .position("Engineer")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Create and persist an assignment with metadata
        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .key("role")
                .value("engineer")
                .build();
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                List.of(new AssignmentMetadataRequestDto(metadata.getKey(), metadata.getValue())));

        Assignment assignment = assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(
                department.getId(), employee.getId()).orElseThrow();

        AssignmentMetadata persistedMetadata = (AssignmentMetadata) assignment.getMetadata().iterator().next();

        // When: Send DELETE request to remove the metadata by ID
        webTestClient.delete()
                .uri(BASE_URL + "/{assignmentId}/metadata/{metadataId}",
                        assignment.getId(), persistedMetadata.getId())
                .exchange()
                .expectStatus().isNoContent();

        // Then: Verify that the metadata is removed from the assignment
        Assignment updatedAssignment = assignmentRepository.findById(assignment.getId())
                .orElseThrow();
        assertTrue(updatedAssignment.getMetadata().isEmpty());
    }

    /**
     * Test removing metadata from a non-existent assignment by metadata ID.
     * <p>
     * Given: No assignment exists with the provided ID
     * When: A DELETE request is made to remove metadata
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataById_AssignmentNotFound() {
        // Given: Prepare a non-existent assignment ID
        Long nonExistentAssignmentId = -1L;
        Long metadataId = 1L;  // Arbitrary ID for the metadata

        // When: Send DELETE request to remove metadata from a non-existent assignment
        webTestClient.delete()
                .uri(BASE_URL + "/{assignmentId}/metadata/{metadataId}",
                        nonExistentAssignmentId, metadataId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing metadata with invalid input.
     * <p>
     * Given: A valid assignment exists
     * When: A DELETE request is made with an invalid metadata ID (e.g., -1L)
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataById_InvalidInput() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("Marketing")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("Bob")
                .lastName("White")
                .dateOfBirth(LocalDate.now().minusYears(32))
                .email("bob.white@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Create and persist an assignment with metadata
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                List.of(new AssignmentMetadataRequestDto("role", "manager")));

        Assignment assignment = assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(
                department.getId(), employee.getId()).orElseThrow();

        // When: Send DELETE request with invalid metadata ID (-1L)
        webTestClient.delete()
                .uri(BASE_URL + "/{assignmentId}/metadata/{metadataId}",
                        assignment.getId(), -1L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Metadata not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing metadata by hierarchical entity ID, assignable entity ID, and metadata ID successfully.
     * <p>
     * Given: An existing hierarchical entity, assignable entity, and metadata
     * When: A DELETE request is made to remove the metadata
     * Then: The metadata should be removed, and the response should be 204 No Content
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityId_Success() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("HR")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(28))
                .email("jane.smith@email.com")
                .position("HR Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Create and persist an assignment with metadata
        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .key("role")
                .value("HR Manager")
                .build();
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                List.of(new AssignmentMetadataRequestDto(metadata.getKey(), metadata.getValue())));

        Assignment assignment = assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(
                department.getId(), employee.getId()).orElseThrow();

        AssignmentMetadata persistedMetadata = (AssignmentMetadata) assignment.getMetadata().iterator().next();

        // When: Send DELETE request to remove the metadata by hierarchical and assignable entity IDs and metadata ID
        webTestClient.delete()
                .uri(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata/{metadataId}",
                        department.getId(), employee.getId(), persistedMetadata.getId())
                .exchange()
                .expectStatus().isNoContent();

        // Then: Verify that the metadata is removed from the assignment
        Assignment updatedAssignment = assignmentRepository.findById(assignment.getId())
                .orElseThrow();
        assertTrue(updatedAssignment.getMetadata().isEmpty());
    }

    /**
     * Test removing metadata with non-existent hierarchical entity ID.
     * <p>
     * Given: No hierarchical entity exists with the provided ID
     * When: A DELETE request is made to remove metadata
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityId_HierarchicalEntityNotFound() {
        // Given: Prepare a non-existent hierarchical entity ID
        Long nonExistentHierarchicalEntityId = -1L;
        Long assignableEntityId = 1L;  // Arbitrary ID for the assignable entity
        Long metadataId = 1L;  // Arbitrary ID for the metadata

        // When: Send DELETE request to remove metadata by non-existent hierarchical entity ID
        webTestClient.delete()
                .uri(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata/{metadataId}",
                        nonExistentHierarchicalEntityId, assignableEntityId, metadataId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Hierarchical entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing metadata with non-existent assignable entity ID.
     * <p>
     * Given: A valid hierarchical entity exists but no assignable entity exists with the provided ID
     * When: A DELETE request is made to remove metadata
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityId_AssignableEntityNotFound() {
        // Given: Prepare and persist a hierarchical entity
        Department department = Department.builder()
                .name("Finance")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Long nonExistentAssignableEntityId = -1L;
        Long metadataId = 1L;  // Arbitrary ID for the metadata

        // When: Send DELETE request to remove metadata by non-existent assignable entity ID
        webTestClient.delete()
                .uri(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata/{metadataId}",
                        department.getId(), nonExistentAssignableEntityId, metadataId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignable entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing metadata with non-existent assignment.
     * <p>
     * Given: A valid hierarchical entity and a valid assignable entity exists but no assignment exists
     * When: A DELETE request is made to remove metadata
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityId_AssignmentNotFound() {
        Department department = Department.builder()
                .name("IT")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("Michael")
                .lastName("Jordan")
                .dateOfBirth(LocalDate.now().minusYears(45))
                .email("mjordan@email.com")
                .position("IT Specialist")
                .build();
        employee = assignableEntityRepository.save(employee);

        Long metadataId = 1L;  // Arbitrary ID for the metadata

        // When: Send DELETE request to remove metadata by non-existent assignable entity ID
        webTestClient.delete()
                .uri(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata/{metadataId}",
                        department.getId(), employee.getId(), metadataId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test removing non-existent metadata from an assignment by hierarchical and assignable entity IDs.
     * <p>
     * Given: An existing hierarchical entity and assignable entity but no metadata with the provided ID
     * When: A DELETE request is made to remove non-existent metadata by ID
     * Then: The response should be 404 Not Found
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityId_MetadataNotFound() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("IT")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Employee employee = Employee.builder()
                .firstName("Michael")
                .lastName("Jordan")
                .dateOfBirth(LocalDate.now().minusYears(45))
                .email("mjordan@email.com")
                .position("IT Specialist")
                .build();
        employee = assignableEntityRepository.save(employee);

        // Given: Create and persist an assignment without the specified metadata ID
        assignmentService.assignEntityToHierarchicalEntity(department.getId(), employee.getId(),
                List.of(new AssignmentMetadataRequestDto("role", "IT Specialist")));

        Assignment assignment = assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(
                department.getId(), employee.getId()).orElseThrow();

        Long nonExistentMetadataId = -1L;  // Arbitrary ID for non-existent metadata

        // When: Send DELETE request to remove non-existent metadata by hierarchical and assignable entity IDs
        webTestClient.delete()
                .uri(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata/{metadataId}",
                        department.getId(), employee.getId(), nonExistentMetadataId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Metadata not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Successfully remove metadata by hierarchical and assignable entity key.
     * <p>
     * Given an existing hierarchical entity, assignable entity, and metadata,
     * When a DELETE request is made to remove the metadata by the key,
     * Then the metadata should be removed, and the response should be 204 No Content.
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityKey_Success() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("HR")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(28))
                .email("jane.smith@email.com")
                .position("HR Manager")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // Given: Create and persist an assignment with metadata
        String metadataKey = "role";
        AssignmentMetadata metadata = AssignmentMetadata.builder()
                .key(metadataKey)
                .value("HR Manager")
                .build();
        assignmentService.assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId,
                List.of(new AssignmentMetadataRequestDto(metadata.getKey(), metadata.getValue())));

        // When: Send DELETE request to remove the metadata by key
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata")
                        .queryParam("metadataKey", metadataKey)
                        .build(hierarchicalEntityId, assignableEntityId))
                .exchange()
                .expectStatus().isNoContent();

        // Then: Verify that the metadata is removed from the assignment
        Assignment updatedAssignment = assignmentRepository.findByHierarchicalEntityIdAndAssignableEntityId(
                hierarchicalEntityId, assignableEntityId).orElseThrow();
        assertTrue(updatedAssignment.getMetadata().isEmpty());
    }

    /**
     * Test case: Attempt to remove metadata by hierarchical and assignable entity key,
     * but the hierarchical entity is not found.
     * <p>
     * Given no hierarchical entity exists with the provided ID,
     * When a DELETE request is made to remove metadata by the key,
     * Then the response should be 404 Not Found.
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityKey_HierarchicalEntityNotFound() {
        // Given: Prepare a non-existent hierarchical entity ID
        Long nonExistentHierarchicalEntityId = -1L;
        Long assignableEntityId = 1L;  // Arbitrary ID for the assignable entity
        String metadataKey = "role";

        // When: Send DELETE request to remove metadata by non-existent hierarchical entity ID
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata")
                        .queryParam("metadataKey", metadataKey)
                        .build(nonExistentHierarchicalEntityId, assignableEntityId))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Hierarchical entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to remove metadata by hierarchical and assignable entity key,
     * but the assignable entity is not found.
     * <p>
     * Given a valid hierarchical entity exists but no assignable entity exists with the provided ID,
     * When a DELETE request is made to remove metadata by the key,
     * Then the response should be 404 Not Found.
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityKey_AssignableEntityNotFound() {
        // Given: Prepare and persist a hierarchical entity
        Department department = Department.builder()
                .name("Finance")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Long nonExistentAssignableEntityId = -1L;
        String metadataKey = "role";

        // When: Send DELETE request to remove metadata by non-existent assignable entity ID
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata")
                        .queryParam("metadataKey", metadataKey)
                        .build(hierarchicalEntityId, nonExistentAssignableEntityId))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignable entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to remove metadata by hierarchical and assignable entity key,
     * but the assignment is not found.
     * <p>
     * Given a valid hierarchical entity and assignable entity exist, but no assignment exists between them,
     * When a DELETE request is made to remove metadata by the key,
     * Then the response should be 404 Not Found.
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityKey_AssignmentNotFound() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("IT")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("Michael")
                .lastName("Jordan")
                .dateOfBirth(LocalDate.now().minusYears(45))
                .email("mjordan@email.com")
                .position("IT Specialist")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        String metadataKey = "role";

        // When: Send DELETE request to remove metadata by key but no assignment exists
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata")
                        .queryParam("metadataKey", metadataKey)
                        .build(hierarchicalEntityId, assignableEntityId))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to remove metadata by hierarchical and assignable entity key,
     * but the metadata is not found.
     * <p>
     * Given an existing hierarchical entity and assignable entity, but no metadata with the provided key,
     * When a DELETE request is made to remove metadata by the key,
     * Then the response should be 404 Not Found.
     */
    @Test
    void testRemoveMetadataByHierarchicalAndAssignableEntityKey_MetadataNotFound() {
        // Given: Prepare and persist a hierarchical entity and assignable entity
        Department department = Department.builder()
                .name("IT")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("Michael")
                .lastName("Jordan")
                .dateOfBirth(LocalDate.now().minusYears(45))
                .email("mjordan@email.com")
                .position("IT Specialist")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // Given: Create and persist an assignment with a different metadata key
        assignmentService.assignEntityToHierarchicalEntity(hierarchicalEntityId, assignableEntityId,
                List.of(new AssignmentMetadataRequestDto("role", "IT Specialist")));

        String nonExistentMetadataKey = "location";

        // When: Send DELETE request to remove non-existent metadata by key
        webTestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/hierarchical/{hierarchicalEntityId}/assignable/{assignableEntityId}/metadata")
                        .queryParam("metadataKey", nonExistentMetadataKey)
                        .build(hierarchicalEntityId, assignableEntityId))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Metadata with key '" + nonExistentMetadataKey + "' not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Successfully retrieve assignments by hierarchical entity ID.
     * <p>
     * Given an existing hierarchical entity and associated assignments,
     * When a GET request is made to retrieve assignments by hierarchical entity ID,
     * Then the response should be 200 OK with the list of assignments.
     */
    @Test
    void testGetAssignmentsByHierarchicalEntity_Success() {
        // Given: Prepare and persist a hierarchical entity and associated assignments
        Department department = Department.builder()
                .name("HR")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(28))
                .email("jane.doe@email.com")
                .position("HR Manager")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();
        assignmentRepository.save(assignment);

        // When: Send GET request to retrieve assignments by hierarchical entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-entity")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>>() {
                })
                .value(assignments -> {
                    assertNotNull(assignments);
                    assertEquals(1, assignments.size());
                    assertEquals(assignableEntityId, assignments.get(0).getAssignableEntity().getId());
                });
    }

    /**
     * Test case: Attempt to retrieve assignments by hierarchical entity ID, but the hierarchical entity is not found.
     * <p>
     * Given no hierarchical entity exists with the provided ID,
     * When a GET request is made to retrieve assignments by hierarchical entity ID,
     * Then the response should be 404 Not Found with an appropriate error message.
     */
    @Test
    void testGetAssignmentsByHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given: Prepare a non-existent hierarchical entity ID
        Long nonExistentHierarchicalEntityId = -1L;

        // When: Send GET request to retrieve assignments by non-existent hierarchical entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-entity")
                        .queryParam("hierarchicalEntityId", nonExistentHierarchicalEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Hierarchical entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to retrieve assignments by hierarchical entity ID with invalid input.
     * <p>
     * Given an invalid hierarchical entity ID is provided,
     * When a GET request is made to retrieve assignments by hierarchical entity ID,
     * Then the response should be 400 Bad Request with an appropriate error message.
     */
    @Test
    void testGetAssignmentsByHierarchicalEntity_InvalidInput() {
        // Given: Prepare an invalid hierarchical entity ID (e.g., null or negative)

        // When: Send GET request to retrieve assignments by hierarchical entity ID with invalid input
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-entity")
                        .queryParam("hierarchicalEntityId", (Long) null)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Successfully retrieve paginated assignments by hierarchical entity ID.
     * <p>
     * Given an existing hierarchical entity and associated assignments,
     * When a GET request is made to retrieve paginated assignments by hierarchical entity ID,
     * Then the response should be 200 OK with the paginated list of assignments.
     */
    @Test
    void testGetPaginatedAssignmentsByHierarchicalEntity_Success() {
        // Given: Prepare and persist a hierarchical entity and associated assignments
        Department department = Department.builder()
                .name("Finance")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("john.doe@email.com")
                .position("Accountant")
                .build();
        Employee employee2 = Employee.builder()
                .firstName("Alice")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("alice.smith@email.com")
                .position("Financial Analyst")
                .build();
        assignableEntityRepository.saveAll(List.of(employee1, employee2));

        DepartmentEmployeeAssignment assignment1 = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee1)
                .build();
        DepartmentEmployeeAssignment assignment2 = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee2)
                .build();
        assignmentRepository.saveAll(List.of(assignment1, assignment2));

        // When: Send GET request to retrieve paginated assignments by hierarchical entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-entity/paginated")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .queryParam("page", 0)
                        .queryParam("size", 1)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PaginatedResponseDto<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>>() {
                })
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(1, response.getContent().size());
                    assertEquals(2, response.getTotalElements());
                    assertEquals(2, response.getTotalPages());
                });
    }

    /**
     * Test case: Attempt to retrieve paginated assignments by hierarchical entity ID, but the hierarchical entity is not found.
     * <p>
     * Given no hierarchical entity exists with the provided ID,
     * When a GET request is made to retrieve paginated assignments by hierarchical entity ID,
     * Then the response should be 404 Not Found with an appropriate error message.
     */
    @Test
    void testGetPaginatedAssignmentsByHierarchicalEntity_HierarchicalEntityNotFound() {
        // Given: Prepare a non-existent hierarchical entity ID
        Long nonExistentHierarchicalEntityId = -1L;

        // When: Send GET request to retrieve paginated assignments by non-existent hierarchical entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-entity/paginated")
                        .queryParam("hierarchicalEntityId", nonExistentHierarchicalEntityId)
                        .queryParam("page", 0)
                        .queryParam("size", 1)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Hierarchical entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to retrieve paginated assignments by hierarchical entity ID with invalid input.
     * <p>
     * Given an invalid hierarchical entity ID or pagination parameters,
     * When a GET request is made to retrieve paginated assignments by hierarchical entity ID,
     * Then the response should be 400 Bad Request with an appropriate error message.
     */
    @Test
    void testGetPaginatedAssignmentsByHierarchicalEntity_InvalidInput() {
        // Given: Prepare an invalid hierarchical entity ID and invalid pagination parameters

        // When: Send GET request to retrieve paginated assignments by hierarchical entity ID with invalid input
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-entity/paginated")
                        .queryParam("hierarchicalEntityId", (Long) null)  // Invalid ID
                        .queryParam("page", 0)
                        .queryParam("size", 1)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Successfully retrieve assignments by assignable entity ID.
     * <p>
     * Given an existing assignable entity and associated assignments,
     * When a GET request is made to retrieve assignments by assignable entity ID,
     * Then the response should be 200 OK with the list of assignments.
     */
    @Test
    void testGetAssignmentsByAssignableEntity_Success() {
        // Given: Prepare and persist an assignable entity and associated assignments
        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("john.doe@email.com")
                .position("Accountant")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        Department department1 = Department.builder()
                .name("Finance")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("IT")
                .path("/2/")
                .build();
        hierarchicalEntityRepository.saveAll(List.of(department1, department2));

        DepartmentEmployeeAssignment assignment1 = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department1)
                .assignableEntity(employee)
                .build();
        DepartmentEmployeeAssignment assignment2 = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department2)
                .assignableEntity(employee)
                .build();
        assignmentRepository.saveAll(List.of(assignment1, assignment2));

        // When: Send GET request to retrieve assignments by assignable entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-assignable-entity")
                        .queryParam("assignableEntityId", assignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(new ParameterizedTypeReference<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(2, response.size());
                });
    }

    /**
     * Test case: Attempt to retrieve assignments by assignable entity ID, but the assignable entity is not found.
     * <p>
     * Given no assignable entity exists with the provided ID,
     * When a GET request is made to retrieve assignments by assignable entity ID,
     * Then the response should be 404 Not Found with an appropriate error message.
     */
    @Test
    void testGetAssignmentsByAssignableEntity_EntityNotFound() {
        // Given: Prepare a non-existent assignable entity ID
        Long nonExistentAssignableEntityId = -1L;

        // When: Send GET request to retrieve assignments by non-existent assignable entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-assignable-entity")
                        .queryParam("assignableEntityId", nonExistentAssignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignable entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to retrieve assignments by assignable entity ID with invalid input.
     * <p>
     * Given an invalid assignable entity ID,
     * When a GET request is made to retrieve assignments by assignable entity ID,
     * Then the response should be 400 Bad Request with an appropriate error message.
     */
    @Test
    void testGetAssignmentsByAssignableEntity_InvalidInput() {
        // Given: Prepare an invalid assignable entity ID (null)

        // When: Send GET request to retrieve assignments by assignable entity ID with invalid input
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-assignable-entity")
                        .queryParam("assignableEntityId", (Long) null)  // Invalid ID
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Successfully retrieve paginated assignments by assignable entity ID.
     * <p>
     * Given an existing assignable entity and associated assignments,
     * When a GET request is made to retrieve paginated assignments by assignable entity ID,
     * Then the response should be 200 OK with a paginated list of assignments.
     */
    @Test
    void testGetPaginatedAssignmentsByAssignableEntity_Success() {
        // Given: Prepare and persist an assignable entity and associated assignments
        Employee employee = Employee.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(28))
                .email("jane.smith@email.com")
                .position("Software Engineer")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        Department department1 = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        Department department2 = Department.builder()
                .name("Marketing")
                .path("/2/")
                .build();
        hierarchicalEntityRepository.saveAll(List.of(department1, department2));

        DepartmentEmployeeAssignment assignment1 = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department1)
                .assignableEntity(employee)
                .build();
        DepartmentEmployeeAssignment assignment2 = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department2)
                .assignableEntity(employee)
                .build();
        assignmentRepository.saveAll(List.of(assignment1, assignment2));

        // When: Send GET request to retrieve paginated assignments by assignable entity ID with pagination parameters
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-assignable-entity/paginated")
                        .queryParam("assignableEntityId", assignableEntityId)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaginatedResponseDto.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(2, response.getTotalElements());
                    assertEquals(1, response.getTotalPages());
                    assertEquals(0, response.getPage());
                    assertEquals(10, response.getSize());
                    assertFalse(response.getContent().isEmpty());
                });
    }

    /**
     * Test case: Attempt to retrieve paginated assignments by assignable entity ID, but the assignable entity is not found.
     * <p>
     * Given no assignable entity exists with the provided ID,
     * When a GET request is made to retrieve paginated assignments by assignable entity ID,
     * Then the response should be 404 Not Found with an appropriate error message.
     */
    @Test
    void testGetPaginatedAssignmentsByAssignableEntity_EntityNotFound() {
        // Given: Prepare a non-existent assignable entity ID
        Long nonExistentAssignableEntityId = -1L;

        // When: Send GET request to retrieve paginated assignments by non-existent assignable entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-assignable-entity/paginated")
                        .queryParam("assignableEntityId", nonExistentAssignableEntityId)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignable entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to retrieve paginated assignments by assignable entity ID with invalid input.
     * <p>
     * Given an invalid assignable entity ID,
     * When a GET request is made to retrieve paginated assignments by assignable entity ID,
     * Then the response should be 400 Bad Request with an appropriate error message.
     */
    @Test
    void testGetPaginatedAssignmentsByAssignableEntity_InvalidInput() {
        // Given: Prepare an invalid assignable entity ID (null)

        // When: Send GET request to retrieve paginated assignments by assignable entity ID with invalid input
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-assignable-entity/paginated")
                        .queryParam("assignableEntityId", (Long) null)  // Invalid ID
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Successfully retrieve an assignment by hierarchical entity ID and assignable entity ID.
     * <p>
     * Given existing hierarchical and assignable entities with an assignment,
     * When a GET request is made to retrieve the assignment by hierarchical entity ID and assignable entity ID,
     * Then the response should be 200 OK with the assignment DTO.
     */
    @Test
    void testGetAssignmentByHierarchicalAndAssignableEntity_Success() {
        // Given: Prepare and persist hierarchical and assignable entities
        Department department = Department.builder()
                .name("Engineering")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .email("john.doe@email.com")
                .position("Developer")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        DepartmentEmployeeAssignment assignment = DepartmentEmployeeAssignment.builder()
                .hierarchicalEntity(department)
                .assignableEntity(employee)
                .build();
        assignment = assignmentRepository.save(assignment);

        // When: Send GET request to retrieve the assignment by hierarchical entity ID and assignable entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-and-assignable-entity")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .queryParam("assignableEntityId", assignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<AssignmentResponseDto<HierarchyResponseDto, EmployeeResponseDto>>() {
                })
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(hierarchicalEntityId, response.getHierarchicalEntity().getId());
                    assertEquals(assignableEntityId, response.getAssignableEntity().getId());
                });
    }

    /**
     * Test case: Attempt to retrieve an assignment by hierarchical entity ID and assignable entity ID when the hierarchical entity is not found.
     * <p>
     * Given no hierarchical entity exists with the provided ID,
     * When a GET request is made to retrieve the assignment,
     * Then the response should be 404 Not Found with an appropriate error message.
     */
    @Test
    void testGetAssignmentByHierarchicalAndAssignableEntity_HierarchicalEntityNotFound() {
        // Given: Prepare an assignable entity and a non-existent hierarchical entity ID
        Employee employee = Employee.builder()
                .firstName("Alice")
                .lastName("Smith")
                .dateOfBirth(LocalDate.now().minusYears(25))
                .email("alice.smith@email.com")
                .position("Manager")
                .build();
        employee = assignableEntityRepository.save(employee);

        Long nonExistentHierarchicalEntityId = -1L;
        Long assignableEntityId = employee.getId();

        // When: Send GET request to retrieve the assignment by non-existent hierarchical entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-and-assignable-entity")
                        .queryParam("hierarchicalEntityId", nonExistentHierarchicalEntityId)
                        .queryParam("assignableEntityId", assignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Hierarchical entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to retrieve an assignment by hierarchical entity ID and assignable entity ID when the assignable entity is not found.
     *
     * Given no assignable entity exists with the provided ID,
     * When a GET request is made to retrieve the assignment,
     * Then the response should be 404 Not Found with an appropriate error message.
     */
    @Test
    void testGetAssignmentByHierarchicalAndAssignableEntity_AssignableEntityNotFound() {
        // Given: Prepare a hierarchical entity and a non-existent assignable entity ID
        Department department = Department.builder()
                .name("Sales")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);

        Long hierarchicalEntityId = department.getId();
        Long nonExistentAssignableEntityId = -1L;

        // When: Send GET request to retrieve the assignment by non-existent assignable entity ID
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-and-assignable-entity")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .queryParam("assignableEntityId", nonExistentAssignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignable entity not found", errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to retrieve an assignment by hierarchical entity ID and assignable entity ID when the assignment is not found.
     *
     * Given a hierarchical entity and an assignable entity without an assignment,
     * When a GET request is made to retrieve the assignment,
     * Then the response should be 404 Not Found with an appropriate error message.
     */
    @Test
    void testGetAssignmentByHierarchicalAndAssignableEntity_AssignmentNotFound() {
        // Given: Prepare a hierarchical entity and an assignable entity with no existing assignment
        Department department = Department.builder()
                .name("HR")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long hierarchicalEntityId = department.getId();

        Employee employee = Employee.builder()
                .firstName("Bob")
                .lastName("Johnson")
                .dateOfBirth(LocalDate.now().minusYears(40))
                .email("bob.johnson@email.com")
                .position("Consultant")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();

        // When: Send GET request to retrieve an assignment that does not exist
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-and-assignable-entity")
                        .queryParam("hierarchicalEntityId", hierarchicalEntityId)
                        .queryParam("assignableEntityId", assignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertEquals("Assignment not found for the given hierarchical entity ID: " + hierarchicalEntityId + " and assignable entity ID: " + assignableEntityId, errorResponse.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
                });
    }

    /**
     * Test case: Attempt to retrieve an assignment by hierarchical entity ID and assignable entity ID with invalid input.
     *
     * Given invalid hierarchical entity ID or assignable entity ID,
     * When a GET request is made to retrieve the assignment,
     * Then the response should be 400 Bad Request with an appropriate error message.
     */
    @Test
    void testGetAssignmentByHierarchicalAndAssignableEntity_InvalidInput() {
        // Given: Prepare valid hierarchical and assignable entity IDs but provide invalid input
        Department department = Department.builder()
                .name("Legal")
                .path("/1/")
                .build();
        department = hierarchicalEntityRepository.save(department);
        Long invalidHierarchicalEntityId = null;

        Employee employee = Employee.builder()
                .firstName("Emily")
                .lastName("Taylor")
                .dateOfBirth(LocalDate.now().minusYears(35))
                .email("emily.taylor@email.com")
                .position("Analyst")
                .build();
        employee = assignableEntityRepository.save(employee);
        Long assignableEntityId = employee.getId();
        // When: Send GET request with invalid hierarchical entity ID (null)
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_URL + "/by-hierarchical-and-assignable-entity")
                        .queryParam("hierarchicalEntityId", invalidHierarchicalEntityId)  // Invalid ID
                        .queryParam("assignableEntityId", assignableEntityId)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertNotNull(errorResponse);
                    assertThat(errorResponse.getMessage()).contains("Validation error");
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                });
    }

}