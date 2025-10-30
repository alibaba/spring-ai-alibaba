import {CopilotRuntime, copilotRuntimeNextJSAppRouterEndpoint,} from '@copilotkit/runtime';
import {NextRequest} from 'next/server';
import {AgentAdapter} from '@/app/lib/agent';


const serviceAdapter = new AgentAdapter();

const runtime = new CopilotRuntime({});

export const POST = async (req: NextRequest) => {

    const {handleRequest} = copilotRuntimeNextJSAppRouterEndpoint({
        runtime,
        serviceAdapter,
        endpoint: "/api/agent",
    });

    return handleRequest(req);
};
