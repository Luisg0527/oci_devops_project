import React, { useEffect, useState, useMemo, useRef, useCallback } from 'react';
import PageLayout from '../components/layout/PageLayout';
import PriorityBadge from '../components/common/PriorityBadge';
import StatusBadge from '../components/common/StatusBadge';
import { API_BASE } from '../config/apiBase';
import './Backlog.css';

const ITEMS_PER_PAGE = 5;
const PRIORITY_ORDER = { HIGH: 0, MEDIUM: 1, LOW: 2 };

/** Normaliza TaskResponse del API al estado de la tabla */
function mapTaskFromApi(task) {
  let due = task.dueDate;
  if (due && typeof due === 'string' && due.includes('T')) {
    due = due.split('T')[0];
  }
  return {
    taskId: task.taskId,
    title: task.title || 'Sin titulo',
    priority: task.priority || 'MEDIUM',
    status: task.status || 'PENDING',
    sprintId: task.sprintId ?? null,
    assignedTo: task.assignedTo ?? null,
    assignedToName: task.assigneeName || 'Sin asignar',
    dueDate: due || '',
    taskStage: task.taskStage || 'BACKLOG',
  };
}

function authJsonHeaders() {
  const token = localStorage.getItem('authToken');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

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
const SPRINT_STATUS_OPTIONS = [
  { value: 'PLANNED', label: 'Planeado' },
  { value: 'ACTIVE', label: 'Activo' },
  { value: 'CLOSED', label: 'Cerrado' },
];

function parseDateStr(str) {
  if (!str) return Infinity;
  const d = new Date(str);
  return isNaN(d.getTime()) ? Infinity : d.getTime();
}

/** Valor para input type="date" desde API (LocalDate o string). */
function toInputDate(val) {
  if (val == null || val === '') return '';
  if (typeof val === 'string') {
    return val.includes('T') ? val.split('T')[0] : val.slice(0, 10);
  }
  return String(val).slice(0, 10);
}

function Backlog() {
  const [tasks, setTasks] = useState([]);
  const [filterPriority, setFilterPriority] = useState('All');
  const [filterStatus, setFilterStatus] = useState('All');
  /** All | userId de alguien con tarea asignada en la vista actual */
  const [filterAssignee, setFilterAssignee] = useState('All');
  const [page, setPage] = useState(1);
  const [showNewTask, setShowNewTask] = useState(false);
  const [newTask, setNewTask] = useState({ title: '', priority: 'MEDIUM', assignedTo: '', dueDate: '' });
  const [formError, setFormError] = useState('');
  /** null = cerrado; create | edit = modal de sprint */
  const [sprintModalMode, setSprintModalMode] = useState(null);
  const [editingSprintId, setEditingSprintId] = useState(null);
  const [sprintModalSaving, setSprintModalSaving] = useState(false);
  const [newSprint, setNewSprint] = useState({ name: '', startDate: '', endDate: '', status: 'PLANNED' });
  const [sprintError, setSprintError] = useState('');
  const [sprints, setSprints] = useState([]);
  /** ALL = sin filtrar por sprint; BACKLOG = solo tareas en backlog; id numérico = ese sprint */
  const [selectedSprintId, setSelectedSprintId] = useState('ALL');
  /** Solo la primera vez que llegan sprints: fija filtro al sprint activo (si existe). */
  const defaultActiveSprintAppliedRef = useRef(false);
  const [sortBy, setSortBy] = useState(null);
  const [sortDir, setSortDir] = useState('asc');
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [taskActionError, setTaskActionError] = useState('');
  /** Usuarios reales del equipo (mismo criterio que Team Management) */
  const [assignableUsers, setAssignableUsers] = useState([]);

  const loadAssignableUsers = useCallback(async () => {
    const token = localStorage.getItem('authToken');
    if (!token) {
      setAssignableUsers([]);
      return;
    }
    const headers = { Authorization: `Bearer ${token}` };
    try {
      let teamId = localStorage.getItem('userTeamId');
      if (!teamId) {
        const res = await fetch(`${API_BASE}/teams`, { headers });
        if (res.ok) {
          const body = await res.json().catch(() => ({}));
          const teams = Array.isArray(body.data) ? body.data : [];
          if (teams.length === 1) {
            teamId = String(teams[0].teamId);
            localStorage.setItem('userTeamId', teamId);
          }
        }
      }
      if (!teamId) {
        setAssignableUsers([]);
        return;
      }
      const usersRes = await fetch(
        `${API_BASE}/users?teamId=${encodeURIComponent(teamId)}&page=1&limit=200`,
        { headers }
      );
      if (usersRes.status === 401) {
        localStorage.removeItem('authToken');
        window.location.assign('/login');
        return;
      }
      const usersBody = await usersRes.json().catch(() => ({}));
      if (!usersRes.ok) {
        setAssignableUsers([]);
        return;
      }
      const rows = Array.isArray(usersBody.data) ? usersBody.data : [];
      setAssignableUsers(
        rows.map((u) => ({
          userId: u.userId,
          fullName: u.fullName || u.username || `Usuario ${u.userId}`,
        }))
      );
    } catch {
      setAssignableUsers([]);
    }
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('authToken');
    if (token) {
      loadAssignableUsers();
    }
  }, [loadAssignableUsers]);

  useEffect(() => {
    if (showNewTask) {
      loadAssignableUsers();
    }
  }, [showNewTask, loadAssignableUsers]);

  useEffect(() => {
    const projectId = localStorage.getItem('currentProjectId');
    const token = localStorage.getItem('authToken');
    if (!token) {
      setLoadError('Inicia sesion para cargar el backlog.');
      return;
    }
    if (!projectId) {
      setLoadError('Selecciona un proyecto para cargar los sprints.');
      return;
    }

    const loadSprints = async () => {
      try {
        const sprintResponse = await fetch(`${API_BASE}/sprints?project_id=${projectId}&page=1&limit=50`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        const sprintPayload = await sprintResponse.json().catch(() => ({}));
        if (!sprintResponse.ok) {
          throw new Error(sprintPayload.error || 'No fue posible cargar sprints.');
        }
        const loadedSprints = Array.isArray(sprintPayload.data) ? sprintPayload.data : [];
        setSprints(loadedSprints);

        const activeSprint = loadedSprints.find((s) => s.isActive || s.status === 'ACTIVE') || null;

        if (!defaultActiveSprintAppliedRef.current) {
          defaultActiveSprintAppliedRef.current = true;
          if (activeSprint) {
            setSelectedSprintId(String(activeSprint.sprintId));
          } else {
            setSelectedSprintId('ALL');
          }
        } else if (
          selectedSprintId !== 'BACKLOG' &&
          selectedSprintId !== 'ALL' &&
          !loadedSprints.some((sprint) => String(sprint.sprintId) === String(selectedSprintId))
        ) {
          setSelectedSprintId(activeSprint ? String(activeSprint.sprintId) : 'ALL');
        }
      } catch (err) {
        setLoadError(err.message || 'No fue posible cargar sprints.');
      }
    };

    loadSprints();
  }, [selectedSprintId]);

  useEffect(() => {
    const projectId = localStorage.getItem('currentProjectId');
    const token = localStorage.getItem('authToken');
    if (!token || !projectId) {
      setIsLoading(false);
      return;
    }

    const loadTasks = async () => {
      try {
        setLoadError('');
        setIsLoading(true);
        const params = new URLSearchParams({
          page: '1',
          limit: '200',
          project_id: projectId,
        });
        if (selectedSprintId === 'BACKLOG') {
          params.set('task_stage', 'BACKLOG');
        } else if (selectedSprintId !== 'ALL') {
          params.set('sprint_id', selectedSprintId);
        }

        const response = await fetch(`${API_BASE}/tasks?${params.toString()}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
          throw new Error(payload.error || 'No fue posible cargar tareas.');
        }

        const normalized = (payload.data || []).map(mapTaskFromApi);
        setTasks(normalized);
      } catch (err) {
        setLoadError(err.message || 'No fue posible cargar tareas.');
      } finally {
        setIsLoading(false);
      }
    };

    loadTasks();
  }, [selectedSprintId]);

  /** Integrantes que tienen al menos una tarea asignada en la lista cargada */
  const assigneeFilterOptions = useMemo(() => {
    const byId = new Map();
    tasks.forEach((t) => {
      if (t.assignedTo == null) return;
      const id = String(t.assignedTo);
      if (!byId.has(id)) {
        byId.set(id, {
          userId: id,
          fullName: t.assignedToName || `Usuario ${id}`,
        });
      }
    });
    return Array.from(byId.values()).sort((a, b) =>
      a.fullName.localeCompare(b.fullName, 'es', { sensitivity: 'base' })
    );
  }, [tasks]);

  useEffect(() => {
    if (filterAssignee === 'All') return;
    const stillValid = assigneeFilterOptions.some((o) => o.userId === String(filterAssignee));
    if (!stillValid) setFilterAssignee('All');
  }, [assigneeFilterOptions, filterAssignee]);

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
    const base = tasks.filter((t) =>
      (filterPriority === 'All' || t.priority === filterPriority) &&
      (filterStatus === 'All' || t.status === filterStatus) &&
      (filterAssignee === 'All' || String(t.assignedTo) === String(filterAssignee))
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
  }, [tasks, filterPriority, filterStatus, filterAssignee, sortBy, sortDir]);

  const activeSprint = useMemo(
    () => sprints.find((sprint) => sprint.isActive || sprint.status === 'ACTIVE') || null,
    [sprints]
  );

  const otherSprints = useMemo(() => {
    return sprints
      .filter((sprint) => !activeSprint || sprint.sprintId !== activeSprint.sprintId)
      .sort((a, b) => (b.sprintNumber || 0) - (a.sprintNumber || 0));
  }, [sprints, activeSprint]);

  const sprintTitle = (sprint) => {
    if (!sprint) return 'Sin sprint activo';
    if (sprint.sprintNumber) return `Sprint ${sprint.sprintNumber}: ${sprint.name}`;
    return sprint.name;
  };

  const sprintStatusLabel = (status) => {
    if (status === 'ACTIVE') return 'EN CURSO';
    if (status === 'CLOSED') return 'FINALIZADO';
    return 'PLANEADO';
  };

  const totalPages = Math.max(1, Math.ceil(sorted.length / ITEMS_PER_PAGE));
  const paginated = sorted.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE);

  const formatDate = (isoDate) => {
    if (!isoDate) return 'Por definir';
    const d = new Date(isoDate + 'T00:00:00');
    return d.toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  const handleAddTask = async (e) => {
    e.preventDefault();
    const missing = [];
    if (!newTask.title.trim()) missing.push('Título');
    if (!newTask.dueDate) missing.push('Fecha límite');
    if (missing.length > 0) {
      setFormError(`Campos requeridos: ${missing.join(', ')}`);
      return;
    }
    const projectId = localStorage.getItem('currentProjectId');
    const creatorId = localStorage.getItem('userId');
    if (!projectId) {
      setFormError('Selecciona un proyecto en la barra superior.');
      return;
    }
    if (!creatorId) {
      setFormError('Vuelve a iniciar sesión para crear tareas.');
      return;
    }
    const token = localStorage.getItem('authToken');
    if (!token) {
      setFormError('Inicia sesión para crear tareas.');
      return;
    }

    const assignedUserId = Number(newTask.assignedTo);
    const sprintNumeric =
      selectedSprintId !== 'ALL' &&
      selectedSprintId !== 'BACKLOG' &&
      selectedSprintId !== ''
        ? Number(selectedSprintId)
        : null;
    const body = {
      projectId: Number(projectId),
      title: newTask.title.trim(),
      description: '',
      priority: newTask.priority,
      status: 'PENDING',
      taskStage: sprintNumeric ? 'SPRINT' : 'BACKLOG',
      createdBy: Number(creatorId),
      dueDate: newTask.dueDate,
    };
    if (sprintNumeric) {
      body.sprintId = sprintNumeric;
    }
    if (!Number.isNaN(assignedUserId) && newTask.assignedTo) {
      body.assignedTo = assignedUserId;
    }

    setFormError('');
    try {
      const response = await fetch(`${API_BASE}/tasks`, {
        method: 'POST',
        headers: authJsonHeaders(),
        body: JSON.stringify(body),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || 'No fue posible crear la tarea.');
      }
      const normalized = mapTaskFromApi(payload);
      setTasks((prev) => [normalized, ...prev]);
      setNewTask({ title: '', priority: 'MEDIUM', assignedTo: '', dueDate: '' });
      setShowNewTask(false);
      setPage(1);
      setTaskActionError('');
    } catch (err) {
      setFormError(err.message || 'No fue posible crear la tarea.');
    }
  };

  const reloadSprintsList = useCallback(async () => {
    const projectId = localStorage.getItem('currentProjectId');
    const token = localStorage.getItem('authToken');
    if (!token || !projectId) return;
    try {
      const sprintResponse = await fetch(
        `${API_BASE}/sprints?project_id=${projectId}&page=1&limit=50`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const sprintPayload = await sprintResponse.json().catch(() => ({}));
      if (!sprintResponse.ok) return;
      const loadedSprints = Array.isArray(sprintPayload.data) ? sprintPayload.data : [];
      setSprints(loadedSprints);
    } catch (_) {
      /* refresh silencioso */
    }
  }, []);

  const openSprintModalCreate = () => {
    setSprintModalMode('create');
    setEditingSprintId(null);
    setNewSprint({ name: '', startDate: '', endDate: '', status: 'PLANNED' });
    setSprintError('');
  };

  const openSprintModalEdit = (sprint) => {
    if (!sprint || sprint.sprintId == null) return;
    setSprintModalMode('edit');
    setEditingSprintId(sprint.sprintId);
    setNewSprint({
      name: sprint.name || '',
      startDate: toInputDate(sprint.startDate),
      endDate: toInputDate(sprint.endDate),
      status: sprint.status || 'PLANNED',
    });
    setSprintError('');
  };

  const closeSprintModal = () => {
    if (sprintModalSaving) return;
    setSprintModalMode(null);
    setEditingSprintId(null);
    setSprintError('');
  };

  useEffect(() => {
    if (!sprintModalMode) return;
    const onKey = (e) => {
      if (e.key !== 'Escape') return;
      if (sprintModalSaving) return;
      setSprintModalMode(null);
      setEditingSprintId(null);
      setSprintError('');
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [sprintModalMode, sprintModalSaving]);

  const submitSprintModal = async (event) => {
    event.preventDefault();
    const token = localStorage.getItem('authToken');
    const projectId = localStorage.getItem('currentProjectId');
    const missing = [];
    if (!newSprint.name.trim()) missing.push('Nombre');
    if (!newSprint.startDate) missing.push('Fecha inicio');
    if (!newSprint.endDate) missing.push('Fecha fin');
    if (!projectId) missing.push('Proyecto seleccionado');
    if (missing.length > 0) {
      setSprintError(`Campos requeridos: ${missing.join(', ')}`);
      return;
    }
    if (!token) {
      setSprintError('Inicia sesión para guardar el sprint.');
      return;
    }
    if (newSprint.endDate < newSprint.startDate) {
      setSprintError('La fecha fin debe ser mayor o igual que la fecha inicio.');
      return;
    }

    try {
      setSprintError('');
      setSprintModalSaving(true);
      if (sprintModalMode === 'create') {
        const response = await fetch(`${API_BASE}/sprints`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            name: newSprint.name.trim(),
            startDate: newSprint.startDate,
            endDate: newSprint.endDate,
            status: newSprint.status || undefined,
            projectId: Number(projectId),
          }),
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
          throw new Error(payload.error || 'No fue posible crear sprint.');
        }
        await reloadSprintsList();
        setSelectedSprintId(String(payload.sprintId));
        setNewSprint({ name: '', startDate: '', endDate: '', status: 'PLANNED' });
        setSprintModalMode(null);
        setEditingSprintId(null);
      } else if (sprintModalMode === 'edit' && editingSprintId != null) {
        const response = await fetch(`${API_BASE}/sprints/${editingSprintId}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            name: newSprint.name.trim(),
            startDate: newSprint.startDate,
            endDate: newSprint.endDate,
            status: newSprint.status,
          }),
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
          throw new Error(payload.error || 'No fue posible actualizar el sprint.');
        }
        await reloadSprintsList();
        setSprintModalMode(null);
        setEditingSprintId(null);
      }
    } catch (err) {
      setSprintError(err.message || 'No fue posible guardar el sprint.');
    } finally {
      setSprintModalSaving(false);
    }
  };

  const handleDeleteTask = async (taskId) => {
    const token = localStorage.getItem('authToken');
    if (!token) {
      setTaskActionError('Inicia sesión para eliminar tareas.');
      return;
    }
    try {
      setTaskActionError('');
      const response = await fetch(`${API_BASE}/tasks/${taskId}`, {
        method: 'DELETE',
        headers: authJsonHeaders(),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || payload.message || 'No fue posible eliminar la tarea.');
      }
      setTasks((prev) => prev.filter((t) => t.taskId !== taskId));
    } catch (err) {
      setTaskActionError(err.message || 'No fue posible eliminar la tarea.');
    }
  };

  const handleStatusChange = async (taskId, newStatus) => {
    const token = localStorage.getItem('authToken');
    if (!token) {
      setTaskActionError('Inicia sesión para actualizar tareas.');
      return;
    }
    try {
      setTaskActionError('');
      const response = await fetch(`${API_BASE}/tasks/${taskId}`, {
        method: 'PUT',
        headers: authJsonHeaders(),
        body: JSON.stringify({ status: newStatus }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || 'No fue posible actualizar el estado.');
      }
      const normalized = mapTaskFromApi(payload);
      setTasks((prev) => prev.map((t) => (t.taskId === taskId ? normalized : t)));
    } catch (err) {
      setTaskActionError(err.message || 'No fue posible actualizar el estado.');
    }
  };

  const handlePriorityChange = async (taskId, newPriority) => {
    const token = localStorage.getItem('authToken');
    if (!token) {
      setTaskActionError('Inicia sesión para actualizar tareas.');
      return;
    }
    try {
      setTaskActionError('');
      const response = await fetch(`${API_BASE}/tasks/${taskId}`, {
        method: 'PUT',
        headers: authJsonHeaders(),
        body: JSON.stringify({ priority: newPriority }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.error || 'No fue posible actualizar la prioridad.');
      }
      const normalized = mapTaskFromApi(payload);
      setTasks((prev) => prev.map((t) => (t.taskId === taskId ? normalized : t)));
    } catch (err) {
      setTaskActionError(err.message || 'No fue posible actualizar la prioridad.');
    }
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
            <select
              className="backlog-select backlog-select--assignee"
              value={filterAssignee}
              onChange={(e) => {
                setFilterAssignee(e.target.value);
                setPage(1);
              }}
              title="Filtrar por persona asignada"
              aria-label="Filtrar por integrante"
            >
              <option value="All">Todos los integrantes</option>
              {assigneeFilterOptions.map((opt) => (
                <option key={opt.userId} value={opt.userId}>
                  {opt.fullName}
                </option>
              ))}
            </select>
          </div>
          <button className="btn btn--primary" onClick={() => { setShowNewTask(!showNewTask); setFormError(''); }}>
            <span className="material-icons" style={{ fontSize: 18 }}>add</span>
            Nueva Tarea
          </button>
        </div>
      </section>

      {loadError && (
        <div className="card mt-16" style={{ padding: 12, color: '#b42318' }}>
          {loadError}
        </div>
      )}

      {taskActionError && (
        <div className="card mt-16" style={{ padding: 12, color: '#b42318' }}>
          {taskActionError}
        </div>
      )}

      {sprintModalMode && (
        <div
          className="backlog-sprint-modal-overlay"
          role="presentation"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget && !sprintModalSaving) closeSprintModal();
          }}
        >
          <div
            className="backlog-sprint-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="backlog-sprint-modal-title"
          >
            <button
              type="button"
              className="backlog-sprint-modal__close"
              aria-label="Cerrar"
              disabled={sprintModalSaving}
              onClick={closeSprintModal}
            >
              <span className="material-icons">close</span>
            </button>
            <div className="backlog-sprint-modal__header">
              <h3 id="backlog-sprint-modal-title" className="backlog-sprint-modal__title">
                {sprintModalMode === 'edit' ? 'Editar sprint' : 'Crear sprint'}
              </h3>
              <div className="backlog-sprint-modal__title-accent" aria-hidden />
            </div>
            <form className="backlog-sprint-modal__form" onSubmit={submitSprintModal}>
              <div className="backlog-sprint-modal__field">
                <label className="backlog-sprint-modal__label" htmlFor="backlog-sprint-name">
                  Nombre del sprint
                </label>
                <input
                  id="backlog-sprint-name"
                  className="backlog-sprint-modal__input"
                  type="text"
                  value={newSprint.name}
                  onChange={(e) => setNewSprint((p) => ({ ...p, name: e.target.value }))}
                  placeholder="Nombre del sprint"
                  disabled={sprintModalSaving}
                  autoFocus
                />
              </div>
              <div className="backlog-sprint-modal__row">
                <div className="backlog-sprint-modal__field">
                  <label className="backlog-sprint-modal__label" htmlFor="backlog-sprint-start">
                    Fecha inicio
                  </label>
                  <div className="backlog-sprint-modal__input-wrap">
                    <input
                      id="backlog-sprint-start"
                      className="backlog-sprint-modal__input"
                      type="date"
                      value={newSprint.startDate}
                      onChange={(e) => setNewSprint((p) => ({ ...p, startDate: e.target.value }))}
                      disabled={sprintModalSaving}
                    />
                    <span className="material-icons backlog-sprint-modal__input-icon" aria-hidden>
                      calendar_today
                    </span>
                  </div>
                </div>
                <div className="backlog-sprint-modal__field">
                  <label className="backlog-sprint-modal__label" htmlFor="backlog-sprint-end">
                    Fecha fin
                  </label>
                  <div className="backlog-sprint-modal__input-wrap">
                    <input
                      id="backlog-sprint-end"
                      className="backlog-sprint-modal__input"
                      type="date"
                      value={newSprint.endDate}
                      onChange={(e) => setNewSprint((p) => ({ ...p, endDate: e.target.value }))}
                      disabled={sprintModalSaving}
                    />
                    <span className="material-icons backlog-sprint-modal__input-icon" aria-hidden>
                      event
                    </span>
                  </div>
                </div>
              </div>
              <div className="backlog-sprint-modal__field">
                <label className="backlog-sprint-modal__label" htmlFor="backlog-sprint-status">
                  Estado
                </label>
                <div className="backlog-sprint-modal__select-wrap">
                  <select
                    id="backlog-sprint-status"
                    className="backlog-sprint-modal__input backlog-sprint-modal__select"
                    value={newSprint.status}
                    onChange={(e) => setNewSprint((p) => ({ ...p, status: e.target.value }))}
                    disabled={sprintModalSaving}
                  >
                    {SPRINT_STATUS_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <span className="material-icons backlog-sprint-modal__input-icon" aria-hidden>
                    expand_more
                  </span>
                </div>
              </div>
              {sprintError && (
                <div className="backlog-sprint-modal__error" role="alert">
                  <span className="material-icons" style={{ fontSize: 18 }}>warning</span>
                  {sprintError}
                </div>
              )}
              <div className="backlog-sprint-modal__actions">
                <button
                  type="button"
                  className="backlog-sprint-modal__btn backlog-sprint-modal__btn--ghost"
                  disabled={sprintModalSaving}
                  onClick={closeSprintModal}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="backlog-sprint-modal__btn backlog-sprint-modal__btn--primary"
                  disabled={sprintModalSaving}
                >
                  {sprintModalSaving
                    ? 'Guardando…'
                    : sprintModalMode === 'edit'
                    ? 'Guardar cambios'
                    : 'Crear sprint'}
                </button>
              </div>
            </form>
            <div className="backlog-sprint-modal__footer-bar" aria-hidden />
          </div>
        </div>
      )}

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
            {assignableUsers.map((m) => (
              <option key={m.userId} value={m.userId}>{m.fullName}</option>
            ))}
          </select>
          {showNewTask && assignableUsers.length === 0 && (
            <span className="text-sm text-muted" style={{ alignSelf: 'center', whiteSpace: 'nowrap' }}>
              Sin usuarios del equipo: revisa Gestión de equipo o tu asignación a un equipo.
            </span>
          )}
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
            {isLoading && (
              <tr>
                <td colSpan={6} className="text-sm text-muted">Cargando backlog...</td>
              </tr>
            )}
            {!isLoading && paginated.length === 0 && (
              <tr>
                <td colSpan={6} className="text-sm text-muted">
                  {selectedSprintId === 'BACKLOG'
                    ? 'No hay tareas en backlog.'
                    : 'No hay tareas que coincidan con el filtro.'}
                </td>
              </tr>
            )}
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
                      {(task.assignedToName || 'SA').split(' ').map(n => n[0]).join('')}
                    </div>
                    <span>{task.assignedToName || 'Sin asignar'}</span>
                  </div>
                </td>
                <td className="text-sm">{formatDate(task.dueDate)}</td>
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

      <section className="card card--dark mt-24 backlog-sprint-board">
        <div className="backlog-sprint-board__current">
          <div className="backlog-sprint-board__current-head">
            <span className="backlog-sprint-board__kicker">Sprint actual</span>
            {activeSprint && (
              <button
                type="button"
                className="backlog-sprint-board__edit-link"
                onClick={() => openSprintModalEdit(activeSprint)}
              >
                Editar sprint
              </button>
            )}
          </div>
          <button
            type="button"
            className={`backlog-sprint-board__main ${(
              selectedSprintId === 'ALL' ||
              selectedSprintId === 'BACKLOG' ||
              (activeSprint && selectedSprintId === String(activeSprint.sprintId))
            ) ? 'is-active' : ''}`}
            onClick={() => {
              if (!activeSprint) {
                setSelectedSprintId((prev) => (prev === 'BACKLOG' ? 'ALL' : 'BACKLOG'));
                return;
              }
              const activeId = String(activeSprint.sprintId);
              if (selectedSprintId === 'ALL') {
                setSelectedSprintId(activeId);
              } else if (selectedSprintId === activeId) {
                setSelectedSprintId('BACKLOG');
              } else if (selectedSprintId === 'BACKLOG') {
                setSelectedSprintId('ALL');
              } else {
                setSelectedSprintId(activeId);
              }
            }}
          >
            <h3>{sprintTitle(activeSprint)}</h3>
              {(selectedSprintId === 'BACKLOG' ||
                (activeSprint && selectedSprintId === String(activeSprint.sprintId))) && (
                <span className="backlog-sprint-board__active-pill">Filtrando</span>
              )}
              {selectedSprintId === 'ALL' && (
                <span className="backlog-sprint-board__active-pill">Todas las tareas</span>
              )}
            <p className="backlog-sprint-board__status">
              {activeSprint
                ? sprintStatusLabel(activeSprint.status)
                : selectedSprintId === 'ALL'
                  ? 'Sin filtro de sprint'
                  : 'Mostrando tareas en backlog'}
            </p>
          </button>
          <div className="backlog-sprint-board__progress-wrap">
            <div className="backlog-sprint-board__progress-label">
              <span>Progreso general</span>
              <strong>{activeSprint?.velocityPercent ?? 0}%</strong>
            </div>
            <div className="backlog-sprint-board__progress-track">
              <div
                className="backlog-sprint-board__progress-fill"
                style={{ width: `${Math.max(0, Math.min(100, activeSprint?.velocityPercent ?? 0))}%` }}
              />
            </div>
          </div>
        </div>

        <div className="backlog-sprint-board__others">
          <span className="backlog-sprint-board__kicker">Otros sprints</span>
          <div className="backlog-sprint-board__list">
            {otherSprints.slice(0, 2).map((sprint) => (
              <div key={sprint.sprintId} className="backlog-sprint-board__item-row">
                <button
                  type="button"
                  className={`backlog-sprint-board__item ${selectedSprintId === String(sprint.sprintId) ? 'is-active' : ''}`}
                  onClick={() => setSelectedSprintId(String(sprint.sprintId))}
                >
                  <div>
                    <strong>{sprintTitle(sprint)}</strong>
                    {selectedSprintId === String(sprint.sprintId) && (
                      <span className="backlog-sprint-board__active-pill">Filtrando</span>
                    )}
                    <span className="backlog-sprint-board__status">{sprintStatusLabel(sprint.status)}</span>
                  </div>
                  <em>{sprint.velocityPercent ?? 0}%</em>
                </button>
                <button
                  type="button"
                  className="backlog-sprint-board__edit-link backlog-sprint-board__edit-link--side"
                  onClick={() => openSprintModalEdit(sprint)}
                >
                  Editar
                </button>
              </div>
            ))}
            {otherSprints.length === 0 && (
              <div className="backlog-sprint-board__empty">No hay otros sprints registrados.</div>
            )}
          </div>
          <button
            type="button"
            className="backlog-sprint-board__add"
            onClick={() => {
              openSprintModalCreate();
            }}
          >
            <span className="material-icons" style={{ fontSize: 16 }}>add</span>
            Añadir sprint
          </button>
        </div>
      </section>

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
