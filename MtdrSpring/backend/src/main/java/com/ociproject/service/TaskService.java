package com.ociproject.service;

import com.ociproject.model.*;
import com.ociproject.repository.*;
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
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusHistoryRepository statusHistoryRepository;
    private final TaskSprintHistoryRepository sprintHistoryRepository;

    public List<Task> findAll() {
        return taskRepository.findAllByDeletedFalse();
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> findByProject(Long projectId) {
        return taskRepository.findByProjectProjectIdAndDeletedFalse(projectId);
    }

    public List<Task> findBySprint(Long sprintId) {
        return taskRepository.findBySprintSprintIdAndDeletedFalse(sprintId);
    }

    public List<Task> findByAssignee(Long userId) {
        return taskRepository.findByAssignedToUserIdAndDeletedFalse(userId);
    }

    public List<Task> findByProjectAndStage(Long projectId, Task.Stage stage) {
        return taskRepository.findByProjectProjectIdAndTaskStageAndDeletedFalse(projectId, stage);
    }

    public List<Task> findByProjectAndStatus(Long projectId, Task.Status status) {
        return taskRepository.findByProjectProjectIdAndStatusAndDeletedFalse(projectId, status);
    }

    public List<TaskStatusHistory> findStatusHistory(Long taskId) {
        return statusHistoryRepository.findByTaskTaskIdOrderByChangedAtAsc(taskId);
    }

    public List<TaskSprintHistory> findSprintHistory(Long taskId) {
        return sprintHistoryRepository.findByTaskTaskIdOrderByChangedAtAsc(taskId);
    }

    @Transactional
    public Task save(Task task) {
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateStatus(Long taskId, Task.Status newStatus, User changedBy) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));

        statusHistoryRepository.save(TaskStatusHistory.builder()
                .task(task)
                .oldStatus(task.getStatus() != null ? task.getStatus().name() : null)
                .newStatus(newStatus.name())
                .changedBy(changedBy)
                .build());

        task.setStatus(newStatus);
        if (newStatus == Task.Status.DONE || newStatus == Task.Status.CANCELLED) {
            task.setTaskStage(Task.Stage.COMPLETED);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task assignToSprint(Long taskId, Sprint sprint, User changedBy) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));

        sprintHistoryRepository.save(TaskSprintHistory.builder()
                .task(task)
                .oldSprint(task.getSprint())
                .newSprint(sprint)
                .changedBy(changedBy)
                .build());

        task.setSprint(sprint);
        task.setTaskStage(Task.Stage.SPRINT);
        return taskRepository.save(task);
    }

    @Transactional
    public void softDelete(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + id));
        task.setDeleted(true);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }
}
