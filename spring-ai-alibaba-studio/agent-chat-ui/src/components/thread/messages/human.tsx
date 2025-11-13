import { UIMessage } from "@/types/messages";

export function HumanMessage({
  message,
}: {
  message: UIMessage;
  isLoading?: boolean;
}) {
  const contentString = message.message.content;

  return (
    <div className="group ml-auto flex items-center gap-2">
      <div className="flex flex-col gap-2">
        <p className="bg-muted ml-auto w-fit rounded-3xl px-4 py-2 text-right whitespace-pre-wrap">
          {contentString}
        </p>
      </div>
    </div>
  );
}

