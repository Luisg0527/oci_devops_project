import React, { useState } from 'react';
import './Login.css';

const API_BASE = '/api/v1';

function Login() {
  const [credentials, setCredentials] = useState({
    username: '',
    password: '',
    rememberMe: false,
  });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (field, value) => {
    setCredentials((prev) => ({ ...prev, [field]: value }));
    if (error) setError('');
    if (message) setMessage('');
  };

  const handleForgotPassword = () => {
    setMessage('Recuperacion de contrasena pendiente de integracion con backend.');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const missing = [];
    if (!credentials.username.trim()) missing.push('correo');
    if (!credentials.password.trim()) missing.push('contrasena');

    if (missing.length > 0) {
      setError(`Completa: ${missing.join(' y ')}.`);
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: credentials.username.trim(),
          password: credentials.password,
        }),
      });

      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || 'No fue posible iniciar sesion.');
      }

      if (!payload.token) {
        throw new Error('El backend no regreso token de acceso.');
      }

      localStorage.setItem('authToken', payload.token);
      if (payload.refreshToken) localStorage.setItem('refreshToken', payload.refreshToken);
      if (payload.userId != null) localStorage.setItem('userId', String(payload.userId));
      if (payload.role) localStorage.setItem('userRole', payload.role);

      setMessage('Inicio de sesion correcto. Redirigiendo...');
      window.location.assign('/dashboard');
    } catch (err) {
      setError(err.message || 'No fue posible iniciar sesion.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <h1 className="login-card__brand">PROJECT STUDIO</h1>

        <div className="login-card__content">
          <h2 className="login-card__title">Welcome to the Studio</h2>
          <p className="login-card__subtitle">
            Access your architectural portfolio and projects.
          </p>

          <form className="login-form" onSubmit={handleSubmit}>
            <label className="login-form__label" htmlFor="login-email">
              EMAIL ADDRESS
            </label>
            <input
              id="login-email"
              type="email"
              className="login-form__input"
              placeholder="architect@projectstudio.com"
              value={credentials.username}
              onChange={(e) => handleChange('username', e.target.value)}
              autoComplete="username"
            />

            <div className="login-form__row">
              <label className="login-form__label" htmlFor="login-password">
                PASSWORD
              </label>
              <button
                type="button"
                className="login-form__link"
                onClick={handleForgotPassword}
              >
                FORGOT PASSWORD?
              </button>
            </div>
            <input
              id="login-password"
              type="password"
              className="login-form__input"
              placeholder="************"
              value={credentials.password}
              onChange={(e) => handleChange('password', e.target.value)}
              autoComplete="current-password"
            />

            <label className="login-form__remember">
              <input
                type="checkbox"
                checked={credentials.rememberMe}
                onChange={(e) => handleChange('rememberMe', e.target.checked)}
              />
              <span>Remember me for 30 days</span>
            </label>

            <button type="submit" className="login-form__submit" disabled={isSubmitting}>
              {isSubmitting ? 'SIGNING IN...' : 'SIGN IN'}
            </button>

            {error && <p className="login-form__feedback login-form__feedback--error">{error}</p>}
            {message && <p className="login-form__feedback login-form__feedback--ok">{message}</p>}
          </form>
        </div>
      </div>
    </div>
  );
}

export default Login;
