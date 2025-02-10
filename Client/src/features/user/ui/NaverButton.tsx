import NaverLogo from '@/app/ui/logo/NaverLogo';
import { Button } from '@/shared/ui/shadcn/button';

export default function NaverButton() {
    const handleLogin = () => {
        // Memo: 백엔드 서버 배포 시 주석 해제
        // window.location.href = PATH.API_PATH.OAUTH.LOGIN;
    };

    return (
        <Button
            className="bg-green-500 text-white hover:bg-green-600 text-lg p-6 rounded-lg"
            onClick={handleLogin}>
            <NaverLogo />
            네이버로 로그인
        </Button>
    );
}
