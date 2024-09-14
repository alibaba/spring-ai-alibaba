import {useEffect, useState} from "react";
import {AssistantService, BookingService} from "Frontend/generated/endpoints";
import BookingDetails from "../generated/ai/spring/demo/ai/playground/services/BookingTools/BookingDetails";
import {GridColumn} from "@vaadin/react-components/GridColumn";
import {Grid} from "@vaadin/react-components/Grid";
import {MessageInput} from "@vaadin/react-components/MessageInput";
import {nanoid} from "nanoid";
import {SplitLayout} from "@vaadin/react-components/SplitLayout";
import Message, {MessageItem} from "../components/Message";
import MessageList from "Frontend/components/MessageList";

export default function Index() {
  const [chatId, setChatId] = useState(nanoid());
  const [working, setWorking] = useState(false);
  const [bookings, setBookings] = useState<BookingDetails[]>([]);
  const [messages, setMessages] = useState<MessageItem[]>([{
    role: 'assistant',
    content: 'Welcome to Funnair! How can I help you?'
  }]);

  useEffect(() => {
    // Update bookings when we have received the full response
    if (!working) {
      BookingService.getBookings().then(setBookings);
    }
  }, [working]);

  function addMessage(message: MessageItem) {
    setMessages(messages => [...messages, message]);
  }

  function appendToLatestMessage(chunk: string) {
    setMessages(messages => {
      const latestMessage = messages[messages.length - 1];
      latestMessage.content += chunk;
      return [...messages.slice(0, -1), latestMessage];
    });
  }

  async function sendMessage(message: string) {
    setWorking(true);
    addMessage({
      role: 'user',
      content: message
    });
    let first = true;
    AssistantService.chat(chatId, message)
      .onNext(token => {
        if (first && token) {
          addMessage({
            role: 'assistant',
            content: token
          });

          first = false;
        } else {
          appendToLatestMessage(token);
        }
      })
      .onError(() => setWorking(false))
      .onComplete(() => setWorking(false));
  }

  return (
    <SplitLayout className="h-full">
      <div className="flex flex-col gap-m p-m box-border h-full" style={{width: '30%'}}>
        <h3>Funnair support</h3>
        <MessageList messages={messages} className="flex-grow overflow-scroll"/>
        <MessageInput onSubmit={e => sendMessage(e.detail.value)} className="px-0" disabled={working}/>
      </div>
      <div className="flex flex-col gap-m p-m box-border" style={{width: '70%'}}>
        <h3>Bookings database</h3>
        <Grid items={bookings} className="flex-shrink-0">
          <GridColumn path="bookingNumber" autoWidth header="#"/>
          <GridColumn path="firstName" autoWidth/>
          <GridColumn path="lastName" autoWidth/>
          <GridColumn path="date" autoWidth/>
          <GridColumn path="from" autoWidth/>
          <GridColumn path="to" autoWidth/>
          <GridColumn path="bookingStatus" autoWidth header="Status">
            {({item}) => item.bookingStatus === "CONFIRMED" ? "✅" : "❌"}
          </GridColumn>
          <GridColumn path="bookingClass" autoWidth/>
        </Grid>
      </div>
    </SplitLayout>

  );
}