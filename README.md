# Department API

## Overview

The Department API is a Spring Boot application designed to manage department data. It supports CRUD operations and is structured to handle department hierarchies using an adjacency list model. The API includes integration tests using Testcontainers for database management and follows best practices for exception handling.

## Features

- CRUD operations for departments
- Hierarchical department structure management
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
   git clone https://github.com/your-username/department-api.git
   cd department-api
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
   spring.datasource.url=jdbc:mysql://localhost:3306/department_db?createDatabaseIfNotExist=TRUE&useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=yourpassword
   ```
   For integration tests, the Testcontainers setup will handle the database configuration automatically.

####  Profiles
Profiles are used to separate configuration for different environments. For example:

- application.properties for default configuration
- application-test-container.properties for `Testcontainer` specific configuration
- application-test-real-db.properties for real database test specific configuration

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
### Create a Department
#### Request

   ```sh
POST /departments
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
      "parentDepartmentId": null,
      "subDepartments": []
   }
   ```
### Get All Departments
#### Request

   ```sh
GET /departments
   ```
#### Response

   ```json
   [
      {
         "id": 1,
         "name": "Engineering",
         "parentDepartmentId": null,
         "subDepartments": []
      }
   ]
   ```
### Contributing
Contributions are welcome! Please create a pull request or open an issue to discuss your ideas.

### License
This project is licensed under the MIT License - see the LICENSE file for details.

### Contact
For questions or feedback, please contact [sahraoui.rami.1@gmail.com](mailto:sahraoui.rami.1@gmail.com).
