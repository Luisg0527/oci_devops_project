import React, { useEffect, useState } from 'react';
import PriorityBadge from './PriorityBadge';
import StatusBadge from './StatusBadge';
import './DetailModal.css';
import './TaskEditModal.css';

const STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Pendiente' },
  { value: 'IN_PROGRESS', label: 'En Progreso' },
  { value: 'DONE', label: 'Completado' },
  { value: 'CANCELLED', label: 'Cancelado' },
  { value: 'REOPENED', label: 'Reabierto' },
];

const PRIORITY_OPTIONS = [
  { value: 'HIGH', label: 'Alta' },
  { value: 'MEDIUM', label: 'Media' },
  { value: 'LOW', label: 'Baja' },
];

function taskInitials(title) {
  return (title || 'T')
    .split(' ')
    .filter(Boolean)
    .map((word) => word[0])
    .join('')
    .slice(0, 3)
    .toUpperCase();
}

function sprintValueFromTask(task) {
  if (task.sprintId != null) return String(task.sprintId);
  if (task.taskStage === 'BACKLOG') return 'BACKLOG';
  return 'BACKLOG';
}

function TaskEditModal({
  task,
  sprints,
  assignableUsers,
  sprintLabel,
  onClose,
  onSubmit,
  submitting = false,
  error = '',
}) {
  const [form, setForm] = useState({
    title: '',
    priority: 'MEDIUM',
    status: 'PENDING',
    assignedTo: '',
    dueDate: '',
    sprintId: 'BACKLOG',
  });

  useEffect(() => {
    if (!task) return;
    setForm({
      title: task.title || '',
      priority: task.priority || 'MEDIUM',
      status: task.status || 'PENDING',
      assignedTo: task.assignedTo != null ? String(task.assignedTo) : '',
      dueDate: task.dueDate || '',
      sprintId: sprintValueFromTask(task),
    });
  }, [task]);

  const handleSubmit = (event) => {
    event.preventDefault();
    onSubmit(form);
  };

  if (!task) return null;

  const priorityKey = (form.priority || 'MEDIUM').toLowerCase();

  return (
    <div className="member-modal-overlay" onClick={onClose}>
      <div
        className={`member-modal member-modal--form task-edit-modal task-edit-modal--${priorityKey}`}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        aria-labelledby="task-edit-kicker"
        aria-modal="true"
      >
        <button type="button" className="member-modal__close" onClick={onClose}>
          <span className="material-icons">close</span>
        </button>

        <header className="task-edit-modal__hero">
          <div className={`task-edit-modal__avatar task-edit-modal__avatar--${priorityKey}`}>
            {taskInitials(form.title)}
          </div>
        </header>

        <form className="task-edit-modal__form" onSubmit={handleSubmit}>
          <p id="task-edit-kicker" className="task-edit-modal__kicker">Editar tarea</p>
          <div className="task-edit-modal__badges">
            <StatusBadge status={form.status} />
            <PriorityBadge priority={form.priority} />
          </div>

          <div className="task-edit-modal__fields">
            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="task-edit-title-input">
                <span className="material-icons task-edit-modal__label-icon">edit_note</span>
                Título
              </label>
              <input
                id="task-edit-title-input"
                type="text"
                className="task-edit-modal__input"
                value={form.title}
                onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
                required
              />
            </div>

            <div className="task-edit-modal__row-2">
              <div className="task-edit-modal__field">
                <label className="task-edit-modal__label" htmlFor="task-edit-priority">
                  <span className="material-icons task-edit-modal__label-icon">flag</span>
                  Prioridad
                </label>
                <select
                  id="task-edit-priority"
                  className="task-edit-modal__select"
                  value={form.priority}
                  onChange={(e) => setForm((prev) => ({ ...prev, priority: e.target.value }))}
                >
                  {PRIORITY_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="task-edit-modal__field">
                <label className="task-edit-modal__label" htmlFor="task-edit-status">
                  <span className="material-icons task-edit-modal__label-icon">sync</span>
                  Estado
                </label>
                <select
                  id="task-edit-status"
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

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="task-edit-assignee">
                <span className="material-icons task-edit-modal__label-icon">person</span>
                Responsable
              </label>
              <select
                id="task-edit-assignee"
                className="task-edit-modal__select"
                value={form.assignedTo}
                onChange={(e) => setForm((prev) => ({ ...prev, assignedTo: e.target.value }))}
              >
                <option value="">Sin asignar</option>
                {assignableUsers.map((user) => (
                  <option key={user.userId} value={user.userId}>
                    {user.fullName}
                  </option>
                ))}
              </select>
            </div>

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="task-edit-due">
                <span className="material-icons task-edit-modal__label-icon">event</span>
                Fecha límite
              </label>
              <input
                id="task-edit-due"
                type="date"
                className="task-edit-modal__input"
                value={form.dueDate}
                onChange={(e) => setForm((prev) => ({ ...prev, dueDate: e.target.value }))}
                required
              />
            </div>

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="task-edit-sprint">
                <span className="material-icons task-edit-modal__label-icon">bolt</span>
                Sprint
              </label>
              <select
                id="task-edit-sprint"
                className="task-edit-modal__select"
                value={form.sprintId}
                onChange={(e) => setForm((prev) => ({ ...prev, sprintId: e.target.value }))}
              >
                <option value="BACKLOG">Backlog</option>
                {sprints.map((sprint) => (
                  <option key={sprint.sprintId} value={String(sprint.sprintId)}>
                    {sprintLabel ? sprintLabel(sprint) : sprint.name}
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

export default TaskEditModal;
