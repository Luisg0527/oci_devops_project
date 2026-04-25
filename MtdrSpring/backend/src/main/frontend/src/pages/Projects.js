import React, { useEffect, useState } from 'react';
import { useHistory } from 'react-router-dom';
import './Projects.css';

const API_BASE = '/api/v1';

function Projects() {
  const history = useHistory();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [newProjectName, setNewProjectName] = useState('');
  const [newProjectDescription, setNewProjectDescription] = useState('');
  const [createError, setCreateError] = useState('');

  const token = localStorage.getItem('authToken');
  const userId = localStorage.getItem('userId');

  useEffect(() => {
    if (!token) {
      history.replace('/login');
      return;
    }

    const loadProjects = async () => {
      try {
        setLoading(true);
        setError('');
        const response = await fetch(`${API_BASE}/projects?page=1&limit=100`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
          throw new Error(payload.error || 'No fue posible cargar proyectos.');
        }
        setProjects(Array.isArray(payload.data) ? payload.data : []);
      } catch (err) {
        setError(err.message || 'No fue posible cargar proyectos.');
      } finally {
        setLoading(false);
      }
    };

    loadProjects();
  }, [history, token]);

  const openCreateProject = () => {
    setCreateError('');
    setNewProjectName('');
    setNewProjectDescription('');
    setIsCreating(true);
  };

  const handleCreateProject = async (event) => {
    event.preventDefault();
    const projectName = newProjectName.trim();

    if (!projectName) {
      setCreateError('El nombre del proyecto es obligatorio.');
      return;
    }

    if (!userId) {
      setCreateError('No hay usuario autenticado para asignar manager.');
      return;
    }

    try {
      setCreateError('');
      const response = await fetch(`${API_BASE}/projects`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: projectName,
          description: newProjectDescription.trim() || null,
          managerId: Number(userId),
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || 'No fue posible crear el proyecto.');
      }
      setProjects((prev) => [payload, ...prev]);
      setIsCreating(false);
    } catch (err) {
      setCreateError(err.message || 'No fue posible crear el proyecto.');
    }
  };

  const handleSelectProject = (project) => {
    localStorage.setItem('currentProjectId', String(project.projectId));
    localStorage.setItem('currentProjectName', project.name || 'Project Studio');
    history.push('/backlog');
  };

  return (
    <div className="projects-page">
      <header className="projects-page__topbar">
        <span className="projects-page__brand">Studio Projects</span>
        <div className="projects-page__actions">
          {projects.length > 0 && (
            <button type="button" className="projects-page__btn" onClick={openCreateProject}>
              New Project
            </button>
          )}
        </div>
      </header>

      <main className="projects-page__content">
        {loading && <p className="projects-page__feedback">Cargando proyectos...</p>}
        {error && <p className="projects-page__feedback projects-page__feedback--error">{error}</p>}

        {!loading && !error && projects.length === 0 && (
          <section className="projects-empty">
            <div className="projects-empty__icon">
              <span className="material-icons">architecture</span>
            </div>
            <h1 className="projects-empty__title">
              Your studio canvas is currently <em>open.</em>
            </h1>
            <p className="projects-empty__subtitle">
              Begin your next architectural monograph. Define your vision, track milestones, and
              curate project assets in a dedicated workspace.
            </p>
            <button type="button" className="projects-page__btn" onClick={openCreateProject}>
              Create your first project
            </button>
          </section>
        )}

        {!loading && !error && projects.length > 0 && (
          <section>
            <div className="projects-summary">
              <div>
                <p className="projects-summary__label">Workspace overview</p>
                <h2 className="projects-summary__title">Curate your active studio operations.</h2>
              </div>
              <div className="projects-summary__stats">
                <span>{String(projects.length).padStart(2, '0')} total projects</span>
              </div>
            </div>

            <div className="projects-grid">
              {projects.map((project, index) => (
                <article
                  key={project.projectId}
                  className={`projects-card ${index === 0 ? 'projects-card--featured' : ''}`}
                  role="button"
                  tabIndex={0}
                  onClick={() => handleSelectProject(project)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      handleSelectProject(project);
                    }
                  }}
                >
                  <h3>{project.name}</h3>
                  <p>{project.description || 'Sin descripción.'}</p>
                  <small>{project.managerName || 'Sin manager asignado'}</small>
                </article>
              ))}
            </div>
          </section>
        )}
      </main>

      {isCreating && (
        <div className="projects-modal" role="dialog" aria-modal="true">
          <div className="projects-modal__backdrop" onClick={() => setIsCreating(false)} />
          <form className="projects-modal__panel" onSubmit={handleCreateProject}>
            <h3>Nuevo proyecto</h3>
            <label htmlFor="project-name">Nombre</label>
            <input
              id="project-name"
              value={newProjectName}
              onChange={(event) => setNewProjectName(event.target.value)}
              placeholder="Nombre del proyecto"
            />
            <label htmlFor="project-description">Descripción</label>
            <textarea
              id="project-description"
              value={newProjectDescription}
              onChange={(event) => setNewProjectDescription(event.target.value)}
              placeholder="Descripción breve"
              rows={3}
            />
            {createError && <p className="projects-page__feedback projects-page__feedback--error">{createError}</p>}
            <div className="projects-modal__actions">
              <button type="button" className="projects-page__btn projects-page__btn--ghost" onClick={() => setIsCreating(false)}>
                Cancelar
              </button>
              <button type="submit" className="projects-page__btn">
                Crear
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

export default Projects;
