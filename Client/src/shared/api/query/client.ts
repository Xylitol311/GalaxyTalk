import { QueryClient } from '@tanstack/react-query';
import { toast } from '@/shared/model/hooks/use-toast';

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            // 여기서 onError를 지정하지 않으면, 개별 쿼리에서 onError로 처리된 후 전역 구독에서도 에러를 감지합니다.
        },
    },
});

// 전역 에러 핸들링 구독
queryClient.getQueryCache().subscribe((event) => {
    // 쿼리의 상태가 error인 경우
    const queryState = event?.query?.state;
    if (queryState?.status !== 'error' || !queryState.error) return;
    const error = queryState.error;
    // 예시에서는 error 객체 안에 response가 있고, 그 안에 status 코드가 있다고 가정
    const status = error?.response?.status;
    const msg = error?.response?.data?.message;
    console.log(msg);
    if (!status) return;

    switch (status) {
        case 400:
            toast({
                variant: 'destructive',
                title: '잘못된 요청입니다',
            });
            break;
        case 401:
            // 개별 쿼리에서 토큰 갱신 등 추가 작업이 있다면,
            // 여기서는 단순히 toast만 전달합니다.
            toast({
                variant: 'destructive',
                title: '인증 정보 갱신 실패',
            });
            break;
        case 403:
            toast({
                variant: 'destructive',
                title: '권한이 없습니다',
            });
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
            // 세션이 만료되면 페이지 이동
            window.location.href = '/';
            break;
        default:
            toast({
                variant: 'destructive',
                title: '알 수 없는 에러가 발생했습니다.',
            });
    }
});
