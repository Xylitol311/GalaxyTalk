import { setupWorker } from 'msw/browser';
import { chatHandlers } from './chatHandler';
import { feedbackHandler } from './feedbackHandler';
import { letterHandler } from './letterHandler';
import { matchHandlers } from './matchHandler';
import { userHandlers } from './userHandlers';

export const worker = setupWorker(
    ...userHandlers,
    ...matchHandlers,
    ...chatHandlers,
    ...feedbackHandler,
    ...letterHandler
);
