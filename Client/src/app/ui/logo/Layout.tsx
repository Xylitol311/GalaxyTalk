import { AnimatePresence, motion } from 'framer-motion';
import { Outlet, useLocation } from 'react-router';

const pageVariants = {
    initial: { opacity: 0 },
    animate: {
        opacity: 1,
        transition: { duration: 3 },
    },
};

export default function Layout() {
    const location = useLocation();

    return (
        <div className="relative w-full h-100dvh">
            <AnimatePresence mode="wait">
                <motion.div
                    key={location.pathname}
                    variants={pageVariants}
                    initial="initial"
                    animate="animate"
                    className="absolute inset-0 flex justify-center items-center">
                    <Outlet />
                </motion.div>
            </AnimatePresence>
        </div>
    );
}
