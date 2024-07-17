# Hierarchical Entity API

## Overview

The Hierarchical Entity API is a Spring Boot application designed to manage hierarchical data structures for various entities. It supports CRUD operations and is designed to handle hierarchical relationships using different models such as Adjacency List, Nested Set, Parameterized Path, and Closure Table. The API includes integration tests using Testcontainers for database management and follows best practices for exception handling.

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
   git https://github.com/rami-sahraoui/hierarchical-entity-api.git
   cd hierarchical-entity-api
   ```
2. Build the project:
   ```sh
   mvn clean install
   ```
###   Configuration
####  Application Properties
The application can be configured using `application.properties` or `application.yml` files located in the `src/main/resources` directory.

For local development and real database, configure the database connection:

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/hierarchical_entity_db?createDatabaseIfNotExist=TRUE&useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=yourpassword
   ```
   For integration tests, the Testcontainers setup will handle the database configuration automatically.

####  Profiles
Profiles are used to separate configuration for different environments. For example:

- `application.properties` for default configuration
- `application-test-container.properties` for `Testcontainer` specific configuration
- `application-test-real-db.properties` for real database test specific configuration

Activate a profile using:

   ```sh
   -Dspring.profiles.active=test
   ```   
###   Running the Application
To run the application locally:

   ```sh
./mvnw spring-boot:run
   ```
###   Running Tests
To run all tests including integration tests:

   ```sh
./mvnw verify
   ```
## API Documentation
The API documentation is available at `/swagger-ui.html` when the application is running. It provides an interactive interface to test the API endpoints.

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

   import jakarta.persistence.*;
   import lombok.*;
   import lombok.experimental.SuperBuilder;
   
   import java.util.ArrayList;
   import java.util.List;
   
   @Getter
   @Setter
   @NoArgsConstructor
   @AllArgsConstructor
   @SuperBuilder
   @Entity
   @DiscriminatorValue("Department")
   @EqualsAndHashCode(callSuper = true)
   public class Department extends HierarchyBaseEntity<Department> {
   
      @ManyToOne(fetch = FetchType.LAZY)
      @JoinColumn(name = "parent_entity_id")
      private Department parentEntity;
   
      @Builder.Default
      @OneToMany(mappedBy = "parentEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
      private List<Department> subEntities = new ArrayList<>();
   
      @Override
      public void addSubEntity(Department subEntity) {
         subEntities.add(subEntity);
         subEntity.setParentEntity(this);
      }
   
      @Override
      public void removeSubEntity(Department subEntity) {
         subEntities.remove(subEntity);
         subEntity.setParentEntity(null);
      }
   }
   ```
#### Department Repository
   ```java
   package tn.engn.hierarchicalentityapi.repository;

   import org.springframework.stereotype.Repository;
   import tn.engn.hierarchicalentityapi.model.Department;
   
   @Repository
   public interface DepartmentRepository extends HierarchyBaseRepository<Department> {}
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
The `@SubEntitiesPath` annotation dynamically sets the path for sub-entities. This allows flexibility in managing different hierarchical structures and ensures that sub-entity paths are correctly resolved.
### Contributing
Contributions are welcome! Please create a pull request or open an issue to discuss your ideas.

### License
This project is licensed under the MIT License - see the LICENSE file for details.

### Contact
For questions or feedback, please contact [sahraoui.rami.1@gmail.com](mailto:sahraoui.rami.1@gmail.com).
