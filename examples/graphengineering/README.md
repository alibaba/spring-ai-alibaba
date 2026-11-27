# Graph Engineering Examples

This module collects runnable Graph Engineering examples for Spring AI Alibaba.

## Examples

| Example | Main class | Requires API key | Purpose |
| --- | --- | --- | --- |
| AgentScope RepoOps issue | `AgentScopeRepoOpsIssueGraphExample` | Yes | Read a local issue JSON file or GitHub issue URL, then run RepoOps triage/planning/review/audit |

## Startup Classes

| Startup class | Input | Output |
| --- | --- | --- |
| `com.alibaba.cloud.ai.examples.graphengineering.AgentScopeRepoOpsIssueGraphExample` | Local issue JSON file or GitHub issue URL plus `AI_DASHSCOPE_API_KEY` | API-backed RepoOps issue triage, plan, review, and audit |

## Which One To Run

| Startup class | Use it when | What it proves | What it does not prove |
| --- | --- | --- | --- |
| `AgentScopeRepoOpsIssueGraphExample` | You want the closest practical RepoOps demo | Graph reads a local issue JSON file or GitHub issue URL, normalizes issue state, routes by issue type, and invokes AgentScope nodes for triage, planning, review, and audit | It does not post comments, create PRs, run CI, or mutate a repository |

## Recommended Demo Order

Run `AgentScopeRepoOpsIssueGraphExample` with a GitHub issue URL when presenting
RepoOps. It is the practical demo because it starts from real issue intake and
then executes real AgentScope model calls under Spring AI Alibaba Graph control.

For offline explanation without an API key, use the diagrams and state contract
in `docs/graph-engineering-repoops-lightweight.md` instead of maintaining a
separate deterministic startup class.

## Current Scope

These examples intentionally stop at analysis and audit output. They do not make
external write actions such as GitHub comments, branch creation, pull requests,
CI reruns, releases, deployments, or rollbacks.

Those write actions should be added as separate tool nodes behind explicit
approval gates, for example:

```text
agentscope_reviewer
  -> approval_gate
  -> github_comment_tool
  -> ci_rerun_tool
  -> release_guardian
  -> auditor
```

This keeps the example safe to run while still showing the Graph Engineering
control plane: topology, issue intake, state normalization, route policy,
specialized workers, verifier decisions, node trace, route history, and evidence
output.

## Graph Engineering Contract

The executable graph has an explicit topology contract in
`src/main/resources/repoops-graph-topology.yaml`. The Java code and this
contract describe the same graph:

| Node | Type | Graph Engineering role |
| --- | --- | --- |
| `initialize_run` | deterministic initializer | Creates `graph_id` and `run_id` before business work starts |
| `read_issue` | deterministic intake | Normalizes classpath JSON, local JSON, or GitHub issue URL into graph state |
| `agentscope_triager` | AgentScope agentic worker | Produces triage result from normalized issue state |
| `route_issue` | deterministic router | Converts triager `route_candidate` into auditable `route_decision` and `route_history` |
| `agentscope_bug_diagnoser` | AgentScope agentic worker | Produces bug diagnosis and regression-test plan |
| `agentscope_feature_planner` | AgentScope agentic worker | Produces feature plan and acceptance criteria |
| `agentscope_question_responder` | AgentScope agentic worker | Answers questions without entering implementation workflow |
| `record_question_evidence` | deterministic evidence node | Records question bypass evidence and review outcome |
| `agentscope_implementer` | AgentScope agentic worker | Produces reviewable implementation proposal |
| `agentscope_reviewer` | AgentScope verifier agent | Reviews another node's proposal and writes `review_decision` |
| `review_gate` | deterministic verifier/router | Converts reviewer output into `review_gate_result` and controls loop-back |
| `agentscope_auditor` | AgentScope audit agent | Produces final lifecycle report from state and evidence |

The state contract includes run identity and graph observability keys:

