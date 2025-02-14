import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import { MatchStartFormValues } from '../model/schema';
import {
    deleteMatchCancel,
    getMatchTime,
    getMatchUsers,
    postMatchApprove,
    postMatchStart,
} from './apis';

export const usePostMatchStart = () => {
    const navigate = useNavigate();

    return useMutation({
        mutationFn: (formData: MatchStartFormValues) =>
            postMatchStart(formData),
        onSuccess: (response) => {
            if (response.success) {
                navigate(PATH.ROUTE.MATCH);
            }
        },
        onError: (error) => {
            console.error('매치 시작 실패:', error);
        },
    });
};

export const useDeleteMatchCancel = () => {
    return useMutation({
        mutationFn: deleteMatchCancel,
        onSuccess: (data) => {
            console.log('매치 취소 성공:', data);
        },
        onError: (error) => {
            console.error('매치 취소 실패:', error);
        },
    });
};

export const useMatchUsersQuery = () => {
    return useQuery({
        queryKey: ['match-users'],
        queryFn: getMatchUsers,
    });
};

export const useMatchTimeQuery = () => {
    return useQuery({
        queryKey: ['match-time'],
        queryFn: getMatchTime,
    });
};

export const useMatchApprove = () => {
    return useMutation({
        mutationFn: postMatchApprove,
        onSuccess: (data) => {
            console.log('매치 수락 여부 전송 성공:', data);
        },
        onError: (error) => {
            console.error('매치 수락 여부 전송 실패:', error);
        },
    });
};
