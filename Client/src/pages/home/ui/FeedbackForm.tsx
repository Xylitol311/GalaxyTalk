import { zodResolver } from '@hookform/resolvers/zod';
import { Html } from '@react-three/drei';
import { useState } from 'react';
import { SubmitHandler, useForm } from 'react-hook-form';
import { usePostFeedbackSubmit } from '@/features/feedback/api/queries';
import {
    FeedBackFormValues,
    feedbackSchema,
} from '@/features/feedback/model/schema';
import { Input } from '@/shared/ui/shadcn/input';
import { Textarea } from '@/shared/ui/shadcn/textarea';
import WalkieTalkie from '@/widget/WalkieTalkie';
import { Button } from '../../../shared/ui/shadcn/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '../../../shared/ui/shadcn/dialog';
import { Label } from '../../../shared/ui/shadcn/label';

export default function FeedbackForm() {
    const [open, setOpen] = useState(false);
    const { mutate } = usePostFeedbackSubmit();
    const {
        register,
        handleSubmit,
        formState: { errors },
        reset,
    } = useForm<FeedBackFormValues>({
        resolver: zodResolver(feedbackSchema),
        defaultValues: {
            title: '',
            content: '',
        },
    });

    const handleClickTrigger = () => {
        setOpen(true);
    };

    const handleFormSubmit: SubmitHandler<FeedBackFormValues> = (data) => {
        console.log(data);
        mutate(data, {
            onSuccess: () => {
                reset();
                setOpen(false);
            },
        });
    };

    return (
        <>
            <WalkieTalkie onClick={handleClickTrigger} />

            <Html position={[0, 0, 0]} center>
                <Dialog open={open} onOpenChange={setOpen}>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>
                                개발팀에게 하고 싶은 이야기가 있으신가요?
                            </DialogTitle>
                            <DialogDescription>
                                불편한 점이나 개선이 필요한 부분, 또는 좋았던
                                점까지 자유롭게 이야기해 주세요. 여러분의 의견
                                하나하나가 서비스 발전에 큰 도움이 됩니다.
                            </DialogDescription>
                        </DialogHeader>

                        <form
                            onSubmit={handleSubmit(handleFormSubmit)}
                            className="space-y-6 h-full flex flex-col justify-between">
                            <div className="pb-24">
                                <Label
                                    htmlFor="title"
                                    className="text-gray-400">
                                    제목
                                </Label>
                                <Input
                                    id="title"
                                    {...register('title')}
                                    className={
                                        errors.title ? 'border-red-500' : ''
                                    }
                                    placeholder="제목을 입력해주세요"
                                />
                                {errors.title && (
                                    <p className="text-sm text-red-500">
                                        {errors.title.message}
                                    </p>
                                )}

                                <Label
                                    htmlFor="content"
                                    className="text-gray-400">
                                    내용
                                </Label>

                                <Textarea
                                    id="content"
                                    {...register('content')}
                                    className={
                                        errors.content
                                            ? 'border-red-500 h-full'
                                            : ' h-full'
                                    }
                                    placeholder="내용을 입력해주세요"
                                />

                                {errors.content && (
                                    <p className="text-sm text-red-500">
                                        {errors.content.message}
                                    </p>
                                )}
                            </div>
                            <DialogFooter>
                                <Button type="submit">제출</Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>
            </Html>
        </>
    );
}
