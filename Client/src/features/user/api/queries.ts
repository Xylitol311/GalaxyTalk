import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import useFetcher from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { useUserStore } from '@/app/model/stores/user';
import { BaseResponseType } from '@/app/model/types/api';
import { UserBaseType, UserStatusType } from '@/app/model/types/user';
import { SignupFormValues } from '../model/schema';

export const useUserInfoQuery = (enabled = true) => {
    const { fetcher } = useFetcher();

    return useQuery({
        queryKey: ['userInfo'],
        queryFn: async () => {
            const { data } = await fetcher.get<
                BaseResponseType & { data: UserBaseType }
            >(PATH.API_PATH.OAUTH.INFO);

            return data;
        },
        enabled,
    });
};

export const useUserStatusQuery = (enabled = true) => {
    const { fetcher } = useFetcher();

    return useQuery({
        queryKey: ['userStatus'],
        queryFn: async () => {
            const { data } = await fetcher.get<
                BaseResponseType & { data: UserStatusType }
            >(PATH.API_PATH.OAUTH.STATUS);

            return data;
        },
        enabled,
    });
};

export const usePostSignUp = () => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { setUserBase, setUserStatus } = useUserStore();

    return useMutation({
        // mutationFn: (formData: SignupFormValues) => postSignup(formData),
        mutationFn: (formData: SignupFormValues) => {
            return { mbti: '', planetId: '' };
        },
        onSuccess: async (response) => {
            // if (response.success) {
            //     const [updatedUserBase, updatedUserStatus] = await Promise.all([
            //         queryClient.fetchQuery({
            //             queryKey: ['userInfo'],
            //             queryFn: getUserInfo,
            //         }),
            //         queryClient.fetchQuery({
            //             queryKey: ['userStatus'],
            //             queryFn: getUserStatus,
            //         }),
            //     ]);
            //     if (updatedUserBase?.success && updatedUserStatus?.success) {
            //         setUserBase(updatedUserBase.data);
            //         setUserStatus(updatedUserStatus.data);
            //         navigate(PATH.ROUTE.HOME);
            //     }
            // }
        },
        onError: (error) => {
            console.error('회원가입 실패:', error);
        },
    });
};

export const usePostLogout = () => {
    const { fetcher } = useFetcher();

    const { reset } = useUserStore();

    return useMutation({
        mutationFn: async () => {
            const { data } = await fetcher.post<BaseResponseType>(
                PATH.API_PATH.OAUTH.LOGOUT
            );

            return data;
        },
        onSuccess: (response) => {
            if (response.success) {
                reset();
            }
        },
        onError: (error) => {
            console.error('로그아웃 실패:', error);
        },
    });
};

// export const usePostRefresh = () => {
//     const queryClient = useQueryClient();
//     const { setUserBase, setUserStatus } = useUserStore();

//     return useMutation({
//         mutationFn: postLogout,
//         onSuccess: async (response) => {
//             if (response.success) {
//                 const [updatedUserBase, updatedUserStatus] = await Promise.all([
//                     queryClient.fetchQuery({
//                         queryKey: ['userInfo'],
//                         queryFn: getUserInfo,
//                     }),
//                     queryClient.fetchQuery({
//                         queryKey: ['userStatus'],
//                         queryFn: getUserStatus,
//                     }),
//                 ]);

//                 if (updatedUserBase?.success && updatedUserStatus?.success) {
//                     setUserBase(updatedUserBase.data);
//                     setUserStatus(updatedUserStatus.data);
//                 }
//             }
//         },
//         onError: (error) => {
//             console.error('refresh 요청 실패:', error);
//         },
//     });
// };
