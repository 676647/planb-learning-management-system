import { useCallback, useState } from 'react'
import '../Home.css'
import { api } from '../api.js'
import { useApiData } from '../useApiData.js'
import Notifications from '../components/Notifications.jsx'

export default function Dashboard({ user, onLogout, onSelectCourse }) {
    // The user's enrollments: each is { id, course, role }
    const fetchEnrollments = useCallback(() => api.myEnrollments(), [])
    const { data, error, loading: isLoading, reload } = useApiData(fetchEnrollments)
    const enrollments = data ?? []
    const [showCreate, setShowCreate] = useState(false)

    const teaching = enrollments.filter(e => e.role === 'TEACHER')
    const studying = enrollments.filter(e => e.role === 'STUDENT')

    return (
        <div className="dashboard-container">

            <header className="dashboard-header">
                <h1>Welcome, {user.name}!</h1>
                <button className="logout-btn" onClick={onLogout}>
                    Sign out
                </button>
            </header>

            <Notifications enrollments={enrollments} onOpenCourse={onSelectCourse} />

            <div className="section-header">
                <h2 className="section-title">Your classes</h2>
                <button className="btn-small" onClick={() => setShowCreate(!showCreate)}>
                    {showCreate ? 'Cancel' : '+ New course'}
                </button>
            </div>

            {showCreate && (
                <CreateCourseForm onCreated={() => {
                    setShowCreate(false)
                    reload()
                }} />
            )}

            {error && <p className="form-error">{error}</p>}

            {isLoading ? (
                <p className="loading-text">Loading your courses…</p>
            ) : enrollments.length === 0 ? (
                <p className="loading-text">
                    You are not in any course yet. Create one, or ask a teacher to invite you
                    using your e-mail address ({user.email}).
                </p>
            ) : (
                <>
                    <CourseGrid title="Teaching" enrollments={teaching} onSelectCourse={onSelectCourse} />
                    <CourseGrid title="Studying" enrollments={studying} onSelectCourse={onSelectCourse} />
                </>
            )}
        </div>
    )
}

function CourseGrid({ title, enrollments, onSelectCourse }) {
    if (enrollments.length === 0) return null
    return (
        <section className="dashboard-section">
            <h3 className="subsection-title">{title}</h3>
            <div className="course-grid">
                {enrollments.map(enrollment => (
                    <div key={enrollment.id} className="course-card">
                        <span className={`role-badge role-${enrollment.role.toLowerCase()}`}>
                            {enrollment.role === 'TEACHER' ? 'Teacher' : 'Student'}
                        </span>
                        <h3>{enrollment.course.title}</h3>
                        <p>
                            {enrollment.course.semester && <>Semester: {enrollment.course.semester}<br /></>}
                            {enrollment.course.description}
                        </p>
                        <button className="open-course-btn" onClick={() => onSelectCourse(enrollment)}>
                            Open course
                        </button>
                    </div>
                ))}
            </div>
        </section>
    )
}

function CreateCourseForm({ onCreated }) {
    const [title, setTitle] = useState('')
    const [semester, setSemester] = useState('')
    const [description, setDescription] = useState('')
    const [error, setError] = useState('')

    const submit = async (e) => {
        e.preventDefault()
        try {
            await api.createCourse({ title, semester, description })
            onCreated()
        } catch (err) {
            setError(err.message)
        }
    }

    return (
        <form className="inline-form card" onSubmit={submit}>
            <h3>New course</h3>
            <p className="hint">You will be the teacher of this course.</p>
            <input className="auth-input" placeholder="Title, e.g. DAT250" value={title}
                   onChange={e => setTitle(e.target.value)} required />
            <input className="auth-input" placeholder="Semester, e.g. Autumn 2026" value={semester}
                   onChange={e => setSemester(e.target.value)} />
            <textarea className="auth-input" placeholder="Description" rows={3} value={description}
                      onChange={e => setDescription(e.target.value)} />
            {error && <p className="form-error">{error}</p>}
            <button type="submit" className="btn-primary">Create course</button>
        </form>
    )
}
