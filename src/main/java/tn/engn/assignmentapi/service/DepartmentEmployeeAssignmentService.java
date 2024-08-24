package tn.engn.assignmentapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.DepartmentEmployeeAssignment;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service for managing assignments between departments and employees.
 * Extends {@link BaseAssignmentService} to provide specific functionality for handling
 * Department and Employee entities.
 */
@Service
@Slf4j
public class DepartmentEmployeeAssignmentService extends BaseAssignmentService<DepartmentEmployeeAssignment, Department, Employee, EmployeeRequestDto, EmployeeResponseDto, HierarchyRequestDto, HierarchyResponseDto> {

    public DepartmentEmployeeAssignmentService(
            HierarchyBaseRepository<Department> hierarchicalEntityRepository,
            AssignableEntityRepository<Employee> assignableEntityRepository,
            AssignmentRepository<Department, Employee, DepartmentEmployeeAssignment> assignmentRepository,
            AssignmentMetadataRepository assignmentMetadataRepository,
            HierarchyMapper<Department, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper,
            AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper,
            AssignmentMapper<Department, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper
    ) {
        super(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository,
                assignmentMetadataRepository, hierarchyMapper, assignableEntityMapper, assignmentMapper);
    }

    /**
     * Adds an employee to a department.
     * Updates the bidirectional relationship between the Department and Employee entities.
     *
     * @param department the Department entity to which the Employee will be added
     * @param employee   the Employee entity to be added to the Department
     */
    @Override
    protected void addEntityToHierarchicalEntity(Department department, Employee employee) {
        department.addEmployee(employee);
    }

    /**
     * Removes an employee from a department.
     * Updates the bidirectional relationship between the Department and Employee entities.
     *
     * @param department the Department entity from which the Employee will be removed
     * @param employee   the Employee entity to be removed from the Department
     */
    @Override
    protected void removeEntityFromHierarchicalEntity(Department department, Employee employee) {
        department.removeEmployee(employee);
    }

    /**
     * Returns the class type of the hierarchical entity associated with this repository.
     * <p>
     * This method is abstract and should be implemented by subclasses to specify the
     * actual type of the hierarchical entity managed by the repository. It is used
     * primarily to assist in constructing type-safe queries and for other type-specific
     * operations within the repository.
     * </p>
     *
     * @return the {@link Class} object representing the type of the hierarchical entity
     */
    @Override
    Class<Department> getHierarchicalEntityClass() {
        return Department.class;
    }

    /**
     * Returns the class type of the assignable entity associated with this repository.
     * <p>
     * This method is abstract and should be implemented by subclasses to specify the
     * actual type of the assignable entity managed by the repository. It is used
     * primarily to assist in constructing type-safe queries and for other type-specific
     * operations within the repository.
     * </p>
     *
     * @return the {@link Class} object representing the type of the assignable entity
     */
    @Override
    Class<Employee> getAssignableEntityClass() {
        return Employee.class;
    }

    /**
     * Creates a new instance of the assignment.
     *
     * @return a new assignment instance
     */
    @Override
    protected DepartmentEmployeeAssignment createAssignmentInstance() {
        return new DepartmentEmployeeAssignment();
    }
}
