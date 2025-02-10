import { http, HttpResponse } from 'msw';
import { API_PATH, BASE_URL, VERSION } from '@/app/config/constants/path';

export const userHandlers = [
    http.get(`${BASE_URL}/${VERSION}${API_PATH.OAUTH.INFO}`, () => {
        return HttpResponse.json({
            success: true,
            message: '유저 정보 조회에 성공했습니다',
            data: {
                userId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                energy: 15,
                mbti: 'ENFJ',
                planetId: 1,
                role: 'USER',
            },
        });
    }),

    http.get(`${BASE_URL}/${VERSION}${API_PATH.OAUTH.STATUS}`, () => {
        return HttpResponse.json({
            success: true,
            message: '유저 접속 상태 조회에 성공했습니다',
            data: {
                userInteractionState: 'idle',
            },
        });
    }),
];
