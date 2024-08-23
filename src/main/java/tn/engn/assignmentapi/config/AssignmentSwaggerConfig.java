package tn.engn.assignmentapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssignmentSwaggerConfig {

    @Bean
    @Qualifier("assignmentOpenAPI")
    public OpenAPI assignmentOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Assignment API")
                                .version("1.0")
                                .description("Endpoints for assigning entities with hierarchical entities.")
                );
    }

    @Bean
    public GroupedOpenApi assignmentApi() {
        return GroupedOpenApi.builder()
                .group("assignment")
                .packagesToScan("tn.engn.assignmentapi")
                .build();
    }
}
