# How to create a custom checkpointer using Postgres

When creating LangGraph.js agents, you can also set them up so that they persist
their state. This allows you to do things like interact with an agent multiple
times and have it remember previous interactions.

This example shows how to use `Postgres` as the backend for persisting
checkpoint state.

NOTE: this is just an example implementation. You can implement your own
checkpointer using a different database or modify this one as long as it
conforms to the `BaseCheckpointSaver` interface.

### Checkpointer implementation


```typescript
import {
  BaseCheckpointSaver,
  Checkpoint,
  CheckpointMetadata,
  CheckpointTuple,
  SerializerProtocol,
} from "@langchain/langgraph";
import { load } from "@langchain/core/load";
```


```typescript
// define custom serializer, since we'll be using bytea Postgres type for `checkpoint` and `metadata` values
const CustomSerializer = {
  stringify(obj) {
    return Buffer.from(JSON.stringify(obj));
  },

  async parse(data) {
    return await load(data.toString());
  },
};
```


```typescript
import { Pool } from "pg";
import { RunnableConfig } from "@langchain/core/runnables";

// snake_case is used to match Python implementation
interface Row {
  checkpoint: string;
  metadata: string;
  parent_id?: string;
  thread_id: string;
  checkpoint_id: string;
}

// define Postgres checkpointer
class PostgresSaver extends BaseCheckpointSaver {
  private pool: Pool;
  private isSetup: boolean;

  constructor(pool: Pool) {
    // @ts-ignore
    super(CustomSerializer);
    this.pool = pool;
    this.isSetup = false;
  }

  static fromConnString(connString: string): PostgresSaver {
    return new PostgresSaver(new Pool({ connectionString: connString }));
  }

  private async setup(): Promise<void> {
    if (this.isSetup) return;

    const client = await this.pool.connect();
    try {
      await client.query(`
CREATE TABLE IF NOT EXISTS checkpoints (
  thread_id TEXT NOT NULL,
  checkpoint_id TEXT NOT NULL,
  parent_id TEXT,
  checkpoint BYTEA NOT NULL,
  metadata BYTEA NOT NULL,
  PRIMARY KEY (thread_id, checkpoint_id)
);
      `);
      this.isSetup = true;
    } catch (error) {
      console.error("Error creating checkpoints table", error);
      throw error;
    } finally {
      client.release();
    }
  }

  // below 3 methods are necessary for any checkpointer implementation: getTuple, list and put
  async getTuple(config: RunnableConfig): Promise<CheckpointTuple | undefined> {
    await this.setup();
    const { thread_id, checkpoint_id } = config.configurable || {};

    const client = await this.pool.connect();
    try {
      if (checkpoint_id) {
        const res = await client.query<Row>(
          `SELECT checkpoint, parent_id, metadata FROM checkpoints WHERE thread_id = $1 AND checkpoint_id = $2`,
          [thread_id, checkpoint_id],
        );
        const row = res.rows[0];
        if (row) {
          return {
            config,
            checkpoint: (await this.serde.parse(row.checkpoint)) as Checkpoint,
            metadata: (await this.serde.parse(
              row.metadata,
            )) as CheckpointMetadata,
            parentConfig: row.parent_id
              ? {
                configurable: {
                  thread_id,
                  checkpoint_id: row.parent_id,
                },
              }
              : undefined,
          };
        }
      } else {
        const res = await client.query<Row>(
          `SELECT thread_id, checkpoint_id, parent_id, checkpoint, metadata FROM checkpoints WHERE thread_id = $1 ORDER BY checkpoint_id DESC LIMIT 1`,
          [thread_id],
        );
        const row = res.rows[0];
        if (row) {
          return {
            config: {
              configurable: {
                thread_id: row.thread_id,
                checkpoint_id: row.checkpoint_id,
              },
            },
            checkpoint: (await this.serde.parse(row.checkpoint)) as Checkpoint,
            metadata: (await this.serde.parse(
              row.metadata,
            )) as CheckpointMetadata,
            parentConfig: row.parent_id
              ? {
                configurable: {
                  thread_id: row.thread_id,
                  checkpoint_id: row.parent_id,
                },
              }
              : undefined,
          };
        }
      }
    } catch (error) {
      console.error("Error retrieving checkpoint", error);
      throw error;
    } finally {
      client.release();
    }

    return undefined;
  }

  async *list(
    config: RunnableConfig,
    limit?: number,
    before?: RunnableConfig,
  ): AsyncGenerator<CheckpointTuple> {
    await this.setup();
    const { thread_id } = config.configurable || {};
    let query =
      `SELECT thread_id, checkpoint_id, parent_id, checkpoint, metadata FROM checkpoints WHERE thread_id = $1`;
    const params: (string | number)[] = [thread_id];
    if (before?.configurable?.checkpoint_id) {
      query += " AND checkpoint_id < $2";
      params.push(before.configurable.checkpoint_id);
    }
    query += " ORDER BY checkpoint_id DESC";
    if (limit) {
      query += " LIMIT $" + (params.length + 1);
      params.push(limit);
    }

    const client = await this.pool.connect();
    try {
      const res = await client.query<Row>(query, params);
      for (const row of res.rows) {
        yield {
          config: {
            configurable: {
              thread_id: row.thread_id,
              checkpoint_id: row.checkpoint_id,
            },
          },
          checkpoint: (await this.serde.parse(row.checkpoint)) as Checkpoint,
          metadata: (await this.serde.parse(
            row.metadata,
          )) as CheckpointMetadata,
          parentConfig: row.parent_id
            ? {
              configurable: {
                thread_id: row.thread_id,
                checkpoint_id: row.parent_id,
              },
            }
            : undefined,
        };
      }
    } catch (error) {
      console.error("Error listing checkpoints", error);
      throw error;
    } finally {
      client.release();
    }
  }

  async put(
    config: RunnableConfig,
    checkpoint: Checkpoint,
    metadata: CheckpointMetadata,
  ): Promise<RunnableConfig> {
    await this.setup();
    const client = await this.pool.connect();
    try {
      await client.query(
        `INSERT INTO checkpoints (thread_id, checkpoint_id, parent_id, checkpoint, metadata) VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (thread_id, checkpoint_id) DO UPDATE SET checkpoint = EXCLUDED.checkpoint, metadata = EXCLUDED.metadata`,
        [
          config.configurable?.thread_id,
          checkpoint.id,
          config.configurable?.checkpoint_id,
          this.serde.stringify(checkpoint),
          this.serde.stringify(metadata),
        ],
      );
    } catch (error) {
      console.error("Error saving checkpoint", error);
      throw error;
    } finally {
      client.release();
    }

    return {
      configurable: {
        thread_id: config.configurable?.thread_id,
        checkpoint_id: checkpoint.id,
      },
    };
  }
}
```

