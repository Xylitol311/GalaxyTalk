import { SmilePlus } from 'lucide-react';
import React, { useState } from 'react';
import { useReward } from 'react-rewards';
import { Button } from '@/shared/ui/shadcn/button';

function ReactionPanel() {
    const [isReactionPanelOpen, setIsReactionPanelOpen] = useState(false);

    // 각 이모지에 대해 개별적으로 useReward 선언
    const reward0 = useReward('emoji-0', 'emoji', { emoji: ['❤️'] });
    const reward1 = useReward('emoji-1', 'emoji', { emoji: ['👍'] });
    const reward2 = useReward('emoji-2', 'emoji', { emoji: ['🤣'] });
    const reward3 = useReward('emoji-3', 'emoji', { emoji: ['😥'] });
    const reward4 = useReward('emoji-4', 'emoji', { emoji: ['😡'] });
    const reward5 = useReward('emoji-5', 'emoji', { emoji: ['😱'] });

    const rewards = [reward0, reward1, reward2, reward3, reward4, reward5];
    const emojiList = ['❤️', '👍', '🤣', '😥', '😡', '😱'];

    const togglePanel = () => {
        setIsReactionPanelOpen(!isReactionPanelOpen);
    };

    return (
        <div className="relative">
            {/* 이모지 효과를 위한 span들을 패널 밖에 배치 */}
            <div className="absolute">
                {emojiList.map((_, index) => (
                    <span
                        key={`emoji-target-${index}`}
                        id={`emoji-${index}`}
                        className="absolute"
                    />
                ))}
            </div>

            <Button
                variant="secondary"
                className="h-10 w-10"
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
                                onClick={() => {
                                    rewards[index].reward();
                                    setIsReactionPanelOpen(false);
                                }}>
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
