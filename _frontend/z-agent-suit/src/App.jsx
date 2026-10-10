import {Navigate, Route, Routes} from 'react-router-dom'
import {AppLayout} from '../../../../_shared/z-frontend-common-local/dist/z-frontend-common.es.js'
import {menuItems, routeTable} from '@yuku123/z-agent-component/pages'
import '@yuku123/z-agent-component/style.css'

export default function App() {
    return (
        <Routes>
            <Route path="*" element={<Navigate to="/" replace/>}/>
            <Route element={
                <AppLayout menuItems={menuItems} appTitle="z-agent 智能体工作台" appShort="AGT" appIcon={{icon: <img src="/icon.png" alt="AGT" style={{width: "100%", height: "100%", objectFit: "cover", borderRadius: 8}}/>, color: '#06b6d4', label: 'AGT'}}/>
            }>
                {routeTable.map((r) => (
                    <Route key={r.path} path={r.path} element={<r.Component/>}/>
                ))}
            </Route>
        </Routes>
    )
}
