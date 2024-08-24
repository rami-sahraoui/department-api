# Hierarchical Entity API

## Overview

The Hierarchical Entity API is a Spring Boot application designed to manage hierarchical data structures for various
entities. It supports CRUD operations and is designed to handle hierarchical relationships using different models such
as Adjacency List, Nested Set, Parameterized Path, and Closure Table. The API includes integration tests using
Testcontainers for database management and follows best practices for exception handling.

## Features

- CRUD operations for hierarchical entities
- Support for multiple hierarchical models (Adjacency List, Nested Set, Parameterized Path, Closure Table)
- Custom error handling
- Integration tests with Testcontainers
- OpenAPI documentation

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.6.0 or later
- Docker (for running Testcontainers)

### Installation

1. Clone the repository:
   ```sh
   git clone https://github.com/rami-sahraoui/hierarchical-entity-api.git
   cd hierarchical-entity-api
   ```
2. Build the project:
   ```sh
   mvn clean install
   ```

### Configuration

#### Application Properties

The application can be configured using `application.properties` or `application.yml` files located in
the `src/main/resources` directory.

For local development and real database, configure the database connection:

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/hierarchical_entity_db?createDatabaseIfNotExist=TRUE&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=yourpassword
   ```

For integration tests, the Testcontainers setup will handle the database configuration automatically.

#### Profiles

Profiles are used to separate configuration for different environments. For example:

- `application.properties` for default configuration
- `application-test-container.properties` for `Testcontainer` specific configuration
- `application-test-real-db.properties` for real database test-specific configuration

Activate a profile using:

   ```sh
   -Dspring.profiles.active=test
   ```

### Running the Application

To run the application locally:

   ```sh
   ./mvnw spring-boot:run
   ```

### Running Tests

To run all tests including integration tests:

   ```sh
   ./mvnw verify
   ```

## API Documentation

The API documentation is available at `/swagger-ui.html` when the application is running. It provides an interactive
interface to test the API endpoints.

## Usage

### Create an Entity

#### Request

   ```sh
POST /api/v1/entities
Content-Type: application/json
   ```

#### Body

   ```json
   {
  "name": "Engineering",
  "parentDepartmentId": null
}
   ```

#### Response

   ```json
   {
  "id": 1,
  "name": "Engineering",
  "parentEntityId": null,
  "subEntities": []
}
   ```

### Get All Entities

#### Request

   ```sh
GET /api/v1/entities
   ```

#### Response

   ```json
   [
  {
    "id": 1,
    "name": "Engineering",
    "parentEntityId": null,
    "subEntities": []
  }
]
   ```

### Example: Department Entity

Here’s an example of how to configure and use the API for a Department entity:

#### Department Model

   ```java
package tn.engn.hierarchicalentityapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a department entity in the Nested Set Model with support for multi-tree structures.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = true) // Include fields from the superclass
public class Department extends HierarchyBaseEntity<Department> {

   /**
    * Override method to return the entity type class.
    */
   @Override
   public Class<Department> getEntityType() {
      return Department.class;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      Department that = (Department) o;
      return true; // Additional specific checks if needed
   }

   @Override
   public int hashCode() {
      return super.hashCode(); // Include superclass hash code
   }
}
   ```

#### Department Repository

   ```java
   package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.Department;

@Repository
public interface DepartmentRepository extends HierarchyBaseRepository<Department> {
}
   ```

#### Department Mapper

   ```java
   package tn.engn.hierarchicalentityapi.mapper;

import org.springframework.stereotype.Component;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Department;

@Component
public class DepartmentMapper extends AbstractHierarchyMapper<Department, HierarchyRequestDto, HierarchyResponseDto> {
    @Override
    protected boolean shouldFetchSubEntities() {
        return true;
    }

    @Override
    protected Department createNewEntityInstance() {
        return new Department();
    }

    @Override
    protected HierarchyResponseDto createNewResponseDtoInstance() {
        return new HierarchyResponseDto();
    }
}
   ```

#### Department Service

   ```java
   package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.DepartmentMapper;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

