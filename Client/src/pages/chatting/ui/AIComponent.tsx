import { Bot, ChevronLeft, ChevronRight, Loader } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Button } from '@/shared/ui/shadcn/button';
import { useAIQuestionsQuery } from '../api/queries';
import { AIQuestion } from '../model/interfaces';

interface AIComponentProps {
    chatRoomId: string;
}

export default function AIComponent({ chatRoomId }: AIComponentProps) {
    const [AIQuestions, setAIQuestions] = useState<AIQuestion[]>([]);
    const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
    const [isAiModalOpen, setAiModalOpen] = useState(false);

    const {
        isSuccess,
        isError,
        isLoading,
        data: aiQuestionsData,
    } = useAIQuestionsQuery(chatRoomId);

    useEffect(() => {
        if (aiQuestionsData) {
            setAIQuestions(aiQuestionsData.data);
        }
    }, [aiQuestionsData]);

    const handleClickPrevQuestion = () => {
        if (!AIQuestions.length) return;
        setCurrentQuestionIndex((prev) =>
            prev - 1 < 0 ? AIQuestions.length - 1 : prev - 1
        );
    };

    const handleClickNextQuestion = () => {
        if (!AIQuestions.length) return;
        setCurrentQuestionIndex((prev) => (prev + 1) % AIQuestions.length);
    };

    const toggleOpenModal = () => {
        setAiModalOpen((prev) => !prev);
    };

    return (
        <div className="absolute top-0 left-0 z-50 w-full h-48 bg-gray-300 p-2 rounded-lg flex flex-col justify-between">
            <div className="flex gap-4 flex-col">
                <div className="flex items-center">
                    <Button
                        variant="outline"
                        className="w-16 h-16 mr-2"
                        disabled={!isSuccess}
                        onClick={toggleOpenModal}>
                        {isLoading ? (
                            <Loader className="animate-spin" size={32} />
                        ) : (
                            <Bot size={32} />
                        )}
                    </Button>
                    <p className="font-medium text-black">AI 추천 질문</p>
                </div>

                {isAiModalOpen && (
                    <>
                        {isLoading && (
                            <div className="w-full h-24 flex items-center justify-center bg-white rounded-bl-lg rounded-br-lg">
                                <p className="text-gray-500">
                                    질문을 불러오는 중...
                                </p>
                            </div>
                        )}

                        {(isError || !AIQuestions.length) && (
                            <div className="w-full h-24 flex items-center justify-center bg-red-100 text-red-500 rounded-bl-lg rounded-br-lg">
                                <p>질문 생성 실패</p>
                            </div>
                        )}

                        {isSuccess && AIQuestions.length && (
                            <div className="w-full h-24 bg-white rounded-bl-lg rounded-br-lg p-2 flex justify-between items-center">
                                <Button
                                    size="icon"
                                    variant="ghost"
                                    onClick={handleClickPrevQuestion}>
                                    <ChevronLeft size={20} />
                                </Button>
                                <div className="px-4 py-2 text-center">
                                    {AIQuestions[currentQuestionIndex].content}
                                </div>
                                <Button
                                    size="icon"
                                    variant="ghost"
                                    onClick={handleClickNextQuestion}>
                                    <ChevronRight size={20} />
                                </Button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}
