import { ExitIcon } from '@radix-ui/react-icons';
import { Link } from 'react-router';
import { PATH } from '@/app/config/constants';
import { Button } from '@/shared/ui/shadcn/button';
import HealingMessage from './ui/HealingMessage';
import TimerConfirm from './ui/TimerConfirm';

export default function MatchingRoom() {
    return (
        <div className="relative h-screen flex flex-col justify-between">
            <Link to={PATH.ROUTE.HOME}>
                <Button variant="link" className="text-white">
                    <ExitIcon />
                    이전 페이지로 이동하기
                </Button>
            </Link>
            <TimerConfirm />
            <HealingMessage />
        </div>
    );
}
