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
                    userId: '1a2a3a1',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a2',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a3',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a4',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a5',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a6',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a7',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a8',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a9',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a10',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a11',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a12',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a13',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
                },
                {
                    userId: '1a2a3a14',
                    concern: '고민이 있어요',
                    mbti: 'ISTP',
                    status: 'WAITING',
                    startTime: '12355123',
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
