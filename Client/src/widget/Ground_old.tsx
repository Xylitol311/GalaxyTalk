import { useLoader } from '@react-three/fiber';
import { TextureLoader } from 'three';

function Ground_old() {
    const diff = useLoader(
        TextureLoader,
        'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_diff_4k.jpg'
    );
    const nor = useLoader(
        TextureLoader,
        'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_nor_gl_4k.jpg'
    );
    const rough = useLoader(
        TextureLoader,
        'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_rough_4k.jpg'
    );
    const ao = useLoader(
        TextureLoader,
        'https://dl.polyhaven.org/file/ph-assets/Textures/jpg/4k/snow_02/snow_02_ao_4k.jpg'
    );

    return (
        <mesh position={[0, -3, 0]} receiveShadow>
            <cylinderGeometry args={[10, 10, 0.5, 100]} />
            <meshStandardMaterial
                toneMapped={false}
                map={diff}
                normalMap={nor}
                roughnessMap={rough} // 또는 roughnessMap에 arm 텍스처의 적절한 채널을 쪼개서 사용
                aoMap={ao} // arm 텍스처 사용 시에는 aoMap 대신 arm의 red 채널 정보를 활용
            />
        </mesh>
    );
}

export default Ground_old;
