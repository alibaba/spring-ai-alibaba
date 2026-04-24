declare module "mermaid" {
  interface MermaidConfig {
    startOnLoad?: boolean;
    theme?: string;
    securityLevel?: string;
  }

  interface MermaidAPI {
    initialize(config: MermaidConfig): void;
    render(id: string, text: string): Promise<{ svg: string }>;
  }

  const mermaid: MermaidAPI;
  export default mermaid;
}
