import React, { useEffect, useState } from 'react';
import './DetailModal.css';
import './TaskEditModal.css';
import './ProjectEditModal.css';

const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'INACTIVE', label: 'En pausa' },
  { value: 'CLOSED', label: 'Cerrado' },
];

function projectInitials(name) {
  return (name || 'P')
    .split(' ')
    .filter(Boolean)
    .map((word) => word[0])
    .join('')
    .slice(0, 3)
    .toUpperCase();
}

function normalizeProjectStatus(status) {
  const legacy = {
    ON_HOLD: 'INACTIVE',
    COMPLETED: 'CLOSED',
    CANCELLED: 'CLOSED',
  };
  return legacy[status] || status || 'ACTIVE';
}

function ProjectEditModal({
  project,
  onClose,
  onSubmit,
  submitting = false,
  error = '',
}) {
  const [form, setForm] = useState({
    name: '',
    description: '',
    status: 'ACTIVE',
  });

  useEffect(() => {
    if (!project) return;
    setForm({
      name: project.name || '',
      description: project.description || '',
      status: normalizeProjectStatus(project.status),
    });
  }, [project]);

  const handleSubmit = (event) => {
    event.preventDefault();
    onSubmit(form);
  };

  if (!project) return null;

  const statusLabel =
    STATUS_OPTIONS.find((opt) => opt.value === form.status)?.label || form.status;

  return (
    <div className="member-modal-overlay" onClick={onClose}>
      <div
        className="member-modal member-modal--form task-edit-modal project-edit-modal"
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        aria-labelledby="project-edit-kicker"
        aria-modal="true"
      >
        <button type="button" className="member-modal__close" onClick={onClose}>
          <span className="material-icons">close</span>
        </button>

        <header className="task-edit-modal__hero">
          <div className="task-edit-modal__avatar project-edit-modal__avatar">
            {projectInitials(form.name)}
          </div>
        </header>

        <form className="task-edit-modal__form" onSubmit={handleSubmit}>
          <p id="project-edit-kicker" className="task-edit-modal__kicker">
            Editar proyecto
          </p>
          <div className="task-edit-modal__badges">
            <span className="project-edit-modal__status-pill">{statusLabel}</span>
          </div>

          <div className="task-edit-modal__fields">
            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="project-edit-name">
                <span className="material-icons task-edit-modal__label-icon">folder</span>
                Nombre
              </label>
              <input
                id="project-edit-name"
                type="text"
                className="task-edit-modal__input"
                value={form.name}
                onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                required
              />
            </div>

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="project-edit-description">
                <span className="material-icons task-edit-modal__label-icon">description</span>
                Descripción
              </label>
              <textarea
                id="project-edit-description"
                className="task-edit-modal__input project-edit-modal__textarea"
                rows={3}
                value={form.description}
                onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                placeholder="Descripción opcional del proyecto"
              />
            </div>

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="project-edit-status">
                <span className="material-icons task-edit-modal__label-icon">sync</span>
                Estado
              </label>
              <select
                id="project-edit-status"
                className="task-edit-modal__select"
                value={form.status}
                onChange={(e) => setForm((prev) => ({ ...prev, status: e.target.value }))}
              >
                {STATUS_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {error && (
            <p className="task-edit-modal__error" role="alert">
              <span className="material-icons">warning</span>
              {error}
            </p>
          )}

          <div className="task-edit-modal__actions">
            <button
              type="button"
              className="btn btn--ghost btn--small"
              onClick={onClose}
              disabled={submitting}
            >
              Cancelar
            </button>
            <button type="submit" className="btn btn--primary btn--small" disabled={submitting}>
              {submitting ? 'Guardando…' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ProjectEditModal;
