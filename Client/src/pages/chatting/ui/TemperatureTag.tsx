function TemperatureTag({ energy }: { energy: number | undefined }) {
    if (!energy) return;

    const getBgColor = (temp: number) => {
        if (temp <= 0) return 'bg-blue-300';
        if (temp <= 10) return 'bg-blue-200';
        if (temp <= 20) return 'bg-green-200';
        if (temp <= 25) return 'bg-yellow-200';
        if (temp <= 30) return 'bg-orange-300';
        return 'bg-red-400';
    };

    const getTextColor = (temp: number) => {
        if (temp <= 0) return 'text-blue-900';
        if (temp <= 10) return 'text-blue-800';
        if (temp <= 20) return 'text-green-800';
        if (temp <= 25) return 'text-yellow-800';
        if (temp <= 30) return 'text-orange-800';
        return 'text-red-900';
    };

    return (
        <span
            className={`
                inline-block px-4 py-1 rounded-full font-bold 
                ${getBgColor(energy)} 
                ${getTextColor(energy)}
                transition-colors duration-200
            `}>
            {energy}°C
        </span>
    );
}

export default TemperatureTag;
