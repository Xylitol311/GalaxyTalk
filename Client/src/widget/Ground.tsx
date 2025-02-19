import { useLoader } from '@react-three/fiber';
import { useEffect, useState } from 'react';
import { Texture, TextureLoader } from 'three';

function Ground() {
    // 1. low resolution 텍스처를 먼저 useLoader로 불러옵니다.
    // texture.png는 프로젝트의 public 폴더에 있다고 가정합니다.
    const lowResTexture = useLoader(TextureLoader, '/snow.jpg');

    // 2. hi-resolution 텍스처를 담기 위한 상태 변수 선언 (초기엔 null)
    const [highResTexture, setHighResTexture] = useState<Texture | null>(null);

    // 3. 컴포넌트 마운트 후, useEffect를 사용해 비동기로 4k 텍스처를 로드합니다.
    useEffect(() => {
        const loader = new TextureLoader();
        loader.load(
            'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_diff_4k.jpg',
            (tex) => {
                // 텍스처의 업데이트를 강제로 알림 (필요한 경우)
                tex.needsUpdate = true;
                setHighResTexture(tex);
            },
            undefined,
            (error) => {
                console.error('Error loading high resolution texture:', error);
            }
        );
    }, []);

    // 4. hi-resolution 텍스처가 준비되어 있으면 hi-res를, 아니면 low-res를 사용합니다.
    const currentTexture = highResTexture || lowResTexture;

    return (
        <mesh position={[0, -3, 0]} receiveShadow>
            <cylinderGeometry args={[10, 10, 0.5, 100]} />
            <meshStandardMaterial toneMapped={false} map={currentTexture} />
        </mesh>
    );
}

export default Ground;
