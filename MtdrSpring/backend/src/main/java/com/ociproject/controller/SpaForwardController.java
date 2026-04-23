package com.ociproject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/dashboard", "/dev", "/backlog", "/team", "/reports", "/login"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
