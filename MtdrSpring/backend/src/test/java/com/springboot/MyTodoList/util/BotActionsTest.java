package com.springboot.MyTodoList.util;

import com.ociproject.model.*;
import com.ociproject.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotActionsTest {

    static final long CHAT_ID = 42L;
    static final String TELEGRAM_ID = String.valueOf(CHAT_ID);

    @Mock TelegramClient telegramClient;
    @Mock TaskService taskService;
    @Mock UserService userService;
    @Mock SprintService sprintService;
    @Mock ProjectService projectService;

    BotActions actions;

    @BeforeEach
    void setUp() {
        actions = new BotActions(telegramClient, taskService, userService, sprintService, projectService);
        actions.setChatId(CHAT_ID);
    }

    /**
     * Returns the text of the last SendMessage sent via the mocked TelegramClient.
     * Uses mockingDetails so no ArgumentCaptor generic-type gymnastics are needed.
     */
    private String lastSentText() {
        return Mockito.mockingDetails(telegramClient).getInvocations().stream()
                .filter(inv -> "execute".equals(inv.getMethod().getName()))
                .reduce((first, second) -> second)
                .map(inv -> ((SendMessage) inv.getArgument(0)).getText())
                .orElseThrow(() -> new AssertionError("telegramClient.execute() was never called"));
    }

    // ── Fixture helpers ───────────────────────────────────────────────────────

    private static User registeredUser() {
        return User.builder().userId(99L).telegramId(TELEGRAM_ID).build();
    }

    private static Project sampleProject() {
        return Project.builder().projectId(1L).name("Alpha").deleted(false).build();
    }

    // ── /help ─────────────────────────────────────────────────────────────────

    @Test
    void fnHelp_sendsHelpText() {
        actions.setRequestText(BotCommands.HELP.getCommand());
        actions.fnHelp();
        assertThat(lastSentText()).isEqualTo(BotMessages.HELP_TEXT.getMessage());
    }

    // ── /hide ─────────────────────────────────────────────────────────────────

    @Test
    void fnHide_sendsByeMessage() {
        actions.setRequestText(BotCommands.HIDE_COMMAND.getCommand());
        actions.fnHide();
        assertThat(lastSentText()).isEqualTo(BotMessages.BYE.getMessage());
    }

    // ── /start ────────────────────────────────────────────────────────────────

    @Test
    void fnStart_sendsWelcomeContainingChatId() {
        actions.setRequestText(BotCommands.START_COMMAND.getCommand());
        actions.fnStart();
        assertThat(lastSentText())
                .contains("Hello")
                .contains(String.valueOf(CHAT_ID));
    }

    // ── Add New Item button ───────────────────────────────────────────────────

    @Test
    void fnAddItem_sendsAddTaskInstructions() {
        actions.setRequestText(BotLabels.ADD_NEW_ITEM.getLabel());
        actions.fnAddItem();
        assertThat(lastSentText()).isEqualTo(BotMessages.TYPE_NEW_TODO_ITEM.getMessage());
    }

    // ── /llm ─────────────────────────────────────────────────────────────────

    @Test
    void fnLLM_sendsNotAvailableMessage() {
        actions.setRequestText(BotCommands.LLM_REQ.getCommand());
        actions.fnLLM();
        assertThat(lastSentText()).contains("LLM integration not available");
    }

    // ── catch-all ─────────────────────────────────────────────────────────────

    @Test
    void fnElse_unknownCommand_sendsHelpText() {
        actions.setRequestText("¿qué haces?");
        actions.fnElse();
        assertThat(lastSentText()).isEqualTo(BotMessages.HELP_TEXT.getMessage());
    }

    // ── /todolist ─────────────────────────────────────────────────────────────

    @Test
    void fnListAll_callsTaskServiceAndSendsReply() {
        Task pending = Task.builder().taskId(1L).title("Write tests").status(Task.Status.PENDING).build();
        Task done    = Task.builder().taskId(2L).title("Fix bug").status(Task.Status.DONE).build();
        when(taskService.findAll()).thenReturn(List.of(pending, done));

        actions.setRequestText(BotCommands.TODO_LIST.getCommand());
        actions.fnListAll();

        verify(taskService).findAll();
        assertThat(lastSentText()).isEqualTo(BotLabels.MY_TODO_LIST.getLabel());
    }

    // ── /addtask — argument validation ────────────────────────────────────────

    @Test
    void fnAddTask_noArgs_sendsUsageMessage() {
        actions.setRequestText(BotCommands.ADD_TASK.getCommand());
        actions.fnAddTask();
        assertThat(lastSentText()).contains("Usage:");
    }

    @Test
    void fnAddTask_nonNumericProjectId_sendsInvalidFormatMessage() {
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(registeredUser()));
        actions.setRequestText("/addtask abc My task");
        actions.fnAddTask();
        assertThat(lastSentText()).contains(BotMessages.INVALID_FORMAT.getMessage());
    }

    // ── /addtask — service-level failures ────────────────────────────────────

    @Test
    void fnAddTask_userNotRegistered_sendsNotRegisteredMessage() {
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.empty());
        actions.setRequestText("/addtask 1 Fix login bug");
        actions.fnAddTask();
        assertThat(lastSentText()).isEqualTo(BotMessages.USER_NOT_REGISTERED.getMessage());
    }

    @Test
    void fnAddTask_projectNotFound_sendsProjectNotFoundMessage() {
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(registeredUser()));
        when(projectService.findById(1L)).thenReturn(Optional.empty());
        actions.setRequestText("/addtask 1 Fix login bug");
        actions.fnAddTask();
        assertThat(lastSentText()).isEqualTo(BotMessages.PROJECT_NOT_FOUND.getMessage());
    }

    // ── /addtask — happy path ─────────────────────────────────────────────────

    @Test
    void fnAddTask_valid_asksForEstimatedHours() {
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(registeredUser()));
        when(projectService.findById(1L)).thenReturn(Optional.of(sampleProject()));
        actions.setRequestText("/addtask 1 Fix login bug");
        actions.fnAddTask();
        assertThat(lastSentText()).isEqualTo(BotMessages.ASK_ADDTASK_HOURS.getMessage());
    }

    // ── /complete — argument validation ───────────────────────────────────────

    @Test
    void fnCompleteTask_noArg_sendsUsageMessage() {
        actions.setRequestText(BotCommands.COMPLETE_TASK.getCommand());
        actions.fnCompleteTask();
        assertThat(lastSentText()).contains("Usage:");
    }

    // ── /complete — service-level failures ───────────────────────────────────

    @Test
    void fnCompleteTask_userNotRegistered_sendsNotRegisteredMessage() {
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.empty());
        actions.setRequestText("/complete 5");
        actions.fnCompleteTask();
        assertThat(lastSentText()).isEqualTo(BotMessages.USER_NOT_REGISTERED.getMessage());
    }

    @Test
    void fnCompleteTask_taskNotFound_sendsTaskNotFoundMessage() {
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(registeredUser()));
        when(taskService.findById(5L)).thenReturn(Optional.empty());
        actions.setRequestText("/complete 5");
        actions.fnCompleteTask();
        assertThat(lastSentText()).isEqualTo(BotMessages.TASK_NOT_FOUND.getMessage());
    }

    @Test
    void fnCompleteTask_taskBelongsToAnotherUser_sendsNotYoursMessage() {
        User currentUser = User.builder().userId(99L).build();
        User otherUser   = User.builder().userId(77L).build();
        Task task = Task.builder().taskId(5L).status(Task.Status.PENDING).assignedTo(otherUser).build();
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(currentUser));
        when(taskService.findById(5L)).thenReturn(Optional.of(task));
        actions.setRequestText("/complete 5");
        actions.fnCompleteTask();
        assertThat(lastSentText()).contains("not the assignee");
    }

    // ── /complete — happy path ────────────────────────────────────────────────

    @Test
    void fnCompleteTask_validSubtask_asksForActualHours() {
        User currentUser = User.builder().userId(99L).build();
        Task task = Task.builder()
                .taskId(5L).status(Task.Status.PENDING)
                .assignedTo(currentUser).subtask(true).build();
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(currentUser));
        when(taskService.findById(5L)).thenReturn(Optional.of(task));
        actions.setRequestText("/complete 5");
        actions.fnCompleteTask();
        assertThat(lastSentText())
                .contains("Task #5 found")
                .contains("actual hours");
    }

    // ── /assignsprint — argument validation ──────────────────────────────────

    @Test
    void fnAssignSprint_missingSprintId_sendsUsageMessage() {
        actions.setRequestText("/assignsprint 1");
        actions.fnAssignSprint();
        assertThat(lastSentText()).contains("Usage:");
    }

    // ── /assignsprint — service-level failures ────────────────────────────────

    @Test
    void fnAssignSprint_userNotRegistered_sendsNotRegisteredMessage() {
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.empty());
        actions.setRequestText("/assignsprint 1 2");
        actions.fnAssignSprint();
        assertThat(lastSentText()).isEqualTo(BotMessages.USER_NOT_REGISTERED.getMessage());
    }

    @Test
    void fnAssignSprint_sprintNotFound_sendsSprintNotFoundMessage() {
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(registeredUser()));
        when(sprintService.findById(2L)).thenReturn(Optional.empty());
        actions.setRequestText("/assignsprint 1 2");
        actions.fnAssignSprint();
        assertThat(lastSentText()).isEqualTo(BotMessages.SPRINT_NOT_FOUND.getMessage());
    }

    // ── /assignsprint — happy path ────────────────────────────────────────────

    @Test
    void fnAssignSprint_valid_sendsAssignedConfirmation() {
        Sprint sprint = Sprint.builder().sprintId(2L).name("Sprint 2").build();
        when(userService.findByTelegramId(TELEGRAM_ID)).thenReturn(Optional.of(registeredUser()));
        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        actions.setRequestText("/assignsprint 1 2");
        actions.fnAssignSprint();
        assertThat(lastSentText())
                .contains("Task #1")
                .contains("sprint #2");
    }

    // ── Keyboard buttons ──────────────────────────────────────────────────────

    @Test
    void fnDone_callsUpdateStatusDoneAndSendsConfirmation() {
        actions.setRequestText("1-DONE");
        actions.fnDone();
        verify(taskService).updateStatus(eq(1L), eq(Task.Status.DONE), any());
        assertThat(lastSentText()).isEqualTo(BotMessages.ITEM_DONE.getMessage());
    }

    @Test
    void fnUndo_callsUpdateStatusPendingAndSendsConfirmation() {
        actions.setRequestText("1-UNDO");
        actions.fnUndo();
        verify(taskService).updateStatus(eq(1L), eq(Task.Status.PENDING), any());
        assertThat(lastSentText()).isEqualTo(BotMessages.ITEM_UNDONE.getMessage());
    }

    @Test
    void fnDelete_callsSoftDeleteAndSendsConfirmation() {
        actions.setRequestText("1-DELETE");
        actions.fnDelete();
        verify(taskService).softDelete(1L);
        assertThat(lastSentText()).isEqualTo(BotMessages.ITEM_DELETED.getMessage());
    }

    // ── Multi-step conversation: /addtask → hours reply ───────────────────────
    //
    // Uses a distinct chatId (200L) to avoid interference with other tests
    // that may have put chatId 42L into the shared PENDING map.

    @Test
    void pendingConversation_addTaskFlow_createsTaskAndSendsConfirmation() {
        final long PENDING_CHAT_ID = 200L;
        final String PENDING_TID   = String.valueOf(PENDING_CHAT_ID);

        Task saved = Task.builder()
                .taskId(10L).title("New feature")
                .estimatedHours(new BigDecimal("2.00")).build();

        when(userService.findByTelegramId(PENDING_TID)).thenReturn(Optional.of(registeredUser()));
        when(projectService.findById(1L)).thenReturn(Optional.of(sampleProject()));
        when(taskService.save(any(Task.class))).thenReturn(saved);

        // Step 1: /addtask — bot asks for estimated hours and stores state in PENDING
        BotActions step1 = new BotActions(telegramClient, taskService, userService, sprintService, projectService);
        step1.setChatId(PENDING_CHAT_ID);
        step1.setRequestText("/addtask 1 New feature");
        step1.fnAddTask();

        // Step 2: user replies with hours — bot creates the task
        BotActions step2 = new BotActions(telegramClient, taskService, userService, sprintService, projectService);
        step2.setChatId(PENDING_CHAT_ID);
        step2.setRequestText("2.0");
        step2.fnPendingConversation();

        verify(taskService).save(any(Task.class));
        assertThat(lastSentText())
                .contains("Task #10")
                .contains("New feature");
    }

    // ── Multi-step conversation: /complete → hours reply ─────────────────────

    @Test
    void pendingConversation_completeFlow_marksTaskDoneAndSendsConfirmation() {
        final long PENDING_CHAT_ID = 300L;
        final String PENDING_TID   = String.valueOf(PENDING_CHAT_ID);

        User currentUser = User.builder().userId(99L).build();
        Task task = Task.builder()
                .taskId(7L).status(Task.Status.PENDING)
                .assignedTo(currentUser).subtask(true).build();

        when(userService.findByTelegramId(PENDING_TID)).thenReturn(Optional.of(currentUser));
        when(taskService.findById(7L)).thenReturn(Optional.of(task));

        // Step 1: /complete 7 — bot asks for actual hours and stores state in PENDING
        BotActions step1 = new BotActions(telegramClient, taskService, userService, sprintService, projectService);
        step1.setChatId(PENDING_CHAT_ID);
        step1.setRequestText("/complete 7");
        step1.fnCompleteTask();

        // Step 2: user replies with actual hours — bot marks task DONE
        BotActions step2 = new BotActions(telegramClient, taskService, userService, sprintService, projectService);
        step2.setChatId(PENDING_CHAT_ID);
        step2.setRequestText("3.5");
        step2.fnPendingConversation();

        verify(taskService).completeTask(eq(7L), eq(new BigDecimal("3.5")), any(User.class));
        assertThat(lastSentText())
                .contains("Task #7")
                .contains("DONE");
    }
}
