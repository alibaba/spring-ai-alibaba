import type { AppConfig, RouteConfig as DefaultRouteConfig } from '@ice/runtime';

type ExtendsRouteConfig = {};
type PageConfig = DefaultRouteConfig<ExtendsRouteConfig>;
type PageConfigDefinitionContext<DataType = any> = {
  data?: DataType;
};
type PageConfigDefinition = (context: PageConfigDefinitionContext) => PageConfig;

export type {
  AppConfig,
  PageConfig,
  PageConfigDefinition,
};
