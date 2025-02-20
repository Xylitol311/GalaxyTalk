import { useRef } from 'react';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { toast } from '@/shared/model/hooks/use-toast';
import useIsMobile from '@/shared/model/hooks/useIsMobile';
import { Button } from '@/shared/ui/shadcn/button';
import { Textarea } from '@/shared/ui/shadcn/textarea';
import { usePostLetter } from '../api/queries';

function LetterFormModal({
    chatRoomId,
    receiverId,
}: {
    chatRoomId: string;
    receiverId: string;
}) {
    const isMobile = useIsMobile();
    const letterContentRef = useRef<HTMLTextAreaElement>(null);
    const navigate = useNavigate();

    const { mutate: postLetter, isPending } = usePostLetter();

    const handleSubmitLetter = () => {
        if (!letterContentRef.current) return;

        // ref를 통해 현재 입력된 편지 내용을 가져옵니다.
        const letterContent = letterContentRef.current?.value;

        // 편지 내용이 비어있는 경우 간단한 유효성 검사
        if (!letterContent.trim()) {
            toast({
                variant: 'destructive',
                title: '편지 내용을 입력해주세요.',
            });
            return;
        }

        // 전송 데이터
        const payload = {
            receiverId,
            content: letterContent,
            chatRoomId,
        };

        // 편지 전송 요청 실행
        postLetter(payload, {
            onError: (error) => {
                console.error('편지 전송 실패:', error);
                toast({
                    variant: 'destructive',
                    title: '편지를 보내는 데 실패했습니다. 다시 시도해주세요.',
                });
            },
        });
    };

    const handlePostponeLetter = () => {
        navigate(PATH.ROUTE.HOME);
    };

    return (
        <>
            {isMobile ? (
                <div className="fixed p-10 w-dvw h-dvh bg-white text-black top-0 left-0 z-50">
                    <div className="flex flex-col justify-between h-full">
                        <h1 className="font-bold text-2xl mb-2">
                            마음을 전하는 편지
                        </h1>
                        <p>
                            당신의 마음을 자유롭게 적어주세요. 오늘의 대화가
                            당신에게 선물이 되었다면, 그 특별했던 순간을 편지에
                            담아 상대방에게 전해보세요.
                        </p>{' '}
                        <Textarea
                            className="my-4 h-full"
                            placeholder="편지 내용을 입력하세요..."
                            ref={letterContentRef}
                        />
                        <div className="flex justify-between">
                            <Button
                                variant="destructive"
                                onClick={handlePostponeLetter}>
                                나중에 보내기
                            </Button>
                            <Button
                                onClick={handleSubmitLetter}
                                disabled={isPending}>
                                보내기
                            </Button>
                        </div>
                    </div>
                </div>
            ) : (
                <div className="fixed p-10 w-full h-full bg-white text-black top-0 left-0 z-50">
                    <div className="flex flex-col justify-between h-full">
                        <h1 className="font-bold text-2xl">
                            마음을 전하는 편지
                        </h1>
                        <p>
                            당신의 마음을 자유롭게 적어주세요. 오늘의 대화가
                            당신에게 선물이 되었다면, 그 특별했던 순간을 편지에
                            담아 상대방에게 전해보세요.
                        </p>
                        <Textarea
                            className="mt-2 mb-4 h-full"
                            ref={letterContentRef}
                            placeholder="편지 내용을 입력하세요..."
                        />
                        <div className="flex justify-between">
                            <Button
                                variant="destructive"
                                onClick={handlePostponeLetter}>
                                나중에 보내기
                            </Button>
                            <Button
                                onClick={handleSubmitLetter}
                                disabled={isPending}>
                                보내기
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}

export default LetterFormModal;
