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

    http.post(`${BASE_URL}/${VERSION}${API_PATH.OAUTH.SIGNUP}`, () => {
        return HttpResponse.json({
            success: true,
            message: '회원가입에 성공했습니다',
            data: null,
        });
    }),

    http.post(`${BASE_URL}/${VERSION}${API_PATH.OAUTH.LOGOUT}`, () => {
        return HttpResponse.json({
            success: true,
            message: '로그아웃에 성공했습니다',
            data: null,
        });
    }),
];
