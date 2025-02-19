import {
    ControlBarProps,
    TrackToggle,
    useMediaDevices,
    usePersistentUserChoices,
    useTracks,
} from '@livekit/components-react';
import clsx from 'clsx';
import { Track } from 'livekit-client';
import { Mic, MicOff } from 'lucide-react';
import { useCallback } from 'react';
import { useUserStore } from '@/app/model/stores/user';
import useIsMobile from '@/shared/model/hooks/useIsMobile';
import { Button } from '@/shared/ui/shadcn/button';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
} from '@/shared/ui/shadcn/select';

function CustomAudioControl({
    saveUserChoices = true,
    onDeviceError,
}: ControlBarProps) {
    const { userId } = useUserStore();
    const micTracks = useTracks([Track.Source.Microphone]);
    const micTrackRef = micTracks.find(
        (trackRef) => trackRef.participant.identity === userId
    );

    const isAudioEnabled =
        micTrackRef && !micTrackRef?.publication?.track?.isMuted ? true : false;

    const isMobile = useIsMobile();

    const { userChoices, saveAudioInputEnabled, saveAudioInputDeviceId } =
        usePersistentUserChoices({ preventSave: !saveUserChoices });

    const microphoneOnChange = useCallback(
        (enabled: boolean, isUserInitiated: boolean) =>
            isUserInitiated ? saveAudioInputEnabled(enabled) : null,
        [saveAudioInputEnabled]
    );

    const audioDevices = useMediaDevices({ kind: 'audioinput' });
    if (isMobile)
        return (
            <Button
                size="icon"
                variant="ghost"
                className="dark w-12 h-12"
                onClick={() =>
                    microphoneOnChange(
                        userChoices.audioEnabled ? false : true,
                        true
                    )
                }>
                {isAudioEnabled ? (
                    <Mic
                        style={{
                            height: '20px',
                            width: '20px',
                        }}
                    />
                ) : (
                    <MicOff
                        style={{
                            height: '20px',
                            width: '20px',
                        }}
                    />
                )}
            </Button>
        );

    return (
        <Select
            onValueChange={(deviceId) => {
                saveAudioInputDeviceId(deviceId ?? 'default');
            }}>
            <div
                className={clsx(
                    'w-30 h-9 rounded-3xl p-4 flex justify-between items-center mb-2',
                    {
                        'bg-white': userChoices.videoEnabled,
                        'bg-[#F3F3F3]': !userChoices.videoEnabled,
                    }
                )}>
                <TrackToggle
                    source={Track.Source.Microphone}
                    onChange={microphoneOnChange}
                    onDeviceError={(error) =>
                        onDeviceError?.({
                            source: Track.Source.Microphone,
                            error,
                        })
                    }
                    className="p-0 m-0 flex items-center">
                    {isAudioEnabled ? (
                        <span className="ml-1 font-bold text-[#00E600]">
                            ON
                        </span>
                    ) : (
                        <span className="ml-1 font-bold text-[#8B8B8B]">
                            OFF
                        </span>
                    )}
                </TrackToggle>
                <SelectTrigger className="p-0 m-0 !border-none shadow-none flex justify-end ml-2" />
                <SelectContent>
                    {audioDevices.map((device: MediaDeviceInfo) => (
                        <SelectItem
                            key={device.deviceId}
                            value={device.deviceId}>
                            {device.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </div>
        </Select>
    );
}

export default CustomAudioControl;
