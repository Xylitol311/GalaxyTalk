import { fetcher } from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { BaseResponseType } from '@/app/model/types/api';
import { MatchStartFormValues } from '../model/schema';

export async function postMatchStart(formData: MatchStartFormValues) {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.MATCH.START,
        formData
    );

    return data;
}

export async function deleteMatchCancel() {
    const { data } = await fetcher.delete(PATH.API_PATH.MATCH.CANCEL);

    return data;
}

export async function getMatchUsers() {
    const { data } = await fetcher.get(PATH.API_PATH.MATCH.CANCEL);

    return data;
}

export async function getMatchTime() {
    const { data } = await fetcher.get(PATH.API_PATH.MATCH.TIME);

    return data;
}

export async function postMatchApprove() {
    const { data } = await fetcher.post(PATH.API_PATH.MATCH.APPROVE);

    return data;
}
