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
    DialogTrigger,
} from '@/shared/ui/shadcn/dialog';

export default function TimerConfirm() {
    const [open, setOpen] = useState(false);
    const [remainingTime, setRemainingTime] = useState(60);
    const confirmRef = useRef<HTMLButtonElement | null>(null);
    const passRef = useRef<HTMLButtonElement | null>(null);
    const cancelRef = useRef<HTMLButtonElement | null>(null);

    useEffect(() => {
        if (open) {
            setRemainingTime(60);
            const interval = setInterval(() => {
                setRemainingTime((prev) => {
                    if (prev <= 1) {
                        confirmRef.current?.click();
                        return 0;
                    }
                    return prev - 1;
                });
            }, 1000);

            return () => clearInterval(interval);
        }
    }, [open]);

    return (
        <Dialog>
            <DialogTrigger asChild>
                <button className="btn" onClick={() => setOpen(true)}>
                    Open Dialog
                </button>
            </DialogTrigger>
            {open && (
                <DialogPortal>
                    <DialogOverlay />
                    <DialogContent
                        showCloseButton={false}
                        onInteractOutside={(e) => e.preventDefault()}
                        onEscapeKeyDown={(e) => e.preventDefault()}>
                        <DialogHeader>
                            <DialogTitle>Confirm Action</DialogTitle>
                            <DialogDescription>
                                {remainingTime}초 후 자동으로 Confirm이
                                선택됩니다.
                            </DialogDescription>
                        </DialogHeader>
                        <DialogFooter className="flex justify-between">
                            <Button
                                ref={confirmRef}
                                variant="outline"
                                onClick={() => setOpen(false)}>
                                Confirm
                            </Button>
                            <Button
                                ref={passRef}
                                variant="outline"
                                onClick={() => setOpen(false)}>
                                Pass
                            </Button>
                            <Button
                                ref={cancelRef}
                                variant="outline"
                                onClick={() => setOpen(false)}>
                                Cancel
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </DialogPortal>
            )}
        </Dialog>
    );
}
