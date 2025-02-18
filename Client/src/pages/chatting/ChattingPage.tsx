// import '@livekit/components-styles';
import {
    Toast,
    useConnectionState,
    useDataChannel,
    useDisconnectButton,
    useParticipants,
    useRoomContext,
} from '@livekit/components-react';
import { useMutation } from '@tanstack/react-query';
import { RemoteParticipant, RoomEvent } from 'livekit-client';
import { Bot, ChevronLeft, ChevronRight, LogOut, Menu } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getPlanetNameById } from '@/app/config/constants/planet';
import { useUserStore } from '@/app/model/stores/user';
import useIsMobile from '@/shared/model/hooks/useIsMobile';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/shared/ui/shadcn/alert-dialog';
import { Button } from '@/shared/ui/shadcn/button';
import { postAIQuestions } from './api/apis';
import { useDeleteChatRoom, useGetChatParticipants } from './api/queries';
import { AIQuestion, ChatData, Participant } from './model/interfaces';
import AudioRenderer from './ui/AudioRenderer';
import CustomAudioControl from './ui/CustomAudioControl';
import CustomVideoControl from './ui/CustomVideoControl';
import LetterFormModal from './ui/LetterFormModal';
import MbtiTag from './ui/MbtiTag';
import ReactionPanel from './ui/ReactionPanel';
import TemperatureTag from './ui/TemperatureTag';
import TextChat from './ui/TextChat';
import VideoRenderer from './ui/VideoRenderer';

interface ChattingPageProps {
    chatData: ChatData;
}

