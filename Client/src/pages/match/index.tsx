import { ExitIcon } from '@radix-ui/react-icons';
import { Html } from '@react-three/drei';
import { Canvas } from '@react-three/fiber';
import { Client } from '@stomp/stompjs';
import { parse, stringify } from 'flatted';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import SockJS from 'sockjs-client';
import { PATH } from '@/app/config/constants';
import { BASE_URL } from '@/app/config/constants/path';
import { useUserStore } from '@/app/model/stores/user';
import {
    useDeleteMatchCancel,
    useMatchApprove,
} from '@/features/match/api/queries';
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
    const { mutate: matchApproveMutate } = useMatchApprove();

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
                console.log('메시지 출력 :', message);
                console.log('메시지 바디 출력 :', message.body);
                // console.log('메시지 바디 타입 출력 :', message.body.type);
                // console.log('메시지 바디 데이터 출력 :', message.body.data);
                const stringifiedData = stringify(message.body);
                console.log('직렬화 : ', stringifiedData);

                const data = parse(stringifiedData);
                console.log('파싱 데이터 : ', data);

                if (data.type === 'MATCH_SUCCESS') {
                    setMatchData(data.data);
                }
                if (data.type === 'CHAT_CREATED') {
                    navigate(PATH.ROUTE.CHAT);
                }
                if (data.type === 'WAITING') {
                    toast({
                        title: '다른 사람을 찾아볼게요',
                    });
                    resetMatchData();
                }

                if (data.type === 'MATCH_FAILED') {
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

    const handleConfirm = () => {
        matchApproveMutate({
            matchId: `${matchData?.matchId}`,
            accepted: true,
        });
    };

    const handleCancel = () => {
        matchApproveMutate({
            matchId: `${matchData?.matchId}`,
            accepted: false,
        });
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
                        <div className="relative w-screen h-screen flex flex-col justify-between">
                            <Button
                                variant="link"
                                className="text-white self-start"
                                onClick={handleToHome}
                                style={{ pointerEvents: 'auto' }}>
                                <ExitIcon />
                                이전 페이지로 이동하기
                            </Button>
                            <Button
                                variant="link"
                                className="text-white self-start"
                                onClick={handleCancel}
                                style={{ pointerEvents: 'auto' }}>
                                매칭 거절
                            </Button>
                            <Button
                                variant="link"
                                className="text-white self-start"
                                onClick={handleConfirm}
                                style={{ pointerEvents: 'auto' }}>
                                매칭 수락
                            </Button>
                            {!isMoving && matchData && (
                                <TimerConfirm matchData={matchData} />
                            )}
                            <HealingMessage />
                        </div>
                    </Html>
                </Canvas>
            )}
        </>
    );
}
