import React, { useState } from 'react';
import '../Home.css';

export default function Course({ course, onBackToDashboard }) {
    // Standardmäßig ist das erste Modul geöffnet
    const [expandedModules, setExpandedModules] = useState({
        [course.modules[0]?.id]: true
    });

    // Hilfsfunktion, um Module einzeln auf-/zuzuklappen
    const toggleModule = (id) => {
        setExpandedModules(prev => ({
            ...prev,
            [id]: !prev[id]
        }));
    };

    // Funktion für "Alles einklappen" / "Alles ausklappen"
    const toggleAll = (expand) => {
        const updated = {};
        if (expand) {
            course.modules.forEach(m => { updated[m.id] = true; });
        }
        setExpandedModules(updated);
    };

    // Prüft, ob überhaupt Module existieren, um "Alles einklappen" anzuzeigen
    const anyExpanded = Object.values(expandedModules).some(val => val === true);

    // Icons
    const getItemIcon = (type) => {
        switch (type) {
            case 'PAGE': return '📖';
            case 'QUIZ': return '📝';
            case 'ASSIGNMENT': return '📤';
            case 'CODING': return '💻';
            default: return '📄';
        }
    };

    return (
        <div className="course-page-container">
            {/* 1. Navigation Bar */}
            <nav className="top-nav-breadcrumbs">
                <span onClick={onBackToDashboard} className="breadcrumb-link">Landing Page</span>
                <span className="breadcrumb-divider">/</span>
                <span onClick={onBackToDashboard} className="breadcrumb-link">Dashboard</span>
                <span className="breadcrumb-divider">/</span>
                <span className="breadcrumb-active">My Courses</span>
            </nav>

            {/* 2. Headline */}
            <header className="course-header-section">
                <h1 className="course-full-title">{course.title} ({course.semester})</h1>
            </header>

            {/* 3. Tab-Menü (wie Moodle) */}
            <div className="course-tabs-bar">
                <button className="tab-item active">Course</button>
                <button className="tab-item">Attendance</button>
                <button className="tab-item">Grades</button>
                <button className="tab-item">Badges</button>
                <button className="tab-item">Other</button>
            </div>

            <div className="course-content-area">
                {/* 4. Globaler Toggle "Alles einklappen" */}
                <div className="collapse-toggle-wrapper">
                    {anyExpanded ? (
                        <button onClick={() => toggleAll(false)} className="btn-toggle-all">Toggle All</button>
                    ) : (
                        <button onClick={() => toggleAll(true)} className="btn-toggle-all">See All</button>
                    )}
                </div>

                {/* 5. Modul-Karten (Accordions) */}
                <div className="modules-list">
                    {course.modules.map((module) => {
                        const isExpanded = !!expandedModules[module.id];
                        return (
                            <div key={module.id} className={`module-card ${isExpanded ? 'expanded' : ''}`}>
                                {/* Header der Karte */}
                                <div className="module-card-header" onClick={() => toggleModule(module.id)}>
                                    <div className={`chevron-btn ${isExpanded ? 'rotate-down' : ''}`}>
                                        ❯
                                    </div>
                                    <h2 className="module-title">{module.title}</h2>
                                </div>

                                {/* Inhalt der Karte (Aufgaben, Seiten) */}
                                {isExpanded && (
                                    <div className="module-card-body">
                                        {module.items && module.items.length > 0 ? (
                                            <ul className="module-items-list">
                                                {module.items.map((item) => (
                                                    <li key={item.id} className="module-item-row">
                                                        <span className="item-icon">{getItemIcon(item.type)}</span>
                                                        <span className="item-text-title">{item.title}</span>
                                                    </li>
                                                ))}
                                            </ul>
                                        ) : (
                                            <p className="no-items-text">No Information for this Module. </p>
                                        )}
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
}