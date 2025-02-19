export const PLANETS = [
    {
        id: 1,
        name: '솔라리스',
        description: '태양계의 중심에 위치한 별',
        imageUrl: '1.png',
    },
    {
        id: 2,
        name: '루나리아',
        description: '달의 중심에 위치한 별',
        imageUrl: '2.png',
    },
    {
        id: 3,
        name: '테라피스',
        description: '지구의 중심에 위치한 별',
        imageUrl: '3.png',
    },
    {
        id: 4,
        name: '판타지아',
        description: '행성의 중심에 위치한 별',
        imageUrl: '4.png',
    },
];

export const getPlanetNameById = (planetId: number | undefined) => {
    if (!planetId) return '';
    return PLANETS.find((planet) => planet.id === planetId)?.name || '';
};

export const getPlanetInfoById = (planetId: number) => {
    return PLANETS.find((planet) => planet.id === planetId);
};
