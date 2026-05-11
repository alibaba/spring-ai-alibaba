import { GraphPageClient } from "./GraphPageClient";

export function generateStaticParams() {
  return [{ graphName: "__" }];
}

export default async function GraphPage({
  params,
}: {
  params: Promise<{ graphName: string }>;
}) {
  const { graphName } = await params;
  return <GraphPageClient graphName={graphName} />;
}