| State key | Strategy | Purpose |
| --- | --- | --- |
| `graph_id` | replace | Stable graph identity for trace correlation |
| `run_id` | replace | Unique workflow run identity |
| `current_node` | replace | Last completed graph node |
| `route_decision` | replace | Issue route selected from `triage_result`, with `issue_type` as fallback |
| `review_gate_result` | replace | Pass/fail result selected by `review_gate` |
| `node_trace` | append | Per-node execution metadata: node id, type, inputs, outputs, timestamp |
| `route_history` | append | Auditable route decisions and reasons |
| `evidence_bundle` | append | Operational evidence collected across nodes |

## Implemented Graph Flow

The current startup class implements this graph:

```text
START
  -> initialize_run
  -> read_issue
  -> agentscope_triager
  -> route_issue
  -> bug: agentscope_bug_diagnoser -> agentscope_implementer -> agentscope_reviewer -> review_gate
  -> feature: agentscope_feature_planner -> agentscope_implementer -> agentscope_reviewer -> review_gate
  -> question: agentscope_question_responder -> record_question_evidence

review_gate
  -> PASS: agentscope_auditor
  -> FAIL: agentscope_implementer

record_question_evidence
  -> agentscope_auditor

agentscope_auditor
  -> END
```

```mermaid
flowchart LR
    start["START"]
    init["initialize_run<br/>graph_id + run_id"]
    read["read_issue<br/>file or GitHub URL"]
    triage["agentscope_triager<br/>classify and normalize"]
    route["route_issue<br/>route_decision"]
    bug["agentscope_bug_diagnoser"]
    feature["agentscope_feature_planner"]
    question["agentscope_question_responder"]
    evidence["record_question_evidence"]
    implement["agentscope_implementer"]
    review["agentscope_reviewer<br/>review_decision"]
    gate{"review_gate"}
    audit["agentscope_auditor<br/>audit_report"]
    end["END"]

    start --> init
    init --> read
    read --> triage
    triage --> route
    route -->|bug| bug
    route -->|feature| feature
    route -->|question| question
    bug --> implement
    feature --> implement
    question --> evidence
    evidence --> audit
    implement --> review
    review --> gate
    gate -->|PASS| audit
    gate -->|FAIL| implement
    audit --> end

    classDef business fill:#E99151,color:#FFFFFF,stroke:none,rx:10,ry:10
    classDef gateway fill:#7B68EE,color:#FFFFFF,stroke:none,rx:10,ry:10
    classDef warning fill:#F39C12,color:#FFFFFF,stroke:none,rx:10,ry:10
    classDef success fill:#4CA497,color:#FFFFFF,stroke:none,rx:10,ry:10

    class init,read,triage,bug,feature,question,evidence,implement,review business
    class route,gate warning
    class audit success
    class start,end gateway

    linkStyle default stroke-width:2px,stroke:#333333,opacity:0.8
```

## Build

```bash
cd examples/graphengineering
../../mvnw -DskipTests compile
```

## Validate Topology Contract

```bash
../../mvnw -DskipTests \
  -Dexec.mainClass=com.alibaba.cloud.ai.examples.graphengineering.AgentScopeRepoOpsIssueGraphExample \
  -Dexec.args=--validate-topology \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

## Run With DashScope API Key

```bash
export AI_DASHSCOPE_API_KEY=your-api-key

../../mvnw -DskipTests \
  -Dexec.mainClass=com.alibaba.cloud.ai.examples.graphengineering.AgentScopeRepoOpsIssueGraphExample \
  -Dexec.args=https://github.com/alibaba/spring-ai-alibaba/issues/4830 \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

`AgentScopeRepoOpsIssueGraphExample` also accepts a local issue JSON file:

```bash
../../mvnw -DskipTests \
  -Dexec.mainClass=com.alibaba.cloud.ai.examples.graphengineering.AgentScopeRepoOpsIssueGraphExample \
  -Dexec.args=src/main/resources/issues/bug_issue.json \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

When no argument is provided, the example uses the bundled
`classpath:issues/bug_issue.json` sample.
