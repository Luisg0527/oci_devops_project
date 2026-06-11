import React, { useEffect, useMemo, useRef, useState } from 'react';
import { API_BASE } from '../../config/apiBase';
import { useProject } from '../../context/ProjectContext';
import { canManageProjects } from '../../utils/labelsEs';
import ChatWidget from '../common/ChatWidget';
import './TopNavBar.css';

function TopNavBar() {
  const { projectId: currentProjectId, projectName: currentProjectName, setProject } = useProject();
  const [showCreateProject, setShowCreateProject] = useState(false);
  const [newProjectName, setNewProjectName] = useState('');
  const [newProjectDescription, setNewProjectDescription] = useState('');
  const [createProjectError, setCreateProjectError] = useState('');
  const [creatingProject, setCreatingProject] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isProjectMenuOpen, setIsProjectMenuOpen] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [projects, setProjects] = useState([]);
  const menuRef = useRef(null);
  const projectMenuRef = useRef(null);

  const userData = useMemo(() => {
    const fullName = localStorage.getItem('userFullName');
    const role = localStorage.getItem('userRole');

    return {
      fullName: fullName || 'Usuario',
      role: role || 'Sin rol',
      canManageProjects: canManageProjects(role),
    };
  }, []);

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsMenuOpen(false);
      }
      if (projectMenuRef.current && !projectMenuRef.current.contains(event.target)) {
        setIsProjectMenuOpen(false);
        setShowCreateProject(false);
        setNewProjectName('');
        setNewProjectDescription('');
        setCreateProjectError('');
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  const loadProjects = async () => {
    const token = localStorage.getItem('authToken');
    const userId = localStorage.getItem('userId');
    if (!token || !userId) return;

    try {
      const response = await fetch(
        `${API_BASE}/projects?user_id=${encodeURIComponent(userId)}&page=1&limit=100`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) return;
      const data = Array.isArray(payload.data) ? payload.data : [];
      setProjects(data);
      if (data.length === 0) {
        setProject('', 'Sin proyectos');
        return;
      }

      const storedId = currentProjectId || localStorage.getItem('currentProjectId');
      const selected =
        data.find((project) => String(project.projectId) === String(storedId)) || data[0];
      setProject(selected.projectId, selected.name || 'Proyecto');
    } catch (error) {
      // Si falla, se conserva el último proyecto seleccionado.
    }
  };

  useEffect(() => {
    loadProjects();
  }, []);

  const resetCreateProjectForm = () => {
    setShowCreateProject(false);
    setNewProjectName('');
    setNewProjectDescription('');
    setCreateProjectError('');
  };

  const handleCreateProject = async (event) => {
    event.preventDefault();
    if (!userData.canManageProjects) return;
    const name = newProjectName.trim();
    const userId = localStorage.getItem('userId');
    const token = localStorage.getItem('authToken');

    if (!name) {
      setCreateProjectError('El nombre es obligatorio.');
      return;
    }
    if (!userId || !token) {
      setCreateProjectError('Inicia sesión para crear proyectos.');
      return;
    }

    setCreatingProject(true);
    setCreateProjectError('');
    try {
      const response = await fetch(`${API_BASE}/projects`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name,
          description: newProjectDescription.trim() || null,
          managerId: Number(userId),
          status: 'ACTIVE',
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || 'No fue posible crear el proyecto.');
      }

      const projectId = payload.projectId;
      const userRole = localStorage.getItem('userRole') || 'MANAGER';
      const memberResponse = await fetch(`${API_BASE}/projects/${projectId}/members`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          userId: Number(userId),
          roleInProject: userRole,
        }),
      });
      const memberPayload = await memberResponse.json().catch(() => ({}));
      if (!memberResponse.ok) {
        throw new Error(memberPayload.error || 'Proyecto creado, pero no se pudo asignar tu membresía.');
      }

      setProject(projectId, payload.name || name);
      setProjects((prev) => [payload, ...prev]);
      resetCreateProjectForm();
      setIsProjectMenuOpen(false);
    } catch (error) {
      setCreateProjectError(error.message || 'No fue posible crear el proyecto.');
    } finally {
      setCreatingProject(false);
    }
  };

  const handleProjectSelect = (project) => {
    const projectId = String(project.projectId);
    if (projectId === currentProjectId) {
      setIsProjectMenuOpen(false);
      return;
    }
    setProject(project.projectId, project.name || 'Proyecto');
    setIsProjectMenuOpen(false);
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
              <div className="topnav__project-list">
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
                {projects.length === 0 && !showCreateProject && (
                  <div className="topnav__project-empty">No hay proyectos disponibles</div>
                )}
              </div>

              <div className="topnav__project-footer">
                {showCreateProject && userData.canManageProjects ? (
                  <form className="topnav__project-create" onSubmit={handleCreateProject}>
                    <label className="topnav__project-create-label" htmlFor="topnav-new-project-name">
                      Nombre del proyecto
                    </label>
                    <input
                      id="topnav-new-project-name"
                      type="text"
                      className="topnav__project-create-input"
                      value={newProjectName}
                      onChange={(e) => setNewProjectName(e.target.value)}
                      placeholder="Ej. Portal de clientes"
                      autoFocus
                      disabled={creatingProject}
                    />
                    <label className="topnav__project-create-label" htmlFor="topnav-new-project-desc">
                      Descripción (opcional)
                    </label>
                    <input
                      id="topnav-new-project-desc"
                      type="text"
                      className="topnav__project-create-input"
                      value={newProjectDescription}
                      onChange={(e) => setNewProjectDescription(e.target.value)}
                      placeholder="Breve descripción"
                      disabled={creatingProject}
                    />
                    {createProjectError && (
                      <p className="topnav__project-create-error">{createProjectError}</p>
                    )}
                    <div className="topnav__project-create-actions">
                      <button
                        type="button"
                        className="topnav__project-create-btn topnav__project-create-btn--ghost"
                        onClick={resetCreateProjectForm}
                        disabled={creatingProject}
                      >
                        Cancelar
                      </button>
                      <button
                        type="submit"
                        className="topnav__project-create-btn topnav__project-create-btn--primary"
                        disabled={creatingProject}
                      >
                        {creatingProject ? 'Creando…' : 'Crear'}
                      </button>
                    </div>
                  </form>
                ) : userData.canManageProjects ? (
                  <button
                    type="button"
                    className="topnav__project-create-trigger"
                    onClick={() => {
                      setCreateProjectError('');
                      setShowCreateProject(true);
                    }}
                  >
                    <span className="material-icons">add</span>
                    Crear proyecto
                  </button>
                ) : null}
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="topnav__right">
        <button
          type="button"
          className={`topnav__chat-btn ${isChatOpen ? 'topnav__chat-btn--active' : ''}`}
          onClick={() => setIsChatOpen((prev) => !prev)}
          aria-label={isChatOpen ? 'Cerrar Chat IA' : 'Abrir Chat IA'}
          aria-expanded={isChatOpen}
        >
          <span className="material-icons" aria-hidden>
            chat_bubble_outline
          </span>
          <span className="topnav__chat-btn__label">Chat IA</span>
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

      <ChatWidget open={isChatOpen} onClose={() => setIsChatOpen(false)} />
    </header>
  );
}

export default TopNavBar;
