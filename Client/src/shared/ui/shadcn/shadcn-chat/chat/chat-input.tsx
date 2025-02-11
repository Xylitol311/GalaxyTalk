import * as React from 'react';
import { cn } from '@/shared/lib/utils';
import { AutosizeTextarea } from '../../autosizetextarea';

interface ChatInputProps
    extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
    maxHeight?: number;
    minHeight?: number;
}

const ChatInput = React.forwardRef<HTMLTextAreaElement, ChatInputProps>(
    ({ className, ...props }, ref) => (
        <AutosizeTextarea
            autoComplete="off"
            // ref={ref}
            name="message"
            className={cn(
                'max-h-12 px-4 py-3 bg-background text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 w-full rounded-md flex items-center h-16 resize-none',
                className
            )}
            {...props}
        />
    )
);
ChatInput.displayName = 'ChatInput';

export { ChatInput };
