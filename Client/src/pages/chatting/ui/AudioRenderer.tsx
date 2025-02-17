import {
    AudioTrack,
    TrackReference,
    useAudioWaveform,
    useTracks,
} from '@livekit/components-react';
import { getEmptyAudioStreamTrack, Track } from 'livekit-client';
import { useEffect, useRef } from 'react';
import * as Tone from 'tone';
import { throttle } from '@/shared/lib/utils';

function AudioRenderer({ userId }: { userId: string }) {
    const trackRefs = useTracks([Track.Source.Microphone]);
    const micTrackRef = trackRefs.find(
        (trackRef) => trackRef.participant.identity === userId
    );

    return (
        <>
            {micTrackRef ? (
                <div className="flex justify-center w-full pl-[10px]">
                    <AudioTrack trackRef={micTrackRef} />
                    <AudioVisualizer
                        micTrackRef={micTrackRef}
                        groupCount={7}
                        width={200}
                        height={100}
                        color={'grey'}
                        spacing={10}
                    />
                    <AudioModulator />
                </div>
            ) : (
                <div>audio is offline</div>
            )}
        </>
    );
}

interface AudioVisualizerProps {
    micTrackRef: TrackReference;
    groupCount: number;
    width: number;
    height: number;
    color: string;
    spacing: number;
}

function AudioVisualizer({
    micTrackRef,
    groupCount,
    width,
    height,
    color,
    spacing,
}: AudioVisualizerProps) {
    const audioWaves = useAudioWaveform(micTrackRef);
    const canvasRef = useRef<HTMLCanvasElement | null>(null);
    // 처리된 그룹 평균 데이터를 저장하는 Ref
    const processedDataRef = useRef<number[]>(new Array(groupCount).fill(0));

    // Throttle을 사용해서 오디오 데이터 처리 (예: 300ms마다 업데이트)
    const processAudioData = throttle((barsOriginal: number[]) => {
        let groupedAverages: number[] = [];

        if (barsOriginal.length > 0) {
            for (let i = 0; i < groupCount; i++) {
                const start = Math.floor(
                    (i * barsOriginal.length) / groupCount
                );
                const end = Math.floor(
                    ((i + 1) * barsOriginal.length) / groupCount
                );
                const group = barsOriginal.slice(start, end);
                const avg =
                    group.reduce((sum, value) => sum + value, 0) / group.length;
                groupedAverages.push(avg);
            }
        } else {
            groupedAverages = new Array(groupCount).fill(0);
        }

        processedDataRef.current = groupedAverages;
    }, 1000); // 1000ms 쓰로틀

    // Canvas에 그리기: 이제 requestAnimationFrame 대신 setInterval을 사용하여 1000ms마다 업데이트
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const draw = () => {
            // 캔버스 클리어
            ctx.clearRect(0, 0, width, height);

            const groupedAverages = processedDataRef.current;
            const barWidth = width / groupCount;
            const center = height / 2;

            groupedAverages.forEach((avg, index) => {
                const halfBarHeight = avg * (height / 2);
                const x = index * barWidth;
                const y = center - halfBarHeight;
                ctx.fillStyle = color;
                ctx.fillRect(x, y, barWidth - spacing, halfBarHeight * 2);
            });
        };

        // 처음 한 번 그린 후 1000ms마다 업데이트
        draw();
        const intervalId = setInterval(draw, 1000);

        return () => {
            clearInterval(intervalId);
        };
    }, [width, height, groupCount, color, spacing]);

    return <canvas ref={canvasRef} width={width} height={height} />;
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
