package tn.engn.assignmentapi.mapper;

import org.springframework.stereotype.Component;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.assignmentapi.model.TeamManagerAssignment;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Team;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

@Component
public class TeamManagerAssignmentMapper extends AssignmentMapperImpl<Team, Employee, HierarchyRequestDto, HierarchyResponseDto, EmployeeRequestDto, EmployeeResponseDto> {
    /**
     * Constructor for {@link AssignmentMapper}.
     * Initializes the mappers required for converting hierarchical and assignable entities.
     *
     * @param hierarchyMapper            the mapper for hierarchical entities
     * @param assignableEntityMapper     the mapper for assignable entities
     * @param hierarchyEntityRepository  the repository for hierarchical entities
     * @param assignableEntityRepository the repository for assignable entities
     */
    public TeamManagerAssignmentMapper(HierarchyMapper<Team, HierarchyRequestDto, HierarchyResponseDto> hierarchyMapper, AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> assignableEntityMapper, HierarchyBaseRepository<Team> hierarchyEntityRepository, AssignableEntityRepository<Employee> assignableEntityRepository) {
        super(hierarchyMapper, assignableEntityMapper, hierarchyEntityRepository, assignableEntityRepository);
    }

    /**
     * Abstract method to create a new instance of a specific {@link Assignment} subclass.
     * Concrete subclasses must implement this method to instantiate the correct {@link Assignment} type.
     *
     * @return a new instance of a concrete {@link Assignment} subclass
     */
    @Override
    protected Assignment<Team, Employee> createAssignmentInstance() {
        return new TeamManagerAssignment();
    }
}
