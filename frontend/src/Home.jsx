import { useCallback, useEffect, useMemo, useState } from 'react'
import './Home.css'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Course from './pages/Course.jsx'
import LiveGame from './pages/LiveGame.jsx'
import { LiveContext } from './live/LiveContext.js'
import { api, hasToken, setToken, setUnauthorizedHandler } from './api.js'

/**
 * The app's top level. Navigation is kept in state:
 * no user -> Login; a live quiz -> the live quiz screen; a selected course -> Course page;
 * otherwise -> Dashboard.
 */
const LIVE_GAME_KEY = 'lms_live_game'

function readLiveGame() {
    try {
        return sessionStorage.getItem(LIVE_GAME_KEY)
    } catch {
        return null
    }
}

function Home() {
    const [currentUser, setCurrentUser] = useState(null)
    // The enrollment that was opened: { course, role } — the role decides what the page shows
    const [activeEnrollment, setActiveEnrollment] = useState(null)
    // While we check a token saved from an earlier visit
    const [checkingSession, setCheckingSession] = useState(hasToken())
    // The live quiz being hosted or played, shown full page over everything else.
    // Remembered for this tab, so reloading the page goes straight back into the game.
    const [liveGameId, setLiveGameIdState] = useState(readLiveGame)
    const setLiveGameId = useCallback((gameId) => {
        setLiveGameIdState(gameId)
        try {
            if (gameId) sessionStorage.setItem(LIVE_GAME_KEY, gameId)
            else sessionStorage.removeItem(LIVE_GAME_KEY)
        } catch {
            // storage unavailable: only reloading loses the game
        }
    }, [])
    const live = useMemo(() => ({ openLive: setLiveGameId }), [setLiveGameId])

    const logout = () => {
        setToken(null)
        setCurrentUser(null)
        setActiveEnrollment(null)
        setLiveGameId(null)
    }

    useEffect(() => {
        // If the token expires, the API layer calls this and we go back to the login page
        setUnauthorizedHandler(() => {
            setCurrentUser(null)
            setActiveEnrollment(null)
            setLiveGameId(null)
        })
        if (!hasToken()) return
        api.me()
            .then(setCurrentUser)
            .catch(() => setToken(null))
            .finally(() => setCheckingSession(false))
    }, [setLiveGameId])

    if (checkingSession) {
        return <p className="loading-text">Loading…</p>
    }
    if (!currentUser) {
        return <Login onLoginSuccess={setCurrentUser} />
    }
    return (
        <LiveContext.Provider value={live}>
            {/* The live quiz covers the page; the page stays underneath (hidden), so closing the
                quiz brings you back exactly where you were */}
            {liveGameId && <LiveGame gameId={liveGameId} onExit={() => setLiveGameId(null)} />}
            <div hidden={!!liveGameId}>
            {activeEnrollment ? (
                <Course
                    enrollment={activeEnrollment}
                    user={currentUser}
                    onBackToDashboard={() => setActiveEnrollment(null)}
                />
            ) : (
                <Dashboard
                    user={currentUser}
                    onLogout={logout}
                    onSelectCourse={setActiveEnrollment}
                    onUserChanged={setCurrentUser}
                />
            )}
            </div>
        </LiveContext.Provider>
    )
}

export default Home
