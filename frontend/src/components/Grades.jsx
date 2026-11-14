import { useCallback } from 'react'
import { api } from '../api.js'
import { useApiData } from '../useApiData.js'
import { CONTENT_TYPES, STATUS_LABELS, formatDateTime } from '../format.js'

/** The "Grades" tab: the gradebook for teachers (US3), the student's own results otherwise. */
export default function Grades({ course, isTeacher }) {
    return isTeacher ? <Gradebook course={course} /> : <MyGrades course={course} />
}

function Gradebook({ course }) {
    const fetchGradebook = useCallback(() => api.gradebook(course.id), [course.id])
    const { data: book, error } = useApiData(fetchGradebook)

    if (error) return <p className="form-error">{error}</p>
    if (!book) return <p className="loading-text">Loading gradebook…</p>
    if (book.items.length === 0) {
        return <p className="no-items-text">No quizzes, exercises or assignments in this course yet.</p>
    }

    return (
        <div className="table-scroll">
            <p className="hint">
                Every student against every graded item. Low averages point to topics that may need
                follow-up; rows with many gaps point to students who may need it.
            </p>
            <table className="data-table gradebook">
                <thead>
                <tr>
                    <th>Student</th>
                    {book.items.map(item => (
                        <th key={item.contentId} title={`Due ${formatDateTime(item.deadline)}`}>
                            {CONTENT_TYPES[item.type]?.icon} {item.title}
                            {item.maxPoints != null && <div className="hint">max {item.maxPoints}</div>}
                        </th>
                    ))}
                    <th>Total</th>
                </tr>
                </thead>
                <tbody>
                {book.students.map(row => (
                    <tr key={row.userId}>
                        <td>{row.name}<div className="hint">{row.email}</div></td>
                        {row.entries.map(e => (
                            <td key={e.contentId} className={`status-${e.status.toLowerCase()}`}>
                                {e.status === 'GRADED' ? <strong>{e.points}</strong> : STATUS_LABELS[e.status]}
                                {e.late && <span className="late-flag">late</span>}
                            </td>
                        ))}
                        <td><strong>{row.totalPoints}</strong></td>
                    </tr>
                ))}
                </tbody>
                <tfoot>
                <tr>
                    <td>Handed in / graded / average</td>
                    {book.items.map(item => (
                        <td key={item.contentId}>
                            {item.submittedCount} / {item.gradedCount} / {item.averagePoints != null ? item.averagePoints.toFixed(1) : '–'}
                        </td>
                    ))}
                    <td />
                </tr>
                </tfoot>
            </table>
        </div>
    )
}

function MyGrades({ course }) {
    const fetchMyGrades = useCallback(() => api.myGrades(course.id), [course.id])
    const { data: grades, error } = useApiData(fetchMyGrades)

    if (error) return <p className="form-error">{error}</p>
    if (!grades) return <p className="loading-text">Loading your grades…</p>
    if (grades.length === 0) return <p className="no-items-text">Nothing to hand in yet.</p>

    return (
        <table className="data-table">
            <thead>
            <tr><th>Item</th><th>Deadline</th><th>Status</th><th>Points</th><th>Feedback</th></tr>
            </thead>
            <tbody>
            {grades.map(g => (
                <tr key={g.contentId}>
                    <td>{CONTENT_TYPES[g.type]?.icon} {g.title}</td>
                    <td>{formatDateTime(g.deadline)}</td>
                    <td className={`status-${g.status.toLowerCase()}`}>
                        {STATUS_LABELS[g.status]}{g.late && <span className="late-flag">late</span>}
                    </td>
                    <td>{g.points != null ? `${g.points}${g.maxPoints != null ? ` / ${g.maxPoints}` : ''}` : '–'}</td>
                    <td>{g.feedback ?? ''}</td>
                </tr>
            ))}
            </tbody>
        </table>
    )
}
