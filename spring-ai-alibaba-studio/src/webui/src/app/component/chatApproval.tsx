"use client";
import { useCopilotAction } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
 
export function SimpleChatWithApproval() {
  

  useCopilotAction( {
    name: "sendEmail",
    description: "Sends an email after user approval.",
    parameters: [
      { name: "arg0", type: "string" },
      { name: "arg1", type: "string" },
      { name: "arg2", type: "string" },
    ],
    renderAndWaitForResponse: ({ args, status, respond }) => {
      console.debug( "renderAndWaitForResponse", respond, status, args );

      if (status === "inProgress") {
        return (
          <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
            <div className="p-6 rounded shadow-lg">
              <h2 className="text-lg font-bold mb-2">Sending Email</h2>
              <p>Preparing to send email...</p>
            </div>
          </div>
        );
      }
      if (status === "executing") {
        return (
            <div className="fixed inset-0 flex items-center justify-center z-50">
            <div className="bg-white text-black p-6 rounded shadow-lg border border-black">
              <h2 className="text-lg font-bold mb-2">Confirm Email</h2>
              <p>
              Send email to <b>{args.arg0}</b> with subject "<b>{args.arg1}</b>"?
              </p>
              <p><i>{args.arg2}</i></p>
              <div className="mt-4 flex gap-2">
              <button className="bg-blue-500 text-white px-4 py-2 rounded" onClick={() => respond?.('APPROVED') }>
                Approve
              </button>
              <button className="bg-blue-300 px-4 py-2 rounded" onClick={() => respond?.('REJECTED')}>
                Cancel
              </button>
              </div>
            </div>
            </div>
      );
      }
     return <></>
  }});
  // <CopilotChat />

  return (
    <CopilotChat
      instructions={"You are assisting the user as best as you can. Answer in the best way possible given the data you have."}
      labels={{
        title: "Your Assistant",
        initial: "Hi! 👋 How can I assist you today?",
      }}
      className="w-full"
    />
  );

}
