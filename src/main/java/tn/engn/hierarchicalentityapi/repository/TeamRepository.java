package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.Team;

@Repository
public interface TeamRepository extends HierarchyBaseRepository<Team> {}
