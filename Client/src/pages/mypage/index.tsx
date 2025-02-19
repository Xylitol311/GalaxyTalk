import { ExitIcon } from '@radix-ui/react-icons';
import { useState } from 'react';
import { IMAGE_PATH } from '@/app/config/constants/path';
import { getPlanetInfoById } from '@/app/config/constants/planet';
import { useUserStore } from '@/app/model/stores/user';
import { Button } from '@/shared/ui/shadcn/button';

export default function MyPage() {
    const { userId, mbti, planetId, energy } = useUserStore();
    const [view, setView] = useState('profile');

    const myPlanet = getPlanetInfoById(planetId);
    const userIdLast4 = userId.slice(-4);

    return (
        <div className="flex flex-col items-center w-full h-full p-6 bg-black min-h-screen relative">
            {view === 'profile' && (
                <div className="w-full max-w-md bg-gray-900 shadow-xl rounded-xl p-6 pt-2 flex flex-col items-center border border-gray-700">
                    <div className="w-full flex justify-start">
                        <Button
                            variant="link"
                            className="flex items-center text-gray-300 hover:text-white pl-0"
                            onClick={() => setView('profile')}>
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
                    <div className="mt-6 w-full max-w-md bg-gray-800 shadow-md rounded-xl p-2 border border-gray-700">
                        <ul className="text-center divide-y divide-gray-700">
                            <li
                                className="cursor-pointer text-gray-300 hover:text-white py-2"
                                onClick={() => setView('reviews')}>
                                후기 모아보기
                            </li>
                            <li
                                className="cursor-pointer text-red-400 hover:text-red-600 py-2"
                                onClick={() => setView('withdraw')}>
                                탈퇴하기
                            </li>
                        </ul>
                    </div>
                </div>
            )}

            {view === 'reviews' && (
                <div className="w-full max-w-md bg-gray-900 shadow-xl rounded-xl p-6 text-center border border-gray-700">
                    <h2 className="text-xl font-semibold text-indigo-300">
                        내가 받은 후기
                    </h2>
                    <p className="text-gray-400 mt-4">
                        후기 내용이 여기에 표시됩니다.
                    </p>
                    <Button className="mt-6" onClick={() => setView('profile')}>
                        돌아가기
                    </Button>
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
                        <Button
                            variant="destructive"
                            onClick={() =>
                                alert('탈퇴 기능은 아직 구현되지 않았습니다.')
                            }>
                            탈퇴하기
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
}
