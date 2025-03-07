package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.AppMetadata;
import com.alibaba.cloud.ai.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.model.workflow.Workflow;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AbstractDSL defines the steps of importing and exporting DSL.
 */
public abstract class AbstractDSLAdapter implements DSLAdapter {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractDSLAdapter.class);

	@Override
	public App importDSL(String dsl) {
		log.info("dsl importing: {}", dsl);
		Map<String, Object> data = getSerializer().load(dsl);
		validateDSLData(data);
		Map<String, Object> immutableData = Map.copyOf(data);
		AppMetadata metadata = mapToMetadata(immutableData);
		Object spec = switch (metadata.getMode()) {
			case AppMetadata.WORKFLOW_MODE -> mapToWorkflow(immutableData);
			case AppMetadata.CHATBOT_MODE -> mapToChatBot(immutableData);
			default -> throw new IllegalArgumentException("unsupported mode: " + metadata.getMode());
		};
		App app = new App(metadata, spec);
		log.info("App imported:" + app);
		return app;
	}

	@Override
	public String exportDSL(App app) {
		log.info("App exporting: \n" + app);
		AppMetadata metadata = app.getMetadata();
		Map<String, Object> metaMap = metadataToMap(metadata);
		Map<String, Object> specMap;
		switch (metadata.getMode()) {
			case AppMetadata.WORKFLOW_MODE -> specMap = workflowToMap((Workflow) app.getSpec());
			case AppMetadata.CHATBOT_MODE -> specMap = chatbotToMap((ChatBot) app.getSpec());
			default -> throw new IllegalArgumentException("unsupported mode: " + metadata.getMode());
		}
		Map<String, Object> data = Stream.concat(metaMap.entrySet().stream(), specMap.entrySet().stream())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
		String dsl = getSerializer().dump(data);
		log.info("App exported: \n" + dsl);
		return dsl;
	}

	public abstract AppMetadata mapToMetadata(Map<String, Object> data);

	public abstract Map<String, Object> metadataToMap(AppMetadata metadata);

	public abstract Workflow mapToWorkflow(Map<String, Object> data);

	public abstract Map<String, Object> workflowToMap(Workflow workflow);

	public abstract ChatBot mapToChatBot(Map<String, Object> data);

	public abstract Map<String, Object> chatbotToMap(ChatBot chatBot);

	public abstract void validateDSLData(Map<String, Object> data);

	public abstract Serializer getSerializer();

}
