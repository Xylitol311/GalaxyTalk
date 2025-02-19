import { useDisconnectButton } from '@livekit/components-react';
import { LogOut } from 'lucide-react';
import { Button } from '@/shared/ui/shadcn/button';
import CustomAudioControl from './CustomAudioControl';
import CustomVideoControl from './CustomVideoControl';

function CustomControlBar({ onLeave }: { onLeave: () => void }) {
    const { buttonProps: disconnectButtonProps } = useDisconnectButton({});

    return (
        <div className="flex justify-around flex-wrap space-x-1">
            <CustomAudioControl />
            <CustomVideoControl />
            <Button
                onClick={onLeave}
                disabled={disconnectButtonProps.disabled}
                className="bg-[#009951] hover:bg-[#009951]/80 font-medium">
                <LogOut size={28} />
                나가기
            </Button>
        </div>
    );
}

export default CustomControlBar;
