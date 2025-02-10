import { fetcher } from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { UserBaseType, UserStatusType } from '@/app/model/types/user';

export async function getUserInfo() {
    const { data } = await fetcher.get<UserBaseType>(PATH.API_PATH.OAUTH.INFO);

    return data;
}

export async function getUserStatus() {
    const { data } = await fetcher.get<UserStatusType>(
        PATH.API_PATH.OAUTH.STATUS
    );

    return data;
}
