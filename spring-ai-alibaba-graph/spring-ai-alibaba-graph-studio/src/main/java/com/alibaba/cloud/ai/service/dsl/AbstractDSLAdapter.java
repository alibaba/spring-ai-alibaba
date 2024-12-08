package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.AppMetadata;
import com.alibaba.cloud.ai.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.model.workflow.Workflow;
import com.alibaba.cloud.ai.saver.AppSaver;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDSLAdapter implements DSLAdapter {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractDSLAdapter.class);

	@Override
	public App importDSL(String dsl) {
		log.info("Yaml imported: {}", dsl);
		Yaml yaml = new Yaml();
		Map<String, Object> data = yaml.load(dsl);
		validateDSLData(data);
		AppMetadata metadata = mapToMetadata(data);
		Object spec = switch (metadata.getMode()) {
			case AppMetadata.WORKFLOW_MODE -> mapToWorkflow((Map<String, Object>) data.get("workflow"));
			case AppMetadata.CHATBOT_MODE -> mapToChatBot((Map<String, Object>) data.get("model_config"));
			default -> null;
		};
		return new App(metadata, spec);
	}

	@Override
	public App importDSL(String dsl, AppSaver appSaver) {
		App app = this.importDSL(dsl);
		appSaver.create(app);
		log.info("App saved: {}", app);
		return app;
	}

	@Override
	public String exportDSL(String id, AppSaver appSaver) {
		App app = appSaver.get(id);
		if (appSaver.get(id) == null) {
			throw new IllegalArgumentException("App not found: " + id);
		}
		log.info("App exporting: \n" + app);
		Map<String, Object> data = new HashMap<>();
		AppMetadata metadata = app.getMetadata();
		String difyMode = metadata.getMode().equals(AppMetadata.WORKFLOW_MODE) ? "workflow" : "agent-chat";
		data.put("app", Map.of("name", metadata.getName(), "description", metadata.getDescription(), "mode", difyMode));
		data.put("kind", "app");
		switch (metadata.getMode()) {
			case AppMetadata.WORKFLOW_MODE -> data.put("workflow", workflowToMap((Workflow) app.getSpec()));
			case AppMetadata.CHATBOT_MODE -> data.put("model_config", chatbotToMap((ChatBot) app.getSpec()));
			default -> throw new IllegalArgumentException("unsupported mode: " + metadata.getMode());
		}
		// configure DumperOptions to force block style
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		Yaml yaml = new Yaml(options);
		String dsl = yaml.dump(data);
		log.info("App exported: \n" + dsl);
		return dsl;
	}

	public abstract AppMetadata mapToMetadata(Map<String, Object> data);

	public abstract Workflow mapToWorkflow(Map<String, Object> data);

	public abstract ChatBot mapToChatBot(Map<String, Object> data);

	public abstract Map<String, Object> workflowToMap(Workflow workflow);

	public abstract Map<String, Object> chatbotToMap(ChatBot chatBot);

	public abstract void validateDSLData(Map<String, Object> data);

}
