import { CapacitorConfig } from '@capacitor/cli';
import dotenv from 'dotenv';

// 환경 변수 로드 (.env 파일 위치에 따라 옵션 지정 가능)
dotenv.config();

const config: CapacitorConfig = {
    appId: process.env.CAP_APP_ID || 'com.example.app',
    appName: process.env.CAP_APP_NAME || 'galaxy-chat-client',
    // Vite 빌드 결과물이 보통 dist 폴더라면:
    webDir: process.env.WEB_DIR || 'dist',
    bundledWebRuntime: false,
};

export default config;
