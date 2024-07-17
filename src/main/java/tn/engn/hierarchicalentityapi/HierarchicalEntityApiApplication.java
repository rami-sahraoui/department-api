package tn.engn.hierarchicalentityapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@ComponentScan(basePackages = {"tn.engn.hierarchicalentityapi", "tn.engn.hierarchicalentityapi.mapper", "tn.engn.hierarchicalentityapi.controlleradvice"})
public class HierarchicalEntityApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HierarchicalEntityApiApplication.class, args);
    }

}
