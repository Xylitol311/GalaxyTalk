import axios from 'axios';
import { BASE_URL, VERSION } from '../config/constants/path';

export const fetcher = axios.create({
    baseURL: `${BASE_URL}/${VERSION}`,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

// Memo: axios 요청 인터셉터 설정
fetcher.interceptors.request.use(
    (config) => {
        return config;
    },
    (error) => {
        Promise.reject(error);
    }
);

// Memo: axios 응답 인터셉터 설정
fetcher.interceptors.response.use(
    (response) => response,
    (error) => {
        // Todo: 에러 처리
        console.error('API 요청 실패:', error);
        return Promise.reject(error);
    }
);
