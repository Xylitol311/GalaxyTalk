import { Canvas } from '@react-three/fiber';
import Galaxy from '@/widget/Galaxy';

export default function ErrorFallback() {
    return (
        <Canvas camera={{ position: [4, 2, 5], fov: 40 }}>
            <Galaxy />
        </Canvas>
    );
}
