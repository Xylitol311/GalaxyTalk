import { ExitIcon } from '@radix-ui/react-icons';
import { Html } from '@react-three/drei';
import { Canvas } from '@react-three/fiber';
import { Bloom, EffectComposer } from '@react-three/postprocessing';
import { Client } from '@stomp/stompjs';
import { QueryClientProvider } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import SockJS from 'sockjs-client';
import * as THREE from 'three';
import { PATH } from '@/app/config/constants';
import { BASE_URL } from '@/app/config/constants/path';
import { useUserStore } from '@/app/model/stores/user';
import {
    useDeleteMatchCancel,
    useMatchUsersQuery,
} from '@/features/match/api/queries';
import { WaitingUserType } from '@/features/match/model/types';
import { queryClient } from '@/shared/api/query/client';
import { toast } from '@/shared/model/hooks/use-toast';
import { Button } from '@/shared/ui/shadcn/button';
import Galaxy from '@/widget/Galaxy';
import Planet from '@/widget/Planet';
import WarpPage from '../warp';
import HealingMessage from './ui/HealingMessage';
import TimerConfirm from './ui/TimerConfirm';

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
    const { mutate } = useDeleteMatchCancel();
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
        if (isSuccess && userInfos) {
            // userId와 비교하여 본인을 제외한 유저 리스트로 갱신
            const filteredUsers = userInfos.data.filter(
                (user) => user.userId !== userId
            );
            setUserList({ ...filteredUsers });
        }
    }, []);

    const resetMatchData = () => {
        setMatchData(null);
    };

    const client = new Client({
        brokerURL: `${BASE_URL}/match/ws`,
        webSocketFactory: () => new SockJS(`${BASE_URL}/match/ws`),
        onConnect: () => {
            client.subscribe(`/topic/matching/${userId}`, (message) => {
                const parsedData = JSON.parse(message.body);

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
            client.subscribe('/topic/matching/users/new', (message) => {
                const parsedData = JSON.parse(message.body);

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
                console.log(`Received: ${message.body}`);
            });

            client.subscribe('/topic/matching/users/exit', (message) => {
                const parsedData = JSON.parse(message.body);

                if (parsedData.type === 'EXIT_USER') {
                    const exitedUser = parsedData.data;

                    // userList에서 해당 유저를 제거
                    setUserList((prevList) => {
                        return {
                            ...prevList.filter(
                                (user) => user.userId !== exitedUser.userId
                            ),
                        };
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
                    {!!userList.length &&
                        userList.map((userInfo, index) => {
                            return (
                                <Planet
                                    key={userInfo.userId}
                                    position={fixedPositions[index]}
                                    color={fixedColors[index]}
                                    userInfo={userInfo}
                                />
                            );
                        })}
                    <EffectComposer>
                        <Bloom intensity={0.3} radius={0.4} threshold={0.1} />
                    </EffectComposer>

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

const fixedColors: THREE.Color[] = [
    new THREE.Color('#4B0082'), // Indigo (어두운 보라색)
    new THREE.Color('#8A2BE2'), // BlueViolet (보라색)
    new THREE.Color('#6A5ACD'), // SlateBlue (어두운 파랑)
    new THREE.Color('#483D8B'), // DarkSlateBlue (어두운 청록)
    new THREE.Color('#20B2AA'), // LightSeaGreen (청록색)
    new THREE.Color('#1E90FF'), // DodgerBlue (밝은 파랑)
    new THREE.Color('#7B68EE'), // MediumSlateBlue (중간톤의 파랑)
    new THREE.Color('#8B008B'), // DarkMagenta (어두운 자주색)
    new THREE.Color('#556B2F'), // DarkOliveGreen (어두운 올리브)
    new THREE.Color('#D3D3D3'), // LightGray (밝은 회색)
    new THREE.Color('#2F4F4F'), // DarkSlateGray (어두운 회색)
    new THREE.Color('#00CED1'), // DarkTurquoise (어두운 청록)
    new THREE.Color('#191970'), // MidnightBlue (미드나잇 블루)
    new THREE.Color('#800080'), // Purple (보라색)
    new THREE.Color('#C71585'), // MediumVioletRed (보라색과 빨강 혼합)
    new THREE.Color('#B0C4DE'), // LightSteelBlue (연한 철강색)
    new THREE.Color('#E6E6FA'), // Lavender (연한 보라색)
    new THREE.Color('#A9A9A9'), // DarkGray (어두운 회색)
    new THREE.Color('#4682B4'), // SteelBlue (강철색 파랑)
    new THREE.Color('#00008B'), // DarkBlue (어두운 파랑)
];

const fixedPositions: [number, number, number][] = [
    [-1, 1, 0.5],
    [1.5, -1, 1.5],
    [-0.5, -2, 1],
    [1, 0.5, -1.5],
    [0, 1.5, 2],
    [0.5, 2, -1],
    [-2, -1.5, 1.5],
    [1.5, 0, -0.5],
    [1, 1.5, 0.5],
    [-1, 0.5, 1.5],
    [2, -0.5, -1],
    [-0.5, 1.5, -1],
    [1.5, 2, 0],
    [-1.5, 0, -2],
    [0.5, -1.5, 1],
    [-2, 1, -0.5],
    [0, -1, 1.5],
    [-1, 2, 0],
    [1, -0.5, -2],
    [-1.5, 0.5, 1],
];
