import { useState } from 'react'
import { api } from '../api.js'
import { useCountdown, useLiveConnection, useLiveGame } from '../live/useLiveGame.js'
import QuestionInput from '../components/quiz/QuestionInput.jsx'
import AnswerView from '../components/quiz/AnswerView.jsx'
import { emptyAnswer } from '../components/quiz/questionTypes.js'
import './LiveGame.css'

// Kahoot-style answer tiles: a color and a shape each, so they can be told apart without color too
const TILES = [
    { color: 'red', shape: '▲' },
    { color: 'blue', shape: '◆' },
    { color: 'yellow', shape: '●' },
    { color: 'green', shape: '■' },
]
const tile = (i) => TILES[i % TILES.length]
const MEDALS = ['🥇', '🥈', '🥉']

/**
 * The live quiz screen, full page. The host (the teacher) controls the game and shows it on the
 * projector; players answer on their own devices. Everything updates live over the WebSocket.
 */
export default function LiveGame({ gameId, onExit }) {
    const { game, error, apply } = useLiveGame(gameId)
    const connected = useLiveConnection()
    const [actionError, setActionError] = useState('')

    const run = async (action) => {
        setActionError('')
        try {
            apply(await action())
        } catch (err) {
            setActionError(err.message)
        }
    }

    if (error) {
        return (
            <div className="live-page">
                <p className="live-error">{error}</p>
                <button className="live-btn" onClick={onExit}>Back</button>
            </div>
        )
    }
    if (!game) return <div className="live-page"><p className="live-waiting">Connecting…</p></div>

    const isHost = game.me?.host
    const finished = game.phase === 'FINISHED'

    return (
        <div className={`live-page phase-${game.phase.toLowerCase()}`}>
            <header className="live-header">
                <span className="live-title">⚡ {game.quizTitle}</span>
                {game.phase !== 'LOBBY' && !finished && (
                    <span className="live-progress">Question {game.questionNumber} of {game.questionCount}</span>
                )}
                {!finished && <span className="live-pin-small">PIN <strong>{formatPin(game.pin)}</strong></span>}
                {isHost && !finished
                    ? <button className="live-btn subtle" onClick={() => {
                        if (window.confirm('End the live quiz for everyone?')) run(() => api.liveEnd(gameId))
                    }}>End quiz</button>
                    : <button className="live-btn subtle" onClick={onExit}>{finished ? 'Close' : 'Leave'}</button>}
            </header>

            {actionError && <p className="live-error">{actionError}</p>}
            {!connected && !finished && (
                <p className="live-offline" role="status">
                    No live connection right now: the screen refreshes itself every few seconds.
                </p>
            )}

            {isHost
                ? <HostView game={game} run={run} />
                : <PlayerView game={game} apply={apply} onError={setActionError} />}
        </div>
    )
}

// ---------- the host's screen (for the projector) ----------

function HostView({ game, run }) {
    const id = game.gameId
    switch (game.phase) {
        case 'LOBBY':
            return (
                <main className="live-main">
                    <div className="live-join-box">
                        <p>Join on the dashboard or the course page with the PIN</p>
                        <div className="live-pin">{formatPin(game.pin)}</div>
                    </div>
                    <p className="live-count">{game.playerCount} {game.playerCount === 1 ? 'player' : 'players'}</p>
                    <ul className="live-players">
                        {game.players?.map(name => <li key={name}>{name}</li>)}
                    </ul>
                    <button className="live-btn big" disabled={game.playerCount === 0}
                            onClick={() => run(() => api.liveNext(id))}>
                        Start
                    </button>
                </main>
            )
        case 'QUESTION':
            return (
                <main className="live-main">
                    <QuestionHeader game={game} />
                    <QuestionDisplay question={game.question} />
                    <div className="live-host-actions">
                        <span className="live-count">{game.answeredCount} of {game.playerCount} answered</span>
                        <button className="live-btn" onClick={() => run(() => api.liveReveal(id))}>Show answer</button>
                    </div>
                </main>
            )
        case 'REVEAL':
            return (
                <main className="live-main">
                    <h2 className="live-prompt">{game.question.prompt}</h2>
                    <RevealDisplay game={game} />
                    <Leaderboard entries={game.leaderboard} />
                    <button className="live-btn big" onClick={() => run(() => api.liveNext(id))}>
                        {game.questionNumber < game.questionCount ? 'Next question' : 'Show results'}
                    </button>
                </main>
            )
        case 'FINISHED':
            return <main className="live-main"><Podium entries={game.leaderboard} /></main>
        default:
            return null
    }
}

