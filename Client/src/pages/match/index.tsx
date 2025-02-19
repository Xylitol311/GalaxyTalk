import { ErrorBoundary } from 'react-error-boundary';
import ErrorFallback from './ui/ErrrorFallback';
import MatchingRoom from './ui/MatchingRoom';

export default function MatchingPage() {
    return (
        <ErrorBoundary FallbackComponent={ErrorFallback}>
            <MatchingRoom />
        </ErrorBoundary>
    );
}
