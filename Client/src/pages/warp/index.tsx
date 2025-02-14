import { Canvas } from '@react-three/fiber';
import SpaceWarp from '@/widget/SpaceWarp';

export default function WarpPage() {
    return (
        <Canvas camera={{ fov: 100, near: 0.2, far: 200 }}>
            <SpaceWarp />
        </Canvas>
    );
}
