import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig(({ command }) => {
    // command가 'serve'이면 개발 환경, 'build'이면 프로덕션 환경
    const isDev = command === 'serve';

    return {
        plugins: [react()],
        resolve: {
            alias: {
                '@': path.resolve(__dirname, './src'),
            },
        },
        define: { global: 'window' },
        server: {
            // 개발 환경에서만 HTTPS 설정 적용
            ...(isDev && {
                https: {
                    key: fs.readFileSync('key.pem'),
                    cert: fs.readFileSync('.cert.pem'),
                },
                host: true,
            }),
        },
    };
});
