import { ExitIcon } from '@radix-ui/react-icons';
import { Html } from '@react-three/drei';
import { Canvas } from '@react-three/fiber';
import { Client } from '@stomp/stompjs';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import SockJS from 'sockjs-client';
import { PATH } from '@/app/config/constants';
import { BASE_URL } from '@/app/config/constants/path';
import { useUserStore } from '@/app/model/stores/user';
import { useDeleteMatchCancel } from '@/features/match/api/queries';
import { Button } from '@/shared/ui/shadcn/button';
import Galaxy from '@/widget/Galaxy';
import HealingMessage from './ui/HealingMessage';
import TimerConfirm from './ui/TimerConfirm';

export type MatchType = {
    userId: string;
    matchId: number;
    matchUserId: string;
    concern: string;
    mbti: string;
    energy: number;
    similarity: number;
};

export default function MatchingRoom() {
    const navigate = useNavigate();
    const { mutate } = useDeleteMatchCancel();
    const { userId } = useUserStore();
    const [matchData, setMatchData] = useState<MatchType | null>(null);

    const resetMatchData = () => {
        setMatchData(null);
    };

    const client = new Client({
        brokerURL: `${BASE_URL}/match/ws`,
        webSocketFactory: () => new SockJS(`${BASE_URL}/match/ws`),
        onConnect: () => {
            console.log(userId);
            client.subscribe(`/topic/matching/${userId}`, (message) => {
                const data = JSON.parse(message.body);
                if (data.type === 'MATCH_SUCCESS') {
                    console.log(data.data);
                    setMatchData(data.data);
                }
                if (data.type === 'CHAT_CREATED') {
                    console.log(data.data);
                    navigate(PATH.ROUTE.CHAT);
                }
                if (data.type === 'WAITING') {
                    console.log(data.data);
                    resetMatchData();
                }

                if (data.type === 'MATCH_FAILED') {
                    console.log(data.data);
                    resetMatchData();
                }
            });
            client.subscribe('/topic/matching/users/new', (message) =>
                console.log(`Received: ${message.body}`)
            );
            client.subscribe('/topic/matching/users/exit', (message) =>
                console.log(`Received: ${message.body}`)
            );
        },
    });

    useEffect(() => {
        client.activate();
        return () => {
            client.deactivate();
        };
    }, []);

    const handleToHome = () => {
        mutate();
        navigate(PATH.ROUTE.HOME);
    };

    return (
        <Canvas camera={{ position: [4, 2, 5], fov: 40 }}>
            <Galaxy />
            <Html
                position={[0, 0, 0]}
                center
                zIndexRange={[0, 0]}
                style={{ pointerEvents: 'none' }}>
                <div className="relative w-screen h-screen flex flex-col justify-between">
                    <Button
                        variant="link"
                        className="text-white self-start"
                        onClick={handleToHome}
                        style={{ pointerEvents: 'auto' }}>
                        <ExitIcon />
                        이전 페이지로 이동하기
                    </Button>
                    {matchData && <TimerConfirm matchData={matchData} />}
                    <HealingMessage />
                </div>
            </Html>
        </Canvas>
    );
}
