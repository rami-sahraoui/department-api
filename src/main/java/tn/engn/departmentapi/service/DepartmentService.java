package tn.engn.departmentapi.service;

import tn.engn.departmentapi.model.Department;

import java.util.List;

public interface DepartmentService {

    /**
     * Creates a new department with the given name and optional parent ID.
     *
     * @param name     the name of the new department
     * @param parentId the ID of the parent department (optional)
     * @return the created department
     */
    Department createDepartment(String name, Long parentId);

    /**
     * Updates the name and parent of an existing department.
     *
     * @param id       the ID of the department to update
     * @param name     the new name of the department
     * @param parentId the new parent ID of the department
     * @return the updated department
     */
    Department updateDepartment(Long id, String name, Long parentId);

    /**
     * Deletes a department by its ID.
     *
     * @param id the ID of the department to delete
     */
    void deleteDepartment(Long id);

    /**
     * Retrieves all departments.
     *
     * @return a list of all departments
     */
    List<Department> getAllDepartments();

    /**
     * Retrieves sub-departments (children) of a given parent department.
     *
     * @param parentId the ID of the parent department
     * @return a list of sub-departments
     */
    List<Department> getSubDepartments(Long parentId);

    /**
     * Retrieves a department by its ID.
     *
     * @param id the ID of the department to retrieve
     * @return the department with the specified ID
     */
    Department getDepartmentById(Long id);

    /**
     * Retrieves the parent department of a given department.
     *
     * @param departmentId the ID of the department
     * @return the parent department of the specified department
     */
    Department getParentDepartment(Long departmentId);

    /**
     * Retrieves all descendants (children, grandchildren, etc.) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all descendants of the specified department
     */
    List<Department> getDescendants(Long departmentId);

    /**
     * Retrieves all ancestors (parent, grandparent, etc.) of a given department.
     *
     * @param departmentId the ID of the department
     * @return a list of all ancestors of the specified department
     */
    List<Department> getAncestors(Long departmentId);
}
