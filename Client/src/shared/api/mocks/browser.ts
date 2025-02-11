import { setupWorker } from 'msw/browser';
import { chatHandlers } from './chatHandler';
import { matchHandlers } from './matchHandler';
import { userHandlers } from './userHandlers';

export const worker = setupWorker(
    ...userHandlers,
    ...matchHandlers,
    ...chatHandlers
);
