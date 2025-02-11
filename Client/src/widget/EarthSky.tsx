import { Html, OrbitControls } from '@react-three/drei';
import { useThree } from '@react-three/fiber';
import {
    Bloom,
    EffectComposer,
    ToneMapping,
} from '@react-three/postprocessing';
import { useEffect } from 'react';
import { useUserStore } from '@/app/model/stores/user';
import MatchingForm from '@/features/match/ui/MatchingForm';
import NaverButton from '@/features/user/ui/NaverButton';
import Introduce from '@/pages/home/ui/Introduce';
import Ground from './Ground';
import RotatingStars from './RotatingStars';

function EarthSky() {
    const { camera } = useThree();
    const { userId } = useUserStore();

    useEffect(() => {
        // camera.position는 Canvas의 camera prop에서 설정한 값(여기서는 [0,0,-0.01])을 그대로 사용합니다.
        // 이후 원하는 방향으로 카메라를 바라보도록 lookAt()을 호출합니다.
        camera.lookAt(-1, 0, 0);
    }, [camera]);

    const levels = 8,
        intensity = 0.4;
    const isLogin = !!userId;

    return (
        // <Canvas
        //     shadows
        //     camera={{
        //         fov: 60,
        //         near: 0.01,
        //         far: 10000,
        //         position: [0, 0, -0.2],
        //     }}>
        <>
            <RotatingStars />
            <OrbitControls enableDamping />
            <ambientLight intensity={2} />
            <directionalLight
                position={[-4, 8, 6]}
                intensity={6}
                castShadow
                scale={10}
            />
            <Ground />
            {isLogin ? (
                <MatchingForm />
            ) : (
                <Html
                    position={[0, 0, 0]}
                    center
                    transform
                    zIndexRange={[100, 0]}
                    style={{ pointerEvents: 'none' }}>
                    <div
                        className="flex flex-col gap-4 items-center"
                        style={{
                            pointerEvents: 'auto',
                            userSelect: 'none',
                        }}>
                        <Introduce />
                        <span>
                            <NaverButton />
                        </span>
                    </div>
                </Html>
            )}

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
        // </Canvas>
    );
}

export default EarthSky;
