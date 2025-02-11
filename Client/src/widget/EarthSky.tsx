import { OrbitControls } from '@react-three/drei';
import {
    Bloom,
    EffectComposer,
    ToneMapping,
} from '@react-three/postprocessing';
import Ground from './Ground';
import RotatingStars from './RotatingStars';

function EarthSky() {
    const levels = 8,
        intensity = 0.4;

    return (
        <>
            <RotatingStars />
            <OrbitControls
                enableDamping
                minAzimuthAngle={-2}
                maxAzimuthAngle={2.5}
                minPolarAngle={0.7}
                maxPolarAngle={1.6}
                minDistance={10}
                maxDistance={14}
            />
            <ambientLight intensity={2} />
            <directionalLight
                position={[-4, 8, 6]}
                intensity={6}
                castShadow
                scale={10}
            />
            <Ground />

            <EffectComposer enableNormalPass={false}>
                <Bloom
                    mipmapBlur
                    luminanceThreshold={1}
                    levels={levels}
                    intensity={intensity * 4}
                />
                <ToneMapping />
            </EffectComposer>
        </>
    );
}

export default EarthSky;
