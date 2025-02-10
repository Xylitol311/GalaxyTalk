import MatchingForm from '@/features/match/ui/MatchingForm';
import Header from '@/widget/home/ui/header';

export default function AfterLogin() {
    return (
        <div className="h-screen flex flex-col">
            <Header />
            <div className="flex-1 flex items-center justify-center">
                <MatchingForm />
            </div>
        </div>
    );
}
