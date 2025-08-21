import { createRouteLoader, WrapRouteComponent, RouteErrorComponent } from '@ice/runtime';
import type { CreateRoutes } from '@ice/runtime';
const createRoutes: CreateRoutes = ({
  requestContext,
  renderMode,
}) => ([
  {
    path: '',
    async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_layout" */ '@/pages/layout');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: 'layout',
          isLayout: true,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: 'layout',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
    errorElement: <RouteErrorComponent />,
    componentName: 'layout',
    index: undefined,
    id: 'layout',
    exact: true,
    exports: ["default"],
    layout: true,
    children: [{
      path: 'evaluate',
      async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_evaluate-index" */ '@/pages/evaluate/index');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: 'evaluate',
          isLayout: false,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: 'evaluate',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
      errorElement: <RouteErrorComponent />,
      componentName: 'evaluate-index',
      index: true,
      id: 'evaluate',
      exact: true,
      exports: ["default"],
    },{
      path: 'traces',
      async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_traces-index" */ '@/pages/traces/index');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: 'traces',
          isLayout: false,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: 'traces',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
      errorElement: <RouteErrorComponent />,
      componentName: 'traces-index',
      index: true,
      id: 'traces',
      exact: true,
      exports: ["default"],
    },{
      path: 'run',
      async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_run-layout" */ '@/pages/run/layout');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: 'run/layout',
          isLayout: true,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: 'run/layout',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
      errorElement: <RouteErrorComponent />,
      componentName: 'run-layout',
      index: undefined,
      id: 'run/layout',
      exact: true,
      exports: ["default"],
      layout: true,
      children: [{
        path: 'clients/:client_name',
        async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_run-clients-$client_name" */ '@/pages/run/clients/$client_name');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: 'run/clients/:client_name',
          isLayout: false,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: 'run/clients/:client_name',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
        errorElement: <RouteErrorComponent />,
        componentName: 'run-clients-$client_name',
        index: undefined,
        id: 'run/clients/:client_name',
        exact: true,
        exports: ["default"],
      },{
        path: 'models/:model_name',
        async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_run-models-$model_name" */ '@/pages/run/models/$model_name');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: 'run/models/:model_name',
          isLayout: false,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: 'run/models/:model_name',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
        errorElement: <RouteErrorComponent />,
        componentName: 'run-models-$model_name',
        index: undefined,
        id: 'run/models/:model_name',
        exact: true,
        exports: ["default"],
      },{
        path: 'clients',
        async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_run-clients-index" */ '@/pages/run/clients/index');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: 'run/clients',
          isLayout: false,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: 'run/clients',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
        errorElement: <RouteErrorComponent />,
        componentName: 'run-clients-index',
        index: true,
        id: 'run/clients',
        exact: true,
        exports: ["default"],
      },{
        path: 'models',
        async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_run-models-index" */ '@/pages/run/models/index');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: 'run/models',
          isLayout: false,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: 'run/models',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
        errorElement: <RouteErrorComponent />,
        componentName: 'run-models-index',
        index: true,
        id: 'run/models',
        exact: true,
        exports: ["default"],
      },]
    },{
      path: '',
      async lazy() {
      const componentModule = await import(/* webpackChunkName: "p_index" */ '@/pages/index');
      return {
        ...componentModule,
        Component: () => WrapRouteComponent({
          routeId: '/',
          isLayout: false,
          routeExports: componentModule,
        }),
        loader: createRouteLoader({
          routeId: '/',
          requestContext,
          renderMode,
          module: componentModule,
        }),
      };
    },
      errorElement: <RouteErrorComponent />,
      componentName: 'index',
      index: true,
      id: '/',
      exact: true,
      exports: ["default"],
    },]
  },
]);
export default createRoutes;
