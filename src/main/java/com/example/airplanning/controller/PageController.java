package com.example.airplanning.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/boards/detail")
    public String boardDetail() {
        return "boards/detail";
    }

    @GetMapping("/board/list")
    public String boardList() {
        return "boards/list";
    }

    @GetMapping("/users/planner")
    public String plannerPage() {
        return "users/planner";
    }

}
