package com.ganpat.devops;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@RestController
public class TaskController {

    private static final Set<String> ALLOWED_PRIORITIES = Set.of("low", "medium", "high");
    private final Map<Long, TaskRecord> tasks = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @GetMapping("/add")
    public synchronized Map<String, Object> addTask(@RequestParam String task) {
        Map<String, Object> created = createTask(task, "medium");
        return Map.of("message", "Added", "task", created);
    }

    @GetMapping("/tasks")
    public synchronized List<Map<String, Object>> getLegacyTasks() {
        return listTasks("all", "", "latest");
    }

    @GetMapping("/api/tasks")
    public synchronized List<Map<String, Object>> getTasks(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "latest") String sort) {
        return listTasks(status, query, sort);
    }

    @GetMapping("/api/summary")
    public synchronized Map<String, Object> getSummary() {
        long total = tasks.size();
        long completed = tasks.values().stream().filter(TaskRecord::isCompleted).count();
        return summaryMap(total, total - completed, completed);
    }

    @PostMapping("/api/tasks")
    public synchronized Map<String, Object> createTask(@RequestBody TaskRequest request) {
        String title = normalizeTitle(request.getTitle());
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task title is required");
        }
        return createTask(title, request.getPriority());
    }

    @PutMapping("/api/tasks/{id}")
    public synchronized Map<String, Object> updateTask(@PathVariable long id, @RequestBody TaskRequest request) {
        TaskRecord existing = requireTask(id);
        String title = request.getTitle() == null ? existing.getTitle() : normalizeTitle(request.getTitle());
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task title is required");
        }
        String priority = request.getPriority() == null ? existing.getPriority() : normalizePriority(request.getPriority());
        TaskRecord updated = new TaskRecord(existing.getId(), title, existing.isCompleted(), priority, existing.getCreatedAt(), now());
        tasks.put(id, updated);
        return toMap(updated);
    }

    @PutMapping("/api/tasks/{id}/toggle")
    public synchronized Map<String, Object> toggleTask(@PathVariable long id) {
        TaskRecord existing = requireTask(id);
        TaskRecord updated = new TaskRecord(
            existing.getId(),
            existing.getTitle(),
            !existing.isCompleted(),
            existing.getPriority(),
            existing.getCreatedAt(),
                now());
        tasks.put(id, updated);
        return toMap(updated);
    }

    @DeleteMapping("/api/tasks/{id}")
    public synchronized void deleteTask(@PathVariable long id) {
        requireTask(id);
        tasks.remove(id);
    }

    @DeleteMapping("/api/tasks/completed")
    public synchronized Map<String, Object> clearCompleted() {
        tasks.entrySet().removeIf(entry -> entry.getValue().isCompleted());
        return getSummary();
    }

    private List<Map<String, Object>> listTasks(String status, String query, String sort) {
        String normalizedStatus = status == null ? "all" : status.trim().toLowerCase(Locale.ROOT);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean oldestFirst = "oldest".equalsIgnoreCase(sort);

        Stream<TaskRecord> stream = tasks.values().stream()
                .filter(task -> matchesStatus(task, normalizedStatus))
                .filter(task -> normalizedQuery.isEmpty() || task.getTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery));

        if (oldestFirst) {
            return stream.sorted((left, right) -> left.getUpdatedAt().compareTo(right.getUpdatedAt())).map(this::toMap).toList();
        }
        return stream.sorted((left, right) -> right.getUpdatedAt().compareTo(left.getUpdatedAt())).map(this::toMap).toList();
    }

    private boolean matchesStatus(TaskRecord task, String status) {
        if ("active".equals(status)) {
            return !task.isCompleted();
        }
        if ("completed".equals(status)) {
            return task.isCompleted();
        }
        return true;
    }

    private Map<String, Object> createTask(String title, String priority) {
        long id = nextId.getAndIncrement();
        TaskRecord task = new TaskRecord(id, normalizeTitle(title), false, normalizePriority(priority), now(), now());
        tasks.put(id, task);
        return toMap(task);
    }

    private TaskRecord requireTask(long id) {
        TaskRecord task = tasks.get(id);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return task;
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private String normalizePriority(String priority) {
        String normalized = priority == null ? "medium" : priority.trim().toLowerCase(Locale.ROOT);
        return ALLOWED_PRIORITIES.contains(normalized) ? normalized : "medium";
    }

    private String now() {
        return Instant.now().toString();
    }

    private Map<String, Object> toMap(TaskRecord task) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("id", task.getId());
        mapped.put("title", task.getTitle());
        mapped.put("completed", task.isCompleted());
        mapped.put("priority", task.getPriority());
        mapped.put("createdAt", task.getCreatedAt());
        mapped.put("updatedAt", task.getUpdatedAt());
        return mapped;
    }

    private Map<String, Object> summaryMap(long total, long active, long completed) {
        Map<String, Object> mapped = new HashMap<>();
        mapped.put("total", total);
        mapped.put("active", active);
        mapped.put("completed", completed);
        return mapped;
    }

}