@Service
@Slf4j
public class AdjacencyListDepartmentService extends AdjacencyListEntityService<Department, HierarchyRequestDto, HierarchyResponseDto> implements DepartmentService {
    public AdjacencyListDepartmentService(DepartmentRepository entityRepository, DepartmentMapper entityMapper, JPAQueryFactory jpaQueryFactory) {
        super(entityRepository, entityMapper, jpaQueryFactory, Department.class);
    }
}
   ```

#### Department Controller

   ```java
   package tn.engn.hierarchicalentityapi.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.hierarchicalentityapi.annotation.SubEntitiesPath;
import tn.engn.hierarchicalentityapi.service.DepartmentService;

@RestController
@RequestMapping("/api/v1/departments")
@SubEntitiesPath("sub-departments")
public class DepartmentController extends HierarchyController {
    public DepartmentController(DepartmentService hierarchyService) {
        super(hierarchyService);
    }
}
   ```

### Dynamic Sub-Entities Path

The `@SubEntitiesPath` annotation dynamically sets the path for sub-entities. This allows flexibility in managing
different hierarchical structures and ensures that sub-entity paths are correctly resolved.

## Assignable Entity API (Employee API)

### Overview

The Employee API provides assignable entities that can be related to hierarchical entities using the Assignment API.
This API supports CRUD operations for employees and facilitates their assignment to different hierarchical structures.

### Create an Employee

#### Request

   ```sh
POST /api/v1/employees
Content-Type: application/json
   ```

#### Body

   ```json
   {
     "firstName": "John",
     "lastName": "Doe",
     "email": "john.doe@example.com",
     "dateOfBirth": "1990-01-01",
     "position": "Developer"
  }
   ```

#### Response

   ```json
   {
      "id": 1,
      "name": "John Doe",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "dateOfBirth": "1990-01-01",
      "position": "Developer"
   }
   ```

### Get All Employees

#### Request

   ```sh
GET /api/v1/employees
   ```

#### Response

   ```json
  [
      {
         "id": 1,
         "name": "John Doe",
         "firstName": "John",
         "lastName": "Doe",
         "email": "john.doe@example.com",
         "dateOfBirth": "1990-01-01",
         "position": "Developer"
      }
  ]
   ```

### The Assignable Entity
To define an assignable entity, extend the `AssignableEntity` abstract class:

   ```java
package tn.engn.assignmentapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Abstract class representing an entity that can be assigned to hierarchical entities.
 * Extending classes will represent specific types of assignable entities.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AssignableEntity<A extends AssignableEntity<A>> {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // Additional common fields (if any) go here
}
   ```

For example, an `Employee` entity:

   ```java
package tn.engn.employeeapi.model;

import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tn.engn.assignmentapi.model.AssignableEntity;

/**
 * Represents an employee entity.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Employee extends AssignableEntity<Employee> {
   // Employee-specific fields
   private String firstName;
   private String lastName;
   private String email;
   private String dateOfBirth;
   private String position;
}
   ```

### The Assignable Entity Repository
To define a repository for an assignable entity, extend the `AssignableEntityRepository` interface:

   ```java
package tn.engn.assignmentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import tn.engn.assignmentapi.model.AssignableEntity;

/**
 * Repository interface for handling assignable entities.
 * Extends JpaRepository for CRUD operations and QuerydslPredicateExecutor for query capabilities.
 *
 * @param <E> the type of assignable entity
 */
@NoRepositoryBean
public interface AssignableEntityRepository<E extends AssignableEntity<E>>
        extends JpaRepository<E, Long>, QuerydslPredicateExecutor<E> {
}
   ```

For example, the `EmployeeRepository`:

   ```java
package tn.engn.employeeapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.employeeapi.model.Employee;

/**
 * Repository interface for managing Employee entities.
 */
@Repository
public interface EmployeeRepository extends AssignableEntityRepository<Employee> {}
   ```

### The Assignable Entity DTOs and Mapper
Define DTOs and mapper interfaces for assignable entities:

#### DTO Interfaces:

   ```java
