import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import { formatDateTime } from '../format.js'
import { useApiData } from '../useApiData.js'

const REFRESH_EVERY_MS = 30_000

/**
 * The user's notifications. The backend creates them from domain events sent through
 * RabbitMQ (hand-ins, published grades, being added to a course). Refreshed every 30 seconds.
 * Clicking a notification marks it as read and opens its course.
 */
export default function Notifications({ enrollments, onOpenCourse }) {
    const fetchNotifications = useCallback(() => api.notifications(), [])
    const { data, error, reload } = useApiData(fetchNotifications)
    const [open, setOpen] = useState(true)

    useEffect(() => {
        const timer = setInterval(reload, REFRESH_EVERY_MS)
        return () => clearInterval(timer)
    }, [reload])

    if (error) return <p className="form-error">{error}</p>
    if (!data) return null

    const openNotification = async (n) => {
        if (!n.read) await api.markNotificationRead(n.id).catch(() => {})
        const enrollment = enrollments.find(e => e.course.id === n.courseId)
        if (enrollment) onOpenCourse(enrollment)
        else reload()
    }

    const markAll = async () => {
        await api.markAllNotificationsRead().catch(() => {})
        reload()
    }

    return (
        <section className="card notifications">
            <div className="section-header">
                <h3 className="notifications-title" onClick={() => setOpen(!open)}>
                    🔔 Notifications
                    {data.unread > 0 && <span className="unread-badge">{data.unread}</span>}
                </h3>
                <div className="tool-row">
                    {data.unread > 0 && <button className="btn-text" onClick={markAll}>Mark all as read</button>}
                    <button className="btn-text" onClick={() => setOpen(!open)}>{open ? 'Hide' : 'Show'}</button>
                </div>
            </div>
            {open && (data.items.length === 0 ? (
                <p className="no-items-text">Nothing new.</p>
            ) : (
                <ul className="notification-list">
                    {data.items.map(n => (
                        <li key={n.id} className={`notification ${n.read ? '' : 'unread'}`}
                            onClick={() => openNotification(n)}>
                            <span className="notification-message">{n.message}</span>
                            <span className="hint">{formatDateTime(n.createdAt)}</span>
                        </li>
                    ))}
                </ul>
            ))}
        </section>
    )
}
