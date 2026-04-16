package com.ociproject.repository;

import com.ociproject.model.TaskSprintHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskSprintHistoryRepository extends JpaRepository<TaskSprintHistory, Long> {
    List<TaskSprintHistory> findByTaskTaskIdOrderByChangedAtAsc(Long taskId);
}
