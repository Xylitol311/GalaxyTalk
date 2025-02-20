import { Bloom, EffectComposer } from '@react-three/postprocessing';

export default function Bloomer() {
    return (
        <EffectComposer>
            <Bloom intensity={0.3} radius={0.4} threshold={0.1} />
        </EffectComposer>
    );
}
