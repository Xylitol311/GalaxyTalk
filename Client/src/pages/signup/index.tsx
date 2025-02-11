import { Canvas } from '@react-three/fiber';
import { useEffect } from 'react';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { useUserStore } from '@/app/model/stores/user';
import EarthSky from '@/widget/EarthSky';
import SignupForm from './ui/SignupForm';

export default function Signup() {
    const { role } = useUserStore();
    const isGuest = role === 'GUEST';
    const navigate = useNavigate();

    useEffect(() => {
        if (isGuest) {
            navigate(PATH.ROUTE.HOME);
        }
    }, [isGuest, navigate]);

    return (
        <Canvas
            shadows
            camera={{
                fov: 60,
                near: 0.01,
                far: 10000,
                position: [0, 0, 12],
            }}>
            <EarthSky />
            <SignupForm />
        </Canvas>
    );
}
