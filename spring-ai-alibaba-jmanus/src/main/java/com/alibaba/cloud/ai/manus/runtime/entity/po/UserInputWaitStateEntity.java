/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.manus.runtime.entity.po;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * User Input Wait State Entity for persisting user input wait states.
 *
 * This entity stores the state when a plan is waiting for user input, including form
 * descriptions and input fields.
 *
 * @author AI Assistant
 * @since 2025
 */
@Entity
@Table(name = "user_input_wait_state")
public class UserInputWaitStateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Current plan ID - the specific plan waiting for user input
	 */
	@Column(name = "current_plan_id", nullable = false, length = 255)
	private String currentPlanId;

	/**
	 * Root plan ID - the main identifier for the entire task hierarchy
	 */
	@Column(name = "root_plan_id", nullable = false, length = 255)
	private String rootPlanId;

	/**
	 * Message to display to the user while waiting
	 */
	@Column(name = "message", columnDefinition = "TEXT")
	private String message;

	/**
	 * Whether the plan is currently waiting for user input
	 */
	@Column(name = "waiting", nullable = false)
	private boolean waiting;

	/**
	 * Description of the form to be displayed
	 */
	@Column(name = "form_description", columnDefinition = "TEXT")
	private String formDescription;

	/**
	 * Form inputs as JSON string (will be converted to/from List<Map<String, String>>)
	 */
	@Column(name = "form_inputs", columnDefinition = "TEXT")
	private String formInputsJson;

	/**
	 * Creation time
	 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/**
	 * Last update time
	 */
	@Column(name = "last_updated")
	private LocalDateTime lastUpdated;

	/**
	 * Many-to-one relationship with RootTaskManagerEntity
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "root_task_manager_id", nullable = false)
	private RootTaskManagerEntity rootTaskManager;

	// Constructors
	public UserInputWaitStateEntity() {
		this.createdAt = LocalDateTime.now();
		this.lastUpdated = LocalDateTime.now();
		this.waiting = true;
	}

	public UserInputWaitStateEntity(String currentPlanId, String rootPlanId, String message, boolean waiting) {
		this();
		this.currentPlanId = currentPlanId;
		this.rootPlanId = rootPlanId;
		this.message = message;
		this.waiting = waiting;
	}

	// Pre-persist and pre-update hooks
	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.lastUpdated = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.lastUpdated = LocalDateTime.now();
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCurrentPlanId() {
		return currentPlanId;
	}

	public void setCurrentPlanId(String currentPlanId) {
		this.currentPlanId = currentPlanId;
	}

	public String getRootPlanId() {
		return rootPlanId;
	}

	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isWaiting() {
		return waiting;
	}

	public void setWaiting(boolean waiting) {
		this.waiting = waiting;
	}

	public String getFormDescription() {
		return formDescription;
	}

	public void setFormDescription(String formDescription) {
		this.formDescription = formDescription;
	}

	public String getFormInputsJson() {
		return formInputsJson;
	}

	public void setFormInputsJson(String formInputsJson) {
		this.formInputsJson = formInputsJson;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public RootTaskManagerEntity getRootTaskManager() {
		return rootTaskManager;
	}

	public void setRootTaskManager(RootTaskManagerEntity rootTaskManager) {
		this.rootTaskManager = rootTaskManager;
	}

	@Override
	public String toString() {
		return "UserInputWaitStateEntity{" + "id=" + id + ", currentPlanId='" + currentPlanId + '\'' + ", rootPlanId='"
				+ rootPlanId + '\'' + ", message='" + message + '\'' + ", waiting=" + waiting + ", formDescription='"
				+ formDescription + '\'' + ", createdAt=" + createdAt + ", lastUpdated=" + lastUpdated + '}';
	}

}
