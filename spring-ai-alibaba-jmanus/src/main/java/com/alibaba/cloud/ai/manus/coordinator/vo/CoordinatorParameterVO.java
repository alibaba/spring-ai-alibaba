package com.alibaba.cloud.ai.manus.coordinator.vo;

/**
 * CoordinatorParameterVO Parameter Object
 */
public class CoordinatorParameterVO {

	private String name; // Parameter name

	private String type; // Parameter type, default "String"

	private String description; // Parameter description

	private boolean required; // Whether required, default true

	// Constructor
	public CoordinatorParameterVO() {
		this.type = "String";
		this.required = true;
	}

	public CoordinatorParameterVO(String name, String type, String description, boolean required) {
		this.name = name;
		this.type = type != null ? type : "String";
		this.description = description;
		this.required = required;
	}

	public CoordinatorParameterVO(String name, String description) {
		this.name = name;
		this.type = "String";
		this.description = description;
		this.required = true;
	}

	// Getter and Setter methods
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

	@Override
	public String toString() {
		return "CoordinatorParameterVO{" + "name='" + name + '\'' + ", type='" + type + '\'' + ", description='"
				+ description + '\'' + ", required=" + required + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CoordinatorParameterVO that = (CoordinatorParameterVO) o;
		return name != null ? name.equals(that.name) : that.name == null;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

}