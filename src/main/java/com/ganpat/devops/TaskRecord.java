package com.ganpat.devops;

public class TaskRecord {

	private Long id;
	private String title;
	private boolean completed;
	private String priority;
	private String createdAt;
	private String updatedAt;

	public TaskRecord(Long id, String title, boolean completed, String priority, String createdAt, String updatedAt) {
		this.id = id;
		this.title = title;
		this.completed = completed;
		this.priority = priority;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public boolean isCompleted() {
		return completed;
	}

	public String getPriority() {
		return priority;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}
}