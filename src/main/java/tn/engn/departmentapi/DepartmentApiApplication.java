package tn.engn.departmentapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@ComponentScan(basePackages = {"tn.engn.departmentapi", "tn.engn.departmentapi.mapper", "tn.engn.departmentapi.controlleradvice"})
public class DepartmentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepartmentApiApplication.class, args);
    }

}
