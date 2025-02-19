import { LiveKitRoom } from '@livekit/components-react';
import { ExitIcon } from '@radix-ui/react-icons';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { Button } from '@/shared/ui/shadcn/button';
import { useCancelChatRoom, usePostChatReconnect } from './api/queries';
import ChattingPage from './ChattingPage';
import { ChatData } from './model/interfaces';

export default function ChattingRoom() {
    const navigate = useNavigate();
    const [chatData, setChatData] = useState<ChatData | null>(null);

    const LIVEKIT_URL = `wss://galaxy-6i3e0q51.livekit.cloud`;

    const { mutate: reconnect, isPending } = usePostChatReconnect();
    const { mutate: cancelchat } = useCancelChatRoom();

    useEffect(() => {
        reconnect(undefined, {
            onSuccess: (response) => {
                const { chatRoomId, sessionId, token } = response.data;
                console.log(chatRoomId, sessionId, token);
                setChatData({
                    sessionId,
                    token,
                    chatRoomId,
                });
            },
        });
    }, [navigate, reconnect]);

    const chatRoomId =
        chatData?.chatRoomId ||
        (localStorage.getItem('chatdata')
            ? JSON.parse(localStorage.getItem('chatdata') as string).chatRoomId
            : null);

    const handleToHome = () => {
        cancelchat(chatRoomId);
        navigate(PATH.ROUTE.HOME);
    };

    // 로딩 중일 때 표시할 UI
    if (!isPending || !chatData) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <Button
                    variant="link"
                    className="absolute left-0 top-0 text-white p-6"
                    onClick={handleToHome}
                    style={{ pointerEvents: 'auto' }}>
                    <ExitIcon />
                    홈화면으로 이동하기
                </Button>
                <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-b-white" />
            </div>
        );
    }

    // 채팅방 정보가 유효하면 ChattingPage 렌더링
    if (chatData)
        return (
            <LiveKitRoom
                video={false}
                audio={true}
                // token={chatData.token}
                token={
                    'eyJhbGciOiJIUzI1NiJ9.eyJ2aWRlbyI6eyJyb29tSm9pbiI6dHJ1ZSwicm9vbSI6IlRlc3QgUm9vbSJ9LCJpc3MiOiJkZXZrZXkiLCJleHAiOjE3Mzk5OTU5OTQsIm5iZiI6MCwic3ViIjoiUGFydGljaXBhbnQxMSJ9.hDU5aTSXC1hxtqTcbIabMqaP770vYSoSzphk7XatAAs'
                }
                // serverUrl={LIVEKIT_URL}
                serverUrl={'ws://localhost:7880/'}
                data-lk-theme="default">
                <ChattingPage chatData={chatData} />
            </LiveKitRoom>
        );
}
