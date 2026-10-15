import React, { useState } from 'react'; // useState Import hinzugefügt!
import '../Home.css';
import '../pages/Dashboard.jsx'

export default function Login({onLoginSuccess}) {
    const [view, setView] = useState('INITIAL');

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    // Echte API-Verbindung für den Login
/*    const handleLogin = async (e) => {
        e.preventDefault();

        try {
            // Dies ruft dein Spring Boot Backend auf (Port 8080 ist Standard)
            const response = await fetch('http://localhost:5173/api/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            if (response.ok) {
                // Das Backend gibt die User-Daten zurück (z.B. Name und ID)
                const userData = await response.json();

                // Leitet den User zum Dashboard weiter!
                onLoginSuccess(userData);
            } else {
                alert("Falscher Benutzername oder Passwort.");
            }
        } catch (error) {
            console.error("Fehler bei der Verbindung zum Server:", error);
            alert("Backend nicht erreichbar. Läuft Spring Boot?");
        }
    };

 */

    //Mocking
    const handleLogin = (e) => {
        e.preventDefault();
        console.log("Simulierter Login mit:", username);
        onLoginSuccess({ username: username });
    };

    const handleRegister = (e) => {
        e.preventDefault();
        console.log("Registrierung mit:", username, password);
        alert(`Account erstellt für: ${username}`);
    };

    const resetFormAndGoBack = () => {
        setUsername('');
        setPassword('');
        setView('INITIAL');
    };

    // HIER ist das einzige return, das die schöne auth-box ausgibt!
    return (
        <div className="auth-container">
            <div className="auth-box">

                <h1 className="auth-title">Ideal LMS</h1>

                {/* ANSICHT 1: Startbildschirm */}
                {view === 'INITIAL' && (
                    <div className="auth-buttons">
                        <p>Willkommen! Bitte wähle eine Option:</p>
                        <button className="btn-primary" onClick={() => setView('LOGIN')}>
                            Login
                        </button>
                        <button className="btn-secondary" onClick={() => setView('REGISTER')}>
                            Create new Account
                        </button>
                    </div>
                )}

                {/* ANSICHT 2: Login-Formular */}
                {view === 'LOGIN' && (
                    <form onSubmit={handleLogin} className="auth-form">
                        <h2>Anmelden</h2>
                        <input
                            type="text"
                            placeholder="Benutzername"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            className="auth-input"
                        />
                        <input
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            className="auth-input"
                        />
                        <button type="submit" className="btn-primary">Einloggen</button>
                        <button type="button" className="btn-text" onClick={resetFormAndGoBack}>
                            ← Zurück
                        </button>
                    </form>
                )}

                {/* ANSICHT 3: Registrierungs-Formular */}
                {view === 'REGISTER' && (
                    <form onSubmit={handleRegister} className="auth-form">
                        <h2>Registrieren</h2>
                        <input
                            type="text"
                            placeholder="Gewünschter Benutzername"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            className="auth-input"
                        />
                        <input
                            type="password"
                            placeholder="Passwort wählen"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            className="auth-input"
                        />
                        <button type="submit" className="btn-secondary">Konto erstellen</button>
                        <button type="button" className="btn-text" onClick={resetFormAndGoBack}>
                            ← Zurück
                        </button>
                    </form>
                )}

            </div>
        </div>
    );
}