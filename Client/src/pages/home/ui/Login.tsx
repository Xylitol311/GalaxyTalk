import { Html } from '@react-three/drei';
import SocialButton from '@/features/user/ui/SocialButton';
import Introduce from './Introduce';

export default function Login() {
    return (
        <Html
            position={[0, 0, 0]}
            center
            transform
            zIndexRange={[100, 0]}
            style={{ pointerEvents: 'none' }}>
            <div
                className="flex flex-col gap-4 items-center"
                style={{
                    pointerEvents: 'auto',
                    userSelect: 'none',
                }}>
                <Introduce />
                <span>
                    <SocialButton />
                </span>
            </div>
        </Html>
    );
}
