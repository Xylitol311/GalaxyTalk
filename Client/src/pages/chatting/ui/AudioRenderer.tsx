import {
    AudioTrack,
    TrackReference,
    useAudioWaveform,
    useTracks,
} from '@livekit/components-react';
import { getEmptyAudioStreamTrack, Track } from 'livekit-client';
import { useEffect, useRef, useState } from 'react';
import * as Tone from 'tone';
import { throttle } from '@/shared/lib/utils';

function AudioRenderer({ userId }: { userId: string }) {
    const trackRefs = useTracks([Track.Source.Microphone]);
    const micTrackRef = trackRefs.find(
        (trackRef) => trackRef.participant.identity === userId
    );

    // container ref와 크기를 저장할 상태
    const containerRef = useRef(null);
    const [containerSize, setContainerSize] = useState({
        width: 200,
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
    }, []);

    // container의 크기를 기준으로 AudioVisualizer에 전달할 값들을 계산
    const newWidth = containerSize.width;
    const newHeight = containerSize.height;
    // 예시: 막대의 최소 너비를 10px로 가정하고 그룹 개수를 산출 (최소 10개)
    const newGroupCount = Math.max(1, Math.floor(newWidth / 10));
    // 예시: spacing은 container의 너비에 따라 계산 (최소 5)
    const newSpacing = Math.max(5, Math.floor(newWidth / 100));

    return (
        <>
            {micTrackRef ? (
                <div ref={containerRef} className="bg-black w-full">
                    <AudioTrack trackRef={micTrackRef} />
                    <AudioVisualizer
                        micTrackRef={micTrackRef}
                        groupCount={newGroupCount}
                        width={newWidth}
                        height={newHeight}
                        color="#FFFFFF80"
                        spacing={newSpacing}
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

    // Throttle을 사용해 오디오 데이터를 일정 시간 간격(여기서는 1000ms)마다 처리
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
    }, 100); // 100ms마다 데이터 처리

    useEffect(() => {
        const barsOriginal = audioWaves?.bars || [];
        processAudioData(barsOriginal);
        // throttle된 함수는 내부적으로 업데이트 주기를 관리하므로 별도의 클린업 로직은 필요없습니다.
    }, [audioWaves, groupCount, processAudioData]);

    // Canvas에 그리기: 이제 requestAnimationFrame 대신 setInterval을 사용하여 1000ms마다 업데이트
    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        // 둥근 사각형을 그리는 함수
        function drawRoundedRect(ctx, x, y, width, height, radius) {
            ctx.beginPath();
            ctx.moveTo(x + radius, y);
            ctx.lineTo(x + width - radius, y);
            ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
            ctx.lineTo(x + width, y + height - radius);
            ctx.quadraticCurveTo(
                x + width,
                y + height,
                x + width - radius,
                y + height
            );
            ctx.lineTo(x + radius, y + height);
            ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
            ctx.lineTo(x, y + radius);
            ctx.quadraticCurveTo(x, y, x + radius, y);
            ctx.closePath();
            ctx.fill();
        }

        const draw = () => {
            // 캔버스 클리어
            ctx.clearRect(0, 0, width, height);

            const groupedAverages = processedDataRef.current;
            const barWidth = width / groupCount;
            const center = height / 2;

            groupedAverages.forEach((avg, index) => {
                const halfBarHeight = avg * (height / 2);
                const barHeight = halfBarHeight * 2;
                const x = index * barWidth;
                const y = center - halfBarHeight;
                ctx.fillStyle = color;

                // effectiveRadius 계산:
                // 좌우 최대 반경: (barWidth - spacing)의 절반
                // 상하 최대 반경: barHeight의 절반
                const effectiveRadius = Math.min(
                    (barWidth - spacing) / 2,
                    barHeight / 2
                );

                drawRoundedRect(
                    ctx,
                    x,
                    y,
                    barWidth - spacing,
                    barHeight,
                    effectiveRadius
                );
            });
        };

        // 처음에 한 번 그리고 이후 100ms마다 업데이트
        draw();
        const intervalId = setInterval(draw, 100);

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
