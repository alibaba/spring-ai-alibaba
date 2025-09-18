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
package com.alibaba.cloud.ai.manus.tool.jsxGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

@Service
@Primary
public class JsxGeneratorService implements ApplicationRunner, IJsxGeneratorService {

	private static final Logger log = LoggerFactory.getLogger(JsxGeneratorService.class);

	@Autowired
	private ManusProperties manusProperties;

	@Autowired
	private UnifiedDirectoryManager unifiedDirectoryManager;

	private final ObjectMapper objectMapper = new ObjectMapper();

	// Store component states for each plan
	private final ConcurrentHashMap<String, ComponentState> componentStates = new ConcurrentHashMap<>();

	// Store Handlebars templates
	private final Map<String, String> handlebarsTemplates = new ConcurrentHashMap<>();

	// Supported Vue SFC file extensions (for future validation use)
	@SuppressWarnings("unused")
	private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Set.of(".vue", ".js", ".ts"));

	@Override
	public void run(ApplicationArguments args) {
		log.info("JsxGeneratorService initialized");
		initializeDefaultTemplates();
	}

	private void initializeDefaultTemplates() {
		// Initialize default Handlebars templates
		registerTemplate("counter-button", """
				<template>
				  <div class="counter-container">
				    <button :class="buttonClass" @click="increment">
				      {{buttonText}} ({{count}})
				    </button>
				  </div>
				</template>

				<script>
				export default {
				  name: '{{componentName}}',
				  data() {
				    return {
				      count: {{initialCount}}
				    };
				  },
				  computed: {
				    buttonClass() {
				      return {
				        'btn': true,
				        'btn-primary': this.count <= {{threshold}},
				        'btn-danger': this.count > {{threshold}}
				      };
				    }
				  },
				  methods: {
				    increment() {
				      this.count++;
				    }
				  }
				};
				</script>

				<style scoped>
				.counter-container {
				  padding: 20px;
				}
				.btn {
				  padding: 10px 20px;
				  border: none;
				  border-radius: 4px;
				  cursor: pointer;
				  font-size: 16px;
				}
				.btn-primary {
				  background-color: #007bff;
				  color: white;
				}
				.btn-danger {
				  background-color: #dc3545;
				  color: white;
				}
				</style>
				""");

		registerTemplate("data-form", """
				<template>
				  <div class="form-container">
				    <form @submit.prevent="handleSubmit">
				      {{#each fields}}
				      <div class="form-group">
				        <label for="{{name}}">{{label}}</label>
				        <input
				          type="{{type}}"
				          id="{{name}}"
				          v-model="formData.{{name}}"
				          :required="{{required}}"
				        />
				      </div>
				      {{/each}}
				      <button type="submit" class="btn btn-primary">{{submitText}}</button>
				    </form>
				  </div>
				</template>

				<script>
				export default {
				  name: '{{componentName}}',
				  data() {
				    return {
				      formData: {
				        {{#each fields}}
				        {{name}}: '{{defaultValue}}'{{#unless @last}},{{/unless}}
				        {{/each}}
				      }
				    };
				  },
				  methods: {
				    handleSubmit() {
				      console.log('Form submitted:', this.formData);
				      this.$emit('submit', this.formData);
				    }
				  }
				};
				</script>

				<style scoped>
				.form-container {
				  max-width: 400px;
				  margin: 0 auto;
				  padding: 20px;
				}
				.form-group {
				  margin-bottom: 15px;
				}
				label {
				  display: block;
				  margin-bottom: 5px;
				  font-weight: bold;
				}
				input {
				  width: 100%;
				  padding: 8px 12px;
				  border: 1px solid #ddd;
				  border-radius: 4px;
				}
				.btn {
				  padding: 10px 20px;
				  background-color: #007bff;
				  color: white;
				  border: none;
				  border-radius: 4px;
				  cursor: pointer;
				}
				</style>
				""");

		log.info("Default templates initialized: {}", handlebarsTemplates.keySet());
	}

	private Object getComponentLock(String planId) {
		return getComponentState(planId).getComponentLock();
	}

	@Override
	public ComponentState getComponentState(String planId) {
		return componentStates.computeIfAbsent(planId, k -> new ComponentState());
	}

	@Override
	public void closeComponentForPlan(String planId) {
		synchronized (getComponentLock(planId)) {
			componentStates.remove(planId);
			log.info("Closed component state for plan: {}", planId);
		}
	}

	@Override
	public String generateVueSFC(String componentType, Map<String, Object> componentData) {
		log.info("Generating Vue SFC for component: {}", componentType);

		StringBuilder sfcBuilder = new StringBuilder();

		// Generate template section
		String template;
		Object templateObj = componentData.get("template");
		if (templateObj instanceof String) {
			// If template is provided as a string, wrap it in template tags
			template = "<template>\n" + templateObj + "\n</template>";
		}
		else if (templateObj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> templateData = (Map<String, Object>) templateObj;
			template = generateVueTemplate(componentType, templateData);
		}
		else {
			// Use default template generation
			template = generateVueTemplate(componentType, new HashMap<>());
		}
		sfcBuilder.append(template).append("\n\n");

		// Generate script section
		String script = generateVueScript(componentData);
		sfcBuilder.append(script).append("\n\n");

		// Generate style section
		String style;
		Object styleObj = componentData.get("style");
		if (styleObj instanceof String) {
			// If style is provided as a string, wrap it in style tags
			style = "<style scoped>\n" + styleObj + "\n</style>";
		}
		else if (styleObj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> styleData = (Map<String, Object>) styleObj;
			style = generateVueStyle(styleData);
		}
		else {
			// Use default style generation
			style = generateVueStyle(new HashMap<>());
		}
		sfcBuilder.append(style);

		return sfcBuilder.toString();
	}

	@Override
	public String generateVueTemplate(String componentType, Map<String, Object> templateData) {
		StringBuilder template = new StringBuilder();
		template.append("<template>\n");
		template.append("  <div class=\"").append(componentType).append("-container\">\n");

		// Generate template content based on component type
		switch (componentType) {
			case "button", "counter-button" -> {
				String buttonText = (String) templateData.getOrDefault("text", "Click Me");
				String clickAction = (String) templateData.getOrDefault("action", "handleClick");
				template.append("    <button @click=\"").append(clickAction).append("\">\n");
				template.append("      ").append(buttonText).append("\n");
				template.append("    </button>\n");
			}
			case "form", "data-form" -> {
				template.append("    <form @submit.prevent=\"handleSubmit\">\n");
				template.append("      <!-- Form fields will be generated here -->\n");
				template.append("      <button type=\"submit\">Submit</button>\n");
				template.append("    </form>\n");
			}
			default -> template.append("    <!-- Default component content -->\n");
		}

		template.append("  </div>\n");
		template.append("</template>");
		return template.toString();
	}

	@Override
	public String generateVueScript(Map<String, Object> componentData) {
		StringBuilder script = new StringBuilder();
		script.append("<script>\n");
		script.append("export default {\n");

		// Component name
		String name = (String) componentData.getOrDefault("name", "CustomComponent");
		script.append("  name: '").append(name).append("',\n");

		// Data section
		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) componentData.getOrDefault("data", new HashMap<>());
		if (!data.isEmpty()) {
			script.append("  data() {\n");
			script.append("    return {\n");
			for (Map.Entry<String, Object> entry : data.entrySet()) {
				script.append("      ").append(entry.getKey()).append(": ");
				if (entry.getValue() instanceof String) {
					script.append("'").append(entry.getValue()).append("'");
				}
				else {
					script.append(entry.getValue());
				}
				script.append(",\n");
			}
			script.append("    };\n");
			script.append("  },\n");
		}

		// Methods section
		@SuppressWarnings("unchecked")
		Map<String, Object> methods = (Map<String, Object>) componentData.getOrDefault("methods", new HashMap<>());
		if (!methods.isEmpty()) {
			script.append("  methods: {\n");
			for (Map.Entry<String, Object> entry : methods.entrySet()) {
				String methodName = entry.getKey();
				Object methodImpl = entry.getValue();

				script.append("    ").append(methodName).append(": ");
				if (methodImpl instanceof String) {
					String methodStr = (String) methodImpl;
					// Remove 'function' keyword if present and add it properly
					if (methodStr.startsWith("function")) {
						methodStr = methodStr.substring(8).trim(); // Remove 'function'
																	// keyword
						if (methodStr.startsWith("()")) {
							script.append("function").append(methodStr);
						}
						else {
							script.append("function() ").append(methodStr);
						}
					}
					else {
						// Assume it's a function body, wrap it properly
						script.append("function() ").append(methodStr);
					}
				}
				else {
					// Fallback to empty method
					script.append("function() {\n");
					script.append("      // Method implementation\n");
					script.append("    }");
				}
				script.append(",\n");
			}
			script.append("  },\n");
		}

		// Computed properties
		@SuppressWarnings("unchecked")
		Map<String, Object> computed = (Map<String, Object>) componentData.getOrDefault("computed", new HashMap<>());
		if (!computed.isEmpty()) {
			script.append("  computed: {\n");
			for (String computedName : computed.keySet()) {
				script.append("    ").append(computedName).append("() {\n");
				script.append("      // Computed property implementation\n");
				script.append("      return null;\n");
				script.append("    },\n");
			}
			script.append("  }\n");
		}

		script.append("};\n");
		script.append("</script>");
		return script.toString();
	}

	@Override
	public String generateVueStyle(Map<String, Object> styleData) {
		StringBuilder style = new StringBuilder();
		style.append("<style scoped>\n");

		// Default styles
		style.append(".component-container {\n");
		style.append("  padding: 20px;\n");
		style.append("}\n");

		// Custom styles from styleData
		if (styleData != null && !styleData.isEmpty()) {
			for (Map.Entry<String, Object> entry : styleData.entrySet()) {
				String selector = entry.getKey();
				Object rules = entry.getValue();
				style.append(".").append(selector).append(" {\n");
				if (rules instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> ruleMap = (Map<String, Object>) rules;
					for (Map.Entry<String, Object> rule : ruleMap.entrySet()) {
						style.append("  ").append(rule.getKey()).append(": ").append(rule.getValue()).append(";\n");
					}
				}
				style.append("}\n");
			}
		}

		style.append("</style>");
		return style.toString();
	}

	@Override
	public String applyHandlebarsTemplate(String templateName, Map<String, Object> templateData) {
		String template = handlebarsTemplates.get(templateName);
		if (template == null) {
			throw new IllegalArgumentException("Template not found: " + templateName);
		}

		log.info("Applying Handlebars template: {}", templateName);

		// Simple template replacement (in real implementation, use Handlebars library)
		String result = template;
		for (Map.Entry<String, Object> entry : templateData.entrySet()) {
			String placeholder = "{{" + entry.getKey() + "}}";
			result = result.replace(placeholder, String.valueOf(entry.getValue()));
		}

		return result;
	}

	@Override
	public void registerTemplate(String templateName, String templateContent) {
		handlebarsTemplates.put(templateName, templateContent);
		log.info("Registered template: {}", templateName);
	}

	@Override
	public Set<String> getAvailableTemplates() {
		return new HashSet<>(handlebarsTemplates.keySet());
	}

	@Override
	public String saveVueSfcToFile(String planId, String filePath, String vueSfcCode) throws IOException {
		log.info("Saving Vue SFC to file: {}", filePath);

		// Get absolute path
		Path absolutePath = getAbsolutePath(planId, filePath);

		// Ensure parent directory exists
		Files.createDirectories(absolutePath.getParent());

		// Write Vue SFC code to file
		Files.writeString(absolutePath, vueSfcCode);

		// Update component state
		updateComponentState(planId, filePath, "Vue SFC file saved successfully");

		return absolutePath.toString();
	}

	@Override
	public void updateVueComponent(String planId, String filePath, String sectionType, String newContent)
			throws IOException {
		log.info("Updating Vue component section: {} in file: {}", sectionType, filePath);

		Path absolutePath = getAbsolutePath(planId, filePath);
		if (!Files.exists(absolutePath)) {
			throw new IOException("File does not exist: " + filePath);
		}

		String existingContent = Files.readString(absolutePath);
		String updatedContent = updateVueSfcSection(existingContent, sectionType, newContent);

		Files.writeString(absolutePath, updatedContent);
		updateComponentState(planId, filePath, "Updated " + sectionType + " section");
	}

	private String updateVueSfcSection(String sfcContent, String sectionType, String newContent) {
		Pattern pattern = Pattern.compile("(<" + sectionType + "[^>]*>)(.*?)(</" + sectionType + ">)", Pattern.DOTALL);
		Matcher matcher = pattern.matcher(sfcContent);

		if (matcher.find()) {
			return matcher.replaceFirst("$1\n" + newContent + "\n$3");
		}
		else {
			// If section doesn't exist, append it
			return sfcContent + "\n\n<" + sectionType + ">\n" + newContent + "\n</" + sectionType + ">";
		}
	}

	@Override
	public String generateSandpackConfig(String planId, String filePath, Map<String, String> dependencies) {
		try {
			Map<String, Object> config = new HashMap<>();
			config.put("template", "vue");
			config.put("files",
					Map.of("/src/App.vue", Map.of("code", Files.readString(getAbsolutePath(planId, filePath)))));

			if (dependencies != null && !dependencies.isEmpty()) {
				config.put("dependencies", dependencies);
			}

			return objectMapper.writeValueAsString(config);
		}
		catch (Exception e) {
			log.error("Error generating Sandpack config", e);
			return "{}";
		}
	}

	@Override
	public String generatePreviewUrl(String planId, String filePath) {
		// In a real implementation, this would generate a URL to preview in Sandpack
		String absolutePath = getAbsolutePath(planId, filePath).toString();
		String previewUrl = "http://localhost:3000/sandpack?file=" + absolutePath;
		log.info("Generated preview URL: {}", previewUrl);
		return previewUrl;
	}

	@Override
	public String validateVueSfc(String vueSfcCode) {
		// Basic validation - check for required sections
		if (!vueSfcCode.contains("<template>") || !vueSfcCode.contains("</template>")) {
			return "Error: Missing template section";
		}
		if (!vueSfcCode.contains("<script>") || !vueSfcCode.contains("</script>")) {
			return "Error: Missing script section";
		}
		return "Valid Vue SFC";
	}

	@Override
	public void updateComponentState(String planId, String filePath, String operationResult) {
		ComponentState state = getComponentState(planId);
		synchronized (getComponentLock(planId)) {
			state.setCurrentFilePath(filePath);
			state.setLastOperationResult(operationResult);
		}
	}

	@Override
	public String getCurrentFilePath(String planId) {
		return getComponentState(planId).getCurrentFilePath();
	}

	@Override
	public String getLastOperationResult(String planId) {
		return getComponentState(planId).getLastOperationResult();
	}

	@Override
	public ManusProperties getManusProperties() {
		return manusProperties;
	}

	@PreDestroy
	@Override
	public void cleanup() {
		log.info("Cleaning up JsxGeneratorService resources");
		componentStates.clear();
		handlebarsTemplates.clear();
	}

	/**
	 * Get absolute path for a given relative path
	 * @param planId Plan ID
	 * @param filePath File path
	 * @return Absolute Path
	 */
	private Path getAbsolutePath(String planId, String filePath) {
		return unifiedDirectoryManager.getRootPlanDirectory(planId).resolve(filePath);
	}

	/**
	 * Clean up component state for specified plan
	 * @param planId Plan ID
	 */
	public void cleanupPlanComponents(String planId) {
		synchronized (getComponentLock(planId)) {
			try {
				componentStates.remove(planId);
				log.info("Cleaned up component resources for plan: {}", planId);
			}
			catch (Exception e) {
				log.error("Error cleaning up plan components: {}", planId, e);
			}
		}
	}

}
