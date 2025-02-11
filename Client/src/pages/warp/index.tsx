import { Canvas } from '@react-three/fiber';
import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import SpaceWarp from '@/widget/SpaceWarp';

export default function WarpPage() {
    const navigate = useNavigate();

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            navigate(PATH.ROUTE.MATCH);
        }, 5000);

        return () => clearTimeout(timeoutId);
    }, [navigate]);

    return (
        <Canvas camera={{ fov: 100, near: 0.2, far: 200 }}>
            <SpaceWarp />
        </Canvas>
    );
}
