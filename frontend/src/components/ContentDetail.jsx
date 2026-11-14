import { useCallback, useState } from 'react'
import { api } from '../api.js'
import { useApiData } from '../useApiData.js'
import QuizEditor from './quiz/QuizEditor.jsx'
import QuizStudent from './quiz/QuizStudent.jsx'
import { LiveHostCard } from '../live/LiveEntry.jsx'
import { ALLOWED_FILE_TYPES, CONTENT_TYPES, STATUS_LABELS, formatDateTime, formatSize, isGraded, isLink } from '../format.js'

/**
 * One content item. Pages show their text. For quizzes, exercises and assignments:
 * students see their status and can hand in; teachers see all submissions and grade them.
 */
export default function ContentDetail({ item, course, isTeacher, onBack }) {
    const type = CONTENT_TYPES[item.type]
    // A quiz's max points is the sum of its questions; the editor reports it when questions change
    const [quizPoints, setQuizPoints] = useState(null)
    const maxPoints = quizPoints ?? item.maxPoints

    return (
        <div className="content-detail">
            <button className="btn-text back-link" onClick={onBack}>← Back to modules</button>
            <h2>{type?.icon} {item.title}</h2>
            <p className="item-meta">{type?.label}</p>

            <dl className="facts">
                {item.visibleFrom && <><dt>Visible from</dt><dd>{formatDateTime(item.visibleFrom)}</dd></>}
                {item.visibleUntil && <><dt>Visible until</dt><dd>{formatDateTime(item.visibleUntil)}</dd></>}
                {isGraded(item) && <>
                    <dt>Deadline</dt><dd>{formatDateTime(item.deadline)}</dd>
                    <dt>Late submissions</dt><dd>{item.lateSubmissionsAllowed ? 'Accepted (flagged as late)' : 'Not accepted'}</dd>
                    {maxPoints != null && <><dt>Max points</dt><dd>{maxPoints}</dd></>}
                </>}
                {item.type === 'QUIZ' && <><dt>Attempts</dt><dd>{item.maxAttempts}</dd></>}
                {item.groupWork && <><dt>Work form</dt><dd>Group work: one submission per group</dd></>}
                {item.starterRepositoryUrl && <>
                    <dt>Starter code</dt>
                    <dd><code>git clone {item.starterRepositoryUrl}</code></dd>
                </>}
            </dl>

            {item.body && <div className="page-body">{item.body}</div>}
            {item.description && <div className="page-body">{item.description}</div>}

            {item.type === 'QUIZ' && (isTeacher
                ? <><QuizEditor item={item} onTotalChange={setQuizPoints} /><LiveHostCard item={item} course={course} /><TeacherSubmissions item={item} /></>
                : <QuizStudent item={item} />)}
            {isGraded(item) && item.type !== 'QUIZ' && (isTeacher
                ? <TeacherSubmissions item={item} />
                : <StudentSubmission item={item} course={course} />)}
        </div>
    )
}

/** Student: current status (from "my grades") and a hand-in form. */
function StudentSubmission({ item, course }) {
    const fetchMyGrades = useCallback(() => api.myGrades(course.id), [course.id])
    const { data: grades, error: loadError, reload } = useApiData(fetchMyGrades)
    const myResult = grades?.find(g => g.contentId === item.id) ?? null
    const [answer, setAnswer] = useState('')
    const [file, setFile] = useState(null)
    const [fileInputKey, setFileInputKey] = useState(0) // changing the key empties the file input
    const [sending, setSending] = useState(false)
    const [message, setMessage] = useState('')
    const [actionError, setError] = useState('')
    const error = actionError || loadError

    const submit = async (e) => {
        e.preventDefault()
        setError('')
        setMessage('')
        setSending(true)
        try {
            const s = await api.submit(item.id, answer, file)
            setAnswer('')
            setFile(null)
            setFileInputKey(k => k + 1)
            setMessage(s.late ? 'Handed in — after the deadline, so it is flagged as late.' : 'Handed in. Thank you!')
            reload()
        } catch (err) {
            setError(err.message)
        } finally {
            setSending(false)
        }
    }

    const downloadMine = () =>
        api.downloadSubmissionFile(myResult.submissionId, myResult.fileName).catch(err => setError(err.message))

    return (
        <section className="card">
            <h3>Your submission</h3>
            {myResult && (
                <p>
                    Status: <strong>{STATUS_LABELS[myResult.status]}</strong>
                    {myResult.late && <span className="late-flag">late</span>}
                    {myResult.points != null && <> · Points: <strong>{myResult.points}</strong>
                        {myResult.maxPoints != null && ` / ${myResult.maxPoints}`}</>}
                </p>
            )}
            {myResult?.fileName && (
                <p>Your file: <button type="button" className="link-button" onClick={downloadMine}>📎 {myResult.fileName}</button></p>
            )}
            {myResult?.feedback && <div className="feedback-box"><strong>Feedback:</strong> {myResult.feedback}</div>}
            {myResult?.status === 'SUBMITTED' && (
                <p className="hint">Grades become visible here once your teacher publishes them.</p>
            )}

            <form className="inline-form" onSubmit={submit}>
                <textarea className="auth-input" rows={4} value={answer} onChange={e => setAnswer(e.target.value)}
                          placeholder={item.type === 'CODING_EXERCISE'
                              ? 'Link to your Git repository'
                              : 'Your answer, or a comment to your file'} />
                <label className="file-label">
                    Attach a file (PDF, Word, ZIP, …, max 20 MB)
                    <input key={fileInputKey} type="file" accept={ALLOWED_FILE_TYPES}
                           onChange={e => setFile(e.target.files[0] ?? null)} />
                </label>
                {error && <p className="form-error">{error}</p>}
                {message && <p className="form-success">{message}</p>}
                <button type="submit" className="btn-primary" disabled={sending || (!answer.trim() && !file)}>
                    {myResult && myResult.status !== 'NOT_SUBMITTED' ? 'Hand in again' : 'Hand in'}
                </button>
                {myResult && myResult.status !== 'NOT_SUBMITTED' && (
                    <p className="hint">Your latest submission is the one that counts.</p>
                )}
            </form>
        </section>
    )
}

