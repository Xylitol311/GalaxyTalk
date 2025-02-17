import axios from 'axios';
import { toast } from '@/shared/model/hooks/use-toast';
import { BASE_URL, VERSION } from '../config/constants/path';

export default function useFetcher() {
    const fetcher = axios.create({
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
            // Todo: 에러 처리
            console.error('API 요청 실패:', error);

            console.log(error.status);
            console.log(error.message);

            switch (error.status) {
                case 401:
                    toast({
                        variant: 'destructive',
                        title: '인증 정보 갱신 실패',
                    });
                    try {
                        await refreshTokenMutation.mutateAsync();
                        return fetcher(config); // 토큰 갱신 후 재요청
                    } catch (refreshError) {
                        console.log(refreshError);
                        toast({
                            variant: 'destructive',
                            title: '인증 정보 갱신 실패',
                        });
                    }
                    break;
                case 403:
                    toast({ variant: 'destructive', title: '권한이 없습니다' });
                    break;
                case 404:
                    toast({
                        variant: 'destructive',
                        title: '잘못된 요청입니다',
                    });
                    break;
                case 498:
                    toast({
                        variant: 'destructive',
                        title: 'AI 질문이 아직 생성되지 않았습니다',
                    });
                    break;
                case 499:
                    toast({
                        variant: 'destructive',
                        title: '세션이 만료되었습니다. 다시 로그인해주세요.',
                    });
                    window.location.href = '/';
                    break;
                default:
                    toast({
                        variant: 'destructive',
                        title: '알 수 없는 에러가 발생했습니다.',
                    });
            }

            return Promise.reject(error);
        }
    );

    return { fetcher };
}
