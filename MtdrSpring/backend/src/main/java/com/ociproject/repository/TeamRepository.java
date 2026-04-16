package com.ociproject.repository;

import com.ociproject.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findAllByDeletedFalse();
}
