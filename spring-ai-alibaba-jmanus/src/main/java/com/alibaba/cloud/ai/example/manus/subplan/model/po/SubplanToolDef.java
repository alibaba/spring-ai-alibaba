package com.alibaba.cloud.ai.example.manus.subplan.model.po;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SubplanToolDef - Subplan Tool Definition
 * 
 * Represents a tool definition that can trigger subplan execution
 */
@Entity
@Table(name = "subplan_tool_def")
public class SubplanToolDef {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tool_name", nullable = false, unique = true, length = 255)
    private String toolName;
    
    @Column(name = "tool_description", columnDefinition = "TEXT")
    private String toolDescription;
    
    @OneToMany(mappedBy = "toolDef", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SubplanParamDef> inputSchema = new ArrayList<>();
    
    @Column(name = "plan_template_id", nullable = false, length = 255)
    private String planTemplateId;
    
    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;
    
    @Column(name = "service_group", nullable = false, length = 100)
    private String serviceGroup;
    
    // Constructor
    public SubplanToolDef() {
        this.serviceGroup = "subplan-tools"; // Default service group
    }
    
    public SubplanToolDef(Long id) {
        this.id = id;
        this.serviceGroup = "subplan-tools"; // Default service group
    }
    
    // Helper methods for managing the relationship
    public void addParameter(SubplanParamDef parameter) {
        inputSchema.add(parameter);
        parameter.setToolDef(this);
    }
    
    public void removeParameter(SubplanParamDef parameter) {
        inputSchema.remove(parameter);
        parameter.setToolDef(null);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    
    public String getToolDescription() {
        return toolDescription;
    }
    
    public void setToolDescription(String toolDescription) {
        this.toolDescription = toolDescription;
    }
    
    public List<SubplanParamDef> getInputSchema() {
        return inputSchema;
    }
    
    public void setInputSchema(List<SubplanParamDef> inputSchema) {
        this.inputSchema.clear();
        if (inputSchema != null) {
            inputSchema.forEach(this::addParameter);
        }
    }
    
    public String getPlanTemplateId() {
        return planTemplateId;
    }
    
    public void setPlanTemplateId(String planTemplateId) {
        this.planTemplateId = planTemplateId;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getServiceGroup() {
        return serviceGroup;
    }
    
    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }
    
    @Override
    public String toString() {
        return "SubplanToolDef{" +
                "id=" + id +
                ", toolName='" + toolName + '\'' +
                ", toolDescription='" + toolDescription + '\'' +
                ", planTemplateId='" + planTemplateId + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", serviceGroup='" + serviceGroup + '\'' +
                ", inputSchemaSize=" + (inputSchema != null ? inputSchema.size() : 0) +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SubplanToolDef that = (SubplanToolDef) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
