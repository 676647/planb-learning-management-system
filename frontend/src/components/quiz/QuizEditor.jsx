import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api.js'
import { useApiData } from '../../useApiData.js'
import { QUESTION_TYPES } from './questionTypes.js'

/**
 * The teacher's side of a quiz: the questions with their solutions, and a form to add more.
 * Once a student has answered, the backend refuses changes (and says so).
 */
export default function QuizEditor({ item, onTotalChange }) {
    const fetchQuestions = useCallback(() => api.quizQuestions(item.id), [item.id])
    const { data: questions, error: loadError, reload } = useApiData(fetchQuestions)
    const [adding, setAdding] = useState(false)
    const [error, setError] = useState('')
    const total = questions ? questions.reduce((sum, q) => sum + q.points, 0) : null

    useEffect(() => {
        if (total != null) onTotalChange?.(total)
    }, [total, onTotalChange])

    const remove = async (q) => {
        if (!window.confirm(`Delete question "${q.prompt}"?`)) return
        setError('')
        try {
            await api.deleteQuestion(q.id)
            reload()
        } catch (err) {
            setError(err.message)
        }
    }

    if (loadError) return <p className="form-error">{loadError}</p>
    if (!questions) return <p className="loading-text">Loading questions…</p>


    return (
        <section className="card">
            <div className="section-header">
                <h3>Questions ({questions.length}) · {total} points</h3>
                {!adding && <button className="btn-small" onClick={() => setAdding(true)}>+ Add question</button>}
            </div>
            {error && <p className="form-error">{error}</p>}
            {questions.length === 0 && !adding && (
                <p className="no-items-text">No questions yet. Students can start the quiz once it has questions.</p>
            )}
            <ol className="question-list">
                {questions.map(q => (
                    <li key={q.id} className="question-item">
                        <div className="result-head">
                            <span><strong>{q.prompt}</strong> <span className="hint">
                                {QUESTION_TYPES[q.type].label} · {q.points} {q.points === 1 ? 'point' : 'points'}</span></span>
                            <button className="btn-icon" title="Delete question" onClick={() => remove(q)}>🗑</button>
                        </div>
                        <Solution question={q} />
                        {q.hint && <div className="hint">💡 Hint: {q.hint}</div>}
                    </li>
                ))}
            </ol>
            {adding && (
                <AddQuestionForm quizId={item.id} onCancel={() => setAdding(false)}
                                 onAdded={() => { setAdding(false); reload() }} />
            )}
        </section>
    )
}

/** The solution of a question, as the teacher defined it. */
function Solution({ question: q }) {
    switch (q.type) {
        case 'MULTIPLE_CHOICE':
            return (
                <ul className="solution-list">
                    {q.options.map((o, i) => <li key={i}>{q.correctOptions.includes(i) ? '✓' : '○'} {o}</li>)}
                </ul>
            )
        case 'ORDERING':
            return <div className="solution">{q.items.join(' → ')}</div>
        case 'MATCHING':
            return <div className="solution">{q.pairs.map(p => `${p.left} → ${p.right}`).join(' · ')}</div>
        case 'FILL_IN_BLANKS':
            return <div className="solution"><code>{q.text}</code></div>
        default:
            return null
    }
}

const lines = (text) => text.split('\n').map(l => l.trim()).filter(Boolean)

