package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.*;
import com.ociproject.repository.TaskRepository;
import com.ociproject.repository.TaskSprintHistoryRepository;
import com.ociproject.repository.TaskStatusHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock TaskStatusHistoryRepository statusHistoryRepository;
    @Mock TaskSprintHistoryRepository sprintHistoryRepository;

    @InjectMocks
    TaskService service;

    private Task pendingTask() {
        return Task.builder()
                .taskId(1L)
                .title("Implement login")
                .taskStage(Task.Stage.BACKLOG)
                .status(Task.Status.PENDING)
                .deleted(false)
                .build();
    }

    private User actor() {
        return TestFixtures.user5();
    }

    @Test
    void findById_found_returnsTask() {
        Task t = pendingTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThat(service.findById(1L)).isPresent().contains(t);
    }

    @Test
    void findByProject_returnsList() {
        Task t = pendingTask();
        when(taskRepository.findByProjectProjectIdAndDeletedFalse(1L)).thenReturn(List.of(t));

        assertThat(service.findByProject(1L)).containsExactly(t);
    }

    @Test
    void findBySprint_returnsList() {
        Task t = pendingTask();
        when(taskRepository.findBySprintSprintIdAndDeletedFalse(2L)).thenReturn(List.of(t));

        assertThat(service.findBySprint(2L)).containsExactly(t);
    }

    @Test
    void save_persistsTask() {
        Task t = pendingTask();
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(t)).isSameAs(t);
    }

    @Test
    void updateStatus_savesHistoryRecordWithCorrectOldAndNewStatus() {
        Task t = pendingTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(1L, Task.Status.IN_PROGRESS, actor());

        verify(statusHistoryRepository).save(argThat(h ->
                "PENDING".equals(h.getOldStatus())
                && "IN_PROGRESS".equals(h.getNewStatus())
                && h.getChangedBy().getUserId().equals(5L)
        ));
    }

    @Test
    void updateStatus_changesStatusOnTask() {
        Task t = pendingTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(1L, Task.Status.IN_PROGRESS, actor());

        verify(taskRepository).save(argThat(task -> task.getStatus() == Task.Status.IN_PROGRESS));
    }

    @Test
    void updateStatus_doneSetsStageToCompleted() {
        Task t = pendingTask();
        t.setTaskStage(Task.Stage.SPRINT);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(1L, Task.Status.DONE, actor());

        verify(taskRepository).save(argThat(task -> task.getTaskStage() == Task.Stage.COMPLETED));
    }

    @Test
    void updateStatus_cancelledSetsStageToCompleted() {
        Task t = pendingTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatus(1L, Task.Status.CANCELLED, actor());

        verify(taskRepository).save(argThat(task -> task.getTaskStage() == Task.Stage.COMPLETED));
    }

    @Test
    void updateStatus_notFound_throwsEntityNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.updateStatus(99L, Task.Status.IN_PROGRESS, actor()));
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void assignToSprint_savesSprintHistoryRecord() {
        Task t = pendingTask();
        Sprint newSprint = TestFixtures.sprint2();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sprintHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignToSprint(1L, newSprint, actor());

        verify(sprintHistoryRepository).save(argThat(h ->
                h.getOldSprint() == null    // was BACKLOG, no previous sprint
                && h.getNewSprint().getSprintId().equals(2L)
                && h.getChangedBy().getUserId().equals(5L)
        ));
    }

    @Test
    void assignToSprint_setsSprintAndStageOnTask() {
        Task t = pendingTask();
        Sprint newSprint = TestFixtures.sprint2();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sprintHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assignToSprint(1L, newSprint, actor());

        verify(taskRepository).save(argThat(task ->
                task.getSprint().getSprintId().equals(2L)
                && task.getTaskStage() == Task.Stage.SPRINT
        ));
    }

    @Test
    void assignToSprint_notFound_throwsEntityNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());
        Sprint sprint = TestFixtures.sprint2();

        assertThrows(EntityNotFoundException.class,
                () -> service.assignToSprint(99L, sprint, actor()));
        verify(sprintHistoryRepository, never()).save(any());
    }

    @Test
    void softDelete_setsDeletedFlagAndTimestamp() {
        Task t = pendingTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L);

        verify(taskRepository).save(argThat(task ->
                Boolean.TRUE.equals(task.getDeleted()) && task.getDeletedAt() != null
        ));
    }

    @Test
    void softDelete_notFound_throwsEntityNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.softDelete(99L));
    }
}