package tn.engn.assignmentapi.dto;

/**
 * Interface representing a request DTO for an assignable entity.
 */
public interface AssignableEntityRequestDto {}
   ```

   ```java
package tn.engn.assignmentapi.dto;

/**
 * Interface representing a response DTO for an assignable entity.
 */
public interface AssignableEntityResponseDto {

    /**
     * Retrieves the ID of the assignable entity.
     *
     * @return the ID of the assignable entity
     */
    Long getId();
}
   ```

#### Mapper Interface:

   ```java
package tn.engn.assignmentapi.mapper;

import org.springframework.data.domain.Page;
import tn.engn.assignmentapi.dto.AssignableEntityRequestDto;
import tn.engn.assignmentapi.dto.AssignableEntityResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;

import java.util.List;

/**
 * Interface representing a generic mapper for assignable entities.
 *
 * @param <A> the type of the assignable entity
 * @param <R> the type of the request DTO
 * @param <D> the type of the response DTO
 */
public interface AssignableEntityMapper<A, R extends AssignableEntityRequestDto, D extends AssignableEntityResponseDto> {

    A toEntity(R dto);

    D toDto(A entity);

    List<D> toDtoList(List<A> entities);

    PaginatedResponseDto<D> toDtoPage(Page<A> page);
}
   ```

#### Example for Employee DTOs:

   ```java
package tn.engn.employeeapi.dto;

import lombok.Builder;
import lombok.Data;
import tn.engn.assignmentapi.dto.AssignableEntityRequestDto;

/**
 * Data Transfer Object for creating or updating an employee.
 */
@Data
@Builder
public class EmployeeRequestDto implements AssignableEntityRequestDto {
    // Fields for request, e.g., firstName, lastName, etc.
    private String firstName;
    private String lastName;
    private String email;
    private String dateOfBirth;
    private String position;
}
   ```

   ```java
package tn.engn.employeeapi.dto;

import lombok.Builder;
import lombok.Data;
import tn.engn.assignmentapi.dto.AssignableEntityResponseDto;

/**
 * Data Transfer Object for returning employee details.
 */
@Data
@Builder
public class EmployeeResponseDto implements AssignableEntityResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String dateOfBirth;
    private String position;
}
   ```

#### Example Employee Mapper Implementation:

   ```java
package tn.engn.employeeapi.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Employee entities and DTOs.
 */
@Component
@Slf4j
public class EmployeeMapper implements AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> {

   @Override
   public Employee toEntity(EmployeeRequestDto dto) {
      if (dto == null) {
         return null;
      }
      return Employee.builder()
              .firstName(dto.getFirstName())
              .lastName(dto.getLastName())
              .email(dto.getEmail())
              .dateOfBirth(dto.getDateOfBirth())
              .position(dto.getPosition())
              .build();
   }

   @Override
   public EmployeeResponseDto toDto(Employee entity) {
      if (entity == null) {
         return null;
      }
      return EmployeeResponseDto.builder()
              .id(entity.getId())
              .firstName(entity.getFirstName())
              .lastName(entity.getLastName())
              .email(entity.getEmail())
              .dateOfBirth(entity.getDateOfBirth())
              .position(entity.getPosition())
              .build();
   }

   @Override
   public List<EmployeeResponseDto> toDtoList(List<Employee> entities) {
      if (entities == null) {
         return null;
      }
      return entities.stream()
              .map(this::toDto)
              .collect(Collectors.toList());
   }

