package com.springboot.MyTodoList.util;

public enum BotMessages {

	HELLO_MYTODO_BOT(
	"Hello! I'm the OCI Project Bot!\nUse /help to see available commands, or choose an option below:"),
	BOT_REGISTERED_STARTED("Bot registered and started successfully!"),
	ITEM_DONE("Task marked as done! Use /todolist to see your tasks or /start for the main screen."),
	ITEM_UNDONE("Task reopened! Use /todolist to see your tasks or /start for the main screen."),
	ITEM_DELETED("Task deleted! Use /todolist to see your tasks or /start for the main screen."),
	TYPE_NEW_TODO_ITEM("To add a task use: /addtask <title>"),
	NEW_ITEM_ADDED("Task created! Use /todolist to see your tasks or /start for the main screen."),
	BYE("Bye! Select /start to resume!"),
	TASK_CREATED("Task #%d created and assigned to you: \"%s\""),
	TASK_ASSIGNED_TO_SPRINT("Task #%d successfully assigned to sprint #%d."),
	TASK_COMPLETED("Task #%d marked as DONE."),
	USER_NOT_REGISTERED("Your Telegram account is not linked to the system. Ask an admin to set your telegram_id."),
	INVALID_FORMAT("Invalid command format. Type /help to see usage."),
	TASK_NOT_FOUND("Task not found or could not be updated."),
	SPRINT_NOT_FOUND("Sprint not found."),
	HELP_TEXT("Available commands:\n"
		+ "/addtask <title> — Create a task assigned to yourself\n"
		+ "/assignsprint <taskId> <sprintId> — Assign a task to a sprint\n"
		+ "/complete <taskId> — Mark a task as DONE\n"
		+ "/todolist — List all tasks\n"
		+ "/hide — Hide keyboard\n"
		+ "/start — Show main screen");

	private String message;

	BotMessages(String enumMessage) {
		this.message = enumMessage;
	}

	public String getMessage() {
		return message;
	}

}
