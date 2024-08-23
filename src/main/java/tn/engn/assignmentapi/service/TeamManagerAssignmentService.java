package tn.engn.assignmentapi.service;

import org.springframework.stereotype.Service;
import tn.engn.assignmentapi.exception.AssignmentAlreadyExistsException;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.model.TeamManagerAssignment;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Team;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service for managing assignments between teams and managers.
 */
@Service
public class TeamManagerAssignmentService extends BaseAssignmentService<TeamManagerAssignment, Team, Employee, EmployeeRequestDto, EmployeeResponseDto, HierarchyRequestDto, HierarchyResponseDto> {

    public TeamManagerAssignmentService(HierarchyBaseRepository<Team> hierarchicalEntityRepository,
                                        AssignableEntityRepository<Employee> assignableEntityRepository,
                                        AssignmentRepository<Team, Employee, TeamManagerAssignment> assignmentRepository,
                                        AssignmentMetadataRepository assignmentMetadataRepository,
                                        HierarchyMapper<Team, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper,
                                        AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper,
                                        AssignmentMapper<Team, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> assignmentMapper) {
        super(hierarchicalEntityRepository, assignableEntityRepository, assignmentRepository,
                assignmentMetadataRepository, hierarchyMapper, assignableEntityMapper, assignmentMapper);
    }

    /**
     * Adds an employee to a team, managing the bidirectional relationship.
     *
     * @param hierarchicalEntity the team entity
     * @param assignableEntity   the employee entity
     */
    @Override
    protected void addEntityToHierarchicalEntity(Team hierarchicalEntity, Employee assignableEntity) {
        // Check if the employee is already assigned to a team
        if (hierarchicalEntity.getManager() != null) {
            throw new AssignmentAlreadyExistsException("Team is already assigned to a manager.");
        }

        // Set the relationship from the team side and set the relationship from the manager side
        assignableEntity.addTeam(hierarchicalEntity);
    }

    /**
     * Removes an employee from a team, managing the bidirectional relationship.
     *
     * @param hierarchicalEntity the team entity
     * @param assignableEntity   the employee entity
     */
    @Override
    protected void removeEntityFromHierarchicalEntity(Team hierarchicalEntity, Employee assignableEntity) {
        // Remove the relationship from the team side and remove the relationship from the employee side
        assignableEntity.removeTeam(hierarchicalEntity);
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
    Class<Team> getHierarchicalEntityClass() {
        return Team.class;
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
    protected TeamManagerAssignment createAssignmentInstance() {
        return new TeamManagerAssignment();
    }
}
