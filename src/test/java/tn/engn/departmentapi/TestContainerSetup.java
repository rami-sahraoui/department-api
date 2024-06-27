package tn.engn.departmentapi;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for setting up Testcontainers and test profiles.
 * Extend this class in your integration tests to initialize database containers
 * and manage test profiles consistently across tests.
 */
@Testcontainers
@ActiveProfiles("test-container")
public class TestContainerSetup {

    // MySQL container instance for integration tests
    public static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Static initialization to start the MySQL container before all tests.
     * Sets up the database URL, username, and password as system properties for Spring.
     */
    @BeforeAll
    public static void setUp() {
        // Start the MySQL container
        mysqlContainer.start();

        // Set system properties for Spring to use the MySQL container
        System.setProperty("spring.datasource.url", mysqlContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", mysqlContainer.getUsername());
        System.setProperty("spring.datasource.password", mysqlContainer.getPassword());
    }

    /**
     * Static cleanup method to stop the MySQL container after all tests.
     */
    @AfterAll
    public static void tearDown() {
        // Stop the MySQL container
        mysqlContainer.stop();
    }
}