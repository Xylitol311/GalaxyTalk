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
                position={[-4, 7, 6]}
                intensity={6}
                castShadow
                scale={10}
                // 그림자 지도 해상도 조정 (기본값보다 높게 설정하면 더 선명한 그림자)
                shadow-mapSize-width={1024}
                shadow-mapSize-height={1024}
                // 그림자 카메라 범위를 설정해서 두 객체가 모두 포함되도록 합니다.
                shadow-camera-left={-10}
                shadow-camera-right={10}
                shadow-camera-top={10}
                shadow-camera-bottom={-10}
            />
            <Ground />

            <EffectComposer enableNormalPass={false}>
                <Bloom
                    mipmapBlur
                    luminanceThreshold={1}
                    levels={levels}
                    intensity={intensity * 1}
                />
                <ToneMapping />
            </EffectComposer>
        </>
    );
}

export default EarthSky;
