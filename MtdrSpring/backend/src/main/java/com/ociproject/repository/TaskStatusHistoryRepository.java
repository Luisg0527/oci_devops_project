package com.ociproject.repository;

import com.ociproject.model.TaskStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, Long> {
    List<TaskStatusHistory> findByTaskTaskIdOrderByChangedAtAsc(Long taskId);
}
