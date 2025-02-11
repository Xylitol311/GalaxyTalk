export const BASE_URL = import.meta.env.VITE_API_BASE_URL;
export const VERSION = import.meta.env.VITE_API_VERSION;

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
        LOGIN: `${BASE_URL}/oauth2/authorization/naver`,
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
};
