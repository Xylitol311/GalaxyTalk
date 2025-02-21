import { useMutation } from '@tanstack/react-query';
import { FeedBackFormValues } from '../model/schema';
import { postFeedbackSubmit } from './apis';

export const usePostFeedbackSubmit = (onSuccessCallback?: () => void) => {
    return useMutation({
        mutationFn: (formData: FeedBackFormValues) =>
            postFeedbackSubmit(formData),
        onSuccess: () => {
            if (onSuccessCallback) onSuccessCallback();
        },
        onError: (error) => {
            console.error('피드백 제출 실패:', error);
        },
    });
};
