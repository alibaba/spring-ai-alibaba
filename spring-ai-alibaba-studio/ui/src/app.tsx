import { runApp, IAppConfig } from 'ice';

const appConfig: IAppConfig = {
  app: {
    rootId: 'ice-container',
  },
  router: {
    type: 'hash',
  },
  request: {
    baseURL: 'http://127.0.0.1:4523/m1/5397819-5071455-default/',
  },
};

runApp(appConfig);
