import { setupWorker } from 'msw/browser';
import { matchHandlers } from './matchHandler';
import { userHandlers } from './userHandlers';

export const worker = setupWorker(...userHandlers, ...matchHandlers);
