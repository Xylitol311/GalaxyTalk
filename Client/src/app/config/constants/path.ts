export const BASE_URL = import.meta.env.VITE_API_BASE_URL;
export const OAUTH_URL = import.meta.env.VITE_OAUTH_URL;
export const VERSION = import.meta.env.VITE_API_VERSION;
export const IMAGE_PATH =
    process.env.NODE_ENV === 'development' ? 'public/' : '';

export const ROUTE = {
    HOME: '/',
    SIGN_UP: '/signup',
    MATCH: '/matching-room',
    WARP: '/warp',
    CHAT: '/chatting-room',
    MY_PAGE: '/mypage',
    get PROFILE() {
        return `${this.MY_PAGE}/profile`;
    },
    get RECORDS() {
        return `${this.MY_PAGE}/records`;
    },
    get REVIEWS() {
        return `${this.MY_PAGE}/reviews`;
    },
    get SUPPORT() {
        return `${this.MY_PAGE}/support`;
    },
    // Todo: 탈퇴하기 경로 추가
};

const DOMAIN = {
    OAUTH: '/oauth',
    MATCH: '/match',
    CHAT: '/chat',
    LETTER: '/comment',
    ALARM: '/notification',
    INQUIRY: '/inquiry',
    REPORT: '/report',
};

export const API_PATH = {
    OAUTH: {
        LOGIN: `/oauth2/authorization/kakao`,
        LOGOUT: `${DOMAIN.OAUTH}/logout`,
        SIGNUP: `${DOMAIN.OAUTH}/signup`,
        INFO: DOMAIN.OAUTH,
        STATUS: `${DOMAIN.OAUTH}/status`,
    },
    MATCH: {
        START: DOMAIN.MATCH,
        CANCEL: DOMAIN.MATCH,
        USERS: `${DOMAIN.MATCH}/waiting-users`,
        TIME: `${DOMAIN.MATCH}/start-time`,
        APPROVE: `${DOMAIN.MATCH}/approve`,
    },
    CHAT: {
        // 채팅방 관련
        ROOMS: `${DOMAIN.CHAT}/rooms`,
        GETMSG: `${DOMAIN.CHAT}/messages`,
        RECONNECT: `${DOMAIN.CHAT}/reconnect`,

        // 채팅방 ID가 필요한 엔드포인트를 위한 함수
        room: (chatRoomId: string) => ({
            SENDMSG: `${DOMAIN.CHAT}/${chatRoomId}/message`,
            LEAVE: `${DOMAIN.CHAT}/${chatRoomId}/leave`,
            AI: `${DOMAIN.CHAT}/${chatRoomId}/ai`,
            PARTICIPANTS: `${DOMAIN.CHAT}/${chatRoomId}/participants`,
        }),
    },
};
