package tn.engn.departmentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.engn.departmentapi.model.Department;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, QuerydslPredicateExecutor<Department> {
    @Query("SELECT d FROM Department d WHERE d.leftIndex >= :leftIndex AND d.rightIndex <= :rightIndex ORDER BY d.leftIndex ASC")
    List<Department> findSubDepartments(@Param("leftIndex") int leftIndex, @Param("rightIndex") int rightIndex);

   List<Department> findAllByOrderByLeftIndexAsc();
}
