import { useState } from "react";


export interface MockTool {
  toolDefinition: {
    name: string,
    description: string,
    parameters: string
  },
  output: string
}

function useFunctions(defaultFunctions: MockTool[]) {

  const [functions, setFunctions] = useState(defaultFunctions || []);

  return {
    functions, setFunctions,
  }

}

export default useFunctions;