---

Now we're ready to test the Postgres checkpointer with a graph. Let's define a
simple ReAct agent in LangGraph.

## Setup environment


```typescript
// process.env.OPENAI_API_KEY = "sk-...";
```

## Define the state

The state is the interface for all of the nodes in our graph.



```typescript
import { Annotation } from "@langchain/langgraph";
import { BaseMessage } from "@langchain/core/messages";

const AgentState = Annotation.Root({
  messages: Annotation<BaseMessage[]>({
    reducer: (x, y) => x.concat(y),
  }),
});
```

## Set up the tools

We will first define the tools we want to use. For this simple example, we will
use create a placeholder search engine. However, it is really easy to create
your own tools - see documentation
[here](https://js.langchain.com/v0.2/docs/how_to/custom_tools) on how to do
that.



```typescript
import { DynamicStructuredTool } from "@langchain/core/tools";
import { z } from "zod";

const searchTool = new DynamicStructuredTool({
  name: "search",
  description:
    "Use to surf the web, fetch current information, check the weather, and retrieve other information.",
  schema: z.object({
    query: z.string().describe("The query to use in your search."),
  }),
  func: async ({ query: _query }: { query: string }) => {
    // This is a placeholder for the actual implementation
    return "Cold, with a low of 3℃";
  },
});

await searchTool.invoke({ query: "What's the weather like?" });

const tools = [searchTool];
```

We can now wrap these tools in a simple
[ToolNode](/langgraphjs/reference/classes/langgraph_prebuilt.ToolNode.html).
This object will actually run the tools (functions) whenever they are invoked by
our LLM.



```typescript
import { ToolNode } from "@langchain/langgraph/prebuilt";

const toolNode = new ToolNode<typeof AgentState.State>(tools);
```

## Set up the model

Now we will load the
[chat model](https://js.langchain.com/v0.2/docs/concepts/#chat-models).

1. It should work with messages. We will represent all agent state in the form
   of messages, so it needs to be able to work well with them.
2. It should work with
   [tool calling](https://js.langchain.com/v0.2/docs/how_to/tool_calling/#passing-tools-to-llms),
   meaning it can return function arguments in its response.

<div class="admonition tip">
    <p class="admonition-title">Note</p>
    <p>
        These model requirements are not general requirements for using LangGraph - they are just requirements for this one example.
    </p>
</div>


```typescript
import { ChatOpenAI } from "@langchain/openai";
const model = new ChatOpenAI({ model: "gpt-4o" });
```

After we've done this, we should make sure the model knows that it has these
tools available to call. We can do this by calling
[bindTools](https://v01.api.js.langchain.com/classes/langchain_core_language_models_chat_models.BaseChatModel.html#bindTools).


```typescript
const boundModel = model.bindTools(tools);
```

## Define the graph

We can now put it all together.


```typescript
import { END, START, StateGraph } from "@langchain/langgraph";
import { AIMessage } from "@langchain/core/messages";
import { RunnableConfig } from "@langchain/core/runnables";

const routeMessage = (state: typeof AgentState.State) => {
  const { messages } = state;
  const lastMessage = messages[messages.length - 1] as AIMessage;
  // If no tools are called, we can finish (respond to the user)
  if (!lastMessage?.tool_calls?.length) {
    return END;
  }
  // Otherwise if there is, we continue and call the tools
  return "tools";
};

const callModel = async (
  state: typeof AgentState.State,
  config?: RunnableConfig,
) => {
  // For versions of @langchain/core < 0.2.3, you must call `.stream()`
  // and aggregate the message from chunks instead of calling `.invoke()`.
  const { messages } = state;
  const responseMessage = await boundModel.invoke(messages, config);
  return { messages: [responseMessage] };
};

const workflow = new StateGraph(AgentState)
  .addNode("agent", callModel)
  .addNode("tools", toolNode)
  .addEdge(START, "agent")
  .addConditionalEdges("agent", routeMessage)
  .addEdge("tools", "agent");
```


```typescript
// Initialize our Postgres connection pool & checkpointer
const pool = new Pool({
  connectionString:
    "postgresql://postgres:postgres@localhost:5432/postgres?sslmode=disable",
});
const checkpointer = new PostgresSaver(pool);
```


```typescript
// Compile the graph with Postgres checkpointer
const graph = workflow.compile({ checkpointer: checkpointer });
```

## Run the graph with checkpointer


```typescript
// note: we're invoking the graph with a config that contains thread ID
const config = { configurable: { thread_id: 42 } };
const res = await graph.invoke(
  { "messages": [["user", "what's the weather in sf"]] },
  config,
);
```

### Load checkpoint


```typescript
const checkpointTuple = await checkpointer.getTuple(config);
```


```typescript
checkpointTuple;
```

    {
      config: {
        configurable: {
          thread_id: [32m'42'[39m,
          checkpoint_id: [32m'1ef3fb9c-599d-6ef1-8003-88ab826dad68'[39m
        }
      },
      checkpoint: {
        v: [33m1[39m,
        id: [32m'1ef3fb9c-599d-6ef1-8003-88ab826dad68'[39m,
        ts: [32m'2024-07-11T19:14:18.847Z'[39m,
        channel_values: { messages: [36m[Array][39m, agent: [32m'agent'[39m },
        channel_versions: {
          __start__: [33m1[39m,
          messages: [33m5[39m,
          [32m'start:agent'[39m: [33m2[39m,
          agent: [33m5[39m,
          [32m'branch:agent:routeMessage:tools'[39m: [33m3[39m,
          tools: [33m4[39m
        },
        versions_seen: { __start__: [36m[Object][39m, agent: [36m[Object][39m, tools: [36m[Object][39m }
      },
      metadata: { source: [32m'loop'[39m, step: [33m3[39m, writes: { agent: [36m[Object][39m } },
      parentConfig: {
        configurable: {
          thread_id: [32m'42'[39m,
          checkpoint_id: [32m'1ef3fb9c-50a9-6740-8002-d0dcfbbf0749'[39m
        }
      }
    }



```typescript
checkpointTuple.checkpoint.channel_values["messages"];
```

    [
      [32m"what's the weather in sf"[39m,
      AIMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          content: [32m''[39m,
          tool_calls: [36m[Array][39m,
          invalid_tool_calls: [],
          additional_kwargs: [36m[Object][39m,
          response_metadata: [36m[Object][39m
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m''[39m,
        name: [90mundefined[39m,
        additional_kwargs: { tool_calls: [36m[Array][39m },
        response_metadata: { tokenUsage: [36m[Object][39m, finish_reason: [32m'tool_calls'[39m },
        id: [90mundefined[39m,
        tool_calls: [ [36m[Object][39m ],
        invalid_tool_calls: [],
        usage_metadata: [90mundefined[39m
      },
      ToolMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          name: [32m'search'[39m,
          content: [32m'Cold, with a low of 3℃'[39m,
          tool_call_id: [32m'call_9lZWCPGg6SUP5dg4eTge2xNU'[39m,
          additional_kwargs: {},
          response_metadata: {}
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m'Cold, with a low of 3℃'[39m,
        name: [32m'search'[39m,
        additional_kwargs: {},
        response_metadata: {},
        id: [90mundefined[39m,
        tool_call_id: [32m'call_9lZWCPGg6SUP5dg4eTge2xNU'[39m
      },
      AIMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          content: [32m'The current weather in San Francisco is cold, with a low of 3°C.'[39m,
          tool_calls: [],
          invalid_tool_calls: [],
          additional_kwargs: {},
          response_metadata: [36m[Object][39m
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m'The current weather in San Francisco is cold, with a low of 3°C.'[39m,
        name: [90mundefined[39m,
        additional_kwargs: {},
        response_metadata: { tokenUsage: [36m[Object][39m, finish_reason: [32m'stop'[39m },
        id: [90mundefined[39m,
        tool_calls: [],
        invalid_tool_calls: [],
        usage_metadata: [90mundefined[39m
      }
    ]


### Run on the same conversation thread


```typescript
const newRes = await graph.invoke(
  { "messages": [["user", "what about ny?"]] },
  config,
);
```


```typescript
// verify that we have the new messages added to the latest checkpoint for the thread
(await checkpointer.getTuple(config)).checkpoint.channel_values["messages"];
```

    [
      [32m"what's the weather in sf"[39m,
      AIMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          content: [32m''[39m,
          tool_calls: [36m[Array][39m,
          invalid_tool_calls: [],
          additional_kwargs: [36m[Object][39m,
          response_metadata: [36m[Object][39m
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m''[39m,
        name: [90mundefined[39m,
        additional_kwargs: { tool_calls: [36m[Array][39m },
        response_metadata: { tokenUsage: [36m[Object][39m, finish_reason: [32m'tool_calls'[39m },
        id: [90mundefined[39m,
        tool_calls: [ [36m[Object][39m ],
        invalid_tool_calls: [],
        usage_metadata: [90mundefined[39m
      },
      ToolMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          name: [32m'search'[39m,
          content: [32m'Cold, with a low of 3℃'[39m,
          tool_call_id: [32m'call_9lZWCPGg6SUP5dg4eTge2xNU'[39m,
          additional_kwargs: {},
          response_metadata: {}
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m'Cold, with a low of 3℃'[39m,
        name: [32m'search'[39m,
        additional_kwargs: {},
        response_metadata: {},
        id: [90mundefined[39m,
        tool_call_id: [32m'call_9lZWCPGg6SUP5dg4eTge2xNU'[39m
      },
      AIMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          content: [32m'The current weather in San Francisco is cold, with a low of 3°C.'[39m,
          tool_calls: [],
          invalid_tool_calls: [],
          additional_kwargs: {},
          response_metadata: [36m[Object][39m
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m'The current weather in San Francisco is cold, with a low of 3°C.'[39m,
        name: [90mundefined[39m,
        additional_kwargs: {},
        response_metadata: { tokenUsage: [36m[Object][39m, finish_reason: [32m'stop'[39m },
        id: [90mundefined[39m,
        tool_calls: [],
        invalid_tool_calls: [],
        usage_metadata: [90mundefined[39m
      },
      [32m'what about ny?'[39m,
      AIMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          content: [32m''[39m,
          tool_calls: [36m[Array][39m,
          invalid_tool_calls: [],
          additional_kwargs: [36m[Object][39m,
          response_metadata: [36m[Object][39m
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m''[39m,
        name: [90mundefined[39m,
        additional_kwargs: { tool_calls: [36m[Array][39m },
        response_metadata: { tokenUsage: [36m[Object][39m, finish_reason: [32m'tool_calls'[39m },
        id: [90mundefined[39m,
        tool_calls: [ [36m[Object][39m ],
        invalid_tool_calls: [],
        usage_metadata: [90mundefined[39m
      },
      ToolMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          name: [32m'search'[39m,
          content: [32m'Cold, with a low of 3℃'[39m,
          tool_call_id: [32m'call_7es5lLJH5bW7zVXqwH7id6tq'[39m,
          additional_kwargs: {},
          response_metadata: {}
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m'Cold, with a low of 3℃'[39m,
        name: [32m'search'[39m,
        additional_kwargs: {},
        response_metadata: {},
        id: [90mundefined[39m,
        tool_call_id: [32m'call_7es5lLJH5bW7zVXqwH7id6tq'[39m
      },
      AIMessage {
        lc_serializable: [33mtrue[39m,
        lc_kwargs: {
          content: [32m'The current weather in New York City is also cold, with a low of 3°C.'[39m,
          tool_calls: [],
          invalid_tool_calls: [],
          additional_kwargs: {},
          response_metadata: [36m[Object][39m
        },
        lc_namespace: [ [32m'langchain_core'[39m, [32m'messages'[39m ],
        content: [32m'The current weather in New York City is also cold, with a low of 3°C.'[39m,
        name: [90mundefined[39m,
        additional_kwargs: {},
        response_metadata: { tokenUsage: [36m[Object][39m, finish_reason: [32m'stop'[39m },
        id: [90mundefined[39m,
        tool_calls: [],
        invalid_tool_calls: [],
        usage_metadata: [90mundefined[39m
      }
    ]


### List checkpoints


```typescript
// list last 2 checkpoints
const limit = 2;
for await (
  const chunk of await checkpointer.list(
    { configurable: { thread_id: 1 } },
    limit,
  )
) {
  console.log(chunk);
}
```

    {
      config: {
        configurable: {
          thread_id: [32m'1'[39m,
          checkpoint_id: [32m'1ef3fb99-f829-66d0-8012-e62c07718de3'[39m
        }
      },
      checkpoint: {
        v: [33m1[39m,
        id: [32m'1ef3fb99-f829-66d0-8012-e62c07718de3'[39m,
        ts: [32m'2024-07-11T19:13:14.941Z'[39m,
        channel_values: { messages: [36m[Array][39m, agent: [32m'agent'[39m },
        channel_versions: {
          __start__: [33m16[39m,
          messages: [33m20[39m,
          [32m'start:agent'[39m: [33m17[39m,
          agent: [33m20[39m,
          [32m'branch:agent:routeMessage:tools'[39m: [33m18[39m,
          tools: [33m19[39m
        },
        versions_seen: { __start__: [36m[Object][39m, agent: [36m[Object][39m, tools: [36m[Object][39m }
      },
      metadata: { source: [32m'loop'[39m, step: [33m18[39m, writes: { agent: [36m[Object][39m } },
      parentConfig: {
        configurable: {
          thread_id: [32m'1'[39m,
          checkpoint_id: [32m'1ef3fb99-f1b2-6270-8011-7ecc1fda99cf'[39m
        }
      }
    }
    {
      config: {
        configurable: {
          thread_id: [32m'1'[39m,
          checkpoint_id: [32m'1ef3fb99-f1b2-6270-8011-7ecc1fda99cf'[39m
        }
      },
      checkpoint: {
        v: [33m1[39m,
        id: [32m'1ef3fb99-f1b2-6270-8011-7ecc1fda99cf'[39m,
        ts: [32m'2024-07-11T19:13:14.263Z'[39m,
        channel_values: { messages: [36m[Array][39m, tools: [32m'tools'[39m },
        channel_versions: {
          __start__: [33m16[39m,
          messages: [33m19[39m,
          [32m'start:agent'[39m: [33m17[39m,
          agent: [33m18[39m,
          [32m'branch:agent:routeMessage:tools'[39m: [33m18[39m,
          tools: [33m19[39m
        },
        versions_seen: { __start__: [36m[Object][39m, agent: [36m[Object][39m, tools: [36m[Object][39m }
      },
      metadata: { source: [32m'loop'[39m, step: [33m17[39m, writes: { tools: [36m[Object][39m } },
      parentConfig: {
        configurable: {
          thread_id: [32m'1'[39m,
          checkpoint_id: [32m'1ef3fb99-f1af-6b60-8010-b3c0f6939ab2'[39m
        }
      }
    }

