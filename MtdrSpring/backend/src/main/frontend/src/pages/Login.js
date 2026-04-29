import React, { useState } from 'react';
import './Login.css';

const API_BASE = '/api/v1';

function Login() {
  const [credentials, setCredentials] = useState({
    username: '',
    password: '',
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
    setMessage('Recuperación de contraseña pendiente de integración con el backend.');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const missing = [];
    if (!credentials.username.trim()) missing.push('nombre de usuario');
    if (!credentials.password.trim()) missing.push('contraseña');

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
        throw new Error(payload.error || 'No fue posible iniciar sesión.');
      }

      if (!payload.token) {
        throw new Error('El backend no devolvió token de acceso.');
      }

      localStorage.setItem('authToken', payload.token);
      if (payload.refreshToken) localStorage.setItem('refreshToken', payload.refreshToken);
      if (payload.userId != null) localStorage.setItem('userId', String(payload.userId));
      if (payload.fullName) localStorage.setItem('userFullName', payload.fullName);
      if (payload.role) localStorage.setItem('userRole', payload.role);
      if (payload.teamId != null) localStorage.setItem('userTeamId', String(payload.teamId));
      else localStorage.removeItem('userTeamId');

      try {
        const projectsResponse = await fetch(`${API_BASE}/projects?page=1&limit=100`, {
          headers: {
            Authorization: `Bearer ${payload.token}`,
          },
        });
        const projectsPayload = await projectsResponse.json().catch(() => ({}));
        const projects = Array.isArray(projectsPayload.data) ? projectsPayload.data : [];
        if (projects.length > 0) {
          const storedProjectId = localStorage.getItem('currentProjectId');
          const selectedProject = projects.find((project) => String(project.projectId) === String(storedProjectId)) || projects[0];
          localStorage.setItem('currentProjectId', String(selectedProject.projectId));
          localStorage.setItem('currentProjectName', selectedProject.name || 'Proyecto');
        } else {
          localStorage.removeItem('currentProjectId');
          localStorage.removeItem('currentProjectName');
        }
      } catch (projectError) {
        // Si no se puede cargar proyectos en login, se resolverá en el selector global.
      }

      setMessage('Inicio de sesión correcto. Redirigiendo...');
      window.location.assign('/dashboard');
    } catch (err) {
      setError(err.message || 'No fue posible iniciar sesión.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <h1 className="login-card__brand">PROJECT STUDIO</h1>

        <div className="login-card__content">
          <h2 className="login-card__title">Bienvenido al estudio</h2>
          <p className="login-card__subtitle">
            Accede a tu portafolio de proyectos y al espacio de trabajo del equipo.
          </p>

          <form className="login-form" onSubmit={handleSubmit}>
            <label className="login-form__label" htmlFor="login-username">
              Usuario
            </label>
            <input
              id="login-username"
              type="text"
              className="login-form__input"
              placeholder="tu.nombre.usuario"
              value={credentials.username}
              onChange={(e) => handleChange('username', e.target.value)}
              autoComplete="username"
            />

            <div className="login-form__row">
              <label className="login-form__label" htmlFor="login-password">
                Contraseña
              </label>
              <button
                type="button"
                className="login-form__link"
                onClick={handleForgotPassword}
              >
                ¿Olvidaste tu contraseña?
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

            <button type="submit" className="login-form__submit" disabled={isSubmitting}>
              {isSubmitting ? 'Entrando...' : 'Iniciar sesión'}
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
