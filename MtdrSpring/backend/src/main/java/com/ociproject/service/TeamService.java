package com.ociproject.service;

import com.ociproject.model.Team;
import com.ociproject.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;

    public List<Team> findAll() {
        return teamRepository.findAllByDeletedFalse();
    }

    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }

    @Transactional
    public Team save(Team team) {
        return teamRepository.save(team);
    }

    @Transactional
    public void softDelete(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team not found: " + id));
        team.setDeleted(true);
        team.setDeletedAt(LocalDateTime.now());
        teamRepository.save(team);
    }
}
