import { AgentPageClient } from "./AgentPageClient";

export function generateStaticParams() {
  return [{ agentName: "__" }];
}

export default async function AgentPage({
  params,
}: {
  params: Promise<{ agentName: string }>;
}) {
  const { agentName } = await params;
  return <AgentPageClient agentName={agentName} />;
}
