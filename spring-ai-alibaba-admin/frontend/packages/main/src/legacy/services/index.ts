import * as evaluators from './evaluators';
import * as prompt from './prompt';
import * as model from './model';
import * as tracing from './tracing';

const API = {
  ...evaluators,
  ...prompt,
  ...model,
  observability: {
    ...tracing
  }
};

export default API;