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
import java.util.Optional;
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

    public BotActions(TelegramClient tc, TaskService ts, UserService us, SprintService ss, ProjectService ps) {
        telegramClient = tc;
        taskService = ts;
        userService = us;
        sprintService = ss;
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
        BotHelper.sendMessageToTelegram(chatId, BotMessages.HELLO_MYTODO_BOT.getMessage(), telegramClient, mainKeyboard);
        exit = true;
    }

    // ── /help  ───────────────────────────────────────────────────────────────────
    public void fnHelp() {
        if (!requestText.equals(BotCommands.HELP.getCommand()) || exit)
            return;
        BotHelper.sendMessageToTelegram(chatId, BotMessages.HELP_TEXT.getMessage(), telegramClient);
        exit = true;
    }

    // ── /addtask <projectId> <estimatedHours> <title> [parent:<parentTaskId>]  ──
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

        // Expect: <projectId> <estimatedHours> <title>
        String[] parts = args.split("\\s+", 3);
        if (parts.length < 3) {
            BotHelper.sendMessageToTelegram(chatId,
                    "Usage: /addtask <projectId> <estimatedHours> <title> [parent:<parentTaskId>]\n"
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
            BigDecimal estimatedHours = new BigDecimal(parts[1]);
            String title = parts[2];

            // Validate hours range
            if (estimatedHours.compareTo(BigDecimal.ZERO) <= 0
                    || estimatedHours.compareTo(new BigDecimal("4")) > 0) {
                if (estimatedHours.compareTo(new BigDecimal("4")) > 0) {
                    BotHelper.sendMessageToTelegram(chatId,
                            BotMessages.TASK_HOURS_EXCEEDED.getMessage(), telegramClient);
                } else {
                    BotHelper.sendMessageToTelegram(chatId,
                            "Estimated hours must be greater than 0 and at most 4.\n"
                                    + BotMessages.INVALID_FORMAT.getMessage(),
                            telegramClient);
                }
                exit = true;
                return;
            }

            Optional<Project> projectOpt = projectService.findById(projectId);
            if (projectOpt.isEmpty()) {
                BotHelper.sendMessageToTelegram(chatId, BotMessages.PROJECT_NOT_FOUND.getMessage(), telegramClient);
                exit = true;
                return;
            }

            // Resolve parent task if provided
            Task parentTask = null;
            if (parentTaskId != null) {
                Optional<Task> parentOpt = taskService.findById(parentTaskId);
                if (parentOpt.isEmpty()) {
                    BotHelper.sendMessageToTelegram(chatId,
                            String.format(BotMessages.PARENT_TASK_NOT_FOUND.getMessage(), parentTaskId),
                            telegramClient);
                    exit = true;
                    return;
                }
                parentTask = parentOpt.get();
            }

            User user = userOpt.get();
            boolean isSubtask = parentTask != null;

            Task task = Task.builder()
                    .project(projectOpt.get())
                    .title(title)
                    .status(Task.Status.PENDING)
                    .taskStage(Task.Stage.BACKLOG)
                    .estimatedHours(estimatedHours)
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
        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId,
                    "projectId and estimatedHours must be numbers.\n" + BotMessages.INVALID_FORMAT.getMessage(),
                    telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
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

            // Atomically: set sprint + TASK_STAGE=SPRINT + ASSIGNED_TO=currentUser + STATUS=IN_PROGRESS
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

            // Ownership check: only the assigned developer may complete the task
            if (task.getAssignedTo() != null
                    && !task.getAssignedTo().getUserId().equals(currentUser.getUserId())) {
                BotHelper.sendMessageToTelegram(chatId,
                        String.format(BotMessages.TASK_NOT_YOURS.getMessage(), taskId),
                        telegramClient);
                exit = true;
                return;
            }

            // Pending-subtasks guard: parent tasks must have all subtasks closed first
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

            // Sets STATUS=DONE and TASK_STAGE=COMPLETED
            taskService.updateStatus(taskId, Task.Status.DONE, currentUser);
            BotHelper.sendMessageToTelegram(chatId,
                    String.format(BotMessages.TASK_COMPLETED.getMessage(), taskId),
                    telegramClient);
        } catch (NumberFormatException e) {
            BotHelper.sendMessageToTelegram(chatId, BotMessages.INVALID_FORMAT.getMessage(), telegramClient);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            BotHelper.sendMessageToTelegram(chatId, BotMessages.TASK_NOT_FOUND.getMessage(), telegramClient);
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
            // changedBy is nullable in TaskStatusHistory — safe to pass null if user is not registered
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
