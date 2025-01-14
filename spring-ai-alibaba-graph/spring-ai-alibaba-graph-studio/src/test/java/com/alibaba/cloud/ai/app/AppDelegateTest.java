package com.alibaba.cloud.ai.app;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.AppMetadata;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.Edge;
import com.alibaba.cloud.ai.model.workflow.Graph;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.Workflow;
import com.alibaba.cloud.ai.model.workflow.nodedata.EndNodeData;
import com.alibaba.cloud.ai.model.workflow.nodedata.StartNodeData;
import com.alibaba.cloud.ai.param.CreateAppParam;
import com.alibaba.cloud.ai.saver.AppSaver;
import com.alibaba.cloud.ai.service.app.AppDelegate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SpringBootTest
@Slf4j
public class AppDelegateTest {

	private final AppDelegate appDelegate;

	private final AppSaver appSaver;

	private App simpleApp;

	@Autowired
	public AppDelegateTest(AppDelegate appDelegate, AppSaver appSaver) {
		this.appDelegate = appDelegate;
		this.appSaver = appSaver;
	}

	@BeforeEach
	void setupApp() {
		appSaver.list().forEach(app -> appSaver.delete(app.id()));
		AppMetadata appMetadata = new AppMetadata();
		appMetadata.setId("app-delegate-test")
			.setName("app-delegate-test")
			.setDescription("app-delegate-test")
			.setMode(AppMetadata.CHATBOT_MODE);
		Workflow workflow = new Workflow();
		Node startNode = new Node().setId(UUID.randomUUID().toString())
			.setData(new StartNodeData(List.of(), List.of())
				.setStartInputs(List.of(new StartNodeData.StartInput().setLabel("userQuery"))));
		Node endNode = new Node().setId(UUID.randomUUID().toString())
			.setData(new EndNodeData(List.of(), List.of(new Variable("output", VariableType.STRING.value()))));
		Edge edge = new Edge().setId(UUID.randomUUID().toString())
			.setSource(startNode.getId())
			.setSourceHandle("source")
			.setTarget(endNode.id())
			.setTargetHandle("target");
		workflow.setGraph(new Graph(List.of(edge), List.of(startNode, endNode)));
		simpleApp = appSaver.save(new App(appMetadata, workflow));
	}

	@Test
	void testCreate() {
		CreateAppParam createAppParam = new CreateAppParam().setName("AppCreateTest")
			.setDescription("AppCreateTest")
			.setMode(AppMetadata.CHATBOT_MODE);
		App app = appDelegate.create(createAppParam);
		assert app != null;
		App appSaved = appSaver.get(app.id());
		assert appSaved != null;
		assert Objects.equals(app.id(), appSaved.id());
		log.info("app created: " + app);
	}

	@Test
	void testGet() {
		App app = appDelegate.get("app-delegate-test");
		assert app != null;
		assert Objects.equals(app.id(), simpleApp.id());
		log.info("app get: " + app);
	}

	@Test
	void testList() {
		List<App> apps = appDelegate.list();
		assert apps != null;
		assert apps.size() == 1;
		assert Objects.equals(apps.get(0).id(), simpleApp.id());
		log.info("list apps: " + apps);
	}

	@Test
	void testSync() {
		App app = appDelegate.get("app-delegate-test");
		assert app != null;
		app.getMetadata().setDescription("description-modified");
		app = appDelegate.sync(app);
		assert Objects.equals(app.getMetadata().getDescription(), "description-modified");
		log.info("app synced" + app);
	}

	@Test
	void testDelete() {
		appDelegate.delete("app-delegate-test");
		assert appSaver.list().isEmpty();
		log.info("app deleted");
	}

}
