import { OrbitControls } from '@react-three/drei';
import { Canvas, useThree } from '@react-three/fiber';
import {
    Bloom,
    EffectComposer,
    ToneMapping,
} from '@react-three/postprocessing';
import { useEffect } from 'react';
import Ground from './Ground';
import RotatingStars from './RotatingStars';
import Telescope from './Telescope';

function EarthSky() {
    const { camera } = useThree();

    useEffect(() => {
        // camera.position는 Canvas의 camera prop에서 설정한 값(여기서는 [0,0,-0.01])을 그대로 사용합니다.
        // 이후 원하는 방향으로 카메라를 바라보도록 lookAt()을 호출합니다.
        camera.lookAt(-1, 0, 0);
    }, [camera]);

    const levels = 8,
        intensity = 0.4;

    return (
        <Canvas shadows camera={{ fov: 60, near: 0.01, far: 10000, position: [0, 0, -0.2]}}>
            <RotatingStars />
            <axesHelper />
            <OrbitControls enableDamping />
            <ambientLight intensity={2} />
            <directionalLight
                position={[-4, 8, 6]}
                intensity={6}
                castShadow
                scale={10}
            />
            <Ground />
            <Telescope
                onClick={() => {
                    'telescope is clicked';
                }}
            />
            <EffectComposer enableNormalPass={false}>
                <Bloom
                    mipmapBlur
                    luminanceThreshold={1}
                    levels={levels}
                    intensity={intensity * 4}
                />
                <ToneMapping />
            </EffectComposer>
        </Canvas>
    );
}

export default EarthSky;
