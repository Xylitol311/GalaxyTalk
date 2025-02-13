import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { useUserStore } from '@/app/model/stores/user';
import { BaseResponseType } from '@/app/model/types/api';
import { UserBaseType, UserStatusType } from '@/app/model/types/user';
import { SignupFormValues } from '../model/schema';
import { getUserInfo, getUserStatus, postLogout, postSignup } from './apis';

export const useUserInfoQuery = () => {
    return useQuery({
        queryKey: ['userInfo'],
        queryFn: getUserInfo,
    });
};

export const useUserStatusQuery = () => {
    return useQuery({
        queryKey: ['userStatus'],
        queryFn: getUserStatus,
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
                // React Query에게 기존 데이터를 무효화하고 새로 가져오도록 요청
                await queryClient.invalidateQueries({ queryKey: ['userInfo'] });
                await queryClient.invalidateQueries({
                    queryKey: ['userStatus'],
                });

                const updatedUserBase = queryClient.getQueryData<{
                    data: BaseResponseType & { data: UserBaseType };
                }>(['userInfo']);
                const updatedUserStatus = queryClient.getQueryData<{
                    data: BaseResponseType & { data: UserStatusType };
                }>(['userStatus']);

                if (
                    updatedUserBase?.data.success &&
                    updatedUserStatus?.data.success
                ) {
                    setUserBase(updatedUserBase.data.data);
                    setUserStatus(updatedUserStatus.data.data);
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
    const queryClient = useQueryClient();
    const { setUserBase, setUserStatus } = useUserStore();

    return useMutation({
        mutationFn: postLogout,
        onSuccess: async (response) => {
            if (response.success) {
                await queryClient.invalidateQueries({ queryKey: ['userInfo'] });
                await queryClient.invalidateQueries({
                    queryKey: ['userStatus'],
                });

                const updatedUserBase = queryClient.getQueryData<{
                    data: BaseResponseType & { data: UserBaseType };
                }>(['userInfo']);
                const updatedUserStatus = queryClient.getQueryData<{
                    data: BaseResponseType & { data: UserStatusType };
                }>(['userStatus']);

                if (
                    updatedUserBase?.data.success &&
                    updatedUserStatus?.data.success
                ) {
                    setUserBase(updatedUserBase.data.data);
                    setUserStatus(updatedUserStatus.data.data);
                }
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
                await queryClient.invalidateQueries({ queryKey: ['userInfo'] });
                await queryClient.invalidateQueries({
                    queryKey: ['userStatus'],
                });

                const updatedUserBase = queryClient.getQueryData<{
                    data: BaseResponseType & { data: UserBaseType };
                }>(['userInfo']);
                const updatedUserStatus = queryClient.getQueryData<{
                    data: BaseResponseType & { data: UserStatusType };
                }>(['userStatus']);

                if (
                    updatedUserBase?.data.success &&
                    updatedUserStatus?.data.success
                ) {
                    setUserBase(updatedUserBase.data.data);
                    setUserStatus(updatedUserStatus.data.data);
                }
            }
        },
        onError: (error) => {
            console.error('refresh 요청 실패:', error);
        },
    });
};
