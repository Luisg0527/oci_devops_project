package com.springboot.MyTodoList.util;

import com.ociproject.model.Project;
import com.ociproject.model.Sprint;
import com.ociproject.model.Task;
import com.ociproject.model.User;
import com.ociproject.service.ProjectService;
import com.ociproject.service.SprintService;
import com.ociproject.service.TaskService;
import com.ociproject.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BotActions {

    private static final Logger logger = LoggerFactory.getLogger(BotActions.class);

    private String requestText;
    private long chatId;
    private final TelegramClient telegramClient;
    private boolean exit;

    private final TaskService taskService;
    private final UserService userService;
    private final SprintService sprintService;
    private final ProjectService projectService;

    private static final Pattern PARENT_PATTERN = Pattern.compile("(?i)\\s+parent:(\\d+)$");

    // ── Pending conversation state (shared across instances) ─────────────────────
    private static final Map<Long, PendingConversation> PENDING = new ConcurrentHashMap<>();

    private static class PendingConversation {
        enum Step { ADDTASK_HOURS, COMPLETE_HOURS }

        final Step step;
        // addtask fields
        Long projectId;
        String title;
        Long parentTaskId;
        // complete fields
        Long taskId;

        private PendingConversation(Step step) { this.step = step; }

        static PendingConversation forAddTask(long projectId, String title, Long parentTaskId) {
            PendingConversation p = new PendingConversation(Step.ADDTASK_HOURS);
            p.projectId  = projectId;
            p.title      = title;
            p.parentTaskId = parentTaskId;
            return p;
        }

        static PendingConversation forComplete(long taskId) {
            PendingConversation p = new PendingConversation(Step.COMPLETE_HOURS);
            p.taskId = taskId;
            return p;
        }
    }
    // ─────────────────────────────────────────────────────────────────────────────

    public BotActions(TelegramClient tc, TaskService ts, UserService us, SprintService ss, ProjectService ps) {
        telegramClient = tc;
        taskService    = ts;
        userService    = us;
        sprintService  = ss;
        projectService = ps;
        exit = false;
    }

    public void setRequestText(String cmd) { requestText = cmd; }
    public void setChatId(long chId)       { chatId = chId; }

    /** Resolves the Telegram chatId to a User by the telegramId column. */
    private Optional<User> resolveUser() {
        return userService.findByTelegramId(String.valueOf(chatId));
    }

    // ── /start  ──────────────────────────────────────────────────────────────────
    public void fnStart() {
        if (!(requestText.equals(BotCommands.START_COMMAND.getCommand())
                || requestText.equals(BotLabels.SHOW_MAIN_SCREEN.getLabel())) || exit)
            return;

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BotLabels.LIST_ALL_ITEMS.getLabel());
        row1.add(BotLabels.ADD_NEW_ITEM.getLabel());
        KeyboardRow row2 = new KeyboardRow();
        row2.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        row2.add(BotLabels.HIDE_MAIN_SCREEN.getLabel());
        ReplyKeyboardMarkup mainKeyboard = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .resizeKeyboard(true)
                .build();

        String welcomeMsg = BotMessages.HELLO_MYTODO_BOT.getMessage()
                + "\n\nYour Telegram ID: " + chatId;

        BotHelper.sendMessageToTelegram(chatId, welcomeMsg, telegramClient, mainKeyboard);
        exit = true;
    }

    // ── /help  ───────────────────────────────────────────────────────────────────
    public void fnHelp() {
        if (!requestText.equals(BotCommands.HELP.getCommand()) || exit)
            return;
        BotHelper.sendMessageToTelegram(chatId, BotMessages.HELP_TEXT.getMessage(), telegramClient);
        exit = true;
    }

    // ── /addtask <projectId> <title> [parent:<parentTaskId>]  ────────────────────
    // Bot asks for estimated hours in a follow-up message.
    public void fnAddTask() {
        if (!requestText.startsWith(BotCommands.ADD_TASK.getCommand()) || exit)
            return;

        String args = requestText.substring(BotCommands.ADD_TASK.getCommand().length()).trim();

        // Extract optional parent:<parentTaskId> suffix
        Long parentTaskId = null;
        Matcher parentMatcher = PARENT_PATTERN.matcher(args);
        if (parentMatcher.find()) {
            parentTaskId = Long.parseLong(parentMatcher.group(1));
            args = args.substring(0, parentMatcher.start()).trim();
        }

        // Expect: <projectId> <title>
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            BotHelper.sendMessageToTelegram(chatId,
                    "Usage: /addtask <projectId> <title> [parent:<parentTaskId>]\n"
                            + BotMessages.INVALID_FORMAT.getMessage(),
                    telegramClient);
            exit = true;
            return;
        }

        Optional<User> userOpt = resolveUser();
        if (userOpt.isEmpty()) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.USER_NOT_REGISTERED.getMessage(), telegramClient);
            exit = true;
            return;
        }

        try {
            long projectId = Long.parseLong(parts[0]);
            String title   = parts[1];

            Optional<Project> projectOpt = projectService.findById(projectId);
            if (projectOpt.isEmpty()) {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.PROJECT_NOT_FOUND.getMessage(), telegramClient);
                exit = true;
                return;
            }

            if (parentTaskId != null) {
                Optional<Task> parentOpt = taskService.findById(parentTaskId);
                if (parentOpt.isEmpty()) {
                    BotHelper.sendMessageToTelegram(chatId,
                            String.format(BotMessages.PARENT_TASK_NOT_FOUND.getMessage(), parentTaskId),
                            telegramClient);
                    exit = true;
                    return;
                }
            }

            PENDING.put(chatId, PendingConversation.forAddTask(projectId, title, parentTaskId));
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ASK_ADDTASK_HOURS.getMessage(), telegramClient);

        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId,
                    "projectId must be a number.\n" + BotMessages.INVALID_FORMAT.getMessage(),
                    telegramClient);
        }
        exit = true;
    }

    // ── /assignsprint <taskId> <sprintId>  ───────────────────────────────────────
    public void fnAssignSprint() {
        if (!requestText.startsWith(BotCommands.ASSIGN_SPRINT.getCommand()) || exit)
            return;

        String args = requestText.substring(BotCommands.ASSIGN_SPRINT.getCommand().length()).trim();
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            BotHelper.sendMessageToTelegram(chatId,
                    "Usage: /assignsprint <taskId> <sprintId>\n" + BotMessages.INVALID_FORMAT.getMessage(),
                    telegramClient);
            exit = true;
            return;
        }

        Optional<User> userOpt = resolveUser();
        if (userOpt.isEmpty()) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.USER_NOT_REGISTERED.getMessage(), telegramClient);
            exit = true;
            return;
        }

        try {
            long taskId   = Long.parseLong(parts[0]);
            long sprintId = Long.parseLong(parts[1]);

            Optional<Sprint> sprintOpt = sprintService.findById(sprintId);
            if (sprintOpt.isEmpty()) {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.SPRINT_NOT_FOUND.getMessage(), telegramClient);
                exit = true;
                return;
            }

            taskService.startTask(taskId, sprintOpt.get(), userOpt.get());
            BotHelper.sendMessageToTelegram(chatId,
                    String.format(BotMessages.TASK_ASSIGNED_TO_SPRINT.getMessage(), taskId, sprintId),
                    telegramClient);
        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.INVALID_FORMAT.getMessage(), telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
        }
        exit = true;
    }

    // ── /complete <taskId>  ──────────────────────────────────────────────────────
    // Bot asks for actual hours spent in a follow-up message.
    public void fnCompleteTask() {
        if (!requestText.startsWith(BotCommands.COMPLETE_TASK.getCommand()) || exit)
            return;

        String arg = requestText.substring(BotCommands.COMPLETE_TASK.getCommand().length()).trim();
        if (arg.isEmpty()) {
            BotHelper.sendMessageToTelegram(chatId,
                    "Usage: /complete <taskId>\n" + BotMessages.INVALID_FORMAT.getMessage(), telegramClient);
            exit = true;
            return;
        }

        Optional<User> userOpt = resolveUser();
        if (userOpt.isEmpty()) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.USER_NOT_REGISTERED.getMessage(), telegramClient);
            exit = true;
            return;
        }

        try {
            long taskId = Long.parseLong(arg);
            User currentUser = userOpt.get();

            Optional<Task> taskOpt = taskService.findById(taskId);
            if (taskOpt.isEmpty()) {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
                exit = true;
                return;
            }
            Task task = taskOpt.get();

            // Ownership check
            if (task.getAssignedTo() != null
                    && !task.getAssignedTo().getUserId().equals(currentUser.getUserId())) {
                BotHelper.sendMessageToTelegram(chatId,
                        String.format(BotMessages.TASK_NOT_YOURS.getMessage(), taskId),
                        telegramClient);
                exit = true;
                return;
            }

            // Pending-subtasks guard
            if (!Boolean.TRUE.equals(task.getSubtask())) {
                long pendingSubtasks = taskService.findSubtasks(taskId).stream()
                        .filter(s -> s.getStatus() != Task.Status.DONE
                                && s.getStatus() != Task.Status.CANCELLED)
                        .count();
                if (pendingSubtasks > 0) {
                    BotHelper.sendMessageToTelegram(chatId,
                            String.format(BotMessages.TASK_HAS_PENDING_SUBTASKS.getMessage(),
                                    taskId, pendingSubtasks),
                            telegramClient);
                    exit = true;
                    return;
                }
            }

            // All checks passed — ask for actual hours
            PENDING.put(chatId, PendingConversation.forComplete(taskId));
            BotHelper.sendMessageToTelegram(chatId,
                    String.format(BotMessages.ASK_COMPLETE_HOURS.getMessage(), taskId),
                    telegramClient);

        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.INVALID_FORMAT.getMessage(), telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
        }
        exit = true;
    }

    // ── Handle pending multi-step conversations  ──────────────────────────────────
    public void fnPendingConversation() {
        if (exit) return;

        PendingConversation pending = PENDING.get(chatId);
        if (pending == null) return;

        BigDecimal hours;
        try {
            hours = new BigDecimal(requestText.trim());
        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId,
                    "Please reply with a valid number (e.g. 2.5).", telegramClient);
            exit = true;
            return;
        }

        if (pending.step == PendingConversation.Step.ADDTASK_HOURS) {
            // Validate hours range
            if (hours.compareTo(BigDecimal.ZERO) <= 0 || hours.compareTo(new BigDecimal("4")) > 0) {
                if (hours.compareTo(new BigDecimal("4")) > 0) {
                    BotHelper.sendMessageToTelegram(chatId,
                            BotMessages.TASK_HOURS_EXCEEDED.getMessage(), telegramClient);
                } else {
                    BotHelper.sendMessageToTelegram(chatId,
                            "Estimated hours must be greater than 0 and at most 4.\n"
                                    + BotMessages.ASK_ADDTASK_HOURS.getMessage(),
                            telegramClient);
                }
                exit = true;
                return;
            }

            Optional<User> userOpt = resolveUser();
            if (userOpt.isEmpty()) {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.USER_NOT_REGISTERED.getMessage(), telegramClient);
                PENDING.remove(chatId);
                exit = true;
                return;
            }

            try {
                Optional<Project> projectOpt = projectService.findById(pending.projectId);
                if (projectOpt.isEmpty()) {
                    BotHelper.sendMessageToTelegram(chatId, BotMessages.PROJECT_NOT_FOUND.getMessage(), telegramClient);
                    PENDING.remove(chatId);
                    exit = true;
                    return;
                }

                Task parentTask = null;
                if (pending.parentTaskId != null) {
                    Optional<Task> parentOpt = taskService.findById(pending.parentTaskId);
                    if (parentOpt.isEmpty()) {
                        BotHelper.sendMessageToTelegram(chatId,
                                String.format(BotMessages.PARENT_TASK_NOT_FOUND.getMessage(), pending.parentTaskId),
                                telegramClient);
                        PENDING.remove(chatId);
                        exit = true;
                        return;
                    }
                    parentTask = parentOpt.get();
                }

                User user = userOpt.get();
                boolean isSubtask = parentTask != null;

                Task task = Task.builder()
                        .project(projectOpt.get())
                        .title(pending.title)
                        .status(Task.Status.PENDING)
                        .taskStage(Task.Stage.BACKLOG)
                        .estimatedHours(hours)
                        .subtask(isSubtask)
                        .parentTask(parentTask)
                        .createdBy(user)
                        .assignedTo(user)
                        .deleted(false)
                        .build();
                Task saved = taskService.save(task);

                if (isSubtask) {
                    BotHelper.sendMessageToTelegram(chatId,
                            String.format(BotMessages.TASK_SUBTASK_CREATED.getMessage(),
                                    saved.getTaskId(), saved.getTitle(),
                                    saved.getEstimatedHours(), parentTask.getTaskId()),
                            telegramClient);
                } else {
                    BotHelper.sendMessageToTelegram(chatId,
                            String.format(BotMessages.TASK_CREATED.getMessage(),
                                    saved.getTaskId(), saved.getTitle(), saved.getEstimatedHours()),
                            telegramClient);
                }
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
            }

            PENDING.remove(chatId);

        } else if (pending.step == PendingConversation.Step.COMPLETE_HOURS) {
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                BotHelper.sendMessageToTelegram(chatId,
                        "Actual hours must be greater than 0.\n"
                                + String.format(BotMessages.ASK_COMPLETE_HOURS.getMessage(), pending.taskId),
                        telegramClient);
                exit = true;
                return;
            }

            Optional<User> userOpt = resolveUser();
            if (userOpt.isEmpty()) {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.USER_NOT_REGISTERED.getMessage(), telegramClient);
                PENDING.remove(chatId);
                exit = true;
                return;
            }

            try {
                taskService.completeTask(pending.taskId, hours, userOpt.get());
                BotHelper.sendMessageToTelegram(chatId,
                        String.format(BotMessages.TASK_COMPLETED.getMessage(), pending.taskId, hours),
                        telegramClient);
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
            }

            PENDING.remove(chatId);
        }

        exit = true;
    }

    // ── <taskId>-DONE  (keyboard button)  ────────────────────────────────────────
    public void fnDone() {
        if (requestText.indexOf(BotLabels.DONE.getLabel()) == -1 || exit)
            return;

        String idStr = requestText.substring(0, requestText.indexOf(BotLabels.DASH.getLabel()));
        try {
            Long id = Long.valueOf(idStr);
            User changedBy = resolveUser().orElse(null);
            taskService.updateStatus(id, Task.Status.DONE, changedBy);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DONE.getMessage(), telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
        }
        exit = true;
    }

    // ── <taskId>-UNDO  (keyboard button)  ────────────────────────────────────────
    public void fnUndo() {
        if (requestText.indexOf(BotLabels.UNDO.getLabel()) == -1 || exit)
            return;

        String idStr = requestText.substring(0, requestText.indexOf(BotLabels.DASH.getLabel()));
        try {
            Long id = Long.valueOf(idStr);
            User changedBy = resolveUser().orElse(null);
            taskService.updateStatus(id, Task.Status.PENDING, changedBy);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_UNDONE.getMessage(), telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
        }
        exit = true;
    }

    // ── <taskId>-DELETE  (keyboard button)  ──────────────────────────────────────
    public void fnDelete() {
        if (requestText.indexOf(BotLabels.DELETE.getLabel()) == -1 || exit)
            return;

        String idStr = requestText.substring(0, requestText.indexOf(BotLabels.DASH.getLabel()));
        try {
            Long id = Long.valueOf(idStr);
            taskService.softDelete(id);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.ITEM_DELETED.getMessage(), telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
        }
        exit = true;
    }

    // ── /hide  ───────────────────────────────────────────────────────────────────
    public void fnHide() {
        if (!(requestText.equals(BotCommands.HIDE_COMMAND.getCommand())
                || requestText.equals(BotLabels.HIDE_MAIN_SCREEN.getLabel())) || exit)
            return;
        BotHelper.sendMessageToTelegram(chatId, BotMessages.BYE.getMessage(), telegramClient);
        exit = true;
    }

    // ── /todolist  ───────────────────────────────────────────────────────────────
    public void fnListAll() {
        if (!(requestText.equals(BotCommands.TODO_LIST.getCommand())
                || requestText.equals(BotLabels.LIST_ALL_ITEMS.getLabel())
                || requestText.equals(BotLabels.MY_TODO_LIST.getLabel())) || exit)
            return;

        List<Task> allItems = taskService.findAll();

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .build();

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow topRow = new KeyboardRow();
        topRow.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(topRow);

        KeyboardRow addRow = new KeyboardRow();
        addRow.add(BotLabels.ADD_NEW_ITEM.getLabel());
        keyboard.add(addRow);

        KeyboardRow titleRow = new KeyboardRow();
        titleRow.add(BotLabels.MY_TODO_LIST.getLabel());
        keyboard.add(titleRow);

        List<Task> pending = allItems.stream()
                .filter(t -> t.getStatus() != Task.Status.DONE)
                .collect(Collectors.toList());

        for (Task task : pending) {
            KeyboardRow row = new KeyboardRow();
            row.add(task.getTitle());
            row.add(task.getTaskId() + BotLabels.DASH.getLabel() + BotLabels.DONE.getLabel());
            keyboard.add(row);
        }

        List<Task> done = allItems.stream()
                .filter(t -> t.getStatus() == Task.Status.DONE)
                .collect(Collectors.toList());

        for (Task task : done) {
            KeyboardRow row = new KeyboardRow();
            row.add(task.getTitle());
            row.add(task.getTaskId() + BotLabels.DASH.getLabel() + BotLabels.UNDO.getLabel());
            row.add(task.getTaskId() + BotLabels.DASH.getLabel() + BotLabels.DELETE.getLabel());
            keyboard.add(row);
        }

        KeyboardRow bottomRow = new KeyboardRow();
        bottomRow.add(BotLabels.SHOW_MAIN_SCREEN.getLabel());
        keyboard.add(bottomRow);

        keyboardMarkup.setKeyboard(keyboard);
        BotHelper.sendMessageToTelegram(chatId, BotLabels.MY_TODO_LIST.getLabel(), telegramClient, keyboardMarkup);
        exit = true;
    }

    // ── Add New Item button → guide user to /addtask  ────────────────────────────
    public void fnAddItem() {
        if (!(requestText.contains(BotCommands.ADD_ITEM.getCommand())
                || requestText.contains(BotLabels.ADD_NEW_ITEM.getLabel())) || exit)
            return;
        BotHelper.sendMessageToTelegram(chatId, BotMessages.TYPE_NEW_TODO_ITEM.getMessage(), telegramClient);
        exit = true;
    }

    // ── /llm  ────────────────────────────────────────────────────────────────────
    public void fnLLM() {
        if (!requestText.contains(BotCommands.LLM_REQ.getCommand()) || exit)
            return;
        BotHelper.sendMessageToTelegram(chatId, "LLM integration not available in this version.",
                telegramClient, null);
        exit = true;
    }

    // ── catch-all: show help  ─────────────────────────────────────────────────────
    public void fnElse() {
        if (exit) return;
        BotHelper.sendMessageToTelegram(chatId, BotMessages.HELP_TEXT.getMessage(), telegramClient);
    }
}
