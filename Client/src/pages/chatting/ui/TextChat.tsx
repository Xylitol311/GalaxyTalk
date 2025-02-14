import { useChat } from '@livekit/components-react';
import { ArrowUp, Plus } from 'lucide-react';
import {
    FormEvent,
    KeyboardEventHandler,
    useEffect,
    useRef,
    useState,
} from 'react';
import { useUserStore } from '@/app/model/stores/user';
import useIsMobile from '@/shared/model/hooks/useIsMobile';
import { AutosizeTextAreaRef } from '@/shared/ui/shadcn/autosizetextarea';
import { Button } from '@/shared/ui/shadcn/button';
import {
    ChatBubble,
    ChatBubbleMessage,
} from '@/shared/ui/shadcn/shadcn-chat/chat/chat-bubble';
import { ChatInput } from '@/shared/ui/shadcn/shadcn-chat/chat/chat-input';
import { ChatMessageList } from '@/shared/ui/shadcn/shadcn-chat/chat/chat-message-list';
import { useChatMessagesQuery, usePostChatMessage } from '../api/queries';
import ReactionPanel from './ReactionPanel';

interface From {
    identity: string;
}
interface ChatMessage {
    id: string;
    message: string;
    from: From;
}
interface ApiMessage {
    senderId: string;
    content: string;
    createdAt: string;
}

function TextChat({ chatRoomId }) {
    const { send, update, chatMessages: temp, isSending } = useChat();

    const { mutate: postMessage } = usePostChatMessage(chatRoomId);
    const { data: response } = useChatMessagesQuery();

    const isMobile = useIsMobile();
    const textareaRef = useRef<AutosizeTextAreaRef>(null);
    const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
    const { userId: myUserId } = useUserStore();

    useEffect(() => {
        if (response) {
            const transformedMessages: ChatMessage[] = response.data.map(
                (msg: ApiMessage) => ({
                    id: msg.createdAt, // createdAt을 유니크 키로 사용
                    message: msg.content,
                    from: {
                        identity: msg.senderId,
                    },
                })
            );

            setChatMessages(transformedMessages);
        }
    }, [response]);

    const handleMessageSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const formData = new FormData(e.currentTarget);
        const message = formData.get('message')?.toString().trim();

        if (!message) return;

        send(message);
        // 서버저장용 api
        postMessage(message);

        // 폼과 textarea 초기화
        textareaRef.current?.reset(); // reset 메서드 호출
        e.currentTarget.reset();
    };

    const handleKeyDown: KeyboardEventHandler<HTMLTextAreaElement> = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            const form = e.currentTarget.form;
            if (form) {
                const message = e.currentTarget.value.trim();
                if (message) {
                    form.requestSubmit();
                }
            }
        }
    };

    return (
        <div className="flex flex-col justify-between w-screen md:w-full h-screen md:h-full relative">
            {/* // Wrap with ChatMessageList */}
            <ChatMessageList className="pt-2">
                {chatMessages.map((msg) => {
                    const isSentByMe = myUserId === msg.from?.identity;
                    return (
                        <ChatBubble
                            key={msg?.id}
                            variant={isSentByMe ? 'sent' : 'received'}>
                            <ChatBubbleMessage
                                variant={isSentByMe ? 'sent' : 'received'}>
                                {msg.message}
                            </ChatBubbleMessage>
                        </ChatBubble>
                    );
                })}
            </ChatMessageList>

            <form
                className="relative rounded-lg border bg-background focus-within:ring-1 focus-within:ring-ring p-2 "
                onSubmit={handleMessageSubmit}>
                {isMobile && (
                    <div className="w-full flex justify-end absolute -top-14 right-4">
                        <ReactionPanel />
                    </div>
                )}
                <div className="flex gap-3">
                    {isMobile && (
                        <div className="flex items-center p-1">
                            <Button
                                size="icon"
                                className="bg-slate-400 h-8 w-8 rounded-lg ">
                                <Plus
                                    style={{ width: '18px', height: '18px' }}
                                />
                            </Button>
                        </div>
                    )}

                    <ChatInput
                        placeholder="메시지를 작성해주세요"
                        className="min-h-8 resize-none rounded-lg bg-background border-0 shadow-none focus-visible:ring-0"
                        onKeyDown={handleKeyDown}
                        // ref={textareaRef}
                        maxHeight={120}
                    />
                    <div className="flex items-center p-1">
                        <Button size="icon" className="h-8 w-8 rounded-lg">
                            <ArrowUp
                                style={{ width: '18px', height: '18px' }}
                            />
                        </Button>
                    </div>
                </div>
            </form>
        </div>
    );
}

export default TextChat;
