import { z } from 'zod';

export const matchStartSchema = z.object({
    concern: z
        .string()
        .min(10, '고민은 10자 이상 입력해주세요.')
        .max(100, '고민은 100자 이하로 입력해주세요.'),
    preferredMbti: z.string().nullable(),
});

export type MatchStartFormValues = z.infer<typeof matchStartSchema>;
