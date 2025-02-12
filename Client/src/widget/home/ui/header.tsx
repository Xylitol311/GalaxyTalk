import { PersonIcon } from '@radix-ui/react-icons';
import { BellIcon } from 'lucide-react';
import { toast } from '@/shared/model/hooks/use-toast';
import { Button } from '@/shared/ui/shadcn/button';

export default function Header() {
    // const { mutate } = usePostLogout();

    // const handleLogout = () => {
    //     // Memo: 백엔드 서버 배포 시 주석 해제
    //     mutate();
    // };

    const handleClick = () => {
        toast({
            variant: 'destructive',
            title: '미구현 버튼입니다.',
        });
    };

    return (
        <header className="fixed w-screen flex justify-end items-center gap-2 py-4 px-4 z-50">
            <Button
                variant="ghost"
                size="icon"
                className="text-yellow-500"
                onClick={handleClick}>
                <BellIcon />
            </Button>

            <Button
                variant="ghost"
                size="icon"
                className="text-white"
                onClick={handleClick}>
                <PersonIcon />
            </Button>
        </header>
    );
}
