import { useEffect, useState } from 'react';
import { HEALING_MESSAGES } from '@/app/config/constants/healing';
import { DELAY } from '@/app/config/constants/time';

export default function HealingMessage() {
    const [message, setMessage] = useState(HEALING_MESSAGES[0]);
    const [fade, setFade] = useState(true);

    const { MESSAGE_FADE, MESSAGE_INTERVAL } = DELAY;

    useEffect(() => {
        const interval = setInterval(() => {
            setFade(false);
            setTimeout(() => {
                const randomIndex = Math.floor(
                    Math.random() * HEALING_MESSAGES.length
                );
                setMessage(HEALING_MESSAGES[randomIndex]);
                setFade(true);
            }, MESSAGE_FADE);
        }, MESSAGE_INTERVAL);

        return () => clearInterval(interval);
    }, [MESSAGE_FADE, MESSAGE_INTERVAL]);

    return (
        <div className="flex justify-center pb-5">
            <div
                className={`transition-all duration-500 ${
                    fade ? 'opacity-100 scale-105' : 'opacity-0 scale-95'
                } text-white text-lg text-center 
                bg-gradient-to-r from-white/30 to-blue-500 
                px-4 py-2 rounded-full 
                shadow-[0_0_15px_rgba(255,255,255,0.7)] 
                clip-path-polygon`}>
                {message}
            </div>
        </div>
    );
}
