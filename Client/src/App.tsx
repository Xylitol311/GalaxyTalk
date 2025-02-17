import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router';
import { ApiProvider } from './app/config/providers/ApiProvider';
import { router } from './app/config/providers/routes';
import UserProvider from './app/config/providers/UserProvider';
import { queryClient } from './shared/api/query/client';
import { Toaster } from './shared/ui/shadcn/toaster';

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <ApiProvider>
                <UserProvider>
                    <RouterProvider router={router} />
                    <Toaster />
                </UserProvider>
            </ApiProvider>
        </QueryClientProvider>
    );
}