// ---------- a player's screen (phone or laptop) ----------

function PlayerView({ game, apply, onError }) {
    const me = game.me
    switch (game.phase) {
        case 'LOBBY':
            return (
                <main className="live-main center">
                    <p className="live-big">You're in! 🎉</p>
                    <p className="live-waiting">Waiting for {game.hostName} to start…</p>
                    <p className="live-count">{game.playerCount} {game.playerCount === 1 ? 'player' : 'players'}</p>
                </main>
            )
        case 'QUESTION':
            return me?.answeredCurrent
                ? (
                    <main className="live-main center">
                        <QuestionHeader game={game} />
                        <p className="live-big">Answer sent ⏳</p>
                        <p className="live-waiting">Waiting for the others…</p>
                    </main>
                )
                // The key starts a fresh form for every question
                : <AnswerForm key={game.questionNumber} game={game} apply={apply} onError={onError} />
        case 'REVEAL':
            return (
                <main className="live-main center">
                    <Feedback feedback={me?.lastAnswer} />
                    <p className="live-score">{me?.score ?? 0} points · {ordinal(me?.rank)} place</p>
                    <RevealDisplay game={game} />
                    <p className="live-waiting">Next question coming up…</p>
                </main>
            )
        case 'FINISHED':
            return (
                <main className="live-main center">
                    {me?.rank && (
                        <p className="live-big">
                            {MEDALS[me.rank - 1] ?? '🎯'} You finished {ordinal(me.rank)} with {me.score} points
                        </p>
                    )}
                    <Podium entries={game.leaderboard} />
                </main>
            )
        default:
            return null
    }
}

function AnswerForm({ game, apply, onError }) {
    const question = game.question
    const [answer, setAnswer] = useState(() => emptyAnswer(question))
    const [sending, setSending] = useState(false)
    const secondsLeft = useCountdown(game.deadline)

    const send = async (value) => {
        setSending(true)
        onError('')
        try {
            apply(await api.answerLive(game.gameId, game.questionNumber, value))
        } catch (err) {
            onError(err.message)
            setSending(false)
        }
    }

    if (secondsLeft === 0) {
        return <main className="live-main center"><p className="live-big">Time's up! ⌛</p></main>
    }

    const singleChoice = question.type === 'MULTIPLE_CHOICE' && !question.multipleAnswers
    return (
        <main className="live-main">
            <QuestionHeader game={game} />
            {question.type === 'MULTIPLE_CHOICE' ? (
                <div className="live-tiles">
                    {question.options.map((option, i) => {
                        const chosen = answer.includes(String(i))
                        const toggle = () => {
                            if (singleChoice) return send([String(i)]) // one tap answers
                            setAnswer(chosen ? answer.filter(a => a !== String(i)) : [...answer, String(i)])
                        }
                        return (
                            <button key={i} type="button" disabled={sending} aria-pressed={chosen}
                                    className={`live-tile ${tile(i).color} ${chosen ? 'chosen' : ''}`}
                                    onClick={toggle}>
                                <span className="shape" aria-hidden="true">{tile(i).shape}</span> {option}
                            </button>
                        )
                    })}
                </div>
            ) : (
                <div className="live-input">
                    <QuestionInput question={question} answer={answer} onChange={setAnswer} />
                </div>
            )}
            {!singleChoice && (
                <button className="live-btn big" disabled={sending} onClick={() => send(answer)}>Submit answer</button>
            )}
            {question.multipleAnswers && <p className="live-waiting">Several answers can be correct.</p>}
        </main>
    )
}

// ---------- shared pieces ----------

function QuestionHeader({ game }) {
    const secondsLeft = useCountdown(game.deadline)
    const share = game.timeLimitSeconds ? secondsLeft / game.timeLimitSeconds : 0
    return (
        <div className="live-question-header">
            <h2 className="live-prompt">{game.question.prompt}</h2>
            <div className={`live-timer ${secondsLeft <= 5 ? 'urgent' : ''}`} role="timer"
                 aria-label={`${secondsLeft} seconds left`} style={{ '--share': share }}>
                {secondsLeft}
            </div>
        </div>
    )
}