function AddQuestionForm({ quizId, onAdded, onCancel }) {
    const [type, setType] = useState('MULTIPLE_CHOICE')
    const [prompt, setPrompt] = useState('')
    const [points, setPoints] = useState('1')
    const [hint, setHint] = useState('')
    const [options, setOptions] = useState([{ text: '', correct: true }, { text: '', correct: false }])
    const [itemsText, setItemsText] = useState('')
    const [pairs, setPairs] = useState([{ left: '', right: '' }, { left: '', right: '' }])
    const [blankText, setBlankText] = useState('')
    const [error, setError] = useState('')

    const build = () => {
        const common = { prompt, points: Number(points), hint }
        switch (type) {
            case 'MULTIPLE_CHOICE': {
                const filled = options.filter(o => o.text.trim())
                return {
                    ...common,
                    options: filled.map(o => o.text.trim()),
                    correctOptions: filled.map((o, i) => (o.correct ? i : null)).filter(i => i !== null),
                }
            }
            case 'ORDERING':
                return { ...common, items: lines(itemsText) }
            case 'MATCHING':
                return { ...common, pairs: pairs.filter(p => p.left.trim() || p.right.trim()) }
            default:
                return { ...common, text: blankText }
        }
    }

    const submit = async (e) => {
        e.preventDefault()
        setError('')
        try {
            await api.addQuestion(quizId, QUESTION_TYPES[type].kind, build())
            onAdded()
        } catch (err) {
            setError(err.message)
        }
    }

    return (
        <form className="inline-form card" onSubmit={submit}>
            <div className="type-picker">
                {Object.entries(QUESTION_TYPES).map(([key, t]) => (
                    <button type="button" key={key} className={`type-option ${type === key ? 'active' : ''}`}
                            onClick={() => setType(key)}>{t.label}</button>
                ))}
            </div>
            <input className="auth-input" placeholder="Question" value={prompt}
                   onChange={e => setPrompt(e.target.value)} required />

            {type === 'MULTIPLE_CHOICE' && (
                <div className="option-editor">
                    <p className="hint">Tick the correct option(s). With more than one, students may tick several.</p>
                    {options.map((o, i) => (
                        <div key={i} className="option-row">
                            <input type="checkbox" checked={o.correct} aria-label={`Option ${i + 1} is correct`}
                                   onChange={e => setOptions(options.map((x, j) => (j === i ? { ...x, correct: e.target.checked } : x)))} />
                            <input className="auth-input" placeholder={`Option ${i + 1}`} value={o.text}
                                   onChange={e => setOptions(options.map((x, j) => (j === i ? { ...x, text: e.target.value } : x)))} />
                            {options.length > 2 && (
                                <button type="button" className="btn-icon" title="Remove option"
                                        onClick={() => setOptions(options.filter((_, j) => j !== i))}>✕</button>
                            )}
                        </div>
                    ))}
                    <button type="button" className="btn-text" onClick={() => setOptions([...options, { text: '', correct: false }])}>
                        + Add option
                    </button>
                </div>
            )}

            {type === 'ORDERING' && (
                <textarea className="auth-input" rows={5} value={itemsText} onChange={e => setItemsText(e.target.value)}
                          placeholder={'One item per line, in the CORRECT order.\nStudents will see them shuffled.'} />
            )}

            {type === 'MATCHING' && (
                <div className="option-editor">
                    <p className="hint">The correct pairs. Students will see the right-hand side shuffled.</p>
                    {pairs.map((p, i) => (
                        <div key={i} className="option-row">
                            <input className="auth-input" placeholder="Left" value={p.left}
                                   onChange={e => setPairs(pairs.map((x, j) => (j === i ? { ...x, left: e.target.value } : x)))} />
                            <span className="matching-arrow">→</span>
                            <input className="auth-input" placeholder="Right" value={p.right}
                                   onChange={e => setPairs(pairs.map((x, j) => (j === i ? { ...x, right: e.target.value } : x)))} />
                            {pairs.length > 2 && (
                                <button type="button" className="btn-icon" title="Remove pair"
                                        onClick={() => setPairs(pairs.filter((_, j) => j !== i))}>✕</button>
                            )}
                        </div>
                    ))}
                    <button type="button" className="btn-text" onClick={() => setPairs([...pairs, { left: '', right: '' }])}>
                        + Add pair
                    </button>
                </div>
            )}

            {type === 'FILL_IN_BLANKS' && (
                <>
                    <textarea className="auth-input" rows={4} value={blankText} onChange={e => setBlankText(e.target.value)}
                              placeholder="Spring Boot listens on port [[8080]] and runs on [[Java|the JVM]]." />
                    <p className="hint">
                        Put each answer in double square brackets. Separate accepted alternatives with |.
                        Upper/lower case doesn't matter.
                    </p>
                </>
            )}

            <div className="form-grid">
                <label>Points
                    <input type="number" min="0" className="auth-input" value={points} onChange={e => setPoints(e.target.value)} required />
                </label>
                <label>Hint for wrong answers (optional)
                    <input className="auth-input" value={hint} onChange={e => setHint(e.target.value)} />
                </label>
            </div>
            {error && <p className="form-error">{error}</p>}
            <div className="tool-row">
                <button type="submit" className="btn-small">Add question</button>
                <button type="button" className="btn-text" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    )
}
