package com.alibaba.cloud.ai.studio.core.observability.controller;

import com.alibaba.cloud.ai.studio.core.observability.exception.GraphFlowNotFoundException;
import com.alibaba.cloud.ai.studio.core.observability.service.GraphFlowService;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import com.alibaba.cloud.ai.studio.core.observability.config.SAAGraphFlowRegistry;
import com.alibaba.cloud.ai.studio.core.observability.dto.SAAGraphFlowInfoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Observability", description = "APIs for system observability, including graph flows.")
@RequestMapping("/observability/v1/flows")
public class GraphFlowController {

	private final SAAGraphFlowRegistry graphFlowRegistry;

	private final GraphFlowService graphFlowService;

	public GraphFlowController(SAAGraphFlowRegistry graphFlowRegistry, GraphFlowService graphFlowService) {
		this.graphFlowRegistry = graphFlowRegistry;
		this.graphFlowService = graphFlowService;
	}

	@GetMapping("")
	@Operation(summary = "Get Flows by Owner", description = "Retrieves a list of all graph flows owned by a specific user.")
	public List<SAAGraphFlowInfoDTO> getFlowsByOwner(
			@Parameter(description = "The unique identifier of the owner.", required = true,
					example = "saa") @RequestParam("ownerID") String ownerID) {

		List<SAAGraphFlow> userFlows = graphFlowRegistry.findByOwnerID(ownerID);

		return userFlows.stream().map(graphFlowService::convertToDTO).collect(Collectors.toList());
	}

	@GetMapping("/{flowId}")
	@Operation(summary = "Get Flow by ID", description = "Retrieves a specific graph flow by its unique identifier.")
	public SAAGraphFlowInfoDTO getFlowById(@Parameter(description = "The unique identifier of the flow.",
			required = true, example = "test") @PathVariable String flowId) {

		SAAGraphFlowInfoDTO flow = graphFlowService.findFlowById(flowId);
		if (flow == null) {
			throw new GraphFlowNotFoundException(flowId);
		}

		return flow;
	}

}
