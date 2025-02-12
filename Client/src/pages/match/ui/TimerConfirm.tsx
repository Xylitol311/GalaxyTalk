import {
    CircleCheckBigIcon,
    CirclePauseIcon,
    SkipForwardIcon,
} from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Button } from '@/shared/ui/shadcn/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogOverlay,
    DialogPortal,
    DialogTitle,
} from '@/shared/ui/shadcn/dialog';

export default function TimerConfirm() {
    const [open, setOpen] = useState(false);
    const [remainingTime, setRemainingTime] = useState(60);
    const cancelRef = useRef<HTMLButtonElement | null>(null);
    // const navigate = useNavigate();

    const handleConfirm = () => {
        // navigate(PATH.ROUTE.CHAT);
        window.location.href = '/chatting-room';
    };

    const handleCancel = () => {
        // navigate(PATH.ROUTE.HOME);
        window.location.href = '/';
    };

    useEffect(() => {
        // 5초 후 자동으로 다이얼로그 열기
        const timeout = setTimeout(() => {
            setOpen(true);
        }, 5000);

        return () => clearTimeout(timeout);
    }, []);

    useEffect(() => {
        if (open) {
            setRemainingTime(60);
            const interval = setInterval(() => {
                setRemainingTime((prev) => {
                    if (prev <= 1) {
                        cancelRef.current?.click();
                        return 0;
                    }
                    return prev - 1;
                });
            }, 1000);

            return () => clearInterval(interval);
        }
    }, [open]);

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            {open && (
                <DialogPortal>
                    <DialogOverlay />
                    <DialogContent
                        showCloseButton={false}
                        onInteractOutside={(e) => e.preventDefault()}
                        onEscapeKeyDown={(e) => e.preventDefault()}>
                        <DialogHeader>
                            <DialogTitle>대화 상대를 찾았어요 !</DialogTitle>
                            <DialogDescription className="flex flex-col items-start gap-5">
                                <div className="flex flex-col items-start mt-3">
                                    <p className="text-black">
                                        상대의 고민: 프로젝트가 힘들어요
                                    </p>
                                    <p className="text-black">
                                        상대의 성향: ENTP
                                    </p>
                                    <p className="text-black">
                                        상대의 온도: 62
                                    </p>
                                </div>

                                <p>
                                    {remainingTime}초 후 자동으로 홈으로
                                    나가집니다.
                                </p>
                            </DialogDescription>
                        </DialogHeader>
                        <DialogFooter className="flex justify-between">
                            <Button variant="confirm" onClick={handleConfirm}>
                                <CircleCheckBigIcon />
                                매칭 성사
                            </Button>
                            <Button
                                variant="pass"
                                onClick={() => setOpen(false)}>
                                <SkipForwardIcon />
                                매칭 거절
                            </Button>
                            <Button
                                ref={cancelRef}
                                variant="warn"
                                onClick={handleCancel}>
                                <CirclePauseIcon />
                                매칭 취소
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </DialogPortal>
            )}
        </Dialog>
    );
}
