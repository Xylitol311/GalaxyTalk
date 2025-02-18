import { Html, useGLTF } from '@react-three/drei';
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
        <>
            {hover && (
                <Html
                    position={[-4.2, -1, 3]}
                    center
                    transform
                    zIndexRange={[100, 0]}
                    style={{ pointerEvents: 'none' }}>
                    <div className="rounded-md px-3 py-1.5 text-xs text-primary-foreground animate-in fade-in-0 zoom-in-95 data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 ">
                        마이페이지
                    </div>
                </Html>
            )}
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
        </>
    );
}

export default SnowHouse;
