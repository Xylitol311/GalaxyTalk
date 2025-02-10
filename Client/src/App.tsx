import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router';
import { router } from './app/config/providers/routes';
import UserProvider from './app/config/providers/UserProvider';
import Layout from './app/ui/logo/Layout';
import { queryClient } from './shared/api/query/client';
import { Toaster } from './shared/ui/shadcn/toaster';

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <UserProvider>
                <Layout>
                    <RouterProvider router={router} />
                </Layout>
                <Toaster />
            </UserProvider>
        </QueryClientProvider>
    );
}
