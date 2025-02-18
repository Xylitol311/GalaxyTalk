import { Html } from '@react-three/drei';
import { useFrame } from '@react-three/fiber';
import { useRef, useState } from 'react';
import * as THREE from 'three';
import { WaitingUserType } from '@/features/match/model/types';
import { Card, CardContent } from '@/shared/ui/shadcn/card';

type PlanetProps = {
    userInfo: WaitingUserType;
    position: [number, number, number];
    color: THREE.Color;
};

export default function Planet({ userInfo, position, color }: PlanetProps) {
    const meshRef = useRef<THREE.Mesh>(null);
    const [hovered, setHovered] = useState(false);

    // 각 행성에 고유한 움직임을 주기 위한 변수
    const oscillationSpeed = Math.random() * 0.5 + 0.2; // 고유한 주기 속도

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
                position[0] + oscillation(clock.elapsedTime, oscillationSpeed);
            const targetY =
                position[1] + oscillation(clock.elapsedTime, oscillationSpeed);
            const targetZ =
                position[2] + oscillation(clock.elapsedTime, oscillationSpeed);

            // `lerp`를 사용하여 부드럽게 보간
            meshRef.current.position.lerp(
                new THREE.Vector3(targetX, targetY, targetZ),
                0.1
            );

            // 회전 애니메이션
            meshRef.current.rotation.y += 0.005;
            meshRef.current.rotation.x += 0.005;
        }
    });

    const handlePointOver = () => {
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
                ref={meshRef}
                position={position}
                onPointerOver={handlePointOver}
                onPointerOut={handlePointerOut}>
                <icosahedronGeometry args={[0.1, 1]} />
                <meshStandardMaterial
                    color={color}
                    emissive={color}
                    emissiveIntensity={hovered ? 100 : 50}
                />
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
                                매칭 시작 시간 : {userInfo.startTime}
                            </div>
                        </CardContent>
                    </Card>
                </Html>
            )}
        </>
    );
}
