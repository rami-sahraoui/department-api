package tn.engn.employeeapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeSwaggerConfig {

    @Bean
    @Qualifier("employeeOpenAPI")
    public OpenAPI employeeOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Employee API")
                                .version("1.0")
                                .description("Endpoints for managing Employees.")
                );
    }

    @Bean
    public GroupedOpenApi employeeApi() {
        return GroupedOpenApi.builder()
                .group("employee")
                .packagesToScan("tn.engn.employeeapi")
                .build();
    }
}
