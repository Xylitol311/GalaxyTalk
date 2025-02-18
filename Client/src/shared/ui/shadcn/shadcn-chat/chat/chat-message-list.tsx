import * as React from 'react';
import { useAutoScroll } from '../hooks/useAutoScroll';

interface ChatMessageListProps extends React.HTMLAttributes<HTMLDivElement> {
    smooth?: boolean;
}

const ChatMessageList = React.forwardRef<HTMLDivElement, ChatMessageListProps>(
    ({ className, children, smooth = false, ...props }, _ref) => {
        const {
            scrollRef,
            isAtBottom,
            autoScrollEnabled,
            scrollToBottom,
            disableAutoScroll,
        } = useAutoScroll({
            smooth,
            content: children,
        });

        return (
            <div className="w-full">
                <div
                    className={`flex flex-col w-full h-[78vh] p-4 overflow-y-auto [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none] ${className}`}
                    ref={scrollRef}
                    onWheel={disableAutoScroll}
                    onTouchMove={disableAutoScroll}
                    {...props}>
                    <div className="flex flex-col gap-6">{children}</div>
                </div>
            </div>
        );
    }
);

ChatMessageList.displayName = 'ChatMessageList';

export { ChatMessageList };
