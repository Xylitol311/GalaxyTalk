import { useGLTF } from '@react-three/drei';
import { useState } from 'react';
import RecursiveGLTF from './RecursiveGLTF';

interface SnowHouseProps {
    onClick: () => void;
}

function SnowHouse({ onClick }: SnowHouseProps) {
    const snowhouse = useGLTF('./snow_house.glb');
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
                object={snowhouse.scene}
                castShadow={true}
                receiveShadow={true}
                scale={0.4}
                position={[-4.2, -2.8, 3]}
                rotation={[0, (11 * Math.PI) / 180, 0]}
                hover={hover}
                onClick={onClick}
                hoverEmissiveMultiplier={100}
            />
        </group>
    );
}

export default SnowHouse;
