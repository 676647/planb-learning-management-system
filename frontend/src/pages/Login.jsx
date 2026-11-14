import { useState } from 'react'
import '../Home.css'
import { api, setToken } from '../api.js'

export default function Login({ onLoginSuccess }) {
    const [view, setView] = useState('INITIAL')

    const [email, setEmail] = useState('')
    const [name, setName] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [busy, setBusy] = useState(false)

    /** Log in, keep the token, then load who we are. */
    const logIn = async () => {
        const { accessToken } = await api.login(email, password)
        setToken(accessToken)
        onLoginSuccess(await api.me())
    }

    const handleLogin = async (e) => {
        e.preventDefault()
        setError('')
        setBusy(true)
        try {
            await logIn()
        } catch (err) {
            setError(err.message)
        } finally {
            setBusy(false)
        }
    }

    /** Create the account, then log in straight away. */
    const handleRegister = async (e) => {
        e.preventDefault()
        setError('')
        setBusy(true)
        try {
            await api.register(email, name, password)
            await logIn()
        } catch (err) {
            setError(err.message)
        } finally {
            setBusy(false)
        }
    }

    const resetFormAndGoBack = () => {
        setEmail('')
        setName('')
        setPassword('')
        setError('')
        setView('INITIAL')
    }

    return (
        <div className="auth-container">
            <div className="auth-box">

                <h1 className="auth-title">Ideal LMS</h1>

                {view === 'INITIAL' && (
                    <div className="auth-buttons">
                        <p>Welcome! Please choose an option:</p>
                        <button className="btn-primary" onClick={() => setView('LOGIN')}>
                            Log in
                        </button>
                        <button className="btn-secondary" onClick={() => setView('REGISTER')}>
                            Create new account
                        </button>
                    </div>
                )}

                {view === 'LOGIN' && (
                    <form onSubmit={handleLogin} className="auth-form">
                        <h2>Log in</h2>
                        <input
                            type="email"
                            placeholder="E-mail"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoComplete="username"
                            className="auth-input"
                        />
                        <input
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            autoComplete="current-password"
                            className="auth-input"
                        />
                        {error && <p className="form-error">{error}</p>}
                        <button type="submit" className="btn-primary" disabled={busy}>
                            {busy ? 'Logging in…' : 'Log in'}
                        </button>
                        <button type="button" className="btn-text" onClick={resetFormAndGoBack}>
                            ← Back
                        </button>
                    </form>
                )}

                {view === 'REGISTER' && (
                    <form onSubmit={handleRegister} className="auth-form">
                        <h2>Create account</h2>
                        <input
                            type="text"
                            placeholder="Full name"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                            autoComplete="name"
                            className="auth-input"
                        />
                        <input
                            type="email"
                            placeholder="E-mail"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoComplete="email"
                            className="auth-input"
                        />
                        <input
                            type="password"
                            placeholder="Password (at least 8 characters)"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            minLength={8}
                            autoComplete="new-password"
                            className="auth-input"
                        />
                        {error && <p className="form-error">{error}</p>}
                        <button type="submit" className="btn-secondary" disabled={busy}>
                            {busy ? 'Creating account…' : 'Create account'}
                        </button>
                        <button type="button" className="btn-text" onClick={resetFormAndGoBack}>
                            ← Back
                        </button>
                    </form>
                )}

            </div>
        </div>
    )
}
