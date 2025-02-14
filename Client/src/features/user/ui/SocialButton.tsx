import { API_PATH, BASE_URL } from '@/app/config/constants/path';
import SocialLogo from '@/app/ui/logo/SocialLogo';
import { Button } from '@/shared/ui/shadcn/button';

export default function SocialButton() {
    // const { getUserBase, setUserBase } = useUserStore();
    // const userBase = getUserBase();

    const handleLogin = () => {
        // Memo: 백엔드 서버 배포 시 주석 해제
        window.location.href = `${BASE_URL}/auth${API_PATH.OAUTH.LOGIN}`;
        // setUserBase({
        //     ...userBase,
        //     role: 'GUEST',
        // });
        // window.location.href = '/signup';
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
