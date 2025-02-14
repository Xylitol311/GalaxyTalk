import { useGLTF } from '@react-three/drei';
import { useState } from 'react';
import RecursiveGLTF from './RecursiveGLTF';

interface TelescopeProps {
    onClick: () => void;
}

function Telescope({ onClick }: TelescopeProps) {
    const telescope = useGLTF('./satellite_dish_1k.glb');
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
                object={telescope.scene}
                castShadow={true}
                receiveShadow={true}
                scale={0.3}
                position={[0, -2.8, 5]}
                rotation={[0, 180, 0]}
                hover={hover}
                onClick={onClick}
                hoverEmissiveMultiplier={400}
            />
        </group>
    );
}

export default Telescope;
