import { useCallback, useState } from 'react'
import { api } from '../api.js'
import { useApiData } from '../useApiData.js'

const ROLE_LABELS = { STUDENT: 'Student', TEACHER: 'Teacher', ADMIN: 'Admin' }

/** Admin page: decide who is a teacher (may create courses) or an admin. */
export default function Admin({ user }) {
    const fetchUsers = useCallback(() => api.adminUsers(), [])
    const users = useApiData(fetchUsers)
    const [search, setSearch] = useState('')

    const term = search.trim().toLowerCase()
    const shown = users.data?.filter(u =>
        !term || u.name.toLowerCase().includes(term) || u.email.toLowerCase().includes(term))

    return (
        <div className="admin-page">
            <section className="card">
                <h3>Accounts {users.data && `(${users.data.length})`}</h3>
                <p className="hint">New accounts are students. Teachers and admins can create courses.
                    Changing a role doesn't remove anyone from courses they already teach.</p>
                <input className="auth-input admin-search" type="search" value={search}
                       placeholder="Search by name or e-mail" aria-label="Search accounts"
                       onChange={e => setSearch(e.target.value)} />
                {users.error && <p className="form-error">{users.error}</p>}
                {shown && (
                    <table className="admin-table">
                        <thead><tr><th>Name</th><th>E-mail</th><th>Role</th></tr></thead>
                        <tbody>
                            {/* The role is part of the key, so a row starts fresh when its role changes */}
                            {shown.map(u => <UserRow key={`${u.id}-${u.accountRole}`} account={u}
                                                     isSelf={u.id === user.id} onChanged={users.reload} />)}
                        </tbody>
                    </table>
                )}
                {shown?.length === 0 && <p className="no-items-text">No accounts match “{search}”.</p>}
            </section>
        </div>
    )
}

function UserRow({ account, isSelf, onChanged }) {
    const [role, setRole] = useState(account.accountRole)
    const [error, setError] = useState('')

    const change = async (newRole) => {
        const previous = role
        setRole(newRole) // show the change right away
        setError('')
        try {
            await api.setAccountRole(account.id, newRole)
            onChanged()
        } catch (err) {
            setRole(previous)
            setError(err.message)
        }
    }

    return (
        <tr>
            <td>{account.name}{isSelf && ' (you)'}</td>
            <td>{account.email}</td>
            <td>
                <select value={role} disabled={isSelf} aria-label={`Role of ${account.name}`}
                        title={isSelf ? "You can't change your own role" : undefined}
                        onChange={e => change(e.target.value)}>
                    {Object.entries(ROLE_LABELS).map(([value, label]) =>
                        <option key={value} value={value}>{label}</option>)}
                </select>
                {error && <span className="form-error"> {error}</span>}
            </td>
        </tr>
    )
}
