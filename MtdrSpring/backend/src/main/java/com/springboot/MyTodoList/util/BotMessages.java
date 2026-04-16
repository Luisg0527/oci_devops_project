package com.springboot.MyTodoList.util;

public enum BotMessages {

	HELLO_MYTODO_BOT(
	"Hello! I'm the OCI Project Bot!\nUse /help to see available commands, or choose an option below:"),
	BOT_REGISTERED_STARTED("Bot registered and started successfully!"),
	ITEM_DONE("Task marked as done! Use /todolist to see your tasks or /start for the main screen."),
	ITEM_UNDONE("Task reopened! Use /todolist to see your tasks or /start for the main screen."),
	ITEM_DELETED("Task deleted! Use /todolist to see your tasks or /start for the main screen."),
	TYPE_NEW_TODO_ITEM("To add a task use:\n/addtask <projectId> <estimatedHours> <title>"),
	NEW_ITEM_ADDED("Task created! Use /todolist to see your tasks or /start for the main screen."),
	BYE("Bye! Select /start to resume!"),
	TASK_CREATED("Task #%d created: \"%s\" (%.2f h) — assigned to you."),
	TASK_SUBTASK_CREATED("Subtask #%d created: \"%s\" (%.2f h) — linked to parent task #%d."),
	TASK_ASSIGNED_TO_SPRINT("Task #%d assigned to sprint #%d, status set to IN_PROGRESS and assigned to you."),
	TASK_COMPLETED("Task #%d marked as DONE."),
	TASK_HOURS_EXCEEDED(
		"Task exceeds the 4-hour limit.\n"
		+ "Please break it into subtasks of at most 4 hours each.\n\n"
		+ "First create the parent task (use 0 hours as placeholder or the first subtask's hours):\n"
		+ "  /addtask <projectId> <estimatedHours> <title>\n\n"
		+ "Then add each subtask with parent:<parentTaskId>:\n"
		+ "  /addtask <projectId> <estimatedHours> <title> parent:<parentTaskId>"),
	TASK_NOT_YOURS("You are not the assignee of task #%d. Only the assigned developer can perform this action."),
	TASK_HAS_PENDING_SUBTASKS(
		"Task #%d has %d pending subtask(s). Please complete all subtasks before closing the parent task."),
	PROJECT_NOT_FOUND("Project not found. Check the project ID and try again."),
	PARENT_TASK_NOT_FOUND("Parent task #%d not found. Check the ID and try again."),
	USER_NOT_REGISTERED("Your Telegram account is not linked to the system. Ask an admin to set your telegram_id."),
	INVALID_FORMAT("Invalid command format. Type /help to see usage."),
	TASK_NOT_FOUND("Task not found or could not be updated."),
	SPRINT_NOT_FOUND("Sprint not found."),
	HELP_TEXT("Available commands:\n\n"
		+ "/addtask <projectId> <estimatedHours> <title> [parent:<parentTaskId>]\n"
		+ "  Create a task (max 4 h). Add parent:<id> to link as a subtask.\n\n"
		+ "/assignsprint <taskId> <sprintId>\n"
		+ "  Assign a task to a sprint, set status IN_PROGRESS and assign it to you.\n\n"
		+ "/complete <taskId>\n"
		+ "  Mark a task as DONE (must be assigned to you, all subtasks must be done).\n\n"
		+ "/todolist — List all tasks\n"
		+ "/hide    — Hide keyboard\n"
		+ "/start   — Show main screen");

	private String message;

	BotMessages(String enumMessage) {
		this.message = enumMessage;
	}

	public String getMessage() {
		return message;
	}

}
