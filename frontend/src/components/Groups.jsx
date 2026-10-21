import { useCallback, useState } from 'react'
import { api } from '../api.js'
import { useApiData } from '../useApiData.js'

/**
 * Groups for group work. Teachers create groups and can add anyone; students can join
 * one group themselves and leave it again.
 */
export default function Groups({ course, isTeacher, user }) {
    const fetchGroups = useCallback(() => api.groups(course.id), [course.id])
    const { data, error: loadError, reload } = useApiData(fetchGroups)
    const groups = data ?? []
    const [newName, setNewName] = useState('')
    const [memberEmail, setMemberEmail] = useState({}) // per group, for teachers
    const [actionError, setError] = useState('')
    const error = actionError || loadError

    /** Runs an API action, shows its error if it fails, and reloads on success. */
    const run = async (action) => {
        setError('')
        try {
            await action()
            reload()
        } catch (err) {
            setError(err.message)
        }
    }

    const myGroup = groups.find(g => g.members.some(m => m.id === user.id))

    return (
        <>
            {isTeacher && (
                <form className="inline-form row" onSubmit={e => {
                    e.preventDefault()
                    run(async () => {
                        await api.createGroup(course.id, newName)
                        setNewName('')
                    })
                }}>
                    <input className="auth-input" placeholder="New group name" value={newName}
                           onChange={e => setNewName(e.target.value)} required />
                    <button type="submit" className="btn-small">+ Create group</button>
                </form>
            )}
            {error && <p className="form-error">{error}</p>}
            {!isTeacher && !myGroup && groups.length > 0 && (
                <p className="hint">You are not in a group yet. Join one to hand in group work.</p>
            )}

            {groups.length === 0 ? (
                <p className="no-items-text">No groups yet.</p>
            ) : (
                <div className="course-grid">
                    {groups.map(group => (
                        <div key={group.id} className="course-card group-card">
                            <h3>{group.name}</h3>
                            <ul className="member-list">
                                {group.members.length === 0 && <li className="hint">No members</li>}
                                {group.members.map(m => (
                                    <li key={m.id}>
                                        {m.name}{m.id === user.id && ' (you)'}
                                        {(isTeacher || m.id === user.id) && (
                                            <button className="btn-text danger"
                                                    onClick={() => run(() => api.removeGroupMember(group.id, m.id))}>
                                                {m.id === user.id ? 'Leave' : 'Remove'}
                                            </button>
                                        )}
                                    </li>
                                ))}
                            </ul>
                            {isTeacher ? (
                                <form className="inline-form" onSubmit={e => {
                                    e.preventDefault()
                                    run(async () => {
                                        await api.addGroupMember(group.id, memberEmail[group.id])
                                        setMemberEmail(prev => ({ ...prev, [group.id]: '' }))
                                    })
                                }}>
                                    <input type="email" className="auth-input" placeholder="Student e-mail"
                                           value={memberEmail[group.id] ?? ''}
                                           onChange={e => setMemberEmail(prev => ({ ...prev, [group.id]: e.target.value }))}
                                           required />
                                    <button type="submit" className="btn-small">Add member</button>
                                </form>
                            ) : !myGroup && (
                                <button className="open-course-btn"
                                        onClick={() => run(() => api.addGroupMember(group.id, user.email))}>
                                    Join group
                                </button>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </>
    )
}
