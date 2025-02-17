import { ExitIcon } from '@radix-ui/react-icons';
import { Html } from '@react-three/drei';
import { Canvas } from '@react-three/fiber';
import { Client } from '@stomp/stompjs';
import { QueryClientProvider } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import SockJS from 'sockjs-client';
import { PATH } from '@/app/config/constants';
import { BASE_URL } from '@/app/config/constants/path';
import { useUserStore } from '@/app/model/stores/user';
import { useDeleteMatchCancel } from '@/features/match/api/queries';
import { queryClient } from '@/shared/api/query/client';
import { toast } from '@/shared/model/hooks/use-toast';
import { Button } from '@/shared/ui/shadcn/button';
import Galaxy from '@/widget/Galaxy';
import WarpPage from '../warp';
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
    const [isMoving, setIsMoving] = useState(true);

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            setIsMoving(false);
        }, 6000);

        return () => clearTimeout(timeoutId);
    }, []);

    const resetMatchData = () => {
        setMatchData(null);
    };

    const client = new Client({
        brokerURL: `${BASE_URL}/match/ws`,
        webSocketFactory: () => new SockJS(`${BASE_URL}/match/ws`),
        onConnect: () => {
            client.subscribe(`/topic/matching/${userId}`, (message) => {
                const stringifiedData = JSON.stringify(message.body);
                const stringData = JSON.parse(stringifiedData);
                const parsedData = JSON.parse(stringData);

                if (parsedData.type === 'MATCH_SUCCESS') {
                    setMatchData(parsedData.data);
                }
                if (parsedData.type === 'CHAT_CREATED') {
                    navigate(PATH.ROUTE.CHAT);
                }
                if (parsedData.type === 'WAITING') {
                    toast({
                        title: '다른 사람을 찾아볼게요',
                    });
                    resetMatchData();
                }

                if (parsedData.type === 'MATCH_FAILED') {
                    toast({
                        variant: 'destructive',
                        title: '매칭에 실패했어요',
                    });
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
        <>
            {isMoving ? (
                <WarpPage />
            ) : (
                <Canvas camera={{ position: [4, 2, 5], fov: 40 }}>
                    <Galaxy />
                    <Html
                        position={[0, 0, 0]}
                        center
                        zIndexRange={[0, 0]}
                        style={{ pointerEvents: 'none' }}>
                        <QueryClientProvider client={queryClient}>
                            <div className="relative w-screen h-screen flex flex-col justify-between">
                                <Button
                                    variant="link"
                                    className="text-white self-start"
                                    onClick={handleToHome}
                                    style={{ pointerEvents: 'auto' }}>
                                    <ExitIcon />
                                    이전 페이지로 이동하기
                                </Button>
                                {!isMoving && matchData && (
                                    <TimerConfirm
                                        matchData={matchData}
                                        handleToHome={handleToHome}
                                    />
                                )}
                                <HealingMessage />
                            </div>
                        </QueryClientProvider>
                    </Html>
                </Canvas>
            )}
        </>
    );
}
