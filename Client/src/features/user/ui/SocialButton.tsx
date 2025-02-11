import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { useUserStore } from '@/app/model/stores/user';
import SocialLogo from '@/app/ui/logo/SocialLogo';
import { Button } from '@/shared/ui/shadcn/button';

export default function SocialButton() {
    const navigate = useNavigate();
    const { getUserBase, setUserBase } = useUserStore();
    const userBase = getUserBase();

    const handleLogin = () => {
        // Memo: 백엔드 서버 배포 시 주석 해제
        // window.location.href = `https://i12a503.p.ssafy.io/auth/oauth2/authorization/kakao`;
        setUserBase({
            ...userBase,
            role: 'GUEST',
        });
        navigate(PATH.ROUTE.SIGN_UP);
    };

    return (
        <Button
            className="bg-yellow-400 text-white hover:bg-yellow-500 text-lg p-6 rounded-lg"
            onClick={handleLogin}>
            <SocialLogo />
            카카오 로그인
        </Button>
    );
}
