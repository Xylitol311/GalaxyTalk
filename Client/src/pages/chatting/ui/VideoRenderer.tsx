import { useTracks, VideoTrack } from '@livekit/components-react';
import { Track } from 'livekit-client';
import { useEffect, useRef, useState } from 'react';
import { IMAGE_PATH } from '@/app/config/constants/path';
import AudioVisualizer from './AudioVisualizer';

function VideoRenderer({ userId }: { userId: string }) {
    const cameraTracks = useTracks([Track.Source.Camera]);
    const camTrackRef = cameraTracks.find(
        (trackRef) => trackRef.participant.identity === userId
    );
    const isVideoEnabled =
        camTrackRef && !camTrackRef?.publication?.track?.isMuted ? true : false;

    const micTracks = useTracks([Track.Source.Microphone]);
    const micTrackRef = micTracks.find(
        (trackRef) => trackRef.participant.identity === userId
    );

    const isAudioEnabled =
        micTrackRef && !micTrackRef?.publication?.track?.isMuted ? true : false;

    // container ref와 크기를 저장할 상태
    const containerRef = useRef(null);
    const [containerSize, setContainerSize] = useState({
        width: 350,
        height: 150,
    });

    useEffect(() => {
        function updateContainerSize() {
            if (containerRef.current) {
                const { width, height } =
                    containerRef.current.getBoundingClientRect();
                setContainerSize({ width, height });
            }
        }

        // 창 크기가 변경될 때마다 container 크기를 업데이트
        window.addEventListener('resize', updateContainerSize);

        setTimeout(() => {
            // 컴포넌트가 마운트될 때 한 번 실행해서 초기 크기를 설정
            updateContainerSize();
        }, 1000);

        return () => {
            window.removeEventListener('resize', updateContainerSize);
        };
    }, [isVideoEnabled]);

    // container의 크기를 기준으로 AudioVisualizer에 전달할 값들을 계산
    const newWidth = containerSize.width;
    const newHeight = containerSize.height;
    // 예시: 막대의 최소 너비를 10px로 가정하고 그룹 개수를 산출 (최소 10개)
    const newGroupCount = Math.max(1, Math.floor(newWidth / 10));
    // 예시: spacing은 container의 너비에 따라 계산 (최소 5)
    const newSpacing = Math.max(5, Math.floor(newWidth / 100));

    if (!!camTrackRef && isVideoEnabled) {
        return (
            <>
                <div>dfdf</div>
                <VideoTrack trackRef={camTrackRef} />
            </>
        );
        return <VideoTrack trackRef={camTrackRef} />;
    }

    // placeholder: audio visualizer
    else if (isAudioEnabled) {
        return (
            <div ref={containerRef} className="bg-black w-full text-white">
                <AudioVisualizer
                    micTrackRef={micTrackRef}
                    groupCount={newGroupCount}
                    width={newWidth}
                    height={newHeight}
                    color="#FFFFFF80"
                    spacing={newSpacing}
                />
            </div>
        );
    }

    return (
        <div className="flex text-white bg-gray-500/60 w-full h-full justify-center items-end rounded-xl">
            <img
                src={`${IMAGE_PATH}chat-profile.png`}
                alt="Chat Profile"
                className="w-[45%] object-contain"
            />
        </div>
    );
}

export default VideoRenderer;
