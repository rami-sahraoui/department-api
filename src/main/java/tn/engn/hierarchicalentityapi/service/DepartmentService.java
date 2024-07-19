package tn.engn.hierarchicalentityapi.service;

import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Department;

public interface DepartmentService extends HierarchyService<Department, HierarchyRequestDto, HierarchyResponseDto> {}
