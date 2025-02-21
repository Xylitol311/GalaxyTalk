import { http, HttpResponse } from 'msw';
import { API_PATH, BASE_URL, VERSION } from '@/app/config/constants/path';

export const letterHandler = [
    http.get(`${BASE_URL}/${VERSION}${API_PATH.LETTER.LIST}`, () => {
        return HttpResponse.json({
            success: true,
            message: '후기 조회 성공',
            data: [
                {
                    id: 1,
                    senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                    receiverId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                    content:
                        '우와asdfasdfjklasdjfklasdjf;sdjljfl;asd;fjasdfjla;sdjfkl;asdjfl;asdj;lfjlsdjfl;sjdl;fjasdj',
                    createdAt: '2025-02-15T19:56:43',
                    chatRoomId: '11111',
                    isHide: 0,
                },
            ],
        });
    }),
];
