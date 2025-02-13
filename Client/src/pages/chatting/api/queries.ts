// queries.ts
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import {
    deleteChatRoom,
    getChatMessages,
    getChatParticipants,
    postAIQuestions,
    postChatMessage,
    postChatReconnect,
} from './apis';

// 메시지 전송
export const usePostChatMessage = (chatRoomId: string) => {
    return useMutation({
        mutationFn: (content: string) => postChatMessage(chatRoomId, content),
        onError: (error) => {
            console.error('메시지 전송 실패:', error);
        },
    });
};

// 채팅방 나가기
export const useDeleteChatRoom = () => {
    const navigate = useNavigate();

    return useMutation({
        mutationFn: (chatRoomId: string) => deleteChatRoom(chatRoomId),
        onSuccess: () => {
            navigate(PATH.ROUTE.HOME); // 또는 후기 폼 페이지로 이동
        },
        onError: (error) => {
            console.error('채팅방 나가기 실패:', error);
        },
    });
};

// 메시지 목록 조회
export const useChatMessagesQuery = () => {
    return useQuery({
        queryKey: ['chat-messages'],
        queryFn: getChatMessages,
    });
};

// AI 질문 생성
export const usePostAIQuestions = () => {
    return useMutation({
        mutationFn: (chatRoomId: string) => postAIQuestions(chatRoomId),
        onError: (error) => {
            console.error('AI 질문 생성 실패:', error);
        },
    });
};

// 재연결
export const usePostChatReconnect = () => {
    const navigate = useNavigate();

    return useMutation({
        mutationFn: postChatReconnect,
        retry: true,
        onError: (error) => {
            navigate(PATH.ROUTE.HOME); // 참여할 채팅방이 없다면 홈화면으로 이동
            console.error('채팅 재연결 실패:', error);
        },
    });
};

// 참가자 정보 조회
export const useGetChatParticipants = (chatRoomId: string) => {
    return useQuery({
        queryKey: ['chatParticipants', chatRoomId],
        queryFn: () => getChatParticipants(chatRoomId),
        retry: true,
    });
};
