import { Html, useGLTF } from '@react-three/drei';
import { useFrame } from '@react-three/fiber';
import { useRef, useState } from 'react';
import * as THREE from 'three';
import { IMAGE_PATH } from '@/app/config/constants/path';
import { WaitingUserType } from '@/features/match/model/types';
import { formatTimeDifference } from '@/shared/lib/utils';
import { Card, CardContent } from '@/shared/ui/shadcn/card';
import RecursiveGLTF from './RecursiveGLTF';

type PlanetProps = {
    userInfo: WaitingUserType;
};

export default function Planet({ userInfo }: PlanetProps) {
    const star = useGLTF(`${IMAGE_PATH}star.glb`);

    const meshRef = useRef<THREE.Mesh>(null);
    const [hovered, setHovered] = useState(false);

    // 랜덤한 초기 위치 생성 (x, y, z 모두 -1.5 ~ 1.5 사이)
    const [randomPosition] = useState<[number, number, number]>(() => [
        Math.random() * 3 - 1.5, // -1.5 ~ 1.5
        Math.random() * 3 - 1.5, // -1.5 ~ 1.5
        Math.random() * 3 - 1.5, // -1.5 ~ 1.5
    ]);

    // 랜덤한 회전 속도 생성
    const rotationSpeed = Math.random() * 0.005 + 0.005; // 0.005 ~ 0.015 사이

    // 랜덤한 움직임 속도 생성
    const oscillationSpeed = Math.random() * 0.005 + 0.002;

    // 주기적으로 이동하는 위치 값 생성
    const oscillation = (time: number, speed: number) => {
        return Math.sin(time * speed) * 0.2; // 0.2의 범위로 왔다 갔다
    };

    // 행성을 회전시키고, 위치를 주기적으로 움직이기 위한 애니메이션
    useFrame(({ clock }) => {
        if (!meshRef.current) {
            return;
        }

        // 호버 상태가 아닐 때만 애니메이션 실행
        if (!hovered) {
            // 원래 애니메이션 좌표 계산
            const targetX =
                randomPosition[0] +
                oscillation(clock.elapsedTime, oscillationSpeed);
            const targetY =
                randomPosition[1] +
                oscillation(clock.elapsedTime, oscillationSpeed);
            const targetZ =
                randomPosition[2] +
                oscillation(clock.elapsedTime, oscillationSpeed);

            // `lerp`를 사용하여 부드럽게 보간
            meshRef.current.position.lerp(
                new THREE.Vector3(targetX, targetY, targetZ),
                0.1
            );

            // 회전 애니메이션
            meshRef.current.rotation.y += rotationSpeed;
            meshRef.current.rotation.x += rotationSpeed;
        }
    });

    const handlePointerOver = () => {
        setHovered(true);
        document.body.style.cursor = 'pointer';
    };

    const handlePointerOut = () => {
        setHovered(false);
        document.body.style.cursor = 'auto';
    };

    return (
        <>
            <mesh
                onPointerOver={handlePointerOver}
                onPointerOut={handlePointerOut}
                position={randomPosition} // 랜덤한 위치 적용
                ref={meshRef}>
                <RecursiveGLTF object={star.scene} scale={1} />
            </mesh>

            {hovered && (
                <Html
                    position={[0, 0, 0]}
                    center
                    zIndexRange={[100, 0]}
                    style={{
                        pointerEvents: 'none',
                        width: '360px', // 크기를 고정값으로 설정
                        height: 'auto', // 필요에 따라 높이도 설정
                    }}>
                    <Card className="bg-white p-6 rounded-xl w-full transform transition-all duration-300 hover:scale-105 hover:shadow-inner">
                        <CardContent>
                            <div className="text-sm font-semibold text-gray-900 mb-2">
                                상대방의 고민
                            </div>
                            <div className="text-xs text-gray-700 mb-1">
                                {userInfo.concern}
                            </div>
                            <div className="text-xs text-gray-500">
                                상대방의 MBTI : {userInfo.mbti}
                            </div>
                            <div className="text-xs text-gray-500">
                                매칭 시작 시간 :{' '}
                                {formatTimeDifference(+userInfo.startTime)}
                            </div>
                        </CardContent>
                    </Card>
                </Html>
            )}
        </>
    );
}
