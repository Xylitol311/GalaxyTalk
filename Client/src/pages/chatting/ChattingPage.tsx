// import '@livekit/components-styles';
import { LiveKitRoom } from '@livekit/components-react';
import { useMutation } from '@tanstack/react-query';
import { Bot, ChevronLeft, ChevronRight, LogOut, Menu } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getPlanetNameById } from '@/app/config/constants/planet';
import { useUserStore } from '@/app/model/stores/user';
import useIsMobile from '@/shared/model/hooks/useIsMobile';
import { Button } from '@/shared/ui/shadcn/button';
import { postAIQuestions } from './api/apis';
import { useDeleteChatRoom, useGetChatParticipants } from './api/queries';
import { ChatData } from './model/interfaces';
import ReactionPanel from './ui/ReactionPanel';
import TextChat from './ui/TextChat';

interface ChattingPageProps {
    chatData: ChatData;
}

interface Question {
    questionId: string;
    content: string;
}

interface Participant {
    userId: string;
    mbti: string;
    concern: string;
    planetId: number;
    energy: number;
}

function ChattingPage({ chatData }: ChattingPageProps) {
    // const { sessionId, token, chatRoomId } = chatData;
    const LIVEKIT_URL = 'wss://i12a503.p.ssafy.io/livekitws/';
    const token =
        'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzNDUiLCJpc3MiOiJnYWxheHkiLCJuYW1lIjoidXNlcjEyMzQ1IiwidmlkZW8iOnsicm9vbUpvaW4iOnRydWUsInJvb20iOiIyZWViYjI0OC1jMGM2LTQ4Y2EtYTc2NC1hNmQ4Zjg3OTg1YTcifSwic2lwIjp7fSwiZXhwIjoxNzM5Mjc4Njk3LCJqdGkiOiJ1c2VyMTIzNDUifQ.lV8Ei3Pt_poX8aVY4F-pR8ULkvEhAGjfVZnMS2P6ivk';
    const chatRoomId = 'xxyy';
    const isMobile = useIsMobile();
    const disconnectButtonProps = {};
    // const { buttonProps: disconnectButtonProps } = useDisconnectButton({});

    const [isAiModalOpen, setAiModalOpen] = useState(false);
    const [AIQuestions, setAIQuestions] = useState<Question[]>([]);
    const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
    const [myInfo, setMyInfo] = useState<Participant | null>(null);
    const [partnerInfo, setPartnerInfo] = useState<Participant | null>(null);

    const { mutate: generateAIQuestions } = useMutation({
        mutationFn: () => postAIQuestions(chatRoomId),
        onSuccess: (response) => {
            setAIQuestions(response.data);
            setAiModalOpen(true);
        },
    });

    const { mutate: leaveChatRoom } = useDeleteChatRoom();
    const { data: response } = useGetChatParticipants(chatRoomId);

    const { userId: myUserId } = useUserStore();

    useEffect(() => {
        if (response?.success && response.data) {
            const participants = response.data.participants;

            // 내 정보와 상대방 정보 분리
            const me = participants.find(
                (p: Participant) => p.userId === myUserId
            );
            const partner = participants.find(
                (p: Participant) => p.userId !== myUserId
            );

            if (me) setMyInfo(me);
            if (partner) setPartnerInfo(partner);
        }
    }, [response]);

    const handleAIQuestionButton = () => {
        if (!AIQuestions.length) {
            generateAIQuestions();
        }
        setAiModalOpen(!isAiModalOpen);
    };

    const handleClickPrevQuestion = () => {
        if (!AIQuestions?.length) return;

        setCurrentQuestionIndex((prev) => {
            // 음수가 되는 경우를 처리하기 위해
            const newIndex = prev - 1;
            return newIndex < 0 ? AIQuestions.length - 1 : newIndex;
        });
    };

    const handleClickNextQuestion = () => {
        if (!AIQuestions?.length) return;

        setCurrentQuestionIndex((prev) => (prev + 1) % AIQuestions.length);
    };

    const handleLeaveChat = () => {
        // disconnectButtonProps?.onclick();
        leaveChatRoom(chatRoomId);
    };

    return (
        <LiveKitRoom
            video={false}
            audio={false}
            token={token}
            serverUrl={LIVEKIT_URL}
            data-lk-theme="default">
            {isMobile ? (
                <div className="h-screen flex flex-col justify-center items-end relative">
                    <div className="w-full h-14 bg-black text-white flex justify-between items-center p-2">
                        <div>
                            <Button
                                size="icon"
                                variant="ghost"
                                className="dark w-12 h-12 "
                                onClick={handleLeaveChat}>
                                <ChevronLeft
                                    style={{
                                        height: '20px',
                                        width: '20px',
                                    }}
                                />
                            </Button>
                        </div>
                        <div>
                            <Button
                                size="icon"
                                variant="ghost"
                                className="dark w-12 h-12"
                                onClick={handleAIQuestionButton}>
                                <Bot
                                    style={{
                                        height: '20px',
                                        width: '20px',
                                    }}
                                />
                            </Button>
                            <Button
                                size="icon"
                                variant="ghost"
                                className="dark w-12 h-12"

                                // onClick={handleClickPrevQuestion}
                            >
                                <Menu
                                    style={{
                                        height: '20px',
                                        width: '20px',
                                    }}
                                />
                            </Button>
                        </div>
                    </div>
                    {isAiModalOpen && AIQuestions.length && (
                        <div className="w-full h-48 z-10 absolute top-14 left-0 bg-gray-300 p-3 rounded-bl-lg rounded-br-lg flex flex-col justify-around">
                            <div className="flex gap-4 items-center">
                                <Button
                                    variant="outline"
                                    className="w-12 h-12"
                                    onClick={handleAIQuestionButton}>
                                    <Bot
                                        style={{
                                            height: '20px',
                                            width: '20px',
                                        }}
                                    />
                                </Button>
                                <p className="font-medium text-black">
                                    의 추천 질문!
                                </p>
                            </div>
                            <div className="w-full h-24 bg-white rounded-bl-lg rounded-br-lg p-2 flex justify-between items-center">
                                <Button
                                    size="icon"
                                    variant="ghost"
                                    onClick={handleClickPrevQuestion}>
                                    <ChevronLeft size={20} />
                                </Button>
                                <div>
                                    {AIQuestions[currentQuestionIndex].content}
                                </div>
                                <Button
                                    size="icon"
                                    variant="ghost"
                                    onClick={handleClickNextQuestion}>
                                    <ChevronRight size={20} />
                                </Button>
                            </div>
                        </div>
                    )}
                    <TextChat chatRoomId={chatRoomId} />
                </div>
            ) : (
                <div className="w-full h-screen flex justify-center items-center">
                    <div className="h-screen max-w-full w-11/12 grid grid-cols-[minmax(200px,1fr)_minmax(300px,1.5fr)_minmax(200px,1fr)] gap-8">
                        <div className="flex justify-center items-end my-10">
                            <div className="bg-slate-300 w-full h-2/4 rounded-lg p-6">
                                <h1 className="text-2xl">
                                    {partnerInfo?.planetId
                                        ? getPlanetNameById(
                                              partnerInfo.planetId
                                          )
                                        : ''}
                                    &nbsp;여행자
                                </h1>
                                <p>
                                    나누고 싶은 이야기: {partnerInfo?.concern}
                                    <br /> 친구의 성향: {partnerInfo?.mbti}
                                    <br /> 친구의 매너온도:{' '}
                                    {partnerInfo?.energy}°C
                                </p>
                            </div>
                        </div>
                        <div className="flex justify-center items-end my-10 relative">
                            {!isAiModalOpen ? (
                                <div
                                    className="w-full h-32 z-10 absolute top-0 left-0 p-2"
                                    style={{
                                        backgroundImage:
                                            'linear-gradient(180deg, #000000 0%, #0c0c0c88 50%, #66666600 100%)',
                                    }}>
                                    <div className="flex gap-4 items-center">
                                        <Button
                                            variant="outline"
                                            className="w-16 h-16"
                                            onClick={handleAIQuestionButton}>
                                            <Bot
                                                style={{
                                                    height: '32px',
                                                    width: '32px',
                                                }}
                                            />
                                        </Button>
                                        <p className="text-white font-medium">
                                            도움이 필요하신가요? <br /> AI에게
                                            도움을 요청하세요!
                                        </p>
                                    </div>
                                </div>
                            ) : AIQuestions.length ? (
                                <div className="w-full h-48 z-10 absolute top-0 left-0 bg-gray-300 p-2 rounded-lg flex flex-col justify-between">
                                    <div className="flex gap-4 items-center">
                                        <Button
                                            variant="outline"
                                            className="w-16 h-16"
                                            onClick={handleAIQuestionButton}>
                                            <Bot
                                                style={{
                                                    height: '32px',
                                                    width: '32px',
                                                }}
                                            />
                                        </Button>
                                        <p className="font-medium text-black">
                                            의 추천 질문!
                                        </p>
                                    </div>
                                    <div className="w-full h-24 bg-white rounded-bl-lg rounded-br-lg p-2 flex justify-between items-center">
                                        <Button
                                            size="icon"
                                            variant="ghost"
                                            onClick={handleClickPrevQuestion}>
                                            <ChevronLeft size={20} />
                                        </Button>
                                        <div className="px-4 py-2 text-center">
                                            {
                                                AIQuestions[
                                                    currentQuestionIndex
                                                ].content
                                            }
                                        </div>
                                        <Button
                                            size="icon"
                                            variant="ghost"
                                            onClick={handleClickNextQuestion}>
                                            <ChevronRight size={20} />
                                        </Button>
                                    </div>
                                </div>
                            ) : (
                                ''
                            )}
                            <TextChat chatRoomId={chatRoomId} />
                        </div>
                        <div className="flex justify-end items-end my-10 flex-col">
                            <div className="mb-2">
                                <ReactionPanel />
                            </div>
                            <div className="bg-slate-300 w-full h-2/4 rounded-lg p-6 flex flex-col justify-between relative">
                                <div>
                                    <h1 className="text-2xl">
                                        {myInfo?.planetId
                                            ? getPlanetNameById(myInfo.planetId)
                                            : ''}
                                        &nbsp;여행자
                                    </h1>
                                    <p>
                                        나누고 싶은 이야기: {myInfo?.concern}
                                        <br /> 나의 성향: {myInfo?.mbti}
                                        <br /> 나의 매너온도: {myInfo?.energy}
                                        °C
                                    </p>
                                </div>
                                <div className="">
                                    <Button
                                        onClick={handleLeaveChat}
                                        disabled={
                                            disconnectButtonProps?.disabled
                                        }
                                        className="bg-[#009951] hover:bg-[#009951]/80 font-medium">
                                        <LogOut size={28} />
                                        나가기
                                    </Button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </LiveKitRoom>
    );
}

export default ChattingPage;
