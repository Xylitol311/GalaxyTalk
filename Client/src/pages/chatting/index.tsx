import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { usePostChatReconnect } from './api/queries';
import ChattingPage from './ChattingPage';
import { ChatData } from './model/interfaces';

export default function ChattingRoom() {
    const navigate = useNavigate();
    const [chatData, setChatData] = useState<ChatData | null>(null);

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
    if (isPending) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-b-white" />
            </div>
        );
    }

    // 채팅방 정보가 없으면 null 반환 (이미 리다이렉트 처리됨)
    if (!chatData)
        return (
            <div className="flex items-center justify-center min-h-screen">
                <h1 className="text-white">no chat data</h1>
            </div>
        );

    // 채팅방 정보가 유효하면 ChattingPage 렌더링
    return <ChattingPage chatData={chatData} />;
}
