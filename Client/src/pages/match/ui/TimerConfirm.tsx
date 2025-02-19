import {
    CircleCheckBigIcon,
    CirclePauseIcon,
    SkipForwardIcon,
} from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import {
    useDeleteMatchCancel,
    useMatchApprove,
} from '@/features/match/api/queries';
import { toast } from '@/shared/model/hooks/use-toast';
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
import { MatchUserType } from '..';

type TimerProps = {
    matchData: MatchUserType | null;
    handleToHome: () => void;
    handleResetData: () => void;
};

export default function TimerConfirm({
    matchData,
    handleToHome,
    handleResetData,
}: TimerProps) {
    const [open, setOpen] = useState(false);
    const [remainingTime, setRemainingTime] = useState(60);
    const cancelRef = useRef<HTMLButtonElement | null>(null);
    const { mutate: matchCancelMutate } = useDeleteMatchCancel();
    const { mutate: matchApproveMutate } = useMatchApprove();

    const handleConfirm = () => {
        matchApproveMutate({
            matchId: `${matchData?.matchId}`,
            accepted: true,
        });
        toast({
            title: '상대방의 대화 수락 여부를 기다립니다',
        });
    };

    const handleCancel = () => {
        matchApproveMutate({
            matchId: `${matchData?.matchId}`,
            accepted: false,
        });
        matchCancelMutate();
        handleResetData();
        handleToHome();
    };

    const handlePass = () => {
        matchApproveMutate({
            matchId: `${matchData?.matchId}`,
            accepted: false,
        });
        toast({
            title: '다른 대화 상대를 찾아볼게요',
        });
        setRemainingTime(60);
        handleResetData();
    };

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

    useEffect(() => {
        if (matchData) {
            setOpen(true);
        } else {
            setOpen(false);
        }
    }, [matchData]);

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogPortal>
                <DialogOverlay />
                <DialogContent
                    showCloseButton={false}
                    onInteractOutside={(e) => e.preventDefault()}
                    onEscapeKeyDown={(e) => e.preventDefault()}>
                    <DialogHeader>
                        <DialogTitle>대화 상대를 찾았어요!</DialogTitle>
                        <DialogDescription className="flex flex-col items-start gap-5">
                            <div className="flex flex-col items-start mt-3">
                                <p className="text-black">
                                    상대의 고민: {matchData?.concern}
                                </p>
                                <p className="text-black">
                                    상대의 성향: {matchData?.mbti}
                                </p>
                                <p className="text-black">
                                    상대의 온도: {matchData?.energy}
                                </p>
                                <p className="text-black">
                                    유사도 점수: {matchData?.similarity}%
                                </p>
                            </div>
                            <p>
                                {remainingTime}초 후 자동으로 홈으로 나가집니다.
                            </p>
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter className="flex justify-between">
                        <Button variant="confirm" onClick={handleConfirm}>
                            <CircleCheckBigIcon />
                            매칭 성사
                        </Button>
                        <Button variant="pass" onClick={handlePass}>
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
        </Dialog>
    );
}
