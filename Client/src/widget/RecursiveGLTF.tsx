import * as THREE from 'three';

interface RecursiveGLTFProps {
    object: THREE.Object3D;
    // 최상위에 적용할 옵션들 (Mesh에만 의미있는 castShadow, receiveShadow)
    castShadow?: boolean;
    receiveShadow?: boolean;
    scale?: THREE.Vector3 | number | [number, number, number];
    position?: THREE.Vector3 | [number, number, number];
    rotation?: THREE.Euler | [number, number, number];
    hover?: boolean;
    onClick?: () => void;
}

function RecursiveGLTF({
    object,
    castShadow,
    receiveShadow,
    scale,
    position,
    rotation,
    hover,
    onClick,
}: RecursiveGLTFProps) {
    // object가 Mesh라면 Mesh 컴포넌트를 반환하고,
    // 추가 props가 있다면 해당 값이 없을 때는 원래 object의 속성을 쓰고,
    // 있으면 사용자 지정 props를 사용합니다.

    if (object instanceof THREE.Mesh) {
        const modifiedMaterial = object.material.clone();
        if (hover && 'emissiveIntensity' in modifiedMaterial) {
            modifiedMaterial.emissiveIntensity = 400;
        }

        return (
            <mesh
                key={object.uuid}
                geometry={object.geometry}
                material={modifiedMaterial}
                position={position !== undefined ? position : object.position}
                rotation={rotation !== undefined ? rotation : object.rotation}
                scale={scale !== undefined ? scale : object.scale}
                castShadow={
                    castShadow !== undefined ? castShadow : object.castShadow
                }
                receiveShadow={
                    receiveShadow !== undefined
                        ? receiveShadow
                        : object.receiveShadow
                }
                onClick={(e) => {
                    e.stopPropagation();
                    console.log('Mesh clicked:', object.name);
                    onClick?.();
                }}
            />
        );
    }

    // Mesh가 아니라면 그룹으로 처리합니다.
    // 그룹에는 position, rotation, scale 만 적용합니다.
    return (
        <group
            key={object.uuid}
            position={position !== undefined ? position : object.position}
            rotation={rotation !== undefined ? rotation : object.rotation}
            scale={scale !== undefined ? scale : object.scale}>
            {object.children.map((child) => (
                // 여기서도 필요하다면 기본 shadow 설정을 전달할 수 있습니다.
                <RecursiveGLTF
                    key={child.uuid}
                    object={child}
                    castShadow={castShadow} // 상위에서 전달받은 shadow 설정을 자식에 전달
                    receiveShadow={receiveShadow}
                    hover={hover}
                    onClick={() => {
                        onClick?.();
                    }}
                />
            ))}
        </group>
    );
}

export default RecursiveGLTF;
