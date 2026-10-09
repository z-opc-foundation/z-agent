import {Navigate, Route, Routes} from 'react-router-dom'
import {AppLayout} from '@yuku123/z-frontend-common'
import {menuItems, routeTable} from '@yuku123/z-agent-component/pages'
import '@yuku123/z-agent-component/style.css'

export default function App() {
    return (
        <Routes>
            <Route path="*" element={<Navigate to="/" replace/>}/>
            <Route element={
                <AppLayout menuItems={menuItems} appTitle="z-agent 智能体工作台" appShort="AGT"/>
            }>
                {routeTable.map((r) => (
                    <Route key={r.path} path={r.path} element={<r.Component/>}/>
                ))}
            </Route>
        </Routes>
    )
}
