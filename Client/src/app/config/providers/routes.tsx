import {
    createBrowserRouter,
    createRoutesFromElements,
    Route,
} from 'react-router';
import Home from '@/pages/home';
import MatchingRoom from '@/pages/match';
import WarpPage from '@/pages/warp';
import { PATH } from '../constants';

const ROUTE_PATH = PATH.ROUTE;

export const router = createBrowserRouter(
    createRoutesFromElements(
        <>
            <Route path={ROUTE_PATH.HOME} element={<Home />} />
            <Route path={ROUTE_PATH.MATCH} element={<MatchingRoom />} />
            <Route path={ROUTE_PATH.WARP} element={<WarpPage />} />
        </>
    )
);
