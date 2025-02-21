import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

export function debounce<T extends (...args: any[]) => void>(
    func: T,
    delay: number
): (...args: Parameters<T>) => void {
    let timer: ReturnType<typeof setTimeout> | null = null;

    return (...args: Parameters<T>) => {
        if (timer) {
            clearTimeout(timer);
        }

        timer = setTimeout(() => {
            func(...args);
        }, delay);
    };
}

// throttle.ts
export function throttle<T extends (...args: any[]) => void>(
    func: T,
    delay: number
): T {
    let lastCall = 0;
    return function (...args: Parameters<T>) {
        const now = Date.now();
        if (now - lastCall >= delay) {
            lastCall = now;
            func(...args);
        }
    } as T;
}

export function formatTimeDifference(timestamp: number): string {
    const now = new Date();
    const timeDifference = now.getTime() - timestamp;

    const seconds = Math.floor(timeDifference / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (seconds < 60) {
        return '방금 전';
    } else if (minutes < 60) {
        return `${minutes}분 전`;
    } else if (hours < 24) {
        return `${hours}시간 전`;
    } else {
        return `${days}일 전`;
    }
}
