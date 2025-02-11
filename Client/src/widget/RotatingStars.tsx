import { Stars } from '@react-three/drei';
import { useFrame } from '@react-three/fiber';
import { useRef } from 'react';
import * as THREE from 'three';

function RotatingStars() {
    const groupRef = useRef<THREE.Group>(null);

    useFrame((_, delta) => {
        if (groupRef.current) {
            // Y축으로 매 프레임마다 회전 (원하는 축이나 속도로 조정 가능)
            groupRef.current.rotation.x += delta * 0.02;
            groupRef.current.rotation.y += delta * 0.02;
        }
    });

    return (
        <group ref={groupRef}>
            <Stars
                radius={100}
                depth={50}
                count={5000}
                factor={4}
                saturation={0}
                fade
                speed={1}
            />
        </group>
    );
}

export default RotatingStars;
