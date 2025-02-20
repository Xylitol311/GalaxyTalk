import {
    Toast,
    useConnectionState,
    useDataChannel,
    useDisconnectButton,
    useParticipants,
    useRoomContext,
} from '@livekit/components-react';
import { RemoteParticipant, RoomEvent } from 'livekit-client';
import { ChevronLeft, Menu } from 'lucide-react';
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
import { useDeleteChatRoom, useGetChatParticipants } from './api/queries';
import { ChatData, Participant } from './model/interfaces';
import AIComponent from './ui/AIComponent';
import AudioRenderer from './ui/AudioRenderer';
import CustomAudioControl from './ui/CustomAudioControl';
import CustomControlBar from './ui/CustomControlBar';
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

    const [myInfo, setMyInfo] = useState<Participant | null>(null);
    const [partnerInfo, setPartnerInfo] = useState<Participant | null>(null);
    const [isLetterModalOpen, setLetterModalOpen] = useState(false);
    const [isLeaveDialogOpen, setLeaveDialogOpen] = useState(false);

    const { mutate: leaveChatRoom } = useDeleteChatRoom();
    const { data: response } = useGetChatParticipants(chatRoomId);

    const { userId: myUserId } = useUserStore();

    const participants = useParticipants();
    const connectionState = useConnectionState();

    useEffect(() => {
        if (response?.success && response.data) {
            const participants = response.data.participants;

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

        room.on(
            RoomEvent.ParticipantDisconnected,
            handleParticipantDisconnected
        );

        return () => {
            room.off(
                RoomEvent.ParticipantDisconnected,
                handleParticipantDisconnected
            );
        };
    }, [room]);

    const handleLeaveChat = () => {
        const messageData = {
            text: `leave room`,
            timestamp: Date.now(),
        };

        const payload = new TextEncoder().encode(JSON.stringify(messageData));

        const options = {
            reliability: true,
            topic: 'leave',
        };

        sendLeave(payload, options);
        console.log('send leave chat');

        leaveChatRoom(chatRoomId);
        setLetterModalOpen(true);
        setLeaveDialogOpen(false);

        setTimeout(() => {
            disconnectButtonProps.onClick();
        }, 1000);
    };

    const { send: sendLeave } = useDataChannel('leave');

    useDataChannel('leave', (msg) => {
        try {
            const decoded = new TextDecoder().decode(msg.payload);
            const leaveData = JSON.parse(decoded);
            console.log(leaveData);
            if (leaveData.text === 'leave room') {
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
                            <AIComponent chatRoomId={chatRoomId} />{' '}
                            {/* AIComponent 사용 */}
                            <Button
                                size="icon"
                                variant="ghost"
                                className="dark w-12 h-12"
                                onClick={() => console.log('menu click')}>
                                <Menu
                                    style={{
                                        height: '20px',
                                        width: '20px',
                                    }}
                                />
                            </Button>
                        </div>
                    </div>
                    <TextChat chatRoomId={chatRoomId} />
                </div>
            ) : (
                <div className="w-full flex justify-center items-center">
                    <div className="max-w-full w-11/12 grid grid-cols-[minmax(200px,1fr)_minmax(300px,1.5fr)_minmax(200px,1fr)] gap-8">
                        <div className="flex flex-col justify-end">
                            <VideoRenderer userId={participants[1]?.identity} />
                            <AudioRenderer userId={participants[1]?.identity} />
                            <div className="bg-slate-300 w-full rounded-lg p-4">
                                <div className="absolute -top-[57px] right-0">
                                    <ReactionPanel
                                        userId={participants[1]?.identity}
                                    />
                                </div>
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
                                    <CustomControlBar
                                        onLeave={handleLeaveChat}
                                    />
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
                                <AIComponent chatRoomId={chatRoomId} />{' '}
                                {/* AIComponent 사용 */}
                            </div>
                            <TextChat chatRoomId={chatRoomId} />
                        </div>
                        <div className="flex justify-end items-end flex-col relative">
                            <VideoRenderer userId={participants[0]?.identity} />
                            <AudioRenderer userId={participants[0]?.identity} />
                            <div className="bg-slate-300 w-full rounded-lg p-4 flex flex-col justify-between relative">
                                <div className="absolute -top-[57px] right-0">
                                    <ReactionPanel
                                        userId={participants[0]?.identity}
                                    />
                                </div>
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
                                    <CustomControlBar
                                        onLeave={handleLeaveChat}
                                    />
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
