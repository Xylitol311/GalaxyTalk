import { fetcher } from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { BaseResponseType } from '@/app/model/types/api';
import { UserBaseType, UserStatusType } from '@/app/model/types/user';

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
