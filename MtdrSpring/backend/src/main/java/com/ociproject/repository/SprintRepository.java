package com.ociproject.repository;

import com.ociproject.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findAllByDeletedFalse();
    List<Sprint> findByStatusAndDeletedFalse(Sprint.Status status);
}
