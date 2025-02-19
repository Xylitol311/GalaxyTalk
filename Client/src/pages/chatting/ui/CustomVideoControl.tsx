import {
    ControlBarProps,
    TrackToggle,
    useMediaDevices,
    usePersistentUserChoices,
    useTracks,
} from '@livekit/components-react';
import clsx from 'clsx';
import { Track } from 'livekit-client';
import { useCallback } from 'react';
import { useUserStore } from '@/app/model/stores/user';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
} from '@/shared/ui/shadcn/select';

function CustomVideoControl({
    saveUserChoices = true,
    onDeviceError,
}: ControlBarProps) {
    const { userId } = useUserStore();

    const cameraTracks = useTracks([Track.Source.Camera]);
    const camTrackRef = cameraTracks.find(
        (trackRef) => trackRef.participant.identity === userId
    );
    const isVideoEnabled =
        camTrackRef?.publication?.track?.mediaStream?.active ?? false;

    const { userChoices, saveVideoInputEnabled, saveVideoInputDeviceId } =
        usePersistentUserChoices({ preventSave: !saveUserChoices });

    const cameraOnChange = useCallback(
        (enabled: boolean, isUserInitiated: boolean) =>
            isUserInitiated ? saveVideoInputEnabled(enabled) : null,
        [saveVideoInputEnabled]
    );

    const videoDevices = useMediaDevices({ kind: 'videoinput' });

    return (
        <Select
            onValueChange={(deviceId) => {
                saveVideoInputDeviceId(deviceId ?? 'default');
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
                    source={Track.Source.Camera}
                    onChange={cameraOnChange}
                    onDeviceError={(error) =>
                        onDeviceError?.({ source: Track.Source.Camera, error })
                    }
                    className="p-0 m-0 flex items-center">
                    {isVideoEnabled ? (
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
                    {videoDevices.map((device) => (
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

export default CustomVideoControl;
