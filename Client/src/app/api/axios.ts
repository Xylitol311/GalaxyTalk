import axios from 'axios';
import { PATH } from '../config/constants';
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
    async (error) => {
        const originalRequest = error.config;

        if (!error.response) {
            console.error('네트워크 오류 또는 CORS 문제 발생:', error);
            return Promise.reject(error);
        }

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                await axios.post(
                    `${BASE_URL}/${VERSION}${PATH.API_PATH.OAUTH.REFRESH}`,
                    {},
                    {
                        withCredentials: true,
                    }
                );

                return fetcher(originalRequest);
            } catch (refreshError) {
                console.error('토큰 갱신 실패:', refreshError);
                window.location.href = '/';
            }
        }
        return Promise.reject(error);
    }
);
