import React, { useEffect, useState } from 'react';
import './DetailModal.css';
import './TaskEditModal.css';
import './SprintEditModal.css';

const STATUS_OPTIONS = [
  { value: 'PLANNED', label: 'Planeado' },
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'CLOSED', label: 'Cerrado' },
];

function sprintInitials(sprint) {
  const label = sprint?.name || (sprint?.sprintNumber ? `S${sprint.sprintNumber}` : 'S');
  return label
    .split(' ')
    .filter(Boolean)
    .map((word) => word[0])
    .join('')
    .slice(0, 3)
    .toUpperCase();
}

function toInputDate(value) {
  if (!value) return '';
  if (typeof value === 'string' && value.includes('T')) {
    return value.split('T')[0];
  }
  return value;
}

function sprintStatusLabel(status) {
  if (status === 'ACTIVE') return 'En curso';
  if (status === 'CLOSED') return 'Finalizado';
  return 'Planeado';
}

function SprintEditModal({
  sprint,
  onClose,
  onSubmit,
  submitting = false,
  error = '',
}) {
  const [form, setForm] = useState({
    name: '',
    startDate: '',
    endDate: '',
    status: 'PLANNED',
  });

  useEffect(() => {
    if (!sprint) return;
    setForm({
      name: sprint.name || '',
      startDate: toInputDate(sprint.startDate),
      endDate: toInputDate(sprint.endDate),
      status: sprint.status || 'PLANNED',
    });
  }, [sprint]);

  const handleSubmit = (event) => {
    event.preventDefault();
    onSubmit(form);
  };

  if (!sprint) return null;

  return (
    <div className="member-modal-overlay" onClick={onClose}>
      <div
        className="member-modal member-modal--form task-edit-modal sprint-edit-modal"
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        aria-labelledby="sprint-edit-kicker"
        aria-modal="true"
      >
        <button type="button" className="member-modal__close" onClick={onClose}>
          <span className="material-icons">close</span>
        </button>

        <header className="task-edit-modal__hero">
          <div className="task-edit-modal__avatar sprint-edit-modal__avatar">
            {sprintInitials(sprint)}
          </div>
        </header>

        <form className="task-edit-modal__form" onSubmit={handleSubmit}>
          <p id="sprint-edit-kicker" className="task-edit-modal__kicker">
            Editar sprint
          </p>
          <div className="task-edit-modal__badges">
            <span className="sprint-edit-modal__status-pill">
              {sprintStatusLabel(form.status)}
            </span>
            {sprint.sprintNumber ? (
              <span className="sprint-edit-modal__number-pill">
                Sprint {sprint.sprintNumber}
              </span>
            ) : null}
          </div>

          <div className="task-edit-modal__fields">
            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="sprint-edit-name">
                <span className="material-icons task-edit-modal__label-icon">flag</span>
                Nombre
              </label>
              <input
                id="sprint-edit-name"
                type="text"
                className="task-edit-modal__input"
                value={form.name}
                onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                required
              />
            </div>

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="sprint-edit-start">
                <span className="material-icons task-edit-modal__label-icon">event</span>
                Fecha inicio
              </label>
              <input
                id="sprint-edit-start"
                type="date"
                className="task-edit-modal__input"
                value={form.startDate}
                onChange={(e) => setForm((prev) => ({ ...prev, startDate: e.target.value }))}
                required
              />
            </div>

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="sprint-edit-end">
                <span className="material-icons task-edit-modal__label-icon">event_available</span>
                Fecha fin
              </label>
              <input
                id="sprint-edit-end"
                type="date"
                className="task-edit-modal__input"
                value={form.endDate}
                onChange={(e) => setForm((prev) => ({ ...prev, endDate: e.target.value }))}
                required
              />
            </div>

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="sprint-edit-status">
                <span className="material-icons task-edit-modal__label-icon">sync</span>
                Estado
              </label>
              <select
                id="sprint-edit-status"
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

export default SprintEditModal;
