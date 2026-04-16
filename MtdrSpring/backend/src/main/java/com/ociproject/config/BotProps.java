package com.ociproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BotProps {

    @Value("${telegram.bot.token}")
    private String token;

    @Value("${telegram.bot.name}")
    private String name;

    public String getToken() { return token; }
    public String getName()  { return name; }
}
