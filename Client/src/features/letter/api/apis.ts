import { fetcher } from '@/app/api/axios';
import { PATH } from '@/app/config/constants';
import { BaseResponseType } from '@/app/model/types/api';

type LetterType = {
    id: number;
    senderId: string;
    receiverId: string;
    content: string;
    createdAt: string;
    chatRoomId: string;
    isHide: number;
};

export async function getLetterList() {
    const { data } = await fetcher.get<
        BaseResponseType & { data: LetterType[] | null }
    >(PATH.API_PATH.LETTER.LIST);

    return data;
}
