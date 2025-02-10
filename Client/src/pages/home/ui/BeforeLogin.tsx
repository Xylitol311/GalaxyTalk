import NaverButton from '@/features/user/ui/NaverButton';
import Introduce from './Introduce';

export default function BeforeLogin() {
    return (
        <div className="flex flex-col space-y-8 items-center justify-center h-screen">
            <Introduce />
            <NaverButton />
        </div>
    );
}
