import { zodResolver } from '@hookform/resolvers/zod';
import { Html } from '@react-three/drei';
import { SubmitHandler, useForm } from 'react-hook-form';
import { MBTI_TYPES } from '@/app/config/constants/mbti';
import { PLANETS } from '@/app/config/constants/planet';
import { usePostSignUp } from '@/features/user/api/queries';
import { SignupFormValues, signupSchema } from '@/features/user/model/schema';
import { Button } from '@/shared/ui/shadcn/button';
import {
    Carousel,
    CarouselContent,
    CarouselItem,
    CarouselNext,
    CarouselPrevious,
} from '@/shared/ui/shadcn/carousel';
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogPortal,
    DialogTitle,
} from '@/shared/ui/shadcn/dialog';
import { Label } from '@/shared/ui/shadcn/label';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/shared/ui/shadcn/select';

export default function SignupForm() {
    const { mutate } = usePostSignUp();
    const {
        handleSubmit,
        formState: { errors },
        setValue,
        watch,
    } = useForm<SignupFormValues>({
        resolver: zodResolver(signupSchema),
        defaultValues: {
            mbti: null,
            planetId: 1,
        },
    });

    const handleFormSubmit: SubmitHandler<SignupFormValues> = (data) => {
        mutate(data);
    };

    return (
        <Html position={[0, 0, 0]} center zIndexRange={[100, 0]}>
            <Dialog open={true}>
                <DialogPortal>
                    <DialogContent
                        showCloseButton={false}
                        onInteractOutside={(e) => e.preventDefault()}
                        onEscapeKeyDown={(e) => e.preventDefault()}>
                        <DialogHeader>
                            <DialogTitle>회원가입</DialogTitle>
                        </DialogHeader>
                        <form
                            onSubmit={handleSubmit(handleFormSubmit)}
                            className="space-y-5 flex flex-col">
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="planetId">
                                    나의 행성 고르기
                                </Label>
                                <Carousel className="max-w-sm self-center">
                                    <CarouselContent className="flex gap-4 overflow-visible">
                                        {PLANETS.map((planet) => {
                                            const isSelected =
                                                watch('planetId') === planet.id;
                                            return (
                                                <CarouselItem
                                                    key={planet.id}
                                                    onClick={() =>
                                                        setValue(
                                                            'planetId',
                                                            planet.id
                                                        )
                                                    }
                                                    className={`min-w-[calc(100%/3)] flex flex-col items-center justify-center p-4 space-y-2 cursor-pointer transition duration-200 ease-in-out shrink-0
                        ${isSelected ? 'bg-gray-800' : 'bg-gray-300'}
                        hover:bg-gray-700 hover:scale-105`}>
                                                    <img
                                                        src={`./src/shared/assets/images/planets/${planet.imageUrl}`}
                                                        alt={planet.name}
                                                        className="w-32 h-32 object-cover"
                                                    />
                                                    <h3 className="text-lg font-semibold text-white">
                                                        {planet.name}
                                                    </h3>
                                                    <p className="text-sm text-center text-gray-500">
                                                        {planet.description}
                                                    </p>
                                                </CarouselItem>
                                            );
                                        })}
                                    </CarouselContent>
                                    <CarouselPrevious type="button" />
                                    <CarouselNext type="button" />
                                </Carousel>

                                {watch('planetId') && (
                                    <p className="flex gap-2 mt-2 text-sm text-gray-600 text-center justify-center">
                                        나의 행성:{' '}
                                        <div className="flex gap-1">
                                            <span className="font-medium text-gray-800">
                                                {
                                                    PLANETS.find(
                                                        (planet) =>
                                                            planet.id ===
                                                            watch('planetId')
                                                    )?.name
                                                }
                                            </span>
                                            <img
                                                src={`./src/shared/assets/images/planets/${watch('planetId')}.PNG`}
                                                alt={
                                                    PLANETS.find(
                                                        (planet) =>
                                                            planet.id ===
                                                            watch('planetId')
                                                    )?.name
                                                }
                                                className="w-4 h-4 object-cover"
                                            />
                                        </div>
                                    </p>
                                )}

                                {errors.planetId && (
                                    <p className="text-sm text-red-500">
                                        {errors.planetId.message}
                                    </p>
                                )}
                            </div>

                            <div>
                                <Label htmlFor="preferredMbti">
                                    MBTI (선택)
                                </Label>
                                <Select
                                    onValueChange={(value) =>
                                        setValue(
                                            'mbti',
                                            value === 'null' ? null : value
                                        )
                                    }
                                    value={watch('mbti') || ''}>
                                    <SelectTrigger className="w-full">
                                        <SelectValue placeholder="MBTI를 선택해주세요" />
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

                            <DialogFooter className="flex justify-between">
                                <Button
                                    type="submit"
                                    onClick={() => {
                                        console.log('가입');
                                    }}>
                                    가입
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </DialogPortal>
            </Dialog>
        </Html>
    );
}
