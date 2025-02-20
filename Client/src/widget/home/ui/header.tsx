import { BellIcon, DoorClosedIcon, DoorOpenIcon } from 'lucide-react';
import { useState } from 'react';
import { usePostLogout } from '@/features/user/api/queries';
import { toast } from '@/shared/model/hooks/use-toast';
import { Button } from '@/shared/ui/shadcn/button';

export default function Header() {
    const { mutate } = usePostLogout();
    const [isHovered, setIsHovered] = useState(false);

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
        <header className="top-0 fixed w-100dvw flex justify-end items-center gap-2 py-4 px-4 z-50">
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
                onClick={handleLogout}
                onMouseEnter={() => setIsHovered(true)}
                onMouseLeave={() => setIsHovered(false)}>
                {isHovered ? <DoorClosedIcon /> : <DoorOpenIcon />}
            </Button>
        </header>
    );
}
