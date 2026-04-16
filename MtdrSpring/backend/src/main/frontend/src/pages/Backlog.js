import React, { useState, useMemo } from 'react';
import PageLayout from '../components/layout/PageLayout';
import PriorityBadge from '../components/common/PriorityBadge';
import StatusBadge from '../components/common/StatusBadge';
import { backlogTasks, teamMembers } from '../data/mockData';
import './Backlog.css';

const ITEMS_PER_PAGE = 5;
const PRIORITY_ORDER = { HIGH: 0, MEDIUM: 1, LOW: 2 };

const TASK_STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Pendiente' },
  { value: 'IN_PROGRESS', label: 'En Progreso' },
  { value: 'DONE', label: 'Completado' },
  { value: 'CANCELLED', label: 'Cancelado' },
  { value: 'REOPENED', label: 'Reabierto' },
];
const TASK_PRIORITY_OPTIONS = [
  { value: 'HIGH', label: 'Alta' },
  { value: 'MEDIUM', label: 'Media' },
  { value: 'LOW', label: 'Baja' },
];

function taskStageForStatus(status) {
  if (status === 'DONE') return 'COMPLETED';
  if (status === 'IN_PROGRESS') return 'SPRINT';
  return 'BACKLOG';
}

function parseDateStr(str) {
  if (!str) return Infinity;
  const d = new Date(str);
  return isNaN(d.getTime()) ? Infinity : d.getTime();
}

