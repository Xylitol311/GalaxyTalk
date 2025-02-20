// https://github.com/o2bomb/space-warp
import { useFrame } from '@react-three/fiber';
import { ChromaticAberrationEffect } from 'postprocessing';
import { useEffect, useRef } from 'react';
import * as THREE from 'three';

const COUNT = 100;
const generatePos = () => {
    return (Math.random() - 0.5) * 10;
};
const CHROMATIC_ABBERATION_OFFSET = 0.007;

function SpaceWarp() {
    const ref = useRef<THREE.InstancedMesh>(null);
    const effectsRef = useRef<ChromaticAberrationEffect>(null);

    useEffect(() => {
        if (!ref.current) return;

        const t = new THREE.Object3D();
        let j = 0;
        for (let i = 0; i < COUNT; i++) {
            t.position.x = generatePos();
            t.position.y = generatePos();
            t.position.z = (Math.random() - 0.5) * 10;
            t.updateMatrix();
            ref.current.setMatrixAt(j, t.matrix);

            // update and apply color
            tempColor.setHSL(Math.random(), 0.6, 0.6 + Math.random() * 0.3);
            ref.current.setColorAt(j, tempColor);

            j++;
        }

        ref.current.instanceMatrix.needsUpdate = true;
        if (ref.current.instanceColor)
            ref.current.instanceColor.needsUpdate = true;
    }, []);

    const temp = new THREE.Matrix4();
    const tempPos = new THREE.Vector3();
    // const tempScale = new THREE.Vector3();
    const tempObject = new THREE.Object3D();
    const tempColor = new THREE.Color();
    useFrame((state, delta) => {
        if (!ref.current) return;

        for (let i = 0; i < COUNT; i++) {
            ref.current.getMatrixAt(i, temp);
            // update scale
            tempObject.scale.set(
                1,
                1,
                Math.max(1, Math.pow(0.5, state.clock.elapsedTime) * 100)
            );

            // update position
            tempPos.setFromMatrixPosition(temp);
            if (tempPos.z > 7) {
                tempPos.z = -4;
            } else {
                tempPos.z += Math.max(
                    delta,
                    Math.pow(0.5, state.clock.elapsedTime)
                );
            }
            tempObject.position.set(tempPos.x, tempPos.y, tempPos.z);
            tempObject.updateMatrix();
            ref.current.setMatrixAt(i, tempObject.matrix);
        }
        ref.current.instanceMatrix.needsUpdate = true;
        if (ref.current.instanceColor)
            ref.current.instanceColor.needsUpdate = true;

        // update post processing uniforms
        if (!effectsRef.current) return;
        effectsRef.current.offset.x = Math.max(
            0,
            Math.pow(0.5, state.clock.elapsedTime) * CHROMATIC_ABBERATION_OFFSET
        );
        effectsRef.current.offset.y = Math.max(
            0,
            Math.pow(0.5, state.clock.elapsedTime) * CHROMATIC_ABBERATION_OFFSET
        );
    });

    return (
        // <Canvas camera={{ fov: 100, near: 0.2, far: 200 }}>
        <>
            <instancedMesh
                ref={ref}
                args={[undefined, undefined, COUNT]}
                matrixAutoUpdate>
                <sphereGeometry args={[0.02]} />
                <meshBasicMaterial color={[1.5, 1.5, 1.5]} toneMapped={false} />
            </instancedMesh>
            {/* <EffectComposer>
                <Bloom luminanceThreshold={1} intensity={0.2} />
                <ChromaticAberration
                    ref={effectsRef}
                    blendFunction={BlendFunction.NORMAL}
                    offset={
                        new THREE.Vector2(
                            CHROMATIC_ABBERATION_OFFSET,
                            CHROMATIC_ABBERATION_OFFSET
                        )
                    }
                />
            </EffectComposer> */}
        </>
        // </Canvas>
    );
}

export default SpaceWarp;
