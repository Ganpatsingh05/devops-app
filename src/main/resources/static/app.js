const state = {
    tasks: [],
    filter: "all",
    search: "",
    editingTask: null,
};

const elements = {};

document.addEventListener("DOMContentLoaded", () => {
    bindElements();
    bindEvents();
    refreshAll();
});

function bindElements() {
    elements.taskForm = document.getElementById("taskForm");
    elements.taskInput = document.getElementById("taskInput");
    elements.priorityInput = document.getElementById("priorityInput");
    elements.searchInput = document.getElementById("searchInput");
    elements.taskList = document.getElementById("taskList");
    elements.emptyState = document.getElementById("emptyState");
    elements.totalTasks = document.getElementById("totalTasks");
    elements.activeTasks = document.getElementById("activeTasks");
    elements.completedTasks = document.getElementById("completedTasks");
    elements.refreshButton = document.getElementById("refreshButton");
    elements.clearCompletedButton = document.getElementById("clearCompletedButton");
    elements.editModal = document.getElementById("editModal");
    elements.editForm = document.getElementById("editForm");
    elements.editTaskInput = document.getElementById("editTaskInput");
    elements.editPriorityInput = document.getElementById("editPriorityInput");
    elements.closeModalButton = document.getElementById("closeModalButton");
}

function bindEvents() {
    elements.taskForm.addEventListener("submit", handleAddTask);
    elements.editForm.addEventListener("submit", handleEditTask);
    elements.refreshButton.addEventListener("click", refreshAll);
    elements.clearCompletedButton.addEventListener("click", clearCompletedTasks);
    elements.searchInput.addEventListener("input", (event) => {
        state.search = event.target.value.trim();
        loadTasks();
    });
    elements.taskInput.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            elements.taskForm.requestSubmit();
        }
    });
    elements.closeModalButton.addEventListener("click", closeEditModal);
    elements.editModal.addEventListener("click", (event) => {
        if (event.target === elements.editModal) {
            closeEditModal();
        }
    });

    document.querySelectorAll(".chip[data-filter]").forEach((chip) => {
        chip.addEventListener("click", () => {
            state.filter = chip.dataset.filter;
            document.querySelectorAll(".chip[data-filter]").forEach((button) => button.classList.toggle("active", button === chip));
            loadTasks();
        });
    });
}

async function refreshAll() {
    await Promise.all([loadSummary(), loadTasks()]);
}

async function loadTasks() {
    const params = new URLSearchParams({
        status: state.filter,
        query: state.search,
        sort: "latest",
    });

    const response = await fetch(`/api/tasks?${params.toString()}`);
    state.tasks = await response.json();
    renderTasks();
}

async function loadSummary() {
    const response = await fetch("/api/summary");
    const summary = await response.json();
    elements.totalTasks.textContent = summary.total;
    elements.activeTasks.textContent = summary.active;
    elements.completedTasks.textContent = summary.completed;
}

async function handleAddTask(event) {
    event.preventDefault();

    const title = elements.taskInput.value.trim();
    if (!title) {
        return;
    }

    await fetch("/api/tasks", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            title,
            priority: elements.priorityInput.value,
        }),
    });

    elements.taskInput.value = "";
    elements.priorityInput.value = "medium";
    await refreshAll();
}

function renderTasks() {
    elements.taskList.innerHTML = "";

    if (state.tasks.length === 0) {
        elements.emptyState.classList.remove("hidden");
        return;
    }

    elements.emptyState.classList.add("hidden");

    state.tasks.forEach((task) => {
        const item = document.createElement("li");
        item.className = "task-item";

        const createdAt = formatDate(task.createdAt);
        const updatedAt = formatDate(task.updatedAt);

        item.innerHTML = `
            <input class="task-check" type="checkbox" ${task.completed ? "checked" : ""} aria-label="Mark task as complete">
            <div class="task-main">
                <div class="task-title-row">
                    <span class="task-title ${task.completed ? "completed" : ""}">${escapeHtml(task.title)}</span>
                    <span class="pill ${task.priority}">${task.priority}</span>
                </div>
                <div class="task-meta">
                    <span>Created ${createdAt}</span>
                    <span>Updated ${updatedAt}</span>
                </div>
            </div>
            <div class="task-actions">
                <button class="icon-button" type="button" title="Edit task">✎</button>
                <button class="icon-button danger" type="button" title="Delete task">×</button>
            </div>
        `;

        const checkbox = item.querySelector(".task-check");
        const editButton = item.querySelectorAll(".icon-button")[0];
        const deleteButton = item.querySelectorAll(".icon-button")[1];

        checkbox.addEventListener("change", () => toggleTask(task.id));
        editButton.addEventListener("click", () => openEditModal(task));
        deleteButton.addEventListener("click", () => deleteTask(task.id));

        elements.taskList.appendChild(item);
    });
}

async function toggleTask(id) {
    await fetch(`/api/tasks/${id}/toggle`, { method: "PUT" });
    await refreshAll();
}

async function deleteTask(id) {
    await fetch(`/api/tasks/${id}`, { method: "DELETE" });
    await refreshAll();
}

async function clearCompletedTasks() {
    await fetch("/api/tasks/completed", { method: "DELETE" });
    await refreshAll();
}

function openEditModal(task) {
    state.editingTask = task;
    elements.editTaskInput.value = task.title;
    elements.editPriorityInput.value = task.priority;
    elements.editModal.classList.remove("hidden");
    elements.editModal.setAttribute("aria-hidden", "false");
    elements.editTaskInput.focus();
}

function closeEditModal() {
    state.editingTask = null;
    elements.editModal.classList.add("hidden");
    elements.editModal.setAttribute("aria-hidden", "true");
    elements.editForm.reset();
}

async function handleEditTask(event) {
    event.preventDefault();

    if (!state.editingTask) {
        return;
    }

    await fetch(`/api/tasks/${state.editingTask.id}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            title: elements.editTaskInput.value.trim(),
            priority: elements.editPriorityInput.value,
        }),
    });

    closeEditModal();
    await refreshAll();
}

function formatDate(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat([], {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(date);
}

function escapeHtml(value) {
    return value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}