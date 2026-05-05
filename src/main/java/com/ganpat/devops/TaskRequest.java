package com.ganpat.devops;

public class TaskRequest {

	private String title;
	private String priority;

	public TaskRequest() {
	}

	public TaskRequest(String title, String priority) {
		this.title = title;
		this.priority = priority;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}
}