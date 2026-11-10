import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import { isConnected, onConnected, onConnectionChange, subscribe } from './liveSocket.js'

/** Without a WebSocket connection, screens ask the REST API this often instead. */
const POLL_MS = 2000

/**
 * The live game as it changes. Loads it once over REST (which includes my own part, `me`),
 * then follows the pushed updates:
 * - /topic/live/{gameId}: the game, the same for everyone (without `me`)
 * - /user/queue/live:     my score and how my last answer went
 * After a reconnect it loads the game again, in case updates were missed. While there is no
 * connection, it loads the game every 2 seconds, so the quiz still works (a bit slower).
 */
export function useLiveGame(gameId) {
    const [game, setGame] = useState(null)
    const [error, setError] = useState('')

    const load = useCallback(() => {
        api.liveGame(gameId).then(fresh => setGame(stamp(fresh))).catch(err => setError(err.message))
    }, [gameId])

    useEffect(() => {
        load()
        const stopGame = subscribe(`/topic/live/${gameId}`, update => setGame(prev => merge(prev, update)))
        const stopMine = subscribe('/user/queue/live', personal => {
            if (personal.gameId === gameId) setGame(prev => (prev ? { ...prev, me: personal.me } : prev))
        })
        const stopReconnect = onConnected(load)
        const poll = setInterval(() => { if (!isConnected()) load() }, POLL_MS)
        return () => { stopGame(); stopMine(); stopReconnect(); clearInterval(poll) }
    }, [gameId, load])

    /** Use a REST answer (it includes `me`) right away, without waiting for the push. */
    const apply = useCallback(view => setGame(prev => ({ ...stamp(view), me: view.me ?? prev?.me })), [])

    return { game, error, apply }
}

/** Turns "milliseconds left" into a local deadline, so countdowns don't depend on the server's clock. */
function stamp(view) {
    return { ...view, deadline: view.phase === 'QUESTION' ? Date.now() + view.remainingMillis : null }
}

/** Whether live updates arrive over the WebSocket (false: the screen is refreshing by itself). */
export function useLiveConnection() {
    const [connected, setConnected] = useState(isConnected)
    useEffect(() => onConnectionChange(setConnected), [])
    return connected
}

function merge(prev, update) {
    let me = prev?.me
    // A new question: I haven't answered it yet
    if (me && update.phase === 'QUESTION' && update.questionNumber !== prev.questionNumber) {
        me = { ...me, answeredCurrent: false }
    }
    // The same question again ("3 answered"): keep the countdown we already have
    const stamped = stamp(update)
    if (prev?.phase === 'QUESTION' && update.phase === 'QUESTION' && update.questionNumber === prev.questionNumber) {
        stamped.deadline = prev.deadline
    }
    return { ...stamped, me }
}

/** Seconds left until the deadline, updated a few times per second. */
export function useCountdown(deadline) {
    const [now, setNow] = useState(() => Date.now())
    useEffect(() => {
        if (!deadline) return
        const timer = setInterval(() => setNow(Date.now()), 250)
        return () => clearInterval(timer)
    }, [deadline])
    return deadline ? Math.max(0, Math.ceil((deadline - now) / 1000)) : 0
}

/** Whether a live quiz is running in the course (for the Join banner). */
export function useCourseLive(courseId) {
    const [notice, setNotice] = useState(null)
    useEffect(() => {
        let stale = false
        const load = () => api.courseLive(courseId).then(n => { if (!stale) setNotice(n) }).catch(() => {})
        load()
        const stop = subscribe(`/topic/courses/${courseId}/live`, n => setNotice(n))
        const poll = setInterval(() => { if (!isConnected()) load() }, POLL_MS + 1000)
        return () => { stale = true; stop(); clearInterval(poll) }
    }, [courseId])
    return notice?.active ? notice : null
}
