import { Environment, OrbitControls } from '@react-three/drei';
import { useFrame } from '@react-three/fiber';
import { Suspense, useEffect, useMemo } from 'react';
import * as THREE from 'three';

const size = 1.8;
const colorInside = '#ffffff';
const colorOutside = '#311599';

function Galaxy() {
    const count = 20_000;
    const branches = 5;

    // Leva controls
    // const { size, colorInside, colorOutside } = useControls({
    //   size: { value: 0.4, min: 0, max: 1, step: 0.01 },
    //   colorInside: '#ffffff',
    //   colorOutside: '#311599'
    // })

    // Generate particle data
    const { positions, attributes } = useMemo(() => {
        const positions = new Float32Array(count * 3);
        const radiusRatios = new Float32Array(count);
        const angles = new Float32Array(count);
        const randomOffsets = new Float32Array(count * 3);

        for (let i = 0; i < count; i++) {
            const radiusRatio = Math.random();
            const branch = Math.floor(Math.random() * branches);
            const angle = (branch * Math.PI * 2) / branches;
            const randomOffset = new THREE.Vector3(
                (Math.random() - 0.5) * 2,
                (Math.random() - 0.5) * 2,
                (Math.random() - 0.5) * 2
            )
                .normalize()
                .multiplyScalar(radiusRatio * 0.2 + 0.1); // 오프셋 범위 축소

            radiusRatios[i] = radiusRatio;
            angles[i] = angle;
            randomOffsets.set(
                [randomOffset.x, randomOffset.y, randomOffset.z],
                i * 3
            );
            positions.set([0, 0, 0], i * 3);
        }

        return {
            positions,
            attributes: {
                aRadiusRatio: new THREE.BufferAttribute(radiusRatios, 1),
                aAngle: new THREE.BufferAttribute(angles, 1),
                aRandomOffset: new THREE.BufferAttribute(randomOffsets, 3),
            },
        };
    }, [count]);

    // Shader setup
    const { uniforms, shaders } = useMemo(
        () => ({
            uniforms: {
                uTime: { value: 0 },
                uSize: { value: size },
                uColorInside: { value: new THREE.Color(colorInside) },
                uColorOutside: { value: new THREE.Color(colorOutside) },
            },
            shaders: {
                vertex: `
        uniform float uTime;
        uniform float uSize;
        uniform vec3 uColorInside;
        uniform vec3 uColorOutside;

        attribute float aRadiusRatio;
        attribute float aAngle;
        attribute vec3 aRandomOffset;

        varying vec3 vColor;
        varying float vAlpha;

        void main() {
          // Position calculation
          float radius = pow(aRadiusRatio, 1.5) * 5.0;
          float currentAngle = aAngle + uTime * (1.0 - aRadiusRatio);
          
          vec3 basePosition = vec3(
            cos(currentAngle),
            sin(uTime * 0.5 + aRadiusRatio * 2.0) * 0.3, // Y축 움직임 추가
            sin(currentAngle)
          ) * radius;

          vec3 finalPosition = basePosition + aRandomOffset;

          // Transform
          vec4 modelPosition = modelMatrix * vec4(finalPosition, 1.0);
          vec4 viewPosition = viewMatrix * modelPosition;
          gl_Position = projectionMatrix * viewPosition;

          // Size calculation
          gl_PointSize = uSize * 15.0; // 기본 크기 축소
          gl_PointSize *= (1.0 / -viewPosition.z); // 원근 보정

          // Color calculation
          float colorMix = 1.0 - pow(1.0 - aRadiusRatio, 2.0);
          vColor = mix(uColorInside, uColorOutside, colorMix);
          vAlpha = 0.8;
        }
      `,
                fragment: `
        varying vec3 vColor;
        varying float vAlpha;

        void main() {
          // Circular point shape
          float strength = 1.0 - length(gl_PointCoord - 0.5) * 2.0;
          strength = smoothstep(0.0, 0.2, strength);
          
          gl_FragColor = vec4(vColor, strength * vAlpha);
        }
      `,
            },
        }),
        [colorInside, colorOutside, size]
    );

    // Update uniforms
    useEffect(() => {
        uniforms.uSize.value = size;
        uniforms.uColorInside.value.set(colorInside);
        uniforms.uColorOutside.value.set(colorOutside);
    }, [size, colorInside, colorOutside, uniforms]);

    // Animation
    useFrame((state) => {
        uniforms.uTime.value = state.clock.getElapsedTime() * 0.5;
    });

    return (
        // <Canvas camera={{ position: [4, 2, 5], fov: 40 }}>
        <Suspense fallback={null}>
            <Environment
                files={[
                    '/skybox/right.png', // right
                    '/skybox/left.png', // left
                    '/skybox/top.png', // top
                    '/skybox/bottom.png', // bottom
                    '/skybox/front.png', // front
                    '/skybox/back.png', // back
                ]}
                background={true}
            />
            <OrbitControls enableDamping minDistance={0.1} maxDistance={50} />
            <points>
                <bufferGeometry>
                    <bufferAttribute
                        attach="attributes-position"
                        args={[positions, 3]}
                        count={count}
                    />
                    <bufferAttribute
                        attach="attributes-aRadiusRatio"
                        args={[attributes.aRadiusRatio.array, 1]}
                        count={count}
                    />
                    <bufferAttribute
                        attach="attributes-aAngle"
                        args={[attributes.aAngle.array, 1]}
                        count={count}
                    />
                    <bufferAttribute
                        attach="attributes-aRandomOffset"
                        args={[attributes.aRandomOffset.array, 3]}
                        count={count}
                    />
                </bufferGeometry>
                <shaderMaterial
                    uniforms={uniforms}
                    vertexShader={shaders.vertex}
                    fragmentShader={shaders.fragment}
                    transparent={false}
                    depthWrite={false}
                    blending={THREE.AdditiveBlending}
                />
            </points>
        </Suspense>
        // </Canvas>
    );
}

export default Galaxy;
