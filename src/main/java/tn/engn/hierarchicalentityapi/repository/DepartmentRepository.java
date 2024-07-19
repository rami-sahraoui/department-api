package tn.engn.hierarchicalentityapi.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.Department;

import java.util.List;

@Repository
public interface DepartmentRepository extends HierarchyBaseRepository<Department> {
    @Query("SELECT d FROM Department d WHERE d.leftIndex >= :leftIndex AND d.rightIndex <= :rightIndex ORDER BY d.leftIndex ASC")
    List<Department> findSubDepartments(@Param("leftIndex") int leftIndex, @Param("rightIndex") int rightIndex);

   List<Department> findAllByOrderByLeftIndexAsc();
}
