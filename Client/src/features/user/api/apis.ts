import { fetcher } from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { BaseResponseType } from '@/app/model/types/api';
import { UserBaseType, UserStatusType } from '@/app/model/types/user';
import { SignupFormValues } from '../model/schema';

export async function getUserInfo() {
    const { data } = await fetcher.get<
        BaseResponseType & { data: UserBaseType }
    >(PATH.API_PATH.OAUTH.INFO);

    return data;
}

export async function getUserStatus() {
    const { data } = await fetcher.get<
        BaseResponseType & { data: UserStatusType }
    >(PATH.API_PATH.OAUTH.STATUS);

    return data;
}

export async function postSignup(formData: SignupFormValues) {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.OAUTH.SIGNUP,
        formData
    );

    return data;
}

export async function postLogout() {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.OAUTH.LOGOUT
    );

    return data;
}

export async function postRefresh() {
    const { data } = await fetcher.post<BaseResponseType>(
        PATH.API_PATH.OAUTH.REFRESH
    );

    return data;
}
