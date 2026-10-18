import React, { useState, useEffect } from 'react';
import '../Home.css';
import './Login.jsx';
import { mockCourses } from '../data/mock.js';
import './Course.jsx';

export default function Dashboard({ user, onLogout, onSelectCourse }) {
    const [courses, setCourses] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

//mock to have a course there
    useEffect(() => {
        setTimeout(() => {
            setCourses(mockCourses);
            setIsLoading(false);
        }, 500); // 0.5 Sekunden Verzögerung
    }, []);

    /*
    useEffect(() => {
        const fetchCourses = async () => {
            try {
                // Holt die Kurse aus der Datenbank (über Spring Boot)
                const response = await fetch('http://localhost:8080/api/courses');
                if (response.ok) {
                    const data = await response.json();
                    setCourses(data);
                }
            } catch (error) {
                console.error("Fehler beim Laden der Kurse:", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchCourses();
    }, []);
    */

    return (
        <div className="dashboard-container">

            <header className="dashboard-header">
                <h1>Welcome, {user.username}!</h1>
                <button className="logout-btn" onClick={onLogout}>
                    Sign out
                </button>
            </header>

            <h2 className="section-title">Your Classes</h2>

            {isLoading ? (
                <p className="loading-text">Courses will be snchronized with Database...</p>
            ) : courses.length === 0 ? (
                <p className="loading-text">No courses found.</p>
            ) : (
                <div className="course-grid">
                    {courses.map(course => (
                        <div key={course.id} className="course-card">
                            <h3>{course.title}</h3>
                            <p>Semester: {course.semester}</p>
                            <button className="open-course-btn" onClick={() => onSelectCourse(course)}>
                                open Course
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}