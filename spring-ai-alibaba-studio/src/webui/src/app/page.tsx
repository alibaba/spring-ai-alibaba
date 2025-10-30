"use client";
import Image from "next/image";
import { SimpleChat } from "./component/chat";
import { SimpleChatWithApproval } from "./component/chatApproval";

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-between p-24">
      <SimpleChatWithApproval />
    </main>
  );
}
