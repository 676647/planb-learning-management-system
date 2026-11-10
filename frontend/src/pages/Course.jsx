import { useState } from 'react'
import '../Home.css'
import CourseContent from '../components/CourseContent.jsx'
import Participants from '../components/Participants.jsx'
import Grades from '../components/Grades.jsx'
import Groups from '../components/Groups.jsx'
import { LiveBanner } from '../live/LiveEntry.jsx'

const TABS = ['Course', 'Participants', 'Grades', 'Groups']

/**
 * One course. What you can do depends on your role in this course
 * (roles are per course: you can teach one course and study in another).
 */
export default function Course({ enrollment, user, onBackToDashboard }) {
    const { course, role } = enrollment
    const isTeacher = role === 'TEACHER'
    const [tab, setTab] = useState('Course')

    return (
        <div className="course-page-container">
            <nav className="top-nav-breadcrumbs">
                <span onClick={onBackToDashboard} className="breadcrumb-link">Dashboard</span>
                <span className="breadcrumb-divider">/</span>
                <span className="breadcrumb-active">{course.title}</span>
            </nav>

            <header className="course-header-section">
                <h1 className="course-full-title">
                    {course.title}{course.semester && ` (${course.semester})`}
                </h1>
                {course.description && <p className="course-description">{course.description}</p>}
                <span className={`role-badge role-${role.toLowerCase()}`}>
                    You are {isTeacher ? 'a teacher' : 'a student'} in this course
                </span>
            </header>

            <LiveBanner course={course} user={user} />

            <div className="course-tabs-bar">
                {TABS.map(name => (
                    <button key={name} className={`tab-item ${tab === name ? 'active' : ''}`}
                            onClick={() => setTab(name)}>
                        {name}
                    </button>
                ))}
            </div>

            <div className="course-content-area">
                {tab === 'Course' && <CourseContent course={course} isTeacher={isTeacher} />}
                {tab === 'Participants' && <Participants course={course} isTeacher={isTeacher} user={user} />}
                {tab === 'Grades' && <Grades course={course} isTeacher={isTeacher} />}
                {tab === 'Groups' && <Groups course={course} isTeacher={isTeacher} user={user} />}
            </div>
        </div>
    )
}
