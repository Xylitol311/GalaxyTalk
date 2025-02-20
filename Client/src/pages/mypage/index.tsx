import { ExitIcon } from '@radix-ui/react-icons';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { IMAGE_PATH } from '@/app/config/constants/path';
import { getPlanetInfoById } from '@/app/config/constants/planet';
import { useUserStore } from '@/app/model/stores/user';
import { usePostLogout, usePutWithdraw } from '@/features/user/api/queries';
import { Button } from '@/shared/ui/shadcn/button';
import LetterList from './ui/LetterList';
import { MenuList } from './ui/MenuList';

export default function MyPage() {
    const { userId, mbti, planetId, energy } = useUserStore();
    const [view, setView] = useState('profile');
    const navigate = useNavigate();
    const { mutate: logoutMutate } = usePostLogout();
    const { mutate: withdrawMutate } = usePutWithdraw();

    const handleToHome = () => {
        navigate(PATH.ROUTE.HOME);
    };

    const handleLogout = () => {
        logoutMutate();
    };

    const handleWithdraw = () => {
        withdrawMutate();
    };

    const myPlanet = getPlanetInfoById(planetId);
    const userIdLast4 = userId.slice(-4);

    const menuItems = [
        { label: '후기 모아보기', onClick: () => setView('reviews') },
        {
            label: '탈퇴하기',
            onClick: () => setView('withdraw'),
            className: 'text-red-400 hover:text-red-600',
        },
        {
            label: '로그아웃',
            onClick: handleLogout,
            className: 'text-yellow-400 hover:text-yellow-600',
        },
    ];

    return (
        <div className="flex flex-col items-center w-full h-full p-6 bg-black min-h-100dvh relative">
            {(view === 'profile' ||
                view === 'reviews' ||
                view === 'edit-profile') && (
                <div className="w-full max-w-md bg-gray-900 shadow-xl rounded-xl p-6 pt-2 flex flex-col items-center border border-gray-700">
                    <div className="w-full flex justify-start">
                        <Button
                            variant="link"
                            className="flex items-center text-gray-300 hover:text-white pl-0"
                            onClick={handleToHome}>
                            <ExitIcon />
                            홈으로 나가기
                        </Button>
                    </div>

                    <img
                        src={`${IMAGE_PATH}images/planets/${myPlanet?.imageUrl}`}
                        alt={myPlanet?.name}
                        className="w-24 h-24 rounded-full mb-4 border border-gray-500 p-2"
                    />
                    <h2 className="text-xl font-semibold text-indigo-300">{`${myPlanet?.name}#${userIdLast4}`}</h2>
                    <p className="text-gray-400 text-sm">{mbti}</p>
                    <div className="w-full mt-4 flex flex-col items-center relative">
                        <p className="absolute text-xs text-white font-bold top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-10">
                            에너지 {energy}%
                        </p>
                        <div className="relative w-32 h-6 bg-gray-700 rounded-full overflow-hidden">
                            <div
                                className="absolute top-0 left-0 h-full rounded-full transition-all duration-500"
                                style={{
                                    width: `${energy}%`,
                                    background: `linear-gradient(to right, #4facfe, #00f2fe)`,
                                    boxShadow: `0px 0px 8px rgba(79, 172, 254, 0.7)`,
                                }}
                            />
                        </div>
                    </div>

                    <MenuList items={menuItems} />

                    {view === 'reviews' && (
                        <div className="w-full mt-6 text-center">
                            <h2 className="text-xl font-semibold text-indigo-300 mb-4">
                                내가 받은 후기
                            </h2>
                            <LetterList />
                        </div>
                    )}
                </div>
            )}

            {view === 'withdraw' && (
                <div className="w-full max-w-md bg-gray-900 shadow-xl rounded-xl p-6 text-center border border-gray-700">
                    <h2 className="text-xl font-semibold text-red-400">
                        정말 탈퇴하시겠습니까?
                    </h2>
                    <p className="text-gray-400 mt-4">
                        탈퇴 후에는 복구할 수 없습니다.
                    </p>
                    <div className="mt-6 flex space-x-4">
                        <Button
                            variant="outline"
                            onClick={() => setView('profile')}>
                            취소
                        </Button>
                        <Button variant="destructive" onClick={handleWithdraw}>
                            탈퇴하기
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
}
