import { useUserStore } from '@/app/model/stores/user';
import AfterLogin from './ui/AfterLogin';
import BeforeLogin from './ui/BeforeLogin';

export default function Home() {
    const { userId } = useUserStore();
    const isLogin = !!userId;

    return <>{isLogin ? <AfterLogin /> : <BeforeLogin />}</>;
}