   @Override
   public PaginatedResponseDto<EmployeeResponseDto> toDtoPage(Page<Employee> page) {
      if (page == null) {
         return null;
      }
      List<EmployeeResponseDto> content = page.getContent().stream()
              .map(this::toDto)
              .collect(Collectors.toList());

      return PaginatedResponseDto.<EmployeeResponseDto>builder()
              .content(content)
              .page(page.getNumber())
              .size(page.getSize())
              .totalElements(page.getTotalElements())
              .totalPages(page.getTotalPages())
              .build();
   }
}
   ```

## Generic Assignment API

### Overview

The Generic Assignment API is designed to assign entities to hierarchical structures. It is flexible and can be used
with any hierarchical model, making it adaptable for various use cases.

### Example: Assign employees to departments
This section demonstrates how to implement a Many-to-Many relationship between `Department` and `Employee` entities.
#### Relationship Mapping

**Department Entity**

In the `Department` entity, define a `ManyToMany` relationship to the `Employee` entity. This allows a department to have multiple employees and vice versa. Use the following code:

   ```java
@Entity
public class Department extends HierarchyBaseEntity<Department> {
      // ...
      /**
       * The set of employees assigned to the department.
       */
      @ManyToMany(mappedBy = "departments", fetch = FetchType.EAGER)
      private Set<Employee> employees = new HashSet<>();
   
      /**
       * Adds an employee to the department.
       *
       * @param employee the employee to add
       */
      public void addEmployee(Employee employee) {
         employees.add(employee);
         employee.getDepartments().add(this);
      }
   
      /**
       * Removes an employee from the department.
       *
       * @param employee the employee to remove
       */
      public void removeEmployee(Employee employee) {
         employees.remove(employee);
         employee.getDepartments().remove(this);
      }
}
   ```

**Employee Entity**

In the `Employee` entity, define a `ManyToMany` relationship to the `Department` entity using a join table. This configuration supports bidirectional linking:

   ```java
@Entity   
public class Employee extends AssignableEntity<Employee> {
      /**
       * The set of departments the employee is associated with.
       */
      @ManyToMany(fetch = FetchType.EAGER)
      @JoinTable(
              name = "department_employee",
              joinColumns = @JoinColumn(name = "employee_id"),
              inverseJoinColumns = @JoinColumn(name = "department_id")
      )
      private Set<Department> departments = new HashSet<>();
}
   ```
#### DepartmentEmployeeAssignment Entity

Create the `DepartmentEmployeeAssignment` entity to represent the assignment relationship:

   ```java
package tn.engn.assignmentapi.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.model.Department;

import java.time.LocalDate;
import java.util.Map;

@Entity
@DiscriminatorValue("Department_Employee")
@NoArgsConstructor
@SuperBuilder
public class DepartmentEmployeeAssignment extends Assignment<Department, Employee> {}
   ```

#### DepartmentEmployeeAssignment Service

Implement the `DepartmentEmployeeAssignmentService` to manage assignments:

   ```java
package tn.engn.assignmentapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.DepartmentEmployeeAssignment;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing assignments between departments and employees.
 * Extends {@link BaseAssignmentService} to provide specific functionality for handling
 * Department and Employee entities.
 */
@Service
@Slf4j
public class DepartmentEmployeeAssignmentService extends BaseAssignmentService<DepartmentEmployeeAssignment, Department, Employee, EmployeeRequestDto, EmployeeResponseDto, HierarchyRequestDto, HierarchyResponseDto> {

   public DepartmentEmployeeAssignmentService(
           HierarchyBaseRepository<Department> hierarchicalEntityRepository,
           AssignableEntityRepository<Employee> assignableEntityRepository,
           AssignmentRepository<Department, Employee, DepartmentEmployeeAssignment> assignmentRepository,
           AssignmentMetadataRepository assignmentMetadataRepository,
           HierarchyMapper<Department, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper,
           AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper,
           AssignmentMapper<Department, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper
   ) {
      super(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository,
              assignmentMetadataRepository, hierarchyMapper, assignableEntityMapper, assignmentMapper);
   }

   /**
    * Adds an employee to a department.
    * Updates the bidirectional relationship between the Department and Employee entities.
    *
    * @param department the Department entity to which the Employee will be added
    * @param employee   the Employee entity to be added to the Department
    */
   @Override
   protected void addEntityToHierarchicalEntity(Department department, Employee employee) {
      department.addEmployee(employee);
   }

