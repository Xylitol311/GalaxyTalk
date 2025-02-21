function MbtiTag({ mbti }: { mbti: string | undefined }) {
    if (!mbti) return null;

    const getDetailedStyle = (type: string) => {
        const styles: { [key: string]: string } = {
            INTJ: 'bg-purple-200 text-purple-800',
            INTP: 'bg-indigo-200 text-indigo-800',
            ENTJ: 'bg-blue-200 text-blue-800',
            ENTP: 'bg-cyan-200 text-cyan-800',

            INFJ: 'bg-teal-200 text-teal-800',
            INFP: 'bg-emerald-200 text-emerald-800',
            ENFJ: 'bg-green-200 text-green-800',
            ENFP: 'bg-lime-200 text-lime-800',

            ISTJ: 'bg-slate-200 text-slate-800',
            ISFJ: 'bg-zinc-200 text-zinc-800',
            ESTJ: 'bg-gray-200 text-gray-800',
            ESFJ: 'bg-stone-200 text-stone-800',

            ISTP: 'bg-amber-200 text-amber-800',
            ISFP: 'bg-yellow-200 text-yellow-800',
            ESTP: 'bg-orange-200 text-orange-800',
            ESFP: 'bg-red-200 text-red-800',
        };

        return styles[type] || 'bg-gray-200 text-gray-800';
    };

    return (
        <span
            className={`
                inline-block px-4 py-1 mr-2 rounded-full font-bold
                ${getDetailedStyle(mbti.toUpperCase())}
                transition-colors duration-200
            `}>
            {mbti.toUpperCase()}
        </span>
    );
}

export default MbtiTag;
