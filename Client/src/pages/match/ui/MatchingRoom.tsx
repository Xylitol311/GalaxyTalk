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
import {
    useDeleteMatchCancel,
    useMatchApprove,
    useMatchUsersQuery,
} from '@/features/match/api/queries';
import { WaitingUserType } from '@/features/match/model/types';
import { queryClient } from '@/shared/api/query/client';
import { toast } from '@/shared/model/hooks/use-toast';
import { Button } from '@/shared/ui/shadcn/button';
import Galaxy from '@/widget/Galaxy';
import Planet from '@/widget/Planet';
import WarpPage from '../../warp';
import HealingMessage from './HealingMessage';
import TimerConfirm from './TimerConfirm';

export type MatchUserType = {
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
    const { mutate: matchDeleteMutate } = useDeleteMatchCancel();
    const { mutate: matchApproveMutate } = useMatchApprove();
    const { userId } = useUserStore();
    const [matchData, setMatchData] = useState<MatchUserType | null>(null);
    const [isMoving, setIsMoving] = useState(true);
    const { data: userInfos, isSuccess } = useMatchUsersQuery();
    const [userList, setUserList] = useState<WaitingUserType[]>([]);

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            setIsMoving(false);
        }, 6000);

        return () => clearTimeout(timeoutId);
    }, []);

    useEffect(() => {
        if (isSuccess && userInfos.data) {
            // userId와 비교하여 본인을 제외한 유저 리스트로 갱신
            const filteredUsers = userInfos.data.filter(
                (user) => user.userId !== userId
            );
            setUserList([...filteredUsers]);
        }
    }, []);

    useEffect(() => {
        const handleBeforeUnload = (event: BeforeUnloadEvent) => {
            if (matchData?.matchId) {
                matchApproveMutate({
                    matchId: `${matchData?.matchId}`,
                    accepted: false,
                });
                console.log('매치 거절');
            }
            matchDeleteMutate();
            console.log('매치 정보 삭제');

            event.preventDefault();
        };

        window.addEventListener('beforeunload', handleBeforeUnload);

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [matchDeleteMutate, matchApproveMutate, matchData?.matchId]);

    const resetMatchData = () => {
        setMatchData(null);
    };

    const client = new Client({
        brokerURL: `${BASE_URL}/match/ws`,
        webSocketFactory: () => new SockJS(`${BASE_URL}/match/ws`),
        onConnect: () => {
            client.subscribe(`/topic/matching/${userId}`, (message) => {
                console.log('🟢 메시지 수신 시도!'); // 가장 먼저 찍어야 하는 로그
                console.log('message:', message);
                console.log('message.body:', message.body);
                let parsedData;
                try {
                    // message.body가 문자열이라면 파싱, 객체라면 그대로 사용
                    parsedData =
                        typeof message.body === 'string'
                            ? JSON.parse(message.body)
                            : message.body;
                } catch (error) {
                    console.error('Error parsing message.body:', error);
                }
                console.log('parsedData:', parsedData);
                // 이후 parsedData의 type에 따라 처리

                if (parsedData.type === 'MATCH_SUCCESS') {
                    setMatchData({ ...parsedData.data });
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
            client.subscribe('/topic/matching/users/new', (message) => {
                console.log('message.body:', message.body);
                let parsedData;
                try {
                    // message.body가 문자열이라면 파싱, 객체라면 그대로 사용
                    parsedData =
                        typeof message.body === 'string'
                            ? JSON.parse(message.body)
                            : message.body;
                } catch (error) {
                    console.error('Error parsing message.body:', error);
                }
                console.log('parsedData:', parsedData);
                // 이후 parsedData의 type에 따라 처리

                if (parsedData.type === 'NEW_USER') {
                    const newUser = parsedData.data;

                    // userList가 20명 미만일 때만 유저를 추가
                    if (
                        userList.length < 20 &&
                        !userList.some((user) => user.userId === newUser.userId)
                    ) {
                        setUserList((prevList) => {
                            const newList = [...prevList];

                            if (
                                newList.length < 20 &&
                                !newList.some(
                                    (user) => user.userId === newUser.userId
                                ) &&
                                newUser.userId !== userId
                            ) {
                                newList.push({ ...newUser }); // 깊은 복사 적용
                            }

                            return newList;
                        });
                    }
                }
            });

            client.subscribe('/topic/matching/users/exit', (message) => {
                console.log('message.body:', message.body);
                let parsedData;
                try {
                    // message.body가 문자열이라면 파싱, 객체라면 그대로 사용
                    parsedData =
                        typeof message.body === 'string'
                            ? JSON.parse(message.body)
                            : message.body;
                } catch (error) {
                    console.error('Error parsing message.body:', error);
                }
                console.log('parsedData:', parsedData);
                // 이후 parsedData의 type에 따라 처리

                if (parsedData.type === 'EXIT_USER') {
                    const exitedUser = parsedData.data;

                    // userList에서 해당 유저를 제거
                    setUserList((prevList) => {
                        return [
                            ...prevList.filter(
                                (user) => user.userId !== exitedUser.userId
                            ),
                        ];
                    });
                }
            });
        },
    });

    useEffect(() => {
        client.activate();
        return () => {
            client.deactivate();
        };
    }, []);

    const handleToHome = () => {
        if (matchData?.matchId) {
            matchApproveMutate({
                matchId: `${matchData?.matchId}`,
                accepted: false,
            });
        }
        matchDeleteMutate();
        navigate(PATH.ROUTE.HOME);
    };

    return (
        <>
            {isMoving ? (
                <WarpPage />
            ) : (
                <Canvas camera={{ position: [4, 2, 5], fov: 40 }}>
                    <ambientLight intensity={2.5} />

                    <Galaxy />
                    {!!userList.length &&
                        userList.map((userInfo) => {
                            return (
                                <Planet
                                    key={userInfo.userId}
                                    userInfo={userInfo}
                                />
                            );
                        })}

                    <Html
                        position={[0, 0, 0]}
                        center
                        zIndexRange={[0, 0]}
                        style={{ pointerEvents: 'none' }}>
                        <QueryClientProvider client={queryClient}>
                            <div className="relative w-dvw h-dvh flex flex-col justify-between">
                                <Button
                                    variant="link"
                                    className="text-white self-start"
                                    onClick={handleToHome}
                                    style={{ pointerEvents: 'auto' }}>
                                    <ExitIcon />
                                    이전 페이지로 이동하기
                                </Button>
                                {!isMoving && (
                                    <TimerConfirm
                                        matchData={matchData}
                                        handleToHome={handleToHome}
                                        handleResetData={resetMatchData}
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
