import { http, HttpResponse } from 'msw';
import { API_PATH, BASE_URL, VERSION } from '@/app/config/constants/path';

export const matchHandlers = [
    http.post(`${BASE_URL}/${VERSION}${API_PATH.MATCH.START}`, () => {
        return HttpResponse.json({
            success: true,
            message: '매칭 시작에 성공했습니다',
            data: null,
        });
    }),

    http.delete(`${BASE_URL}/${VERSION}${API_PATH.MATCH.CANCEL}`, () => {
        return HttpResponse.json({
            success: true,
            message: '매칭 취소에 성공했습니다',
            data: null,
        });
    }),

    http.get(`${BASE_URL}/${VERSION}${API_PATH.MATCH.USERS}`, () => {
        return HttpResponse.json({
            success: true,
            message: '매칭 대기 중인 유저 조회에 성공했습니다',
            data: [
                {
                    userId: 'user123',
                    concern: '직업 고민',
                    mbti: 'INFJ',
                    status: 'WAITING',
                    startTime: 1707200000000,
                },
                {
                    userId: 'user456',
                    concern: '연애 고민',
                    mbti: 'ENTP',
                    status: 'WAITING',
                    startTime: 1707205000000,
                },
            ],
        });
    }),

    http.get(`${BASE_URL}/${VERSION}${API_PATH.MATCH.TIME}`, () => {
        return HttpResponse.json({
            success: true,
            message: '매칭 시작 시간 조회에 성공했습니다',
            data: 15234738,
        });
    }),

    http.post(`${BASE_URL}/${VERSION}${API_PATH.MATCH.APPROVE}`, () => {
        return HttpResponse.json({
            success: true,
            message: '매칭 수락에 성공했습니다',
            data: null,
        });
    }),
];