/** The question on the host's screen, without anything to click. */
function QuestionDisplay({ question }) {
    switch (question.type) {
        case 'MULTIPLE_CHOICE':
            return (
                <div className="live-tiles">
                    {question.options.map((option, i) => (
                        <div key={i} className={`live-tile ${tile(i).color}`}>
                            <span className="shape" aria-hidden="true">{tile(i).shape}</span> {option}
                        </div>
                    ))}
                </div>
            )
        case 'ORDERING':
            return <p className="live-instruction">Put in the right order: {question.items.join(' · ')}</p>
        case 'MATCHING':
            return <p className="live-instruction">Match: {question.leftItems.join(' · ')} with {question.rightOptions.join(' · ')}</p>
        case 'FILL_IN_BLANKS':
            return (
                <p className="live-instruction">
                    {question.textParts.map((part, i) => (
                        <span key={i}>{part}{i < question.blankCount && <span className="live-blank">_____</span>}</span>
                    ))}
                </p>
            )
        default:
            return null
    }
}

/** The correct answer, and for multiple choice how many picked each option. */
function RevealDisplay({ game }) {
    const { question, reveal } = game
    if (!reveal) return null
    if (question.type === 'MULTIPLE_CHOICE') {
        const most = Math.max(1, ...reveal.optionCounts)
        return (
            <div className="live-tiles">
                {question.options.map((option, i) => {
                    const correct = reveal.correctAnswer.includes(String(i))
                    return (
                        <div key={i} className={`live-tile ${tile(i).color} ${correct ? 'correct' : 'wrong'}`}>
                            <span className="shape" aria-hidden="true">{tile(i).shape}</span> {option}
                            <span className="live-tile-count">
                                <span className="bar" style={{ width: `${(reveal.optionCounts[i] / most) * 100}%` }} />
                                {reveal.optionCounts[i]} {correct && '✓'}
                            </span>
                        </div>
                    )
                })}
            </div>
        )
    }
    return (
        <div className="live-correct">
            <p>Correct answer: <AnswerView question={question} answer={reveal.correctAnswer} /></p>
            <p className="live-waiting">{reveal.fullyCorrect} of {reveal.answered} got it completely right</p>
        </div>
    )
}

function Feedback({ feedback }) {
    if (!feedback || !feedback.answered) return <p className="live-big feedback none">No answer ⌛</p>
    if (feedback.correct) return <p className="live-big feedback correct">Correct! ✓ +{feedback.points}</p>
    if (feedback.points > 0) return <p className="live-big feedback partly">Partly right · +{feedback.points}</p>
    return <p className="live-big feedback wrong">Wrong ✗</p>
}

function Leaderboard({ entries }) {
    if (!entries?.length) return null
    return (
        <ol className="live-leaderboard">
            {entries.map(e => (
                <li key={e.userId}><span>{e.rank}. {e.name}</span><strong>{e.score}</strong></li>
            ))}
        </ol>
    )
}

function Podium({ entries }) {
    if (!entries?.length) return <p className="live-waiting">The quiz ended before anyone played.</p>
    const top = entries.slice(0, 3)
    // Second place on the left, the winner in the middle, third on the right
    const order = [top[1], top[0], top[2]].filter(Boolean)
    return (
        <div className="live-results">
            <h2>Results</h2>
            <div className="live-podium">
                {order.map(e => (
                    <div key={e.userId} className={`podium-step place-${e.rank}`}>
                        <span className="podium-name">{MEDALS[e.rank - 1] ?? ''} {e.name}</span>
                        <span className="podium-score">{e.score}</span>
                    </div>
                ))}
            </div>
            {entries.length > 3 && <Leaderboard entries={entries.slice(3)} />}
        </div>
    )
}

function formatPin(pin) {
    return pin ? `${pin.slice(0, 3)} ${pin.slice(3)}` : ''
}

function ordinal(n) {
    if (!n) return '–'
    const suffix = n % 100 >= 11 && n % 100 <= 13 ? 'th' : ['th', 'st', 'nd', 'rd'][n % 10] ?? 'th'
    return `${n}${suffix}`
}
