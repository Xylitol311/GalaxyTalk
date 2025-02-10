import { PersonIcon } from '@radix-ui/react-icons';
import { BellIcon } from 'lucide-react';
import { toast } from '@/shared/model/hooks/use-toast';
import { Button } from '@/shared/ui/shadcn/button';

export default function Header() {
    const handleClick = () => {
        toast({
            variant: 'destructive',
            title: '미구현 버튼입니다.',
        });
    };

    return (
        <header className="flex justify-end items-center gap-2 py-4 px-4">
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
