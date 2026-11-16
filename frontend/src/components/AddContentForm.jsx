import { useState } from 'react'
import { api } from '../api.js'
import { CONTENT_TYPES, localInputToIso } from '../format.js'

/** Teacher form to add a page, quiz, coding exercise or assignment to a module. */
export default function AddContentForm({ moduleId, onAdded, onCancel }) {
    const [type, setType] = useState('PAGE')
    const [fields, setFields] = useState({
        title: '', body: '', description: '', visibleFrom: '', deadline: '',
        maxPoints: '', lateSubmissionsAllowed: false, maxAttempts: '1', showHints: false,
        starterRepositoryUrl: '', groupWork: false,
    })
    const [error, setError] = useState('')

    const set = (name) => (e) => {
        const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value
        setFields(prev => ({ ...prev, [name]: value }))
    }

    /** Only send the fields that belong to the chosen type. */
    const buildRequest = () => {
        const common = { title: fields.title, visibleFrom: localInputToIso(fields.visibleFrom) }
        const graded = {
            deadline: localInputToIso(fields.deadline),
            maxPoints: fields.maxPoints === '' ? null : Number(fields.maxPoints),
            lateSubmissionsAllowed: fields.lateSubmissionsAllowed,
        }
        switch (type) {
            case 'PAGE':
                return { ...common, body: fields.body }
            case 'QUIZ':
                return { ...common, ...graded, maxAttempts: Number(fields.maxAttempts), showHints: fields.showHints }
            case 'CODING_EXERCISE':
                return { ...common, ...graded, starterRepositoryUrl: fields.starterRepositoryUrl, groupWork: fields.groupWork }
            default:
                return { ...common, ...graded, description: fields.description, groupWork: fields.groupWork }
        }
    }

    const submit = async (e) => {
        e.preventDefault()
        try {
            await api.addContent(moduleId, CONTENT_TYPES[type].endpoint, buildRequest())
            onAdded()
        } catch (err) {
            setError(err.message)
        }
    }

    const graded = type !== 'PAGE'

    return (
        <form className="inline-form card" onSubmit={submit}>
            <div className="type-picker">
                {Object.entries(CONTENT_TYPES).map(([key, t]) => (
                    <button type="button" key={key} className={`type-option ${type === key ? 'active' : ''}`}
                            onClick={() => setType(key)}>
                        {t.icon} {t.label}
                    </button>
                ))}
            </div>

            <input className="auth-input" placeholder="Title" value={fields.title} onChange={set('title')} required />

            {type === 'PAGE' && (
                <textarea className="auth-input" rows={6} value={fields.body} onChange={set('body')}
                          placeholder="Page text. Links to images and videos can be included." />
            )}
            {type === 'ASSIGNMENT' && (
                <textarea className="auth-input" rows={4} value={fields.description} onChange={set('description')}
                          placeholder="What should students hand in?" />
            )}
            {type === 'CODING_EXERCISE' && (
                <input className="auth-input" placeholder="Starter code repository URL (git clone …)"
                       value={fields.starterRepositoryUrl} onChange={set('starterRepositoryUrl')} />
            )}

            {/* Dates get their own, wider row: date and time don't fit in a narrow field */}
            <div className="form-grid date-grid">
                <label>Visible from (optional)
                    <input type="datetime-local" className="auth-input" value={fields.visibleFrom} onChange={set('visibleFrom')} />
                </label>
                {graded && (
                    <label>Deadline (optional)
                        <input type="datetime-local" className="auth-input" value={fields.deadline} onChange={set('deadline')} />
                    </label>
                )}
            </div>
            {(graded || type === 'QUIZ') && (
                <div className="form-grid">
                    {graded && (
                        <label>Max points (optional)
                            <input type="number" min="0" className="auth-input" value={fields.maxPoints} onChange={set('maxPoints')} />
                        </label>
                    )}
                    {type === 'QUIZ' && (
                        <label>Attempts allowed
                            <input type="number" min="1" className="auth-input" value={fields.maxAttempts} onChange={set('maxAttempts')} required />
                        </label>
                    )}
                </div>
            )}

            <div className="checkbox-row">
                {graded && (
                    <label><input type="checkbox" checked={fields.lateSubmissionsAllowed} onChange={set('lateSubmissionsAllowed')} />
                        Accept late submissions (flagged as late)</label>
                )}
                {type === 'QUIZ' && (
                    <label><input type="checkbox" checked={fields.showHints} onChange={set('showHints')} /> Show hints</label>
                )}
                {(type === 'CODING_EXERCISE' || type === 'ASSIGNMENT') && (
                    <label><input type="checkbox" checked={fields.groupWork} onChange={set('groupWork')} />
                        Group work (one submission per group)</label>
                )}
            </div>

            {error && <p className="form-error">{error}</p>}
            <div className="tool-row">
                <button type="submit" className="btn-small">Add {CONTENT_TYPES[type].label.toLowerCase()}</button>
                <button type="button" className="btn-text" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    )
}
