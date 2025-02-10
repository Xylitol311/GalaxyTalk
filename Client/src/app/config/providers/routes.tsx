import {
    createBrowserRouter,
    createRoutesFromElements,
    Route,
} from 'react-router';
import Home from '@/pages/home';
import MatchingRoom from '@/pages/match';
import { PATH } from '../constants';

const ROUTE_PATH = PATH.ROUTE;

export const router = createBrowserRouter(
    createRoutesFromElements(
        <>
            <Route path={ROUTE_PATH.HOME} element={<Home />} />
            <Route path={ROUTE_PATH.MATCH} element={<MatchingRoom />} />
        </>
    )
);
