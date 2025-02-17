import { useTracks, VideoTrack } from '@livekit/components-react';
import { Track } from 'livekit-client';

function VideoRenderer({ userId }: { userId: string }) {
    const trackRefs = useTracks([Track.Source.Camera]);
    const camTrackRef = trackRefs.find(
        (trackRef) => trackRef.participant.identity === userId
    );

    return (
        <>
            {camTrackRef ? (
                <VideoTrack trackRef={camTrackRef} />
            ) : (
                <div>video is offline</div>
            )}
        </>
    );
}

export default VideoRenderer;
