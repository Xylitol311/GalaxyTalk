import { useChat, useParticipants } from '@livekit/components-react';
import { ArrowUp } from 'lucide-react';
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
    const { send, update, chatMessages, isSending } = useChat();

    const { mutate: postMessage } = usePostChatMessage(chatRoomId);
    const { data: response } = useChatMessagesQuery(chatRoomId);

    const isMobile = useIsMobile();
    const textareaRef = useRef<AutosizeTextAreaRef>(null);
    const [initialMessages, setInitialMessages] = useState<ChatMessage[]>([]);
    const { userId: myUserId } = useUserStore();

    const participants = useParticipants();

    useEffect(() => {
        if (response) {
            const previousChatMessages: ChatMessage[] = response.data.map(
                (msg: ApiMessage) => ({
                    id: msg.createdAt, // createdAt을 유니크 키로 사용
                    message: msg.content,
                    from: {
                        identity: msg.senderId,
                    },
                })
            );

            setInitialMessages(previousChatMessages);
        }
    }, [response]);

    // 디바운스 타이머 저장용 ref
    const debounceTimeout = useRef<number | null>(null);

    const handleMessageSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        // SyntheticEvent의 재활용을 막기 위해 현재 폼 요소를 미리 저장
        const formElement = e.currentTarget;

        // 기존 타이머가 있다면 취소
        if (debounceTimeout.current) {
            clearTimeout(debounceTimeout.current);
        }

        // 100ms 후에 실행하도록 타이머 설정
        debounceTimeout.current = window.setTimeout(async () => {
            const formData = new FormData(formElement);
            const message = formData.get('message')?.toString().trim();

            if (!message) return;

            // 메시지 전송
            send(message);
            // 서버저장용 API 호출 (주석 처리된 부분 예시)
            postMessage(message);

            // 폼과 textarea 초기화
            textareaRef.current?.reset(); // ref를 이용한 초기화 (커스텀 컴포넌트의 경우 채택)
            formElement.reset();
        }, 100);
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
        <div className="flex flex-col justify-between w-dvw md:w-full h-dvh md:h-full relative">
            {/* // Wrap with ChatMessageList */}
            <ChatMessageList className="pt-2">
                {initialMessages.map((msg) => {
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
                    <div className="w-full flex justify-between absolute -top-14 right-0">
                        <ReactionPanel userId={participants[1]?.identity} />
                        <ReactionPanel userId={myUserId} />
                    </div>
                )}
                <div className="flex gap-3">
                    {/* {isMobile && (
                        <div className="flex items-center p-1">
                            <Button
                                size="icon"
                                className="bg-slate-400 h-8 w-8 rounded-lg ">
                                <Plus
                                    style={{ width: '18px', height: '18px' }}
                                />
                            </Button>
                        </div>
                    )} */}

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
