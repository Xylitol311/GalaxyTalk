import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { useUserStore } from '@/app/model/stores/user';
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

    const { data: userBaseInfo, isSuccess: isUserInfoSuccess } =
        useUserInfoQuery();
    const { data: userStatusInfo, isSuccess: isUserStatusSuccess } =
        useUserStatusQuery();
    const { setUserBase, setUserStatus } = useUserStore();

    return useMutation({
        mutationFn: (formData: SignupFormValues) => postSignup(formData),
        onSuccess: (response) => {
            if (response.success && isUserInfoSuccess && isUserStatusSuccess) {
                setUserBase(userBaseInfo.data);
                setUserStatus(userStatusInfo.data);
                navigate(PATH.ROUTE.HOME);
            }
        },
        onError: (error) => {
            console.error('회원가입 실패:', error);
        },
    });
};

export const usePostLogout = () => {
    const { data: userBaseInfo, isSuccess: isUserInfoSuccess } =
        useUserInfoQuery();
    const { data: userStatusInfo, isSuccess: isUserStatusSuccess } =
        useUserStatusQuery();
    const { setUserBase, setUserStatus } = useUserStore();

    return useMutation({
        mutationFn: postLogout,
        onSuccess: (response) => {
            if (response.success && isUserInfoSuccess && isUserStatusSuccess) {
                setUserBase({ ...userBaseInfo.data, userId: '' });
                setUserStatus(userStatusInfo.data);
            }
        },
        onError: (error) => {
            console.error('로그아웃 실패:', error);
        },
    });
};
