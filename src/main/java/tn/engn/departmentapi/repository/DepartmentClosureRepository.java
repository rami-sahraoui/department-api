package tn.engn.departmentapi.repository;

import tn.engn.departmentapi.model.DepartmentClosure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DepartmentClosure entities.
 * Provides CRUD operations and custom query methods for the DepartmentClosure entity.
 */
public interface DepartmentClosureRepository extends JpaRepository<DepartmentClosure, Long> {

    /**
     * Finds all closure table entries where the specified department is the ancestor.
     *
     * @param ancestorId the ID of the ancestor department
     * @return a list of closure table entries where the specified department is the ancestor
     */
    List<DepartmentClosure> findByAncestorId(Long ancestorId);

    /**
     * Finds all closure table entries where the specified department is the ancestor
     * and order them by level on descendant order.
     *
     * @param ancestorId the ID of the ancestor department
     * @return a list of closure table entries where the specified department is the ancestor
     */
    List<DepartmentClosure> findByAncestorIdOrderByLevelDesc(Long ancestorId);

    /**
     * Finds all closure table entries where the specified department is the descendant.
     *
     * @param descendantId the ID of the descendant department
     * @return a list of closure table entries where the specified department is the descendant
     */
    List<DepartmentClosure> findByDescendantId(Long descendantId);

    /**
     * Deletes all closure table entries where the specified department is the descendant.
     *
     * @param descendantId the ID of the descendant department
     */
    void deleteByDescendantId(Long descendantId);

    /**
     * Deletes all closure table entries where the specified department is the ancestor.
     *
     * @param ancestorId the ID of the ancestor department
     */
    void deleteByAncestorId(Long ancestorId);

}
