import { useCallback, useState } from 'react'
import { api } from '../../api.js'
import { formatDateTime } from '../../format.js'
import { useApiData } from '../../useApiData.js'
import AnswerView from './AnswerView.jsx'
import QuestionInput from './QuestionInput.jsx'
import { emptyAnswer } from './questionTypes.js'

/**
 * The student's side of a quiz: an overview of earlier attempts (the best one counts),
 * answering the questions, and the graded result right after handing in.
 */
export default function QuizStudent({ item }) {
    const fetchAttempts = useCallback(() => api.myQuizAttempts(item.id), [item.id])
    const { data: attempts, error: loadError, reload } = useApiData(fetchAttempts)
    const [questions, setQuestions] = useState(null)   // set while answering
    const [answers, setAnswers] = useState({})
    const [shownResult, setShownResult] = useState(null)
    const [error, setError] = useState('')
    const [sending, setSending] = useState(false)

    if (loadError) return <p className="form-error">{loadError}</p>
    if (!attempts) return <p className="loading-text">Loading…</p>

    const used = attempts.length
    const max = item.maxAttempts
    const attemptsLeft = max == null ? Infinity : max - used
    const best = attempts.reduce((b, a) => (b == null || a.points > b.points ? a : b), null)

    const start = async () => {
        setError('')
        setShownResult(null)
        try {
            const qs = await api.quizForStudent(item.id)
            setQuestions(qs)
            setAnswers(Object.fromEntries(qs.map(q => [q.id, emptyAnswer(q)])))
        } catch (err) {
            setError(err.message)
        }
    }

    const handIn = async (e) => {
        e.preventDefault()
        setSending(true)
        setError('')
        try {
            const result = await api.attemptQuiz(item.id, answers)
            setQuestions(null)
            setShownResult(result)
            reload()
        } catch (err) {
            setError(err.message)
        } finally {
            setSending(false)
        }
    }

    // Answering
    if (questions) {
        return (
            <form className="card quiz-form" onSubmit={handIn}>
                <h3>Attempt {used + 1}{max != null && ` of ${max}`}</h3>
                {questions.map((q, i) => (
                    <fieldset key={q.id} className="quiz-question">
                        <legend>
                            {i + 1}. {q.prompt} <span className="hint">({q.points} {q.points === 1 ? 'point' : 'points'})</span>
                        </legend>
                        <QuestionInput question={q} answer={answers[q.id]}
                                       onChange={value => setAnswers(prev => ({ ...prev, [q.id]: value }))} />
                    </fieldset>
                ))}
                {error && <p className="form-error">{error}</p>}
                <div className="tool-row">
                    <button type="submit" className="btn-primary" disabled={sending}>
                        {sending ? 'Grading…' : 'Hand in answers'}
                    </button>
                    <button type="button" className="btn-text" onClick={() => setQuestions(null)}>Cancel</button>
                </div>
            </form>
        )
    }

    return (
        <section className="card">
            <h3>Quiz</h3>
            <p>
                Attempts used: <strong>{used}{max != null && ` of ${max}`}</strong>
                {best && <> · Best result: <strong>{best.points}{best.maxPoints != null && ` / ${best.maxPoints}`}</strong> (this one counts)</>}
            </p>
            {error && <p className="form-error">{error}</p>}
            {attemptsLeft > 0 ? (
                <button className="btn-primary" onClick={start}>
                    {used === 0 ? 'Start quiz' : 'Start new attempt'}
                </button>
            ) : (
                <p className="hint">You have used all your attempts.</p>
            )}

            {shownResult && <QuizResult result={shownResult} />}

            {attempts.length > 0 && (
                <>
                    <h4 className="subsection-title">Your attempts</h4>
                    <ul className="attempt-list">
                        {attempts.map(a => (
                            <li key={a.submissionId}>
                                <button type="button" className="link-button" onClick={() => setShownResult(a)}>
                                    Attempt {a.attemptNumber}
                                </button>
                                {' '}· {a.points}{a.maxPoints != null && ` / ${a.maxPoints}`} points
                                {' '}· {formatDateTime(a.submittedAt)}
                                {a.late && <span className="late-flag">late</span>}
                            </li>
                        ))}
                    </ul>
                </>
            )}
        </section>
    )
}

/** The graded result of one attempt, question by question. */
export function QuizResult({ result }) {
    return (
        <div className="quiz-result">
            <h4>
                Attempt {result.attemptNumber}: {result.points}{result.maxPoints != null && ` / ${result.maxPoints}`} points
            </h4>
            {!result.correctAnswersShown && (
                <p className="hint">The correct answers are shown once you have used all your attempts.</p>
            )}
            {result.questions.map((r, i) => (
                <div key={r.question.id} className={`result-question ${r.correct ? 'right' : 'wrong'}`}>
                    <div className="result-head">
                        <span>{r.correct ? '✓' : '✗'} {i + 1}. {r.question.prompt}</span>
                        <span className="hint">{r.earnedPoints} / {r.question.points}</span>
                    </div>
                    <div>Your answer: <AnswerView question={r.question} answer={r.yourAnswer} /></div>
                    {r.hint && <div className="hint-box">💡 {r.hint}</div>}
                    {r.correctAnswer && !r.correct && (
                        <div>Correct answer: <AnswerView question={r.question} answer={r.correctAnswer} /></div>
                    )}
                </div>
            ))}
        </div>
    )
}