   /**
    * Removes an employee from a department.
    * Updates the bidirectional relationship between the Department and Employee entities.
    *
    * @param department the Department entity from which the Employee will be removed
    * @param employee   the Employee entity to be removed from the Department
    */
   @Override
   protected void removeEntityFromHierarchicalEntity(Department department, Employee employee) {
      department.removeEmployee(employee);
   }

   /**
    * Returns the class type of the hierarchical entity associated with this repository.
    * <p>
    * This method is abstract and should be implemented by subclasses to specify the
    * actual type of the hierarchical entity managed by the repository. It is used
    * primarily to assist in constructing type-safe queries and for other type-specific
    * operations within the repository.
    * </p>
    *
    * @return the {@link Class} object representing the type of the hierarchical entity
    */
   @Override
   Class<Department> getHierarchicalEntityClass() {
      return Department.class;
   }

   /**
    * Returns the class type of the assignable entity associated with this repository.
    * <p>
    * This method is abstract and should be implemented by subclasses to specify the
    * actual type of the assignable entity managed by the repository. It is used
    * primarily to assist in constructing type-safe queries and for other type-specific
    * operations within the repository.
    * </p>
    *
    * @return the {@link Class} object representing the type of the assignable entity
    */
   @Override
   Class<Employee> getAssignableEntityClass() {
      return Employee.class;
   }

   /**
    * Creates a new instance of the assignment.
    *
    * @return a new assignment instance
    */
   @Override
   protected DepartmentEmployeeAssignment createAssignmentInstance() {
      return new DepartmentEmployeeAssignment();
   }
}
   ```

#### DepartmentEmployeeAssignment Controller

Define the `DepartmentEmployeeAssignmentController` to handle HTTP requests related to assignments:

   ```java
package tn.engn.assignmentapi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.engn.assignmentapi.model.DepartmentEmployeeAssignment;
import tn.engn.assignmentapi.service.DepartmentEmployeeAssignmentService;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Department;

/**
 * Controller for managing assignments between departments and employees.
 * This controller provides endpoints for assigning employees to departments,
 * removing assignments, bulk operations, and retrieving assignments, both as
 * individual entities and in paginated forms.
 */
@RestController
@RequestMapping("/api/v1/department-employee-assignments")
@Tag(name = "DepartmentEmployeeAssignment (ManyToMany)", description = "API for managing assignments between departments and employees")
public class DepartmentEmployeeAssignmentController extends AbstractAssignmentController<
        DepartmentEmployeeAssignment,
        Department,
        Employee,
        EmployeeRequestDto,
        EmployeeResponseDto,
        HierarchyRequestDto,
        HierarchyResponseDto> {

      public DepartmentEmployeeAssignmentController(DepartmentEmployeeAssignmentService assignmentService) {
         super(assignmentService);
      }
}
   ```

#### Creating an Assignment

**Request**

```sh
POST /api/v1/department-employee-assignments/assign?hierarchicalEntityId=1&assignableEntityId=1
Content-Type: application/json
```

**Body**

```json
[
      {
         "key": "role",
         "value": "Developer"
      }
]
```

**Response**

```json
{
      "id": 1,
      "hierarchicalEntity": {
            "id": 1,
            "name": "Department",
            "parentEntityId": null,
            "subEntitiesCount": 0,
            "subEntities": []
      },
      "assignableEntity": {
            "id": 1,
            "name": "John Doe",
            "firstName": "John",
            "lastName": "Doe",
            "email": "john.doe@example.com",
            "dateOfBirth": "1990-01-01",
            "position": "Developer"
      },
      "metadata": [
            {
               "id": 1,
               "key": "role",
               "value": "Developer"
            }
      ]
}
```

## Conclusion

By utilizing the Hierarchical Entity API, Assignable Entity API, and the Generic Assignment API, you can effectively manage complex hierarchical relationships and data structures in your applications.

## Contributing

Contributions are welcome! Please create a pull request or open an issue to discuss your ideas.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For questions or feedback, please contact [sahraoui.rami.1@gmail.com](mailto:sahraoui.rami.1@gmail.com).