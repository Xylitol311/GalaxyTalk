import { useFrame, useLoader } from '@react-three/fiber';
import { useEffect, useRef, useState } from 'react';
import { Texture, TextureLoader } from 'three';

type HighResTextures = {
    diff: Texture;
    nor: Texture;
    rough: Texture;
    ao: Texture;
};

function Ground() {
    // 낮은 해상도의 diffuse 텍스처를 먼저 불러옵니다.
    const lowResTexture = useLoader(TextureLoader, '/snow.webp');

    // 고해상도 텍스처들을 담기 위한 상태 변수 (초기엔 null)
    const [highResTextures, setHighResTextures] =
        useState<HighResTextures | null>(null);

    // 고해상도 텍스처를 비동기로 로드합니다.
    useEffect(() => {
        const loader = new TextureLoader();
        const diffUrl =
            'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_diff_4k.jpg';
        const norUrl =
            'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_nor_gl_4k.jpg';
        const roughUrl =
            'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_rough_4k.jpg';
        const aoUrl =
            'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_ao_4k.jpg';

        Promise.all([
            new Promise<Texture>((resolve, reject) => {
                loader.load(
                    diffUrl,
                    (tex) => {
                        tex.needsUpdate = true;
                        resolve(tex);
                    },
                    undefined,
                    reject
                );
            }),
            new Promise<Texture>((resolve, reject) => {
                loader.load(
                    norUrl,
                    (tex) => {
                        tex.needsUpdate = true;
                        resolve(tex);
                    },
                    undefined,
                    reject
                );
            }),
            new Promise<Texture>((resolve, reject) => {
                loader.load(
                    roughUrl,
                    (tex) => {
                        tex.needsUpdate = true;
                        resolve(tex);
                    },
                    undefined,
                    reject
                );
            }),
            new Promise<Texture>((resolve, reject) => {
                loader.load(
                    aoUrl,
                    (tex) => {
                        tex.needsUpdate = true;
                        resolve(tex);
                    },
                    undefined,
                    reject
                );
            }),
        ])
            .then(([diff, nor, rough, ao]) => {
                setHighResTextures({ diff, nor, rough, ao });
            })
            .catch((error) => {
                console.error('고해상도 텍스처 로딩 에러:', error);
            });
    }, []);

    // 고해상도 메쉬의 opacity를 제어하기 위한 ref
    const highResMatRef = useRef<any>(null);
    // blending 단계(0 ~ 1)를 저장하는 ref (추가 효과: 애니메이션 속도 조절 가능)
    const blendRef = useRef(0);

    // 매 프레임마다 highRes 메쉬의 opacity를 서서히 증가시킵니다.
    useFrame((state, delta) => {
        if (highResTextures && blendRef.current < 1 && highResMatRef.current) {
            // delta 값에 비례해서 blend 값을 올립니다.
            blendRef.current += delta * 0.3; // 이 값을 조정해서 전환 속도를 변경할 수 있습니다.
            if (blendRef.current > 1) blendRef.current = 1;
            highResMatRef.current.opacity = blendRef.current;
        }
    });

    return (
        <>
            {/* 낮은 해상도 메쉬 */}
            <mesh position={[0, -3, 0]} receiveShadow renderOrder={0}>
                <cylinderGeometry args={[10, 10, 0.5, 100]} />
                <meshStandardMaterial
                    toneMapped={false}
                    map={lowResTexture}
                    color="#cccccc" // 낮은 해상도 사용 시 약간 어두운 색으로 밝기를 낮춤
                />
            </mesh>

            {/* 고해상도 메쉬 (텍스처 로드 후 표시) */}
            {highResTextures && (
                <mesh position={[0, -3, 0]} receiveShadow renderOrder={1}>
                    <cylinderGeometry args={[10, 10, 0.5, 100]} />
                    <meshStandardMaterial
                        ref={highResMatRef}
                        transparent
                        opacity={0} // 초기 opacity는 0으로 시작
                        toneMapped={false}
                        map={highResTextures.diff}
                        normalMap={highResTextures.nor}
                        roughnessMap={highResTextures.rough}
                        aoMap={highResTextures.ao}
                    />
                </mesh>
            )}
        </>
    );
}

export default Ground;