/** Teacher: all submissions with a grading form, and the switch that publishes grades. */
function TeacherSubmissions({ item }) {
    const fetchSubmissions = useCallback(() => api.submissions(item.id), [item.id])
    const { data, error: loadError, reload } = useApiData(fetchSubmissions)
    const submissions = data ?? []
    const [published, setPublished] = useState(!!item.gradesPublished)
    const [actionError, setError] = useState('')
    const error = actionError || loadError

    const togglePublished = async () => {
        const next = !published
        setPublished(next) // update the switch right away; undo if the request fails
        try {
            await api.publishGrades(item.id, next)
        } catch (err) {
            setPublished(!next)
            setError(err.message)
        }
    }

    return (
        <section className="card">
            <div className="section-header">
                <h3>Submissions ({submissions.length})</h3>
                <label className="switch-label">
                    <input type="checkbox" checked={published} onChange={togglePublished} />
                    Grades visible to students
                </label>
            </div>
            {error && <p className="form-error">{error}</p>}
            {submissions.length === 0 ? (
                <p className="no-items-text">No submissions yet.</p>
            ) : (
                <table className="data-table">
                    <thead>
                    <tr><th>Student</th><th>Handed in</th><th>Answer</th><th>Grade</th></tr>
                    </thead>
                    <tbody>
                    {submissions.map(({ submission, grade }) => (
                        <tr key={submission.id}>
                            <td>
                                {submission.submittedBy.name}
                                {submission.groupId && <span className="hint"> (for group {submission.groupId})</span>}
                            </td>
                            <td>
                                {formatDateTime(submission.submittedAt)}
                                {submission.late && <span className="late-flag">late</span>}
                            </td>
                            <td className="answer-cell">
                                {isLink(submission.answer)
                                    ? <a href={submission.answer} target="_blank" rel="noreferrer">{submission.answer}</a>
                                    : submission.answer}
                                {submission.file && (
                                    <div>
                                        <button type="button" className="link-button"
                                                onClick={() => api.downloadSubmissionFile(submission.id, submission.file.name)
                                                    .catch(err => setError(err.message))}>
                                            📎 {submission.file.name}
                                        </button>
                                        <span className="hint"> {formatSize(submission.file.size)}</span>
                                    </div>
                                )}
                            </td>
                            <td>
                                <GradeForm submissionId={submission.id} grade={grade} maxPoints={item.maxPoints}
                                           onSaved={reload} onError={setError} />
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            )}
        </section>
    )
}

function GradeForm({ submissionId, grade, maxPoints, onSaved, onError }) {
    const [points, setPoints] = useState(grade?.points ?? '')
    const [feedback, setFeedback] = useState(grade?.feedback ?? '')
    const [saved, setSaved] = useState(false)

    const save = async (e) => {
        e.preventDefault()
        try {
            await api.grade(submissionId, Number(points), feedback)
            setSaved(true)
            onSaved()
        } catch (err) {
            onError(err.message)
        }
    }

    return (
        <form className="grade-form" onSubmit={save}>
            <input type="number" min="0" max={maxPoints ?? undefined} step="0.5" className="auth-input points-input"
                   value={points} onChange={e => { setPoints(e.target.value); setSaved(false) }}
                   placeholder="Points" required />
            {maxPoints != null && <span className="hint">/ {maxPoints}</span>}
            <input className="auth-input" value={feedback} placeholder="Feedback"
                   onChange={e => { setFeedback(e.target.value); setSaved(false) }} />
            <button type="submit" className="btn-small">{grade ? 'Update' : 'Grade'}</button>
            {saved && <span className="form-success">Saved</span>}
        </form>
    )
}
