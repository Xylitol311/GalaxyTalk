import { Canvas } from '@react-three/fiber';
import { useUserStore } from '@/app/model/stores/user';
import { usePostLogout } from '@/features/user/api/queries';
import MatchingForm from '@/pages/home/ui/MatchingForm';
import { toast } from '@/shared/model/hooks/use-toast';
import EarthSky from '@/widget/EarthSky';
import SnowHouse from '@/widget/SnowHouse';
import WalkieTalkie from '@/widget/WalkieTalkie';
import Login from './ui/Login';

export default function Home() {
    const { userId } = useUserStore();
    const { mutate } = usePostLogout();
    const isLogin = !userId;

    const handleLogout = () => {
        mutate();
    };

    const handleClick = () => {
        toast({
            variant: 'destructive',
            title: '미구현 버튼입니다.',
        });
    };

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
            {isLogin ? (
                <>
                    <SnowHouse onClick={handleLogout} />
                    <WalkieTalkie onClick={handleClick} />
                    <MatchingForm />
                </>
            ) : (
                <Login />
            )}
        </Canvas>
    );
}
