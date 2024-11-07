# How to install and manage dependencies

LangGraph.js is part of the [LangChain](https://js.langchain.com/) ecosystem,
which includes the primary
[`langchain`](https://www.npmjs.com/package/langchain) package as well as
packages that contain integrations with individual third-party providers. They
can be as specific as
[`@langchain/anthropic`](https://www.npmjs.com/package/@langchain/anthropic),
which contains integrations just for Anthropic chat models, or as broad as
[`@langchain/community`](https://www.npmjs.com/package/@langchain/community),
which contains broader variety of community contributed integrations.

These packages, as well as LangGraph.js itself, all depend on
[`@langchain/core`](https://www.npmjs.com/package/@langchain/core), which
contains the base abstractions that these packages extend.

To ensure that all integrations and their types interact with each other
properly, it is important that they all use the same version of
`@langchain/core`. The best way to guarantee this is to add a `"resolutions"` or
`"overrides"` field like the following in your project's `package.json`. The
specific field name will depend on your package manager. Here are a few
examples:

<div class="admonition tip">
  <p class="admonition-title">Tip</p>
  <p>
    The <code>resolutions</code> or <code>pnpm.overrides</code> fields for <code>yarn</code> or <code>pnpm</code> must be set in the root <code>package.json</code> file.
  </p>
</div>

If you are using `yarn`, you should set
[`"resolutions"`](https://yarnpkg.com/cli/set/resolution):

```json
{
  "name": "your-project",
  "version": "0.0.0",
  "private": true,
  "engines": {
    "node": ">=18"
  },
  "dependencies": {
    "@langchain/anthropic": "^0.2.1",
    "@langchain/langgraph": "0.0.23"
  },
  "resolutions": {
    "@langchain/core": "0.2.6"
  }
}
```

For `npm`, use
[`"overrides"`](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#overrides):

```json
{
  "name": "your-project",
  "version": "0.0.0",
  "private": true,
  "engines": {
    "node": ">=18"
  },
  "dependencies": {
    "@langchain/anthropic": "^0.2.1",
    "@langchain/langgraph": "0.0.23"
  },
  "overrides": {
    "@langchain/core": "0.2.6"
  }
}
```

For `pnpm`, use the nested
[`"pnpm.overrides"`](https://pnpm.io/package_json#pnpmoverrides) field:

```json
{
  "name": "your-project",
  "version": "0.0.0",
  "private": true,
  "engines": {
    "node": ">=18"
  },
  "dependencies": {
    "@langchain/anthropic": "^0.2.1",
    "@langchain/langgraph": "0.0.23"
  },
  "pnpm": {
    "overrides": {
      "@langchain/core": "0.2.6"
    }
  }
}
```

## Next steps

You've now learned about some special considerations around using LangGraph.js
with other LangChain ecosystem packages.

Next, check out
[some how-to guides on core functionality](/langgraphjs/how-tos/#core).
