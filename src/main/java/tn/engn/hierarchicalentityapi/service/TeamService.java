package tn.engn.hierarchicalentityapi.service;

import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Team;

public interface TeamService extends HierarchyService<Team, HierarchyRequestDto, HierarchyResponseDto> {}
