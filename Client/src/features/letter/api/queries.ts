import { useQuery } from '@tanstack/react-query';
import { getLetterList } from './apis';

export const useLetterList = () => {
    return useQuery({
        queryKey: ['letter-list'],
        queryFn: getLetterList,
    });
};
