import { useState } from 'react'
import './Home.css'
import Login from './pages/Login.jsx'
import Dashboard from "./pages/Dashboard.jsx";
import Course from "./pages/Course.jsx"

function Home() {
  const [count, setCount] = useState(0)
    const [currentUser, setCurrentUser] = useState(null);
    const [activeCourse, setActiveCourse] = useState(null);

    if (!currentUser) {
        return (
            <div>
                {/* Wir übergeben eine Funktion, die den User setzt, wenn der Login klappt */}
                <Login onLoginSuccess={(userData) => setCurrentUser(userData)} />
            </div>
        );
    }
    if (activeCourse) {
        return (
            <Course
                course={activeCourse}
                // Diese Funktion übergeben wir, damit der "Zurück"-Button funktioniert:
                onBackToDashboard={() => setActiveCourse(null)}
            />
        );
    }
    return (
        <div>
            <Dashboard user={currentUser}
                       onLogout={() =>{
                           setCurrentUser(null);
                            setActiveCourse(null)}}
                       onSelectCourse={(courseObj) => setActiveCourse(courseObj)}

            />
        </div>
    );
}

export default Home;
