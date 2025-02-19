import {
    createBrowserRouter,
    createRoutesFromElements,
    Route,
} from 'react-router';
import Layout from '@/app/ui/logo/Layout';
import ChattingRoom from '@/pages/chatting';
import Home from '@/pages/home';
import MatchingRoom from '@/pages/match';
import MyPage from '@/pages/mypage';
import Signup from '@/pages/signup';
import { PATH } from '../constants';

const ROUTE_PATH = PATH.ROUTE;

export const router = createBrowserRouter(
    createRoutesFromElements(
        <Route element={<Layout />}>
            <Route path={ROUTE_PATH.HOME} element={<Home />} />
            <Route path={ROUTE_PATH.SIGN_UP} element={<Signup />} />
            <Route path={ROUTE_PATH.MATCH} element={<MatchingRoom />} />
            <Route path={ROUTE_PATH.CHAT} element={<ChattingRoom />} />
            <Route path={ROUTE_PATH.MY_PAGE} element={<MyPage />} />
        </Route>
    )
);
