package tn.engn.departmentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.engn.departmentapi.model.Department;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // Any custom query methods can be added here if needed
}
