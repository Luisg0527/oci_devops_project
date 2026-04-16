package com.springboot.MyTodoList.util;

import com.ociproject.config.BotProps;
import com.ociproject.service.SprintService;
import com.ociproject.service.TaskService;
import com.ociproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TelegramBotHandler implements SpringLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotHandler.class);

    private final BotProps botProps;
    private final TelegramClient telegramClient;
    private final TaskService taskService;
    private final UserService userService;
    private final SprintService sprintService;

    @Override
    public String getBotToken() {
        return botProps.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this::handleUpdates;
    }

    private void handleUpdates(List<Update> updates) {
        for (Update update : updates) {
            if (!update.hasMessage() || !update.getMessage().hasText()) continue;

            long chatId = update.getMessage().getChatId();
            String text  = update.getMessage().getText().trim();

            logger.debug("Telegram update — chatId={} text={}", chatId, text);

            BotActions actions = new BotActions(telegramClient, taskService, userService, sprintService);
            actions.setChatId(chatId);
            actions.setRequestText(text);

            actions.fnStart();
            actions.fnHelp();
            actions.fnAddTask();
            actions.fnAssignSprint();
            actions.fnCompleteTask();
            actions.fnListAll();
            actions.fnAddItem();
            actions.fnDone();
            actions.fnUndo();
            actions.fnDelete();
            actions.fnHide();
            actions.fnLLM();
            actions.fnElse();
        }
    }
}
