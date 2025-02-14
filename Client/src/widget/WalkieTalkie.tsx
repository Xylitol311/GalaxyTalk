import { useGLTF } from '@react-three/drei';
import { useState } from 'react';
import RecursiveGLTF from './RecursiveGLTF';

interface WalkieTalkieProps {
    onClick: () => void;
}

function WalkieTalkie({ onClick }: WalkieTalkieProps) {
    const walkietalkie = useGLTF('./walkie_talkie.glb');
    const [hover, setHover] = useState(false);

    const handlePointerOver = (e: any) => {
        e.stopPropagation();
        setHover(true);
        document.body.style.cursor = 'pointer';
    };

    const handlePointerOut = (e: any) => {
        e.stopPropagation();
        setHover(false);
        document.body.style.cursor = 'default';
    };

    return (
        <group
            onPointerOver={handlePointerOver}
            onPointerOut={handlePointerOut}>
            <RecursiveGLTF
                object={walkietalkie.scene}
                castShadow={true}
                receiveShadow={true}
                scale={0.1}
                position={[3.8, -2.8, 3]}
                rotation={[0, (-45 * Math.PI) / 180, 0]}
                hover={hover}
                onClick={onClick}
                hoverEmissiveMultiplier={200}
            />
        </group>
    );
}

export default WalkieTalkie;
