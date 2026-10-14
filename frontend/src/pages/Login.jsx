import React from 'react';
import '../Home.css';

export default function Login() {
    return (
        <div className="login-container">
            {/* ... */}
            <button className="primary-button">
                Login als Student
            </button>
        </div>
    );
}

const [view, setView] = useState('INITIAL');

// States für die Eingabefelder
const [username, setUsername] = useState('');
const [password, setPassword] = useState('');

// Wird aufgerufen, wenn das Login-Formular abgesendet wird
const handleLogin = (e) => {
    e.preventDefault(); // Verhindert das Neuladen der Seite
    console.log("Login-Versuch mit:", username, password);
    alert(`Eingeloggt als: ${username}`);
    // Hier kommt später der Axios/Fetch Call an dein Spring Boot Backend hin!
};

// Wird aufgerufen, wenn das Registrierungs-Formular abgesendet wird
const handleRegister = (e) => {
    e.preventDefault();
    console.log("Registrierung mit:", username, password);
    alert(`Account erstellt für: ${username}`);
    // Hier kommt später der API Call zum Registrieren hin!
};

const resetFormAndGoBack = () => {
    setUsername('');
    setPassword('');
    setView('INITIAL');
};

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