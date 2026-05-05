package com.ganpat.devops;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class TaskController {

    private List<String> tasks = new ArrayList<>();

    @GetMapping("/add")
    public String addTask(@RequestParam String task) {
        tasks.add(task);
        return "Added";
    }

    @GetMapping("/tasks")
    public List<String> getTasks() {
        return tasks;
    }
}
