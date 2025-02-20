import { Html, useGLTF } from '@react-three/drei';
import { useFrame } from '@react-three/fiber';
import { useRef, useState } from 'react';
import * as THREE from 'three';
import { IMAGE_PATH } from '@/app/config/constants/path';
import { WaitingUserType } from '@/features/match/model/types';
import { formatTimeDifference } from '@/shared/lib/utils';
import useIsMobile from '@/shared/model/hooks/useIsMobile';
import { Card, CardContent } from '@/shared/ui/shadcn/card';
import RecursiveGLTF from './RecursiveGLTF';

type PlanetProps = {
    userInfo: WaitingUserType;
};

export default function Planet({ userInfo }: PlanetProps) {
    const star = useGLTF(`${IMAGE_PATH}star.glb`);
    const isMobile = useIsMobile();
    const meshRef = useRef<THREE.Mesh>(null);
    const [hovered, setHovered] = useState(false);
    const [clicked, setClicked] = useState(false); // 클릭 여부 상태 추가

    const [randomPosition] = useState<[number, number, number]>(() => [
        Math.random() * 3 - 1.5,
        Math.random() * 3 - 1.5,
        Math.random() * 3 - 1.5,
    ]);

    const rotationSpeed = Math.random() * 0.005 + 0.005;
    const oscillationSpeed = Math.random() * 0.005 + 0.002;

    const oscillation = (time: number, speed: number) => {
        return Math.sin(time * speed) * 0.2;
    };

    useFrame(({ clock }) => {
        if (!meshRef.current) return;
        if (!hovered && !clicked) {
            const targetX =
                randomPosition[0] +
                oscillation(clock.elapsedTime, oscillationSpeed);
            const targetY =
                randomPosition[1] +
                oscillation(clock.elapsedTime, oscillationSpeed);
            const targetZ =
                randomPosition[2] +
                oscillation(clock.elapsedTime, oscillationSpeed);

            meshRef.current.position.lerp(
                new THREE.Vector3(targetX, targetY, targetZ),
                0.1
            );

            meshRef.current.rotation.y += rotationSpeed;
            meshRef.current.rotation.x += rotationSpeed;
        }
    });

    const handlePointerOver = () => {
        if (!isMobile) {
            setHovered(true);
            document.body.style.cursor = 'pointer';
        }
    };

    const handlePointerOut = () => {
        if (!isMobile) {
            setHovered(false);
            document.body.style.cursor = 'auto';
        }
    };

    const handlePointerDown = () => {
        if (isMobile) {
            setClicked(true);
        }
    };

    return (
        <>
            <mesh
                onPointerOver={handlePointerOver}
                onPointerOut={handlePointerOut}
                position={randomPosition}
                ref={meshRef}>
                <RecursiveGLTF
                    object={star.scene}
                    scale={1}
                    onClick={handlePointerDown}
                />
            </mesh>

            {(hovered || clicked) && (
                <Html
                    position={[0, 0, 0]}
                    center
                    zIndexRange={[100, 0]}
                    style={{
                        pointerEvents: 'none',
                        width: '360px',
                        height: 'auto',
                    }}>
                    <Card className="bg-white p-6 rounded-xl w-full transform transition-all duration-300 hover:scale-105 hover:shadow-inner">
                        <CardContent>
                            <div className="text-sm font-semibold text-gray-900 mb-2">
                                상대방의 고민
                            </div>
                            <div className="text-xs text-gray-700 mb-2">
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
                    {/* 클릭 시 다른 곳을 누르면 닫히도록 처리 */}
                </Html>
            )}
            {clicked && (
                <Html
                    position={[0, 0, 0]}
                    center
                    zIndexRange={[100, 0]}
                    className="w-screen h-screen">
                    <div
                        className="fixed top-0 left-0 w-full h-full z-100"
                        onClick={() => setClicked(false)}
                    />
                </Html>
            )}
        </>
    );
}
