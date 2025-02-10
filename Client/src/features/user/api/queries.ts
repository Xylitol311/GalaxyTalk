import { useQuery } from '@tanstack/react-query';
import { getUserInfo, getUserStatus } from './apis';

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
