import { useEffect, useState } from 'react';
import { BREAKPOINT, TIME } from '@/app/config/constants';
import { debounce } from '@/shared/lib/utils';

export default function useIsMobile(
    breakpoint: number = BREAKPOINT.MOBILE,
    debounceDelay: number = TIME.DELAY.DEBOUNCE
): boolean {
    const [isMobile, setIsMobile] = useState<boolean>(
        window.innerWidth < breakpoint
    );

    const handleResize = debounce(() => {
        setIsMobile(window.innerWidth < breakpoint);
    }, debounceDelay);

    useEffect(() => {
        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
        };
    }, [handleResize]);

    return isMobile;
}
