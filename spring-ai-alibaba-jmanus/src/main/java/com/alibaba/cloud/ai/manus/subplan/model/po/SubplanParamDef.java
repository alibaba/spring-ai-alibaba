package com.alibaba.cloud.ai.manus.subplan.model.po;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * SubplanParamDef - Subplan Parameter Definition
 *
 * Represents a parameter definition for subplan tools
 */
@Entity
@Table(name = "subplan_param_def")
public class SubplanParamDef {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 255)
	private String name; // Parameter name

	@Column(name = "type", nullable = false, length = 50)
	private String type; // Parameter type, default "String"

	@Column(name = "description", columnDefinition = "TEXT")
	private String description; // Parameter description

	@Column(name = "required", nullable = false)
	private boolean required; // Whether required, default true

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tool_def_id", nullable = false)
	private SubplanToolDef toolDef;

	// Constructor
	public SubplanParamDef() {
		this.type = "String";
		this.required = true;
	}

	public SubplanParamDef(String name, String type, String description, boolean required) {
		this.name = name;
		this.type = type != null ? type : "String";
		this.description = description;
		this.required = required;
	}

	public SubplanParamDef(String name, String description) {
		this.name = name;
		this.type = "String";
		this.description = description;
		this.required = true;
	}

	// Getter and Setter methods
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type != null ? type : "String";
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public SubplanToolDef getToolDef() {
		return toolDef;
	}

	public void setToolDef(SubplanToolDef toolDef) {
		this.toolDef = toolDef;
	}

	@Override
	public String toString() {
		return "SubplanParamDef{" + "id=" + id + ", name='" + name + '\'' + ", type='" + type + '\'' + ", description='"
				+ description + '\'' + ", required=" + required + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SubplanParamDef that = (SubplanParamDef) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
