import { Canvas } from '@react-three/fiber';
import { useUserStore } from '@/app/model/stores/user';
import MatchingForm from '@/pages/home/ui/MatchingForm';
import EarthSky from '@/widget/EarthSky';
import Header from '@/widget/home/ui/header';
import Login from './ui/Login';

export default function Home() {
    const { userId } = useUserStore();
    const isLogin = !!userId;

    return (
        <>
            {isLogin && <Header />}
            <Canvas
                shadows
                camera={{
                    fov: 60,
                    near: 0.01,
                    far: 10000,
                    position: [0, 0, 12],
                }}>
                <EarthSky />
                {isLogin ? <MatchingForm /> : <Login />}
            </Canvas>
        </>
    );
}
