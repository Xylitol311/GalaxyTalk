import { AudioTrack, useTracks } from '@livekit/components-react';
import { getEmptyAudioStreamTrack, Track } from 'livekit-client';
import { useEffect } from 'react';
import * as Tone from 'tone';

function AudioRenderer({ userId }: { userId: string }) {
    const trackRefs = useTracks([Track.Source.Microphone]);
    const micTrackRef = trackRefs.find(
        (trackRef) => trackRef.participant.identity === userId
    );

    return (
        <>
            {micTrackRef ? (
                <div className="bg-black w-full">
                    <AudioTrack trackRef={micTrackRef} />
                    <AudioModulator />
                </div>
            ) : (
                <div>audio is offline</div>
            )}
        </>
    );
}

// Tone.js를 이용해 오디오에 변조 효과를 추가하는 컴포넌트
function AudioModulator() {
    useEffect(() => {
        // MediaStream 생성 (오디오 트랙 포함)
        const mediaStream = new MediaStream([getEmptyAudioStreamTrack()]);
        console.log('MediaStream:', mediaStream);

        if (!mediaStream) return;

        // Tone.js의 AudioContext를 사용해 MediaStreamAudioSourceNode 생성
        const audioContext = Tone.getContext();
        const sourceNode = audioContext.createMediaStreamSource(mediaStream);

        //-------------------------------
        // 모듈레이션 체인 구성 (Tone.js 방식)
        //-------------------------------
        // 1. 모듈레이터 오실레이터: 예를 들어 30Hz 사인파
        const modOsc = new Tone.Oscillator({
            frequency: 440,
            type: 'square',
            detune: 1000,
            mute: false,
        }).start();

        // 2. Tone.Gain 객체 생성 (모듈레이터의 곱셈 효과를 위해 사용)
        const modulatedGain = new Tone.Gain(20);
        modulatedGain.toDestination();

        // 3. 모듈레이터 오실레이터를 Tone.Gain의 gain(AudioParam)에 연결
        modOsc.connect(modulatedGain.gain);

        // 4. 외부 AudioNode(sourceNode)를 Tone.Gain의 input(AudioNode)에 연결
        sourceNode.connect(modulatedGain.input);

        // AudioContext 실행 (사용자 제스처가 필요할 수 있음)
        if (audioContext.state !== 'running') {
            audioContext.resume();
        }

        return () => {
            // 컴포넌트 언마운트 시 자원 해제
            modOsc.dispose();
            modulatedGain.dispose();
        };
    }, []);

    return null;
}

export default AudioRenderer;
