package tn.engn.hierarchicalentityapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class HierarchicalEntitySwaggerConfig {

    @Bean
    @Primary
    @Qualifier("hierarchyOpenAPI")
    public OpenAPI hierarchyOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Hierarchy API")
                                .version("1.0")
                                .description("Endpoints for managing hierarchical entities with a dynamic sub-entities path.")
                );
    }

    @Bean
    public GroupedOpenApi hierarchyApi() {
        return GroupedOpenApi.builder()
                .group("hierarchy")
                .packagesToScan("tn.engn.hierarchicalentityapi")
                .build();
    }
}
