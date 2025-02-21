import { motion } from 'framer-motion';

export default function Introduce() {
    const container = {
        hidden: { opacity: 1 },
        visible: {
            opacity: 1,
            transition: {
                staggerChildren: 0.1,
            },
        },
    };

    const item = {
        hidden: { y: 20, opacity: 0 },
        visible: {
            y: 0,
            opacity: 1,
            transition: {
                duration: 0.5,
                ease: 'easeOut',
            },
        },
    };

    return (
        <div className="space-y-8 mb-8 text-center text-white">
            <h1 className="text-6xl font-semibold font-grandiflora">
                은하수다
            </h1>
            <motion.div
                variants={container}
                initial="hidden"
                animate="visible"
                className="space-y-1">
                <motion.p variants={item} className="text-lg">
                    외로운 별들을 위한 빛나는 만남
                </motion.p>
                <motion.p variants={item} className="text-lg">
                    익명의 마음들이 전하는 따스한 위로
                </motion.p>
                <motion.p variants={item} className="text-lg">
                    고민 속에서 서로를 발견하며 함께 힐링해요
                </motion.p>
            </motion.div>
        </div>
    );
}
