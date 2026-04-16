package com.ociproject.repository;

import com.ociproject.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectProjectIdAndDeletedFalse(Long projectId);
    List<Task> findBySprintSprintIdAndDeletedFalse(Long sprintId);
    List<Task> findByAssignedToUserIdAndDeletedFalse(Long userId);
    List<Task> findByStatusAndDeletedFalse(Task.Status status);
    List<Task> findByTaskStageAndDeletedFalse(Task.Stage stage);
    List<Task> findByProjectProjectIdAndTaskStageAndDeletedFalse(Long projectId, Task.Stage stage);
    List<Task> findByProjectProjectIdAndStatusAndDeletedFalse(Long projectId, Task.Status status);
    List<Task> findAllByDeletedFalse();
}
