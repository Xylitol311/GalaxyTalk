import { Canvas } from '@react-three/fiber';
import EarthSky from '@/widget/EarthSky';
import Header from '@/widget/home/ui/header';

export default function Home() {
    return (
        <>
            <Header />
            <Canvas
                shadows
                camera={{
                    fov: 60,
                    near: 0.01,
                    far: 10000,
                    position: [0, 0, 12],
                }}>
                <EarthSky />
            </Canvas>
        </>
    );
}
