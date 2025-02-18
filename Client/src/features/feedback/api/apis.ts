import { fetcher } from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { BaseResponseType } from '@/app/model/types/api';
import { FeedBackFormValues } from '../model/schema';

export async function postFeedbackSubmit(formData: FeedBackFormValues) {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.FEEDBACK.CREATE,
        formData
    );

    return data;
}
