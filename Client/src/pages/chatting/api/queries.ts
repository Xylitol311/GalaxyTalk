// queries.ts
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { PATH } from '@/app/config/constants';
import {
    cancelChatRoom,
    deleteChatRoom,
    getAIQuestions,
    getChatParticipants,
    getPreviousMessages,
    postChatMessage,
    postChatReconnect,
    postLetter,
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
    return useMutation({
        mutationFn: (chatRoomId: string) => deleteChatRoom(chatRoomId),
        onSuccess: () => {
            // navigate(PATH.ROUTE.HOME); // 또는 후기 폼 페이지로 이동
            // 폼 모달을 띄우기 위해 라우팅 하지 않음.
        },
        onError: (error) => {
            console.error('채팅방 나가기 실패:', error);
        },
    });
};

// 메시지 목록 조회 Hook
export const useChatMessagesQuery = (chatRoomId: string) => {
    return useQuery({
        queryKey: ['chat-messages', chatRoomId],
        queryFn: () => getPreviousMessages(chatRoomId),
        retry: false,
    });
};

// AI 질문 조회
export const useAIQuestionsQuery = (chatRoomId: string) => {
    return useQuery({
        queryKey: ['ai-questions', chatRoomId],
        queryFn: () => getAIQuestions(chatRoomId),
        retry: false,
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

// 편지 보내기
export const usePostLetter = () => {
    const navigate = useNavigate();

    return useMutation({
        mutationFn: postLetter,
        retry: true,
        onSuccess: () => {
            navigate(PATH.ROUTE.HOME); // 또는 후기 폼 페이지로 이동
        },
        onError: (error) => {
            console.error('편지 전송 실패:', error);
        },
    });
};

// 채팅 취소하기
export const useCancelChatRoom = () => {
    const navigate = useNavigate();

    return useMutation({
        mutationFn: (chatRoomId: string) => cancelChatRoom(chatRoomId),
        retry: true, // 필요에 따라 재시도 옵션을 설정합니다.
        onSuccess: () => {
            navigate(PATH.ROUTE.HOME);
        },
        onError: (error: any) => {
            console.error('채팅 취소 실패:', error);
        },
    });
};
