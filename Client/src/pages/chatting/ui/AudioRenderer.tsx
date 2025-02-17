import {
    AudioTrack,
    TrackReference,
    useAudioWaveform,
    useTracks,
} from '@livekit/components-react';
import { getEmptyAudioStreamTrack, Track } from 'livekit-client';
import { useEffect, useRef } from 'react';
import * as Tone from 'tone';

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
    // Canvas DOM을 참조하기 위한 Ref
    const canvasRef = useRef<HTMLCanvasElement | null>(null);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        let animationFrameId: number;

        // Canvas에 그리기 함수
        const draw = () => {
            // Canvas 클리어
            ctx.clearRect(0, 0, width, height);

            // 원본 bars 데이터를 가져오기 (값은 0~1 사이)
            const barsOriginal = audioWaves?.bars || [];

            // 데이터를 7등분하여 각 그룹의 평균값 계산
            let groupedAverages = [];
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
                        group.reduce((sum, value) => sum + value, 0) /
                        group.length;
                    groupedAverages.push(avg);
                }
            } else {
                groupedAverages = new Array(groupCount).fill(0);
            }

            // 각 그룹의 평균값을 사용해 바 그리기
            const barWidth = width / groupCount;
            const center = height / 2;
            groupedAverages.forEach((avg, index) => {
                // 최대 높이의 절반 값으로 확장 (최대값 1이면 양쪽으로 center 만큼 늘어남)
                const halfBarHeight = avg * (height / 2);
                const x = index * barWidth;
                const y = center - halfBarHeight; // 중앙에서 위로 halfBarHeight만큼 이동
                ctx.fillStyle = color;
                // 바의 높이를 위, 아래로 동시에 확장
                ctx.fillRect(x, y, barWidth - spacing, halfBarHeight * 2);
            });

            // 반복 호출 (requestAnimationFrame을 통해 최적화)
            animationFrameId = requestAnimationFrame(draw);
        };

        // 애니메이션 시작
        draw();

        return () => {
            cancelAnimationFrame(animationFrameId);
        };
    }, [audioWaves]);

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
