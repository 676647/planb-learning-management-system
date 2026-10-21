import { useCallback, useState } from 'react'
import { api } from '../api.js'
import { useApiData } from '../useApiData.js'

/** Who is in the course. Teachers can invite people by e-mail and remove them. */
export default function Participants({ course, isTeacher, user }) {
    const fetchParticipants = useCallback(() => api.participants(course.id), [course.id])
    const { data, error: loadError, reload } = useApiData(fetchParticipants)
    const enrollments = data ?? []
    const [email, setEmail] = useState('')
    const [role, setRole] = useState('STUDENT')
    const [actionError, setError] = useState('')
    const error = actionError || loadError

    const invite = async (e) => {
        e.preventDefault()
        setError('')
        try {
            await api.enroll(course.id, email, role)
            setEmail('')
            reload()
        } catch (err) {
            setError(err.status === 404
                ? `${email} has no account yet. Ask them to register first.`
                : err.message)
        }
    }

    const remove = async (enrollment) => {
        if (!window.confirm(`Remove ${enrollment.user.name} from the course?`)) return
        try {
            await api.unenroll(course.id, enrollment.user.id)
            reload()
        } catch (err) {
            setError(err.message)
        }
    }

    const teachers = enrollments.filter(e => e.role === 'TEACHER')
    const students = enrollments.filter(e => e.role === 'STUDENT')

    return (
        <>
            {isTeacher && (
                <form className="inline-form row" onSubmit={invite}>
                    <input type="email" className="auth-input" placeholder="E-mail of the person to invite"
                           value={email} onChange={e => setEmail(e.target.value)} required />
                    <select className="small-select" value={role} onChange={e => setRole(e.target.value)}>
                        <option value="STUDENT">Student</option>
                        <option value="TEACHER">Teacher</option>
                    </select>
                    <button type="submit" className="btn-small">Invite</button>
                </form>
            )}
            {error && <p className="form-error">{error}</p>}

            {[['Teachers', teachers], ['Students', students]].map(([title, list]) => (
                <section key={title}>
                    <h3 className="subsection-title">{title} ({list.length})</h3>
                    <table className="data-table">
                        <tbody>
                        {list.map(e => (
                            <tr key={e.id}>
                                <td>{e.user.name}{e.user.id === user.id && <span className="hint"> (you)</span>}</td>
                                <td>{e.user.email}</td>
                                <td className="actions-cell">
                                    {isTeacher && e.user.id !== user.id && (
                                        <button className="btn-text danger" onClick={() => remove(e)}>Remove</button>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </section>
            ))}
        </>
    )
}
