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
import java.util.ArrayList;
import java.util.List;

/**
 * Root Task Manager Entity for managing high-level task operations and state.
 *
 * This entity serves as the central coordinator for root-level tasks with three main
 * responsibilities: 1. Store the desired state of the task with root plan ID (run, stop,
 * etc.) 2. Store the user input form data for the task 3. Store the task execution
 * results and outcomes
 *
 * @author AI Assistant
 * @since 2025
 */
@Entity
@Table(name = "root_task_manager")
public class RootTaskManagerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Root plan ID - the main identifier for the entire task hierarchy
	 */
	@Column(name = "root_plan_id", nullable = false, unique = true, length = 255)
	private String rootPlanId;

	/**
	 * User's desired task state - what the user wants the task to do Values: START, STOP,
	 * PAUSE, RESUME, CANCEL
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "desired_task_state", nullable = false, length = 50)
	private DesiredTaskState desiredTaskState;

	/**
	 * Task execution result (merged summary and details)
	 */
	@Column(name = "task_result", columnDefinition = "TEXT")
	private String taskResult;

	/**
	 * Task execution start time
	 */
	@Column(name = "start_time")
	private LocalDateTime startTime;

	/**
	 * Task execution end time
	 */
	@Column(name = "end_time")
	private LocalDateTime endTime;

	/**
	 * Last update time for the task
	 */
	@Column(name = "last_updated")
	private LocalDateTime lastUpdated;

	/**
	 * Task creation time
	 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	/**
	 * User who created the task
	 */
	@Column(name = "created_by", length = 100)
	private String createdBy;

	/**
	 * One-to-many relationship with UserInputWaitStateEntity
	 */
	@OneToMany(mappedBy = "rootTaskManager", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<UserInputWaitStateEntity> userInputWaitStates = new ArrayList<>();

	// Enums for user's desired task state
	public enum DesiredTaskState {

		START, // User wants the task to start
		STOP, // User wants the task to stop
		PAUSE, // User wants the task to pause
		RESUME, // User wants the task to resume
		CANCEL, // User wants the task to be cancelled
		WAIT // User wants the task to wait for input

	}

	// Constructors
	public RootTaskManagerEntity() {
		this.createdAt = LocalDateTime.now();
		this.lastUpdated = LocalDateTime.now();
		this.desiredTaskState = DesiredTaskState.WAIT;
	}

	public RootTaskManagerEntity(String rootPlanId) {
		this();
		this.rootPlanId = rootPlanId;
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

	public String getRootPlanId() {
		return rootPlanId;
	}

	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	public DesiredTaskState getDesiredTaskState() {
		return desiredTaskState;
	}

	public void setDesiredTaskState(DesiredTaskState desiredTaskState) {
		this.desiredTaskState = desiredTaskState;
	}

	public String getTaskResult() {
		return taskResult;
	}

	public void setTaskResult(String taskResult) {
		this.taskResult = taskResult;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public LocalDateTime getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(LocalDateTime lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public List<UserInputWaitStateEntity> getUserInputWaitStates() {
		return userInputWaitStates;
	}

	public void setUserInputWaitStates(List<UserInputWaitStateEntity> userInputWaitStates) {
		this.userInputWaitStates = userInputWaitStates;
	}

	/**
	 * Add a user input wait state to this root task manager
	 */
	public void addUserInputWaitState(UserInputWaitStateEntity waitState) {
		if (this.userInputWaitStates == null) {
			this.userInputWaitStates = new ArrayList<>();
		}
		this.userInputWaitStates.add(waitState);
		waitState.setRootTaskManager(this);
	}

	/**
	 * Remove a user input wait state from this root task manager
	 */
	public void removeUserInputWaitState(UserInputWaitStateEntity waitState) {
		if (this.userInputWaitStates != null) {
			this.userInputWaitStates.remove(waitState);
			waitState.setRootTaskManager(null);
		}
	}

	// Business logic methods for setting user's desired state

	/**
	 * Set user's desire to start the task
	 */
	public void setUserWantsToStart() {
		this.desiredTaskState = DesiredTaskState.START;
		this.startTime = LocalDateTime.now();
		this.lastUpdated = LocalDateTime.now();
	}

	/**
	 * Set user's desire to stop the task
	 */
	public void setUserWantsToStop() {
		this.desiredTaskState = DesiredTaskState.STOP;
		this.endTime = LocalDateTime.now();
		this.lastUpdated = LocalDateTime.now();
	}

	/**
	 * Set user's desire to pause the task
	 */
	public void setUserWantsToPause() {
		this.desiredTaskState = DesiredTaskState.PAUSE;
		this.lastUpdated = LocalDateTime.now();
	}

	/**
	 * Set user's desire to resume the task
	 */
	public void setUserWantsToResume() {
		this.desiredTaskState = DesiredTaskState.RESUME;
		this.lastUpdated = LocalDateTime.now();
	}

	/**
	 * Set user's desire to cancel the task
	 */
	public void setUserWantsToCancel() {
		this.desiredTaskState = DesiredTaskState.CANCEL;
		this.endTime = LocalDateTime.now();
		this.lastUpdated = LocalDateTime.now();
	}

	/**
	 * Set user's desire to wait for input
	 */
	public void setUserWantsToWait() {
		this.desiredTaskState = DesiredTaskState.WAIT;
		this.lastUpdated = LocalDateTime.now();
	}

	/**
	 * Check if user wants the task to start
	 */
	public boolean userWantsToStart() {
		return this.desiredTaskState == DesiredTaskState.START;
	}

	/**
	 * Check if user wants the task to stop
	 */
	public boolean userWantsToStop() {
		return this.desiredTaskState == DesiredTaskState.STOP;
	}

	/**
	 * Check if user wants the task to pause
	 */
	public boolean userWantsToPause() {
		return this.desiredTaskState == DesiredTaskState.PAUSE;
	}

	/**
	 * Check if user wants the task to resume
	 */
	public boolean userWantsToResume() {
		return this.desiredTaskState == DesiredTaskState.RESUME;
	}

	/**
	 * Check if user wants the task to cancel
	 */
	public boolean userWantsToCancel() {
		return this.desiredTaskState == DesiredTaskState.CANCEL;
	}

	/**
	 * Check if user wants the task to wait
	 */
	public boolean userWantsToWait() {
		return this.desiredTaskState == DesiredTaskState.WAIT;
	}

	@Override
	public String toString() {
		return "RootTaskManagerEntity{" + "id=" + id + ", rootPlanId='" + rootPlanId + '\'' + ", desiredTaskState="
				+ desiredTaskState + ", startTime=" + startTime + ", endTime=" + endTime + ", userInputWaitStatesCount="
				+ (userInputWaitStates != null ? userInputWaitStates.size() : 0) + '}';
	}

}
