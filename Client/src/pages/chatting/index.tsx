import { LiveKitRoom } from '@livekit/components-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { usePostChatReconnect } from './api/queries';
import ChattingPage from './ChattingPage';
import { ChatData } from './model/interfaces';

export default function ChattingRoom() {
    const navigate = useNavigate();
    const [chatData, setChatData] = useState<ChatData | null>(null);

    const LIVEKIT_URL = `wss://i12a503.p.ssafy.io/livekitws/`;

    const { mutate: reconnect, isPending } = usePostChatReconnect();

    useEffect(() => {
        reconnect(undefined, {
            onSuccess: (response) => {
                const { chatRoomId, sessionId, token } = response.data;
                setChatData({
                    sessionId,
                    token,
                    chatRoomId,
                });
            },
        });
    }, [navigate, reconnect]);

    // 로딩 중일 때 표시할 UI
    if (isPending || !chatData) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-b-white" />
            </div>
        );
    }

    // 채팅방 정보가 유효하면 ChattingPage 렌더링
    if (chatData)
        return (
            <LiveKitRoom
                video={true}
                audio={true}
                token={chatData.token}
                serverUrl={LIVEKIT_URL}
                data-lk-theme="default">
                <ChattingPage chatData={chatData} />
            </LiveKitRoom>
        );
}
