import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { useUserStore } from '@/app/model/stores/user';
import { SignupFormValues } from '../model/schema';
import { getUserInfo, getUserStatus, postLogout, postSignup } from './apis';

export const useUserInfoQuery = (enabled = true) => {
    return useQuery({
        queryKey: ['userInfo'],
        queryFn: getUserInfo,
        enabled,
    });
};

export const useUserStatusQuery = (enabled = true) => {
    return useQuery({
        queryKey: ['userStatus'],
        queryFn: getUserStatus,
        enabled,
    });
};

export const usePostSignUp = () => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { setUserBase, setUserStatus } = useUserStore();

    return useMutation({
        mutationFn: (formData: SignupFormValues) => postSignup(formData),
        onSuccess: async (response) => {
            if (response.success) {
                const [updatedUserBase, updatedUserStatus] = await Promise.all([
                    queryClient.fetchQuery({
                        queryKey: ['userInfo'],
                        queryFn: getUserInfo,
                    }),
                    queryClient.fetchQuery({
                        queryKey: ['userStatus'],
                        queryFn: getUserStatus,
                    }),
                ]);

                if (updatedUserBase?.success && updatedUserStatus?.success) {
                    setUserBase(updatedUserBase.data);
                    setUserStatus(updatedUserStatus.data);
                    navigate(PATH.ROUTE.HOME);
                }
            }
        },
        onError: (error) => {
            console.error('회원가입 실패:', error);
        },
    });
};

export const usePostLogout = () => {
    const { reset } = useUserStore();
    const navigate = useNavigate();

    return useMutation({
        mutationFn: postLogout,
        onSuccess: (response) => {
            if (response.success) {
                reset();
                navigate(PATH.ROUTE.HOME);
            }
        },
        onError: (error) => {
            console.error('로그아웃 실패:', error);
        },
    });
};

export const usePostRefresh = () => {
    const queryClient = useQueryClient();
    const { setUserBase, setUserStatus } = useUserStore();

    return useMutation({
        mutationFn: postLogout,
        onSuccess: async (response) => {
            if (response.success) {
                const [updatedUserBase, updatedUserStatus] = await Promise.all([
                    queryClient.fetchQuery({
                        queryKey: ['userInfo'],
                        queryFn: getUserInfo,
                    }),
                    queryClient.fetchQuery({
                        queryKey: ['userStatus'],
                        queryFn: getUserStatus,
                    }),
                ]);

                if (updatedUserBase?.success && updatedUserStatus?.success) {
                    setUserBase(updatedUserBase.data);
                    setUserStatus(updatedUserStatus.data);
                }
            }
        },
        onError: (error) => {
            console.error('refresh 요청 실패:', error);
        },
    });
};
