import { z } from 'zod';

export const feedbackSchema = z.object({
    title: z.string().min(1, '제목은 1자 이상 입력해주세요.'),
    content: z.string().min(1, '내용은 1자 이상 입력해주세요.'),
});

export type FeedBackFormValues = z.infer<typeof feedbackSchema>;
