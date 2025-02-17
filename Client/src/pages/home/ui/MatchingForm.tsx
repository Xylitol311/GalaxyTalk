import { zodResolver } from '@hookform/resolvers/zod';
import { Html } from '@react-three/drei';
import { useState } from 'react';
import { SubmitHandler, useForm } from 'react-hook-form';
import { MBTI_TYPES } from '@/app/config/constants/mbti';
import { Textarea } from '@/shared/ui/shadcn/textarea';
import Telescope from '@/widget/Telescope';
import { usePostMatchStart } from '../../../features/match/api/queries';
import {
    MatchStartFormValues,
    matchStartSchema,
} from '../../../features/match/model/schema';
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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '../../../shared/ui/shadcn/select';

export default function MatchingForm() {
    const [open, setOpen] = useState(false);
    const { mutate } = usePostMatchStart();
    const {
        register,
        handleSubmit,
        formState: { errors },
        setValue,
        watch,
    } = useForm<MatchStartFormValues>({
        resolver: zodResolver(matchStartSchema),
        defaultValues: {
            concern: '',
            preferredMbti: null,
        },
    });

    const handleClickTrigger = () => {
        setOpen(true);
    };

    const handleFormSubmit: SubmitHandler<MatchStartFormValues> = (data) => {
        const formattedData = {
            ...data,
            preferredMbti:
                data.preferredMbti === 'null' ? null : data.preferredMbti,
        };
        console.log(formattedData);
        mutate(formattedData);
    };

    const handleMbtiChange = (value: string) => {
        setValue('preferredMbti', value);
    };

    const selectValue =
        watch('preferredMbti') === 'null'
            ? '잘 모르겠어요'
            : watch('preferredMbti');

    return (
        <>
            <Telescope onClick={handleClickTrigger} />

            <Html position={[0, 0, 0]} center>
                <Dialog open={open} onOpenChange={setOpen}>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>
                                매칭 프로필을 작성해주세요
                            </DialogTitle>
                            <DialogDescription>
                                나의 고민과 원하는 상대방의 MBTI를 입력해주세요
                            </DialogDescription>
                        </DialogHeader>

                        <form
                            onSubmit={handleSubmit(handleFormSubmit)}
                            className="space-y-6">
                            <div>
                                <Label
                                    htmlFor="concern"
                                    className="text-gray-400">
                                    고민을 적어주세요
                                </Label>

                                <Textarea
                                    id="concern"
                                    {...register('concern')}
                                    className={
                                        errors.concern ? 'border-red-500' : ''
                                    }
                                    placeholder="고민을 10자 이상 100자 이하로 입력해주세요."
                                />

                                {errors.concern && (
                                    <p className="text-sm text-red-500">
                                        {errors.concern.message}
                                    </p>
                                )}
                            </div>

                            <div>
                                <Label
                                    htmlFor="preferredMbti"
                                    className="text-gray-400">
                                    어떤 마음을 가진 상대가 필요하신가요?
                                </Label>
                                <Select
                                    onValueChange={handleMbtiChange}
                                    value={watch('preferredMbti') || ''}>
                                    <SelectTrigger className="w-full">
                                        <SelectValue placeholder="MBTI를 선택해주세요">
                                            {selectValue}
                                        </SelectValue>
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="null">
                                            잘 모르겠어요
                                        </SelectItem>
                                        {MBTI_TYPES.map((type) => (
                                            <SelectItem key={type} value={type}>
                                                {type}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>

                            <DialogFooter>
                                <Button type="submit">매칭 시작</Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>
            </Html>
        </>
    );
}