function Backlog() {
  const [tasks, setTasks] = useState(backlogTasks);
  const [filterPriority, setFilterPriority] = useState('All');
  const [filterStatus, setFilterStatus] = useState('All');
  const [page, setPage] = useState(1);
  const [showNewTask, setShowNewTask] = useState(false);
  const [newTask, setNewTask] = useState({ title: '', priority: 'MEDIUM', assignedTo: '', dueDate: '' });
  const [formError, setFormError] = useState('');
  const [sortBy, setSortBy] = useState(null);
  const [sortDir, setSortDir] = useState('asc');

  const handleSort = (column) => {
    if (sortBy === column) {
      setSortDir(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDir('asc');
    }
    setPage(1);
  };

  const sorted = useMemo(() => {
    const base = tasks.filter(t =>
      (filterPriority === 'All' || t.priority === filterPriority) &&
      (filterStatus === 'All' || t.status === filterStatus)
    );
    if (!sortBy) return base;
    const dir = sortDir === 'asc' ? 1 : -1;
    return [...base].sort((a, b) => {
      if (sortBy === 'priority') {
        return ((PRIORITY_ORDER[a.priority] ?? 3) - (PRIORITY_ORDER[b.priority] ?? 3)) * dir;
      }
      if (sortBy === 'dueDate') {
        return (parseDateStr(a.dueDate) - parseDateStr(b.dueDate)) * dir;
      }
      return 0;
    });
  }, [tasks, filterPriority, filterStatus, sortBy, sortDir]);

  const totalPages = Math.max(1, Math.ceil(sorted.length / ITEMS_PER_PAGE));
  const paginated = sorted.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE);

  const formatDate = (isoDate) => {
    if (!isoDate) return 'Por definir';
    const d = new Date(isoDate + 'T00:00:00');
    return d.toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  const handleAddTask = (e) => {
    e.preventDefault();
    const missing = [];
    if (!newTask.title.trim()) missing.push('Título');
    if (!newTask.assignedTo) missing.push('Responsable');
    if (!newTask.dueDate) missing.push('Fecha límite');
    if (missing.length > 0) {
      setFormError(`Campos requeridos: ${missing.join(', ')}`);
      return;
    }
    setFormError('');
    const assignedUserId = Number(newTask.assignedTo);
    const member = teamMembers.find(m => m.userId === assignedUserId);
    const task = {
      taskId: Date.now(),
      projectId: null,
      sprintId: null,
      title: newTask.title,
      description: '',
      taskStage: 'BACKLOG',
      status: 'PENDING',
      priority: newTask.priority,
      createdBy: 7,
      assignedTo: assignedUserId,
      assignedToName: member ? member.fullName : 'Sin asignar',
      dueDate: formatDate(newTask.dueDate),
      createdAt: new Date().toISOString(),
    };
    setTasks(prev => [task, ...prev]);
    setNewTask({ title: '', priority: 'MEDIUM', assignedTo: '', dueDate: '' });
    setShowNewTask(false);
    setPage(1);
  };

  const handleDeleteTask = (taskId) => {
    setTasks(prev => prev.filter(t => t.taskId !== taskId));
  };

  const handleStatusChange = (taskId, newStatus) => {
    setTasks(prev => prev.map(t =>
      t.taskId === taskId
        ? { ...t, status: newStatus, taskStage: taskStageForStatus(newStatus) }
        : t
    ));
  };
  const handlePriorityChange = (taskId, newPriority) => {
    setTasks(prev => prev.map(t =>
      t.taskId === taskId
        ? { ...t, priority: newPriority }
        : t
    ));
  };

  const overdueCount = tasks.filter(t => t.status !== 'DONE' && t.status !== 'CANCELLED').length;
  const doneCount = tasks.filter(t => t.status === 'DONE').length;
  const deliveryRate = tasks.length > 0 ? Math.round((doneCount / tasks.length) * 100) : 0;

  return (
    <PageLayout>
      {/* Header */}
      <section className="backlog-header">
        <div>
          <span className="section-label">Resumen del Proyecto</span>
          <h2 className="section-title">Backlog del Proyecto</h2>
        </div>
        <div className="backlog-header__actions">
          <div className="backlog-filters">
            <select
              className="backlog-select"
              value={filterPriority}
              onChange={e => { setFilterPriority(e.target.value); setPage(1); }}
            >
              <option value="All">Todas las Prioridades</option>
              <option value="HIGH">Alta</option>
              <option value="MEDIUM">Media</option>
              <option value="LOW">Baja</option>
            </select>
            <select
              className="backlog-select"
              value={filterStatus}
              onChange={e => { setFilterStatus(e.target.value); setPage(1); }}
            >
              <option value="All">Todos los Estados</option>
              <option value="IN_PROGRESS">En Progreso</option>
              <option value="PENDING">Pendiente</option>
              <option value="DONE">Completado</option>
              <option value="CANCELLED">Cancelado</option>
              <option value="REOPENED">Reabierto</option>
            </select>
          </div>
          <button className="btn btn--primary" onClick={() => { setShowNewTask(!showNewTask); setFormError(''); }}>
            <span className="material-icons" style={{ fontSize: 18 }}>add</span>
            Nueva Tarea
          </button>
        </div>
      </section>

      {/* New Task Form */}
      {showNewTask && (
        <form className="backlog-new-task card mt-16" onSubmit={handleAddTask}>
          <input
            type="text"
            className="backlog-input"
            placeholder="Título de la tarea..."
            value={newTask.title}
            onChange={e => setNewTask(p => ({ ...p, title: e.target.value }))}
            autoFocus
          />
          <select
            className="backlog-select"
            value={newTask.priority}
            onChange={e => setNewTask(p => ({ ...p, priority: e.target.value }))}
          >
            <option value="HIGH">Alta</option>
            <option value="MEDIUM">Media</option>
            <option value="LOW">Baja</option>
          </select>
          <select
            className="backlog-select"
            value={newTask.assignedTo}
            onChange={e => setNewTask(p => ({ ...p, assignedTo: e.target.value }))}
          >
            <option value="">Sin asignar</option>
            {teamMembers.map(m => (
              <option key={m.userId} value={m.userId}>{m.fullName}</option>
            ))}
          </select>
          <input
            type="date"
            className="backlog-input backlog-input--sm"
            value={newTask.dueDate}
            onChange={e => setNewTask(p => ({ ...p, dueDate: e.target.value }))}
          />
          <button type="submit" className="btn btn--primary btn--small">Agregar</button>
          <button type="button" className="btn btn--ghost btn--small" onClick={() => { setShowNewTask(false); setFormError(''); }}>Cancelar</button>
          {formError && (
            <span className="backlog-form-error">
              <span className="material-icons" style={{ fontSize: 16 }}>warning</span>
              {formError}
            </span>
          )}
        </form>
      )}

      {/* Table */}
      <div className="card mt-24" style={{ padding: 0, overflow: 'hidden' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Nombre de Tarea</th>
              <th className="backlog-th-sortable" onClick={() => handleSort('priority')}>
                Prioridad
                <span className="material-icons backlog-sort-icon">
                  {sortBy === 'priority' ? (sortDir === 'asc' ? 'arrow_upward' : 'arrow_downward') : 'unfold_more'}
                </span>
              </th>
              <th>Estado</th>
              <th>Responsable</th>
              <th className="backlog-th-sortable" onClick={() => handleSort('dueDate')}>
                Fecha Límite
                <span className="material-icons backlog-sort-icon">
                  {sortBy === 'dueDate' ? (sortDir === 'asc' ? 'arrow_upward' : 'arrow_downward') : 'unfold_more'}
                </span>
              </th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {paginated.map(task => (
              <tr key={task.taskId}>
                <td className="backlog-task-name">{task.title}</td>
                <td>
                  <div className="backlog-priority-cell" title="Cambiar prioridad">
                    <PriorityBadge priority={task.priority} />
                    <span className="material-icons backlog-priority-chevron" aria-hidden>expand_more</span>
                    <select
                      className="backlog-priority-select-overlay"
                      value={task.priority}
                      onChange={e => handlePriorityChange(task.taskId, e.target.value)}
                      aria-label="Prioridad de la tarea"
                    >
                      {TASK_PRIORITY_OPTIONS.map(opt => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                      ))}
                    </select>
                  </div>
                </td>
                <td>
                  <div className="backlog-status-cell" title="Cambiar estado">
                    <StatusBadge status={task.status} />
                    <span className="material-icons backlog-status-chevron" aria-hidden>expand_more</span>
                    <select
                      className="backlog-status-select-overlay"
                      value={task.status}
                      onChange={e => handleStatusChange(task.taskId, e.target.value)}
                      aria-label="Estado de la tarea"
                    >
                      {TASK_STATUS_OPTIONS.map(opt => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                      ))}
                    </select>
                  </div>
                </td>
                <td>
                  <div className="backlog-assignee">
                    <div className="backlog-assignee__avatar">
                      {task.assignedToName.split(' ').map(n => n[0]).join('')}
                    </div>
                    <span>{task.assignedToName}</span>
                  </div>
                </td>
                <td className="text-sm">{task.dueDate}</td>
                <td>
                  <button className="backlog-action-btn" onClick={() => handleDeleteTask(task.taskId)}>
                    <span className="material-icons">delete</span>
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Pagination */}
        <div className="backlog-pagination">
          <span className="text-sm text-muted">
            Mostrando {paginated.length} de {sorted.length} tareas
          </span>
          <div className="backlog-pagination__btns">
            <button
              className="backlog-page-btn"
              disabled={page <= 1}
              onClick={() => setPage(p => p - 1)}
            >
              <span className="material-icons">chevron_left</span>
            </button>
            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i + 1}
                className={`backlog-page-btn ${page === i + 1 ? 'backlog-page-btn--active' : ''}`}
                onClick={() => setPage(i + 1)}
              >
                {i + 1}
              </button>
            ))}
            <button
              className="backlog-page-btn"
              disabled={page >= totalPages}
              onClick={() => setPage(p => p + 1)}
            >
              <span className="material-icons">chevron_right</span>
            </button>
          </div>
        </div>
      </div>

      {/* Contextual Insights */}
      <div className="backlog-insights mt-24">
        <div className="card backlog-insight">
          <span className="text-sm text-muted font-bold">Velocidad Urgente</span>
          <div className="backlog-insight__value">
            <span className="backlog-insight__number">{overdueCount}</span>
            <span className="text-sm text-muted">tareas atrasadas esta semana</span>
          </div>
        </div>
        <div className="card card--dark backlog-insight backlog-insight--wide">
          <span className="text-sm font-bold" style={{ opacity: 0.7 }}>Salud del Proyecto</span>
          <div className="backlog-insight__row">
            <div className="backlog-insight__metric">
              <span className="backlog-insight__number">{deliveryRate}%</span>
              <span className="text-sm" style={{ opacity: 0.7 }}>Tasa de entrega a tiempo</span>
            </div>
            <div className="backlog-insight__metric">
              <span className="backlog-insight__number">{doneCount}</span>
              <span className="text-sm" style={{ opacity: 0.7 }}>Tickets resueltos</span>
            </div>
            <div className="backlog-insight__metric">
              <span className="backlog-insight__number">ELITE</span>
              <span className="text-sm" style={{ opacity: 0.7 }}>Calificación de Eficiencia</span>
            </div>
          </div>
        </div>
      </div>
    </PageLayout>
  );
}

export default Backlog;
