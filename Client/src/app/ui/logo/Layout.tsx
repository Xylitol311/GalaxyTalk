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
        <div className="relative w-100 h-screen">
            <AnimatePresence mode="wait">
                <motion.div
                    key={location.pathname}
                    variants={pageVariants}
                    initial="initial"
                    animate="animate"
                    className="absolute inset-0">
                    <Outlet />
                </motion.div>
            </AnimatePresence>
        </div>
    );
}
