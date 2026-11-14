import { useCallback, useState } from 'react'
import { api } from '../api.js'
import { formatDateTime } from '../format.js'
import { useApiData } from '../useApiData.js'
import { useOpenLive } from './LiveContext.js'
import { useCourseLive } from './useLiveGame.js'
import '../pages/LiveGame.css'

/** Dashboard: join a live quiz with its PIN, like on kahoot.it. */
export function JoinLiveCard() {
    const openLive = useOpenLive()
    const [pin, setPin] = useState('')
    const [error, setError] = useState('')
    const [busy, setBusy] = useState(false)

    const join = async (e) => {
        e.preventDefault()
        setError('')
        setBusy(true)
        try {
            const game = await api.joinLive(pin)
            openLive(game.gameId)
        } catch (err) {
            setError(err.message)
            setBusy(false)
        }
    }

    return (
        <form className="live-join-card" onSubmit={join}>
            <strong>⚡ Live quiz</strong>
            <input className="auth-input" inputMode="numeric" placeholder="Game PIN" aria-label="Game PIN"
                   maxLength={7} value={pin} onChange={e => setPin(e.target.value)} />
            <button type="submit" className="btn-primary" disabled={busy || pin.replace(/\s/g, '').length !== 6}>
                Join
            </button>
            {error && <p className="form-error">{error}</p>}
        </form>
    )
}

/** Course page: shown while a live quiz runs in the course. */
export function LiveBanner({ course, user }) {
    const notice = useCourseLive(course.id)
    const openLive = useOpenLive()
    const [error, setError] = useState('')
    if (!notice) return null

    const isHost = notice.hostId === user.id
    const join = async () => {
        setError('')
        try {
            if (isHost) {
                openLive(notice.gameId)
            } else {
                const game = await api.joinLive(notice.pin)
                openLive(game.gameId)
            }
        } catch (err) {
            setError(err.message)
        }
    }

    return (
        <div className="live-banner" role="status">
            <span>⚡ Live quiz <strong>{notice.quizTitle}</strong> is running · PIN {notice.pin}</span>
            <button className="btn-primary" onClick={join}>{isHost ? 'Open host screen' : 'Join'}</button>
            {error && <p className="form-error">{error}</p>}
        </div>
    )
}

const TIME_CHOICES = [
    { value: '', label: 'By question type (20–45 s)' },
    { value: '10', label: '10 seconds' },
    { value: '20', label: '20 seconds' },
    { value: '30', label: '30 seconds' },
    { value: '60', label: '60 seconds' },
]

/** Quiz page, teachers: host the quiz live, and see earlier live games. */
export function LiveHostCard({ item, course }) {
    const openLive = useOpenLive()
    const [seconds, setSeconds] = useState('')
    const [error, setError] = useState('')
    const [busy, setBusy] = useState(false)
    // Reload the list when a game in the course ends (the running game's id changes)
    const running = useCourseLive(course.id)
    const runningGameId = running?.gameId
    const fetchSessions = useCallback(() => api.liveSessions(item.id), [item.id, runningGameId]) // eslint-disable-line react-hooks/exhaustive-deps
    const { data: sessions } = useApiData(fetchSessions)

    const host = async () => {
        setError('')
        setBusy(true)
        try {
            const game = await api.hostLive(item.id, seconds ? Number(seconds) : null)
            openLive(game.gameId)
        } catch (err) {
            setError(err.message)
            setBusy(false)
        }
    }

    return (
        <section className="card live-host-card">
            <h3>⚡ Live quiz</h3>
            <p className="hint">
                Play this quiz together in class, Kahoot style: students join with a PIN and answer against the clock.
                Just for fun: results don't count toward grades. Students see the questions and answers during the
                game, so use a separate quiz if this one is graded.
            </p>
            <label>
                Time per question
                <select className="small-select" value={seconds} onChange={e => setSeconds(e.target.value)}>
                    {TIME_CHOICES.map(c => <option key={c.value} value={c.value}>{c.label}</option>)}
                </select>
            </label>
            <button className="btn-primary" disabled={busy} onClick={host}>Host live</button>
            {error && <p className="form-error">{error}</p>}
            {sessions?.length > 0 && (
                <>
                    <h4>Earlier live games</h4>
                    <ul className="live-sessions">
                        {sessions.map(s => (
                            <li key={s.id}>
                                {formatDateTime(s.endedAt)}: {s.players.length} players, {s.questionsPlayed} questions
                                · winner <strong>{s.players[0]?.name}</strong> ({s.players[0]?.score} points)
                            </li>
                        ))}
                    </ul>
                </>
            )}
        </section>
    )
}
