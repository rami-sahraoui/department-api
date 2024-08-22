package tn.engn.hierarchicalentityapi.service;

import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Project;

public interface ProjectService extends HierarchyService<Project, HierarchyRequestDto, HierarchyResponseDto> {}
