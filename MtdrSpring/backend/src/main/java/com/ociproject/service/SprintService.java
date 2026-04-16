package com.ociproject.service;

import com.ociproject.model.Sprint;
import com.ociproject.repository.SprintRepository;
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
public class SprintService {

    private final SprintRepository sprintRepository;

    public List<Sprint> findAll() {
        return sprintRepository.findAllByDeletedFalse();
    }

    public Optional<Sprint> findById(Long id) {
        return sprintRepository.findById(id);
    }

    public List<Sprint> findByStatus(Sprint.Status status) {
        return sprintRepository.findByStatusAndDeletedFalse(status);
    }

    @Transactional
    public Sprint save(Sprint sprint) {
        return sprintRepository.save(sprint);
    }

    @Transactional
    public Sprint updateStatus(Long id, Sprint.Status newStatus) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found: " + id));
        sprint.setStatus(newStatus);
        return sprintRepository.save(sprint);
    }

    @Transactional
    public void softDelete(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint not found: " + id));
        sprint.setDeleted(true);
        sprint.setDeletedAt(LocalDateTime.now());
        sprintRepository.save(sprint);
    }
}
