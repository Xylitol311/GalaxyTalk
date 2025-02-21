import { http, HttpResponse } from 'msw';
import { API_PATH, BASE_URL, VERSION } from '@/app/config/constants/path';

export const chatHandlers = [
    // 메시지 전송
    http.post(
        `${BASE_URL}/${VERSION}${API_PATH.CHAT.room(':chatRoomId').SENDMSG}`,
        async () => {
            return HttpResponse.json({
                success: true,
                message: '메시지 전송 성공',
                data: null,
            });
        }
    ),

    // 채팅방 나가기
    http.delete(
        `${BASE_URL}/${VERSION}${API_PATH.CHAT.room(':chatRoomId').LEAVE}`,
        () => {
            return HttpResponse.json({
                success: true,
                message: '채팅방 나가기 성공',
                data: null,
            });
        }
    ),

    // 채팅방 목록 조회
    http.get(`${BASE_URL}/${VERSION}${API_PATH.CHAT.ROOMS}`, ({ request }) => {
        const url = new URL(request.url);
        // const cursor = url.searchParams.get('cursor');
        const limit = Number(url.searchParams.get('limit')) || 2;

        const mockData = [
            {
                chatRoomId: '67a438774de6274f718dae46',
                myConcern: '걱정1',
                participantConcern: '걱정2',
                participantPlanet: '1',
                chatRoomCreatedAt: '2025-02-06T13:20:07.191',
                participantReview: '후기',
            },
            {
                chatRoomId: '67a59f3a92d1445354896450',
                myConcern: '걱정4',
                participantConcern: '걱정5',
                participantPlanet: '2',
                chatRoomCreatedAt: '2025-02-07T14:50:50.386',
                participantReview: '후기',
            },
        ];

        const response = {
            success: true,
            message: '이전 대화 정보 요청 성공',
            data: {
                data: mockData.slice(0, limit),
                nextCursor:
                    limit < mockData.length ? mockData[limit].chatRoomId : null,
            },
        };

        return HttpResponse.json(response);
    }),

    // 메시지 목록 조회
    http.get(
        `${BASE_URL}/${VERSION}${API_PATH.CHAT.room(':chatRoomId').GETMSG}`,
        () => {
            return HttpResponse.json({
                success: true,
                message: '이전 대화 조회 성공',
                data: [
                    {
                        senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                        content: '오늘도 코딩하느라 고생이 많네요 😅',
                        createdAt: '2025-02-11T09:13:44.526',
                    },
                    {
                        senderId: 'id2',
                        content: '네... 버그 잡느라 힘들어 죽을 것 같아요 💀',
                        createdAt: '2025-02-11T09:15:34.788',
                    },
                    {
                        senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                        content:
                            '저도 타입 에러랑 씨름 중이에요. 타입스크립트 진짜... 🤯',
                        createdAt: '2025-02-11T09:17:31.601',
                    },
                    {
                        senderId: 'id2',
                        content:
                            '아... 타입스크립트요? 저는 지금 리액트 상태관리때문에 머리가 터질 것 같네요 😫',
                        createdAt: '2025-02-11T09:18:38.388',
                    },
                    {
                        senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                        content: '리덕스인가요? 아니면 리코일?',
                        createdAt: '2025-02-11T09:18:55.123',
                    },
                    {
                        senderId: 'id2',
                        content:
                            '주스탠드예요... 러닝커브가 생각보다 높네요 😭',
                        createdAt: '2025-02-11T09:19:22.456',
                    },
                    {
                        senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                        content:
                            '아... 저도 한번 써봤는데 처음에 많이 헤맸어요. 나중엔 편해질 거에요! 화이팅입니다 💪',
                        createdAt: '2025-02-11T09:20:15.789',
                    },
                    {
                        senderId: 'id2',
                        content: '감사합니다 ㅠㅠ 우리 둘 다 화이팅해요! ✨',
                        createdAt: '2025-02-11T09:21:03.234',
                    },
                    {
                        senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                        content:
                            '그런데 프론트엔드 개발하신지는 얼마나 되셨어요? 🤔',
                        createdAt: '2025-02-11T09:22:15.123',
                    },
                    {
                        senderId: 'id2',
                        content:
                            '아직 1년 정도밖에 안됐어요. 아직 배울게 너무 많네요 😅',
                        createdAt: '2025-02-11T09:23:45.456',
                    },
                    {
                        senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                        content:
                            '와! 1년차치고는 정말 열심히 하고 계시네요. 주스탠드까지 도전하시고! 👏',
                        createdAt: '2025-02-11T09:24:30.789',
                    },
                    {
                        senderId: 'id2',
                        content:
                            '감사합니다 😊 요즘은 Next.js도 공부하고 있어요',
                        createdAt: '2025-02-11T09:25:12.234',
                    },
                    {
                        senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                        content:
                            '오! Next.js 좋죠. SSR이랑 파일 기반 라우팅이 정말 편해요 👍',
                        createdAt: '2025-02-11T09:26:05.567',
                    },
                    {
                        senderId: 'id2',
                        content:
                            '맞아요! 근데 가끔 hydration 에러가 나서 당황스러울 때가 있어요 😅',
                        createdAt: '2025-02-11T09:27:23.890',
                    },
                    {
                        senderId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                        content:
                            '아... 그거 정말 골치 아프죠. useEffect 타이밍 맞추기가 까다로워요 😫',
                        createdAt: '2025-02-11T09:28:45.123',
                    },
                ],
            });
        }
    ),

    // AI 질문 생성
    http.get(
        `${BASE_URL}/${VERSION}${API_PATH.CHAT.room(':chatRoomId').AI}`,
        () => {
            return HttpResponse.json({
                success: true,
                message: 'AI 질문 생성 성공',
                data: [
                    {
                        questionId: '1',
                        content:
                            '현재의 상황이 만약 당신의 가장 친한 친구의 상황이라면, 어떤 조언을 해주고 싶을까요?',
                    },
                    {
                        questionId: '2',
                        content: '감정을 표현하는 것이 어려운 이유가 있을까요?',
                    },
                    {
                        questionId: '3',
                        content:
                            '취업에 대한 기대감과 두려움은 어떤 것들이 있나요?',
                    },
                    {
                        questionId: '4',
                        content:
                            '당신이 생각하는 이상적인 직장은 어떤 모습인가요?',
                    },
                    {
                        questionId: '5',
                        content:
                            '당신에게 가장 중요한 가치는 무엇이며, 그 가치가 취업에 어떻게 영향을 미치나요?',
                    },
                    {
                        questionId: '6',
                        content:
                            '최근에 자신을 자랑스럽게 생각했던 순간이 있다면, 그것은 무엇인가요?',
                    },
                    {
                        questionId: '7',
                        content:
                            '불안함을 조금이라도 줄일 수 있는 방법이 있다면, 그것은 무엇일까요?',
                    },
                    {
                        questionId: '8',
                        content:
                            '당신이 취업에 성공했을 때, 무엇이 가장 먼저 변할 것 같나요?',
                    },
                    {
                        questionId: '9',
                        content:
                            '지금의 상황을 조금 더 긍정적으로 바라볼 수 있는 방법이 있다면, 그것은 무엇일까요?',
                    },
                    {
                        questionId: '10',
                        content:
                            '자신의 감정을 인정하고 이해하는 것이 어려울 때, 어떤 방법을 사용하나요?',
                    },
                ],
            });
        }
    ),

    // 재연결
    http.post(`${BASE_URL}/${VERSION}${API_PATH.CHAT.RECONNECT}`, () => {
        return HttpResponse.json({
            success: true,
            message: '재연결 성공',
            data: {
                chatRoomId: 'E397AWEXERA', // 생성된 채팅방 id
                sessionId: 1, // 생성된 openvidu sessionId
                token: 'WEIR2Q0973', // session 접속용 토큰
            },
        });
    }),

    // 참가자 정보 조회
    http.get(
        `${BASE_URL}/${VERSION}${API_PATH.CHAT.room(':chatRoomId').PARTICIPANTS}`,
        () => {
            return HttpResponse.json({
                success: true,
                message: '참가자 정보 조회 성공',
                data: {
                    participants: [
                        {
                            userId: 'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs',
                            mbti: 'ISTP',
                            concern: '걱정11',
                            planetId: 1,
                            energy: 30,
                        },
                        {
                            userId: 'user2',
                            mbti: 'ESTP',
                            concern: '걱정22',
                            planetId: 1,
                            energy: 50,
                        },
                    ],
                    similarity: 0.123,
                },
            });
        }
    ),

    // 편지 저장 API mocking
    http.post(`${BASE_URL}/${VERSION}${API_PATH.LETTER.CREATE}`, () => {
        return HttpResponse.json({
            success: true,
            message: '편지 저장 성공',
            data: null,
        });
    }),

    // 채팅 취소 API mocking: PUT 메서드 사용
    http.put(
        `${BASE_URL}/${VERSION}${API_PATH.CHAT.room(':chatRoomId').CANCELCHAT}`,
        () => {
            // 정상적인 경우 200 OK 응답 반환
            return HttpResponse.json({
                statusCode: 200,
                data: { message: '채팅 취소 성공' },
            });
        }
    ),
];
