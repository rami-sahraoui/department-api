package tn.engn.assignmentapi.service;

import org.springframework.stereotype.Service;
import tn.engn.assignmentapi.exception.AssignmentAlreadyExistsException;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.JobEmployeeAssignment;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Job;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service for managing assignments between jobs and employees.
 */
@Service
public class JobEmployeeAssignmentService extends BaseAssignmentService<JobEmployeeAssignment, Job, Employee, EmployeeRequestDto, EmployeeResponseDto, HierarchyRequestDto, HierarchyResponseDto> {

    public JobEmployeeAssignmentService(HierarchyBaseRepository<Job> hierarchicalEntityRepository,
                                        AssignableEntityRepository<Employee> assignableEntityRepository,
                                        AssignmentRepository<Job, Employee, JobEmployeeAssignment> assignmentRepository,
                                        AssignmentMetadataRepository assignmentMetadataRepository,
                                        HierarchyMapper<Job, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper,
                                        AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper,
                                        AssignmentMapper<Job, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper) {
        super(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository,
                assignmentMetadataRepository, hierarchyMapper, assignableEntityMapper, assignmentMapper);
    }

    /**
     * Adds an employee to a job, managing the bidirectional relationship.
     *
     * @param hierarchicalEntity the job entity
     * @param assignableEntity   the employee entity
     */
    @Override
    protected void addEntityToHierarchicalEntity(Job hierarchicalEntity, Employee assignableEntity) {
        // Check if the employee is already assigned to a job
        if (assignableEntity.getJob() != null) {
            throw new AssignmentAlreadyExistsException("Employee is already assigned to a job.");
        }

        // Set the relationship from the job side and set the relationship from the employee side
        hierarchicalEntity.addEmployee(assignableEntity);
    }

    /**
     * Removes an employee from a job, managing the bidirectional relationship.
     *
     * @param hierarchicalEntity the job entity
     * @param assignableEntity   the employee entity
     */
    @Override
    protected void removeEntityFromHierarchicalEntity(Job hierarchicalEntity, Employee assignableEntity) {

        // Remove the relationship from the job side and remove the relationship from the employee side
        hierarchicalEntity.removeEmployee(assignableEntity);
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
    Class<Job> getHierarchicalEntityClass() {
        return Job.class;
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
    protected JobEmployeeAssignment createAssignmentInstance() {
        return new JobEmployeeAssignment();
    }
}
