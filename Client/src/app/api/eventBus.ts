import mitt from 'mitt';

type Events = {
    apiError: { status: number; config: any; error: any };
};

export const eventBus = mitt<Events>();
