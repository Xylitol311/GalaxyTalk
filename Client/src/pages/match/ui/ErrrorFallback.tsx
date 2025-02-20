import { ExitIcon } from '@radix-ui/react-icons';
import { Html } from '@react-three/drei';
import { Canvas } from '@react-three/fiber';
import { QueryClientProvider } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { queryClient } from '@/shared/api/query/client';
import { Button } from '@/shared/ui/shadcn/button';
import Galaxy from '@/widget/Galaxy';
import HealingMessage from './HealingMessage';

export default function ErrorFallback() {
    const navigate = useNavigate();

    const handleToHome = () => {
        navigate(PATH.ROUTE.HOME);
    };

    return (
        <Canvas camera={{ position: [4, 2, 5], fov: 40 }}>
            <Galaxy />
            <Html
                position={[0, 0, 0]}
                center
                zIndexRange={[0, 0]}
                style={{ pointerEvents: 'none' }}>
                <QueryClientProvider client={queryClient}>
                    <div className="relative w-100dvw h-100dvh flex flex-col justify-between">
                        <Button
                            variant="link"
                            className="text-white self-start"
                            onClick={handleToHome}
                            style={{ pointerEvents: 'auto' }}>
                            <ExitIcon />
                            이전 페이지로 이동하기
                        </Button>

                        <HealingMessage />
                    </div>
                </QueryClientProvider>
            </Html>
        </Canvas>
    );
}
