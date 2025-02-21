import { http, HttpResponse } from 'msw';
import { API_PATH, BASE_URL, VERSION } from '@/app/config/constants/path';

export const feedbackHandler = [
    http.post(`${BASE_URL}/${VERSION}${API_PATH.FEEDBACK.CREATE}`, () => {
        return HttpResponse.json({
            success: true,
            message: '피드백 등록 성공',
            data: null,
        });
    }),
];
