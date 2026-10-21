import { useEffect, useState } from 'react'
import './Home.css'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Course from './pages/Course.jsx'
import { api, hasToken, setToken, setUnauthorizedHandler } from './api.js'

/**
 * The app's top level. Navigation is kept in state:
 * no user -> Login; a selected course -> Course page; otherwise -> Dashboard.
 */
function Home() {
    const [currentUser, setCurrentUser] = useState(null)
    // The enrollment that was opened: { course, role } — the role decides what the page shows
    const [activeEnrollment, setActiveEnrollment] = useState(null)
    // While we check a token saved from an earlier visit
    const [checkingSession, setCheckingSession] = useState(hasToken())

    const logout = () => {
        setToken(null)
        setCurrentUser(null)
        setActiveEnrollment(null)
    }

    useEffect(() => {
        // If the token expires, the API layer calls this and we go back to the login page
        setUnauthorizedHandler(() => {
            setCurrentUser(null)
            setActiveEnrollment(null)
        })
        if (!hasToken()) return
        api.me()
            .then(setCurrentUser)
            .catch(() => setToken(null))
            .finally(() => setCheckingSession(false))
    }, [])

    if (checkingSession) {
        return <p className="loading-text">Loading…</p>
    }
    if (!currentUser) {
        return <Login onLoginSuccess={setCurrentUser} />
    }
    if (activeEnrollment) {
        return (
            <Course
                enrollment={activeEnrollment}
                user={currentUser}
                onBackToDashboard={() => setActiveEnrollment(null)}
            />
        )
    }
    return (
        <Dashboard
            user={currentUser}
            onLogout={logout}
            onSelectCourse={setActiveEnrollment}
        />
    )
}

export default Home
