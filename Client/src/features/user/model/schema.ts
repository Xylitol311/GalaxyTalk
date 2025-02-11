import { z } from 'zod';

export const signupSchema = z.object({
    mbti: z.string().nullable(),
    planetId: z.number(),
});

export type SignupFormValues = z.infer<typeof signupSchema>;
