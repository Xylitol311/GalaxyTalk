import { useLetterList } from '@/features/letter/api/queries';
import { Card } from '@/shared/ui/shadcn/card';

export default function LetterList() {
    const { data, isSuccess } = useLetterList();

    return (
        <div className="space-y-4">
            {isSuccess &&
                data.data &&
                !!data.data.length &&
                data.data.map((letter) => (
                    <Card
                        key={letter.id}
                        className="p-4 bg-gray-800 shadow-md rounded-lg">
                        {/* <h3 className="text-lg font-semibold text-indigo-300">
                            {letter.senderId}
                        </h3> */}
                        <p className="text-gray-300 mt-2">{letter.content}</p>
                        <p className="text-gray-500 text-sm mt-2 text-right">
                            {new Date(letter.createdAt).toLocaleDateString()}
                        </p>
                    </Card>
                ))}
        </div>
    );
}