function ChattingPage({ chatData }: ChattingPageProps) {
    const { sessionId, token, chatRoomId } = chatData;

    const isMobile = useIsMobile();
    const { buttonProps: disconnectButtonProps } = useDisconnectButton({});

    const [isAiModalOpen, setAiModalOpen] = useState(false);
    const [AIQuestions, setAIQuestions] = useState<AIQuestion[]>([]);
    const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
    const [myInfo, setMyInfo] = useState<Participant | null>(null);
    const [partnerInfo, setPartnerInfo] = useState<Participant | null>(null);
    const [isLetterModalOpen, setLetterModalOpen] = useState(false);
    const [isLeaveDialogOpen, setLeaveDialogOpen] = useState(false);

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

    const participants = useParticipants();
    const connectionState = useConnectionState();

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
    }, [response, myUserId]);

    const room = useRoomContext();

    useEffect(() => {
        if (!room) return;

        const handleParticipantDisconnected = (
            participant: RemoteParticipant
        ) => {
            console.log('상대방이 나갔습니다.', participant);
        };

        // RoomEvent를 이용해 이벤트 리스너 등록
        room.on(
            RoomEvent.ParticipantDisconnected,
            handleParticipantDisconnected
        );

        return () => {
            // 컴포넌트 언마운트 또는 room 변경 시 리스너 해제
            room.off(
                RoomEvent.ParticipantDisconnected,
                handleParticipantDisconnected
            );
        };
    }, [room]);

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
        // 나가기 메시지 데이터 준비
        const messageData = {
            text: `leave room`,
            timestamp: Date.now(),
        };

        // JSON 문자열로 변환 후 Uint8Array로 인코딩
        const payload = new TextEncoder().encode(JSON.stringify(messageData));

        // DataChannel 메시지 옵션 (topic은 'leave')
        const options = {
            reliability: true,
            topic: 'leave',
        };

        // 나가기 메시지 전송
        sendLeave(payload, options);
        console.log('send leave chat');

        leaveChatRoom(chatRoomId);
        setLetterModalOpen(true);
        setLeaveDialogOpen(false);

        setTimeout(() => {
            disconnectButtonProps.onClick();
        }, 1000);
    };

    // 'leave' topic으로 메시지를 전송할 send 함수를 가져옵니다.
    const { send: sendLeave } = useDataChannel('leave');

    // 'leave' topic 메시지를 수신하여 상대방이 나갔음을 알립니다.
    useDataChannel('leave', (msg) => {
        try {
            const decoded = new TextDecoder().decode(msg.payload);
            const leaveData = JSON.parse(decoded);
            console.log(leaveData);
            if (leaveData.text === 'leave room') {
                // 상대방이 채팅방을 나갔다는 메시지를 받으면 AlertDialog를 열도록 상태 변경
                console.log('receive leave chat');
                setLeaveDialogOpen(true);
            }
        } catch (error) {
            console.error('Failed to process leave message', error);
        }
    });

    if (connectionState !== 'connected') {
        return (
            <>
                <Toast className="text-white">Connecting...</Toast>
                {isLetterModalOpen && partnerInfo && (
                    <LetterFormModal
                        chatRoomId={chatRoomId}
                        receiverId={partnerInfo?.userId}
                    />
                )}
            </>
        );
    }

    return (
        <>
            <AlertDialog
                open={isLeaveDialogOpen}
                onOpenChange={setLeaveDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>
                            상대방이 채팅방을 나갔습니다
                        </AlertDialogTitle>
                        <AlertDialogDescription>
                            채팅이 종료되었어요.
                            <br />
                            채팅방을 나가시겠어요?
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>아니오</AlertDialogCancel>
                        <AlertDialogAction onClick={handleLeaveChat}>
                            네
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {isMobile ? (
                <div className="flex flex-col h-screen justify-center items-end relative">
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
                        <div className="flex items-center">
                            <CustomAudioControl />
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
                                onClick={handleClickPrevQuestion}>
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
                <div className="w-full flex justify-center items-center">
                    <div className="max-w-full w-11/12 grid grid-cols-[minmax(200px,1fr)_minmax(300px,1.5fr)_minmax(200px,1fr)] gap-8">
                        <div className="flex flex-col justify-end">
                            <VideoRenderer userId={participants[1]?.identity} />
                            <AudioRenderer userId={participants[1]?.identity} />
                            <div className="mb-2">
                                <ReactionPanel
                                    userId={participants[1]?.identity}
                                />
                            </div>
                            <div className="bg-slate-300 w-full rounded-lg px-4 py-6  mt-2">
                                <h1 className="text-2xl font-bold mb-4">
                                    {partnerInfo?.planetId
                                        ? getPlanetNameById(
                                              partnerInfo.planetId
                                          )
                                        : ''}
                                    &nbsp;여행자
                                </h1>
                                <div className="mb-8">
                                    <h2 className="font-bold mb-1">
                                        나누고 싶은 이야기
                                    </h2>
                                    <p className="min-h-28 line-clamp-6 text-sm mb-2">
                                        {partnerInfo?.concern}
                                    </p>
                                    <MbtiTag mbti={partnerInfo?.mbti} />
                                    <TemperatureTag
                                        energy={partnerInfo?.energy}
                                    />
                                </div>
                                <div className="invisible">
                                    <div className="flex justify-around flex-wrap">
                                        <CustomAudioControl />
                                        <CustomVideoControl />
                                    </div>
                                    <div className="flex justify-center ">
                                        <Button
                                            onClick={handleLeaveChat}
                                            disabled={
                                                disconnectButtonProps.disabled
                                            }
                                            className="bg-[#009951] hover:bg-[#009951]/80 font-medium">
                                            <LogOut size={28} />
                                            나가기
                                        </Button>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div className="flex flex-col justify-center items-end relative">
                            <div
                                className="w-full p-2 flex items-center"
                                style={{
                                    backgroundImage:
                                        'linear-gradient(180deg, #000000 0%, #0c0c0c88 50%, #0000000 100%)',
                                }}>
                                <Button
                                    variant="outline"
                                    className="w-16 h-16 mr-4"
                                    onClick={handleAIQuestionButton}>
                                    <Bot
                                        style={{
                                            height: '32px',
                                            width: '32px',
                                        }}
                                    />
                                </Button>
                                {!isAiModalOpen ? (
                                    <p className="text-white font-medium">
                                        도움이 필요하신가요? <br /> AI에게
                                        도움을 요청하세요!
                                    </p>
                                ) : AIQuestions.length ? (
                                    <div className="absolute top-0 left-0 z-50 w-full h-48 bg-gray-300 p-2 rounded-lg flex flex-col justify-between">
                                        <div className="flex gap-4 flex-col">
                                            <div className="flex items-center">
                                                <Button
                                                    variant="outline"
                                                    className="w-16 h-16 mr-2"
                                                    onClick={
                                                        handleAIQuestionButton
                                                    }>
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
                                                    onClick={
                                                        handleClickPrevQuestion
                                                    }>
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
                                                    onClick={
                                                        handleClickNextQuestion
                                                    }>
                                                    <ChevronRight size={20} />
                                                </Button>
                                            </div>
                                        </div>
                                    </div>
                                ) : (
                                    ''
                                )}
                            </div>
                            <TextChat chatRoomId={chatRoomId} />
                        </div>
                        <div className="flex justify-end items-end flex-col">
                            <VideoRenderer userId={participants[0]?.identity} />
                            <AudioRenderer userId={participants[0]?.identity} />
                            <div className="mb-2">
                                <ReactionPanel
                                    userId={participants[0]?.identity}
                                />
                            </div>
                            <div className="bg-slate-300 w-full rounded-lg px-4 py-6 flex flex-col justify-between relative">
                                <div>
                                    <h1 className="text-2xl font-bold mb-4">
                                        {myInfo?.planetId
                                            ? getPlanetNameById(myInfo.planetId)
                                            : ''}
                                        &nbsp;여행자
                                    </h1>
                                    <div className="mb-8">
                                        <h2 className="font-bold mb-1">
                                            나누고 싶은 이야기
                                        </h2>
                                        <p className="min-h-28 line-clamp-6 text-sm mb-2">
                                            {myInfo?.concern}
                                        </p>
                                        <MbtiTag mbti={myInfo?.mbti} />
                                        <TemperatureTag
                                            energy={myInfo?.energy}
                                        />
                                    </div>
                                </div>
                                <div>
                                    <div className="flex justify-around flex-wrap">
                                        <CustomAudioControl />
                                        <CustomVideoControl />
                                    </div>
                                    <div className="flex justify-center">
                                        <Button
                                            onClick={handleLeaveChat}
                                            disabled={
                                                disconnectButtonProps.disabled
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
                </div>
            )}
        </>
    );
}

export default ChattingPage;
