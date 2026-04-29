import React, { useEffect, useMemo, useRef, useState } from 'react';
import { API_BASE } from '../../config/apiBase';
import './TopNavBar.css';

function TopNavBar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isProjectMenuOpen, setIsProjectMenuOpen] = useState(false);
  const [projects, setProjects] = useState([]);
  const [currentProjectId, setCurrentProjectId] = useState(localStorage.getItem('currentProjectId') || '');
  const [currentProjectName, setCurrentProjectName] = useState(localStorage.getItem('currentProjectName') || 'Selecciona proyecto');
  const menuRef = useRef(null);
  const projectMenuRef = useRef(null);

  const userData = useMemo(() => {
    const fullName = localStorage.getItem('userFullName');
    const role = localStorage.getItem('userRole');

    return {
      fullName: fullName || 'Usuario',
      role: role || 'Sin rol',
    };
  }, []);

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsMenuOpen(false);
      }
      if (projectMenuRef.current && !projectMenuRef.current.contains(event.target)) {
        setIsProjectMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('authToken');
    if (!token) return;

    const loadProjects = async () => {
      try {
        const response = await fetch(`${API_BASE}/projects?page=1&limit=100`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) return;
        const data = Array.isArray(payload.data) ? payload.data : [];
        setProjects(data);
        if (data.length === 0) {
          setCurrentProjectId('');
          setCurrentProjectName('Sin proyectos');
          localStorage.removeItem('currentProjectId');
          localStorage.removeItem('currentProjectName');
          return;
        }

        const storedId = localStorage.getItem('currentProjectId');
        const selected = data.find((project) => String(project.projectId) === String(storedId)) || data[0];
        setCurrentProjectId(String(selected.projectId));
        setCurrentProjectName(selected.name || 'Proyecto');
        localStorage.setItem('currentProjectId', String(selected.projectId));
        localStorage.setItem('currentProjectName', selected.name || 'Proyecto');
      } catch (error) {
        // Si falla, se conserva el último proyecto seleccionado.
      }
    };

    loadProjects();
  }, []);

  const handleProjectSelect = (project) => {
    const projectId = String(project.projectId);
    if (projectId === currentProjectId) {
      setIsProjectMenuOpen(false);
      return;
    }
    localStorage.setItem('currentProjectId', projectId);
    localStorage.setItem('currentProjectName', project.name || 'Proyecto');
    setCurrentProjectId(projectId);
    setCurrentProjectName(project.name || 'Proyecto');
    setIsProjectMenuOpen(false);
    window.location.reload();
  };

  const handleLogout = async () => {
    const token = localStorage.getItem('authToken');

    try {
      if (token) {
        await fetch(`${API_BASE}/auth/logout`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
      }
    } catch (error) {
      // Best-effort logout: local session is always cleared.
    } finally {
      localStorage.removeItem('authToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userId');
      localStorage.removeItem('userFullName');
      localStorage.removeItem('userEmail');
      localStorage.removeItem('userRole');
      localStorage.removeItem('userTeamId');
      localStorage.removeItem('currentProjectId');
      localStorage.removeItem('currentProjectName');
      window.location.assign('/login');
    }
  };

  return (
    <header className="topnav">
      <div className="topnav__left">
        <div className="topnav__project" ref={projectMenuRef}>
          <span className="topnav__project-label">Proyecto</span>
          <button
            type="button"
            className="topnav__project-trigger"
            onClick={() => setIsProjectMenuOpen((prev) => !prev)}
            aria-expanded={isProjectMenuOpen}
            aria-label="Cambiar proyecto"
          >
            <span className="topnav__project-name">{currentProjectName}</span>
            <span className="material-icons">expand_more</span>
          </button>
          {isProjectMenuOpen && (
            <div className="topnav__project-menu">
              {projects.map((project) => (
                <button
                  key={project.projectId}
                  type="button"
                  className={`topnav__project-item ${String(project.projectId) === currentProjectId ? 'topnav__project-item--active' : ''}`}
                  onClick={() => handleProjectSelect(project)}
                >
                  {project.name}
                </button>
              ))}
              {projects.length === 0 && (
                <div className="topnav__project-empty">No hay proyectos disponibles</div>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="topnav__right">
        <button className="topnav__icon-btn">
          <span className="material-icons">notifications_none</span>
        </button>
        <button className="topnav__icon-btn">
          <span className="material-icons">chat_bubble_outline</span>
        </button>
        <div className="topnav__divider" />
        <div className="topnav__user" ref={menuRef}>
          <div className="topnav__user-info">
            <span className="topnav__user-name">{userData.fullName}</span>
            <span className="topnav__user-role">{userData.role}</span>
          </div>
          <button
            type="button"
            className="topnav__avatar topnav__avatar-btn"
            onClick={() => setIsMenuOpen((prev) => !prev)}
            aria-label="Abrir menú de usuario"
            aria-expanded={isMenuOpen}
          >
            <span className="material-icons">person</span>
          </button>
          {isMenuOpen && (
            <div className="topnav__menu">
              <button type="button" className="topnav__menu-item" onClick={handleLogout}>
                Cerrar sesión
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default TopNavBar;
