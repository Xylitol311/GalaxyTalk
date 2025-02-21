import { Canvas } from '@react-three/fiber';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { useUserStore } from '@/app/model/stores/user';
import MatchingForm from '@/pages/home/ui/MatchingForm';
import EarthSky from '@/widget/EarthSky';
import SnowHouse from '@/widget/SnowHouse';
import FeedbackForm from './ui/FeedbackForm';
import Login from './ui/Login';

export default function Home() {
    const { userId } = useUserStore();
    const isLogin = !!userId;
    const navigate = useNavigate();
    // const { mutate } = usePostLogout();

    // const handleLogout = () => {
    //     mutate();
    // };

    const handleToMyPage = () => {
        navigate(PATH.ROUTE.MY_PAGE);
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
                    <SnowHouse onClick={handleToMyPage} />
                    <MatchingForm />
                    <FeedbackForm />
                </>
            ) : (
                <Login />
            )}
        </Canvas>
    );
}
