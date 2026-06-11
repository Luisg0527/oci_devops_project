package com.ociproject.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/", "/dashboard", "/backlog", "/team", "/reports", "/login"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
