import { useDataChannel, useLocalParticipant } from '@livekit/components-react';
import { SmilePlus } from 'lucide-react';
import React, { useState } from 'react';
import { useReward } from 'react-rewards';
import { Button } from '@/shared/ui/shadcn/button';

function ReactionPanel({ userId }: { userId: string }) {
    const [isReactionPanelOpen, setIsReactionPanelOpen] = useState(false);

    const { localParticipant } = useLocalParticipant();
    const isMyPanel = userId === localParticipant.identity;

    // 각 이모지에 대해 개별적으로 useReward 선언
    const reward0 = useReward(`emoji-${userId}-0`, 'emoji', { emoji: ['❤️'] });
    const reward1 = useReward(`emoji-${userId}-1`, 'emoji', { emoji: ['👍'] });
    const reward2 = useReward(`emoji-${userId}-2`, 'emoji', { emoji: ['🤣'] });
    const reward3 = useReward(`emoji-${userId}-3`, 'emoji', { emoji: ['😥'] });
    const reward4 = useReward(`emoji-${userId}-4`, 'emoji', { emoji: ['😡'] });
    const reward5 = useReward(`emoji-${userId}-5`, 'emoji', { emoji: ['😱'] });

    const rewards = [reward0, reward1, reward2, reward3, reward4, reward5];
    const emojiList = ['❤️', '👍', '🤣', '😥', '😡', '😱'];

    const togglePanel = () => {
        setIsReactionPanelOpen(!isReactionPanelOpen);
    };

    // reaction 데이터를 보내기 위한 전용 send 함수 (topic: "reaction")
    const { send: sendReaction } = useDataChannel('reaction');

    // 다른 참가자들로부터 reaction 메시지를 수신하면 해당 리워드 실행
    useDataChannel('reaction', (msg) => {
        try {
            // msg.payload는 Uint8Array 형태이므로 디코딩 후 JSON으로 파싱합니다.
            const decoded = new TextDecoder().decode(msg.payload);
            const reactionData = JSON.parse(decoded);
            const index = reactionData.text;
            // index가 유효하면 해당 이모지 효과 실행
            if (
                typeof index === 'number' &&
                rewards[index] &&
                userId === msg.from?.identity
            ) {
                rewards[index].reward();
            }
        } catch (error) {
            console.error('Failed to process reaction message', error);
        }
    });

    const handleSendReaction = (emojiIndex: number) => {
        // 로컬에서 즉시 리워드를 실행 (본인에게도 반영되도록)
        rewards[emojiIndex].reward();
        setIsReactionPanelOpen(false);

        // 보낼 데이터 준비: 여기서는 선택한 이모지 index와 timestamp를 포함
        const messageData = {
            text: emojiIndex,
            timestamp: Date.now(),
        };

        // JSON 문자열로 변환 후 Uint8Array로 인코딩
        const payload = new TextEncoder().encode(JSON.stringify(messageData));

        // DataPublishOptions에 topic을 지정하여 reaction 메시지임을 명시
        const options = {
            reliability: true,
            topic: 'reaction',
        };
        sendReaction(payload, options);
    };

    return (
        <div className="relative">
            {/* 이모지 효과를 위한 span들을 패널 밖에 배치 */}
            <div className="absolute">
                {emojiList.map((_, index) => (
                    <span
                        key={`emoji-target-${index}`}
                        id={`emoji-${userId}-${index}`}
                        className="absolute"
                    />
                ))}
            </div>

            <Button
                variant="secondary"
                className={`h-10 w-10 ${isMyPanel ? 'visible' : 'invisible'}`}
                onClick={togglePanel}>
                <SmilePlus style={{ height: '20px', width: '20px' }} />
            </Button>

            {isReactionPanelOpen ? (
                <div className="absolute -top-60 right-0 bg-white w-10 flex flex-col rounded-full p-2 items-center">
                    {emojiList.map((emoji, index) => (
                        <React.Fragment key={`emoji-container-${index}`}>
                            <Button
                                // id={`emoji-${index}`}
                                variant="ghost"
                                size="icon"
                                className="text-lg"
                                onClick={() => handleSendReaction(index)}>
                                {emoji}
                            </Button>
                        </React.Fragment>
                    ))}
                </div>
            ) : null}
        </div>
    );
}

export default ReactionPanel;
