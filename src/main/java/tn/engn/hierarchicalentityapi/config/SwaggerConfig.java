package tn.engn.hierarchicalentityapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Hierarchy API")
                                .version("1.0")
                                .description("Endpoints for managing hierarchical entities with a dynamic sub-entities path.")
                );
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("hierarchy")
                .packagesToScan("tn.engn.hierarchicalentityapi")
                .build();
    }
}

