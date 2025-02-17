// import { useMutation } from '@tanstack/react-query';
// import { createContext, useContext, useEffect } from 'react';
// import { fetcher } from '@/app/api/axios';
// import { eventBus } from '@/app/api/eventBus';
// import { toast } from '@/shared/model/hooks/use-toast';
// import { PATH } from '../constants';

// const ApiContext = createContext<{ fetcher: typeof fetcher }>({
//     fetcher,
// });

// export const ApiProvider = ({ children }: { children: React.ReactNode }) => {
//     const refreshTokenMutation = useMutation({
//         mutationFn: async () => {
//             const response = await fetcher.post(
//                 PATH.API_PATH.OAUTH.REFRESH,
//                 {},
//                 { withCredentials: true }
//             );
//             return response.data;
//         },
//         onSuccess: () => {
//             console.log('refresh succeed');
//         },
//         onError: () => {
//             console.log('refresh failed');
//         },
//     });

//     useEffect(() => {
//         const handleApiError = async ({ status, config, error }: any) => {
//             console.log(error);
//             switch (status) {
//                 case 400:
//                     toast({
//                         variant: 'destructive',
//                         title: '잘못된 요청입니다',
//                     });
//                     break;
//                 case 401:
//                     try {
//                         await refreshTokenMutation.mutateAsync();
//                         return fetcher(config); // 토큰 갱신 후 재요청
//                     } catch (refreshError) {
//                         console.log(refreshError);
//                         toast({
//                             variant: 'destructive',
//                             title: '인증 정보 갱신 실패',
//                         });
//                     }
//                     break;
//                 case 403:
//                     toast({ variant: 'destructive', title: '권한이 없습니다' });
//                     break;
//                 case 404:
//                     toast({
//                         variant: 'destructive',
//                         title: '잘못된 요청입니다',
//                     });
//                     break;
//                 case 498:
//                     toast({
//                         variant: 'destructive',
//                         title: 'AI 질문이 아직 생성되지 않았습니다',
//                     });
//                     break;
//                 case 499:
//                     toast({
//                         variant: 'destructive',
//                         title: '세션이 만료되었습니다. 다시 로그인해주세요.',
//                     });
//                     window.location.href = '/';
//                     break;
//                 default:
//                     toast({
//                         variant: 'destructive',
//                         title: '알 수 없는 에러가 발생했습니다.',
//                     });
//             }
//         };

//         eventBus.on('apiError', handleApiError);

//         return () => {
//             eventBus.off('apiError', handleApiError); // 클린업
//         };
//     }, [refreshTokenMutation]);

//     return (
//         <ApiContext.Provider value={{ fetcher }}>
//             {children}
//         </ApiContext.Provider>
//     );
// };

// export const useApi = () => useContext(ApiContext);
