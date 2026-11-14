import { useCallback, useState } from 'react'
import { api } from '../api.js'
import { useApiData } from '../useApiData.js'
import { CONTENT_TYPES, formatDateTime, isGraded } from '../format.js'
import AddContentForm from './AddContentForm.jsx'
import ContentDetail from './ContentDetail.jsx'

/**
 * The "Course" tab: modules as collapsible cards, each with its content items.
 * Teachers can add/delete modules and items and set module requirements.
 * Students only receive items that are currently visible (the backend filters them).
 */
export default function CourseContent({ course, isTeacher }) {
    // Loads the modules, then each module's contents (as an extra "items" array)
    const fetchModules = useCallback(async () => {
        const list = await api.modules(course.id)
        return Promise.all(list.map(async m => ({ ...m, items: await api.contents(m.id) })))
    }, [course.id])
    const { data, error: loadError, loading: isLoading, reload } = useApiData(fetchModules)
    const modules = data ?? []

    const [actionError, setError] = useState('')
    const error = actionError || loadError
    // Which modules are open. Modules not listed here: only the first one is open.
    const [expandedModules, setExpandedModules] = useState({})
    const [selectedItem, setSelectedItem] = useState(null)
    const [newModuleTitle, setNewModuleTitle] = useState('')
    const [addingTo, setAddingTo] = useState(null) // module id that shows the "add item" form

    const isOpen = (module, index) => expandedModules[module.id] ?? index === 0
    const toggleModule = (module, index) =>
        setExpandedModules(prev => ({ ...prev, [module.id]: !isOpen(module, index) }))

    const toggleAll = (expand) => {
        const updated = {}
        modules.forEach(m => { updated[m.id] = expand })
        setExpandedModules(updated)
    }
    const anyExpanded = modules.some(isOpen)

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

    const createModule = (e) => {
        e.preventDefault()
        run(async () => {
            await api.createModule(course.id, newModuleTitle)
            setNewModuleTitle('')
        })
    }

    if (selectedItem) {
        return (
            <ContentDetail
                item={selectedItem}
                course={course}
                isTeacher={isTeacher}
                onBack={() => setSelectedItem(null)}
            />
        )
    }

    const titleOf = (id) => modules.find(m => m.id === id)?.title ?? `Module ${id}`

    return (
        <>
            {error && <p className="form-error">{error}</p>}

            {isTeacher && (
                <form className="inline-form row" onSubmit={createModule}>
                    <input className="auth-input" placeholder="New module, e.g. Week 1: Introduction"
                           value={newModuleTitle} onChange={e => setNewModuleTitle(e.target.value)} required />
                    <button type="submit" className="btn-small">+ Add module</button>
                </form>
            )}

            {modules.length > 0 && (
                <div className="collapse-toggle-wrapper">
                    <button onClick={() => toggleAll(!anyExpanded)} className="btn-toggle-all">
                        {anyExpanded ? 'Collapse all' : 'Expand all'}
                    </button>
                </div>
            )}

            {isLoading ? (
                <p className="loading-text">Loading modules…</p>
            ) : modules.length === 0 ? (
                <p className="no-items-text">
                    {isTeacher ? 'No modules yet. Add the first one above.' : 'This course has no content yet.'}
                </p>
            ) : (
                <div className="modules-list">
                    {modules.map((module, index) => {
                        const isExpanded = isOpen(module, index)
                        const others = modules.filter(m => m.id !== module.id
                            && !module.requiredModuleIds.includes(m.id))
                        return (
                            <div key={module.id} className={`module-card ${isExpanded ? 'expanded' : ''}`}>
                                <div className="module-card-header" onClick={() => toggleModule(module, index)}>
                                    <div className={`chevron-btn ${isExpanded ? 'rotate-down' : ''}`}>❯</div>
                                    <h2 className="module-title">{module.title}</h2>
                                    {module.requiredModuleIds.length > 0 && (
                                        <span className="requires-note">
                                            Requires: {module.requiredModuleIds.map(titleOf).join(', ')}
                                        </span>
                                    )}
                                </div>

                                {isExpanded && (
                                    <div className="module-card-body">
                                        {module.items.length > 0 ? (
                                            <ul className="module-items-list">
                                                {module.items.map((item) => (
                                                    <li key={item.id} className="module-item-row">
                                                        <span className="item-icon">{CONTENT_TYPES[item.type]?.icon ?? '📄'}</span>
                                                        <span className="item-text-title" onClick={() => setSelectedItem(item)}>
                                                            {item.title}
                                                        </span>
                                                        <span className="item-meta">
                                                            {isGraded(item) && item.deadline && `Due ${formatDateTime(item.deadline)}`}
                                                            {isTeacher && item.visibleFrom && ` · Visible from ${formatDateTime(item.visibleFrom)}`}
                                                        </span>
                                                        {isTeacher && (
                                                            <button className="btn-icon" title="Delete item"
                                                                    onClick={() => window.confirm(`Delete "${item.title}"?`)
                                                                        && run(() => api.deleteContent(item.id))}>
                                                                🗑
                                                            </button>
                                                        )}
                                                    </li>
                                                ))}
                                            </ul>
                                        ) : (
                                            <p className="no-items-text">No content in this module yet.</p>
                                        )}

                                        {isTeacher && (
                                            <div className="module-teacher-tools">
                                                {addingTo === module.id ? (
                                                    <AddContentForm
                                                        moduleId={module.id}
                                                        onCancel={() => setAddingTo(null)}
                                                        onAdded={() => {
                                                            setAddingTo(null)
                                                            reload()
                                                        }}
                                                    />
                                                ) : (
                                                    <div className="tool-row">
                                                        <button className="btn-small" onClick={() => setAddingTo(module.id)}>
                                                            + Add item
                                                        </button>
                                                        {others.length > 0 && (
                                                            <select className="small-select" value=""
                                                                    onChange={e => run(() => api.addRequirement(module.id, e.target.value))}>
                                                                <option value="">Requires module…</option>
                                                                {others.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}
                                                            </select>
                                                        )}
                                                        {module.requiredModuleIds.map(id => (
                                                            <button key={id} className="chip" title="Remove requirement"
                                                                    onClick={() => run(() => api.removeRequirement(module.id, id))}>
                                                                {titleOf(id)} ✕
                                                            </button>
                                                        ))}
                                                        <button className="btn-text danger"
                                                                onClick={() => window.confirm(`Delete module "${module.title}" and all its content?`)
                                                                    && run(() => api.deleteModule(module.id))}>
                                                            Delete module
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        )
                    })}
                </div>
            )}
        </>
    )
}
