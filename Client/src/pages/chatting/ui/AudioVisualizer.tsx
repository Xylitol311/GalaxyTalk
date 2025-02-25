import { TrackReference, useAudioWaveform } from '@livekit/components-react';
import { useEffect, useRef } from 'react';
import { throttle } from '@/shared/lib/utils';

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

    // Throttle을 사용해 오디오 데이터를 일정 시간 간격(여기서는 100ms)마다 처리
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

    // Canvas에 그리기: 이제 requestAnimationFrame 대신 setInterval을 사용하여 100ms마다 업데이트
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

export default AudioVisualizer;
