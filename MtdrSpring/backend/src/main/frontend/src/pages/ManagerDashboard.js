import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useHistory } from 'react-router-dom';
import PageLayout from '../components/layout/PageLayout';
import { API_BASE } from '../config/apiBase';
import './ManagerDashboard.css';

const KPI_SPRINT = 'CUMPLIMIENTO_SPRINT';
const KPI_DEPLOY = 'TASA_EXITO_DESPLIEGUES';

const HEALTH_LABEL_ES = {
  Excellent: 'Excelente',
  Good: 'Bueno',
  Fair: 'Regular',
  'At Risk': 'En riesgo',
};

const PROJECT_STATUS_ES = {
  ACTIVE: 'Activo',
  ON_HOLD: 'En pausa',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
};

const SEVERITY_LABEL_ES = {
  CRITICAL: 'Crítico',
  HIGH: 'Alto',
  MEDIUM: 'Medio',
  LOW: 'Bajo',
};

function latestKpiByName(data, kpiName) {
  const list = (data || []).filter((k) => k.kpiName === kpiName);
  if (!list.length) return null;
  return list.sort((a, b) => new Date(b.recordedAt || 0) - new Date(a.recordedAt || 0))[0];
}

function healthBorderClass(label) {
  const l = label || '';
  if (l === 'Excellent') return 'mdash-portfolio-card--accent-charcoal';
  if (l === 'Good') return 'mdash-portfolio-card--accent-gold';
  if (l === 'Fair') return 'mdash-portfolio-card--accent-gold';
  return 'mdash-portfolio-card--accent-red';
}

function healthTextClass(label) {
  const l = label || '';
  if (l === 'Excellent') return 'mdash-text-charcoal';
  if (l === 'Good') return 'mdash-text-gold';
  if (l === 'Fair') return 'mdash-text-red';
  return 'mdash-text-red';
}

function riskSummary(incidents, projectsWithHealth) {
  const hasCritical = (incidents || []).some((i) => i.severity === 'CRITICAL');
  if (hasCritical) return { label: 'Alto impacto', detail: 'Hay incidentes críticos abiertos.' };
  const hasHigh = (incidents || []).some((i) => i.severity === 'HIGH');
  const atRiskProject = (projectsWithHealth || []).some(
    (p) => p.health && p.health.project_health_label === 'At Risk'
  );
  if (hasHigh || atRiskProject) {
    return { label: 'Impacto moderado', detail: 'Revisa incidentes y salud de proyectos.' };
  }
  return { label: 'Bajo impacto', detail: 'Sin alertas críticas en el portafolio actual.' };
}

function incidentIconName(type) {
  const t = (type || '').toLowerCase();
  if (t.includes('db') || t.includes('data') || t.includes('latenc')) return 'warning';
  if (t.includes('ui') || t.includes('browser') || t.includes('safari')) return 'language';
  if (t.includes('asset') || t.includes('file')) return 'folder_open';
  return 'report_problem';
}

function ManagerDashboard() {
  const history = useHistory();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [dashboard, setDashboard] = useState(null);
  const [projectsWithHealth, setProjectsWithHealth] = useState([]);
  const [incidents, setIncidents] = useState([]);
  const [kpiSprintValue, setKpiSprintValue] = useState(null);
  const [kpiDeployValue, setKpiDeployValue] = useState(null);

  const load = useCallback(async () => {
    const token = localStorage.getItem('authToken');
    if (!token) {
      setError('Inicia sesión para ver el panel.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError('');

    const headers = { Authorization: `Bearer ${token}` };

    try {
      const dashRes = await fetch(`${API_BASE}/dashboard`, { headers });
      if (dashRes.status === 401) {
        localStorage.removeItem('authToken');
        window.location.assign('/login');
        return;
      }
      if (!dashRes.ok) {
        const body = await dashRes.json().catch(() => ({}));
        throw new Error(body.message || `Error ${dashRes.status}`);
      }
      const dash = await dashRes.json();
      const userId = dash.user?.user_id;
      const sprintId = dash.current_sprint?.sprint_id;

      const [projectsPag, incidentsPag, kpiGlobalPag, kpiSprintPag] = await Promise.all([
        fetch(`${API_BASE}/projects?manager_id=${userId}&page=1&limit=20`, { headers }).then((r) => {
          if (r.status === 401) {
            localStorage.removeItem('authToken');
            window.location.assign('/login');
            return {};
          }
          return r.json();
        }),
        fetch(`${API_BASE}/incidents?resolved=false&page=1&limit=5`, { headers }).then((r) => {
          if (r.status === 401) {
            localStorage.removeItem('authToken');
            window.location.assign('/login');
            return {};
          }
          return r.json();
        }),
        fetch(`${API_BASE}/kpi-values?scope_type=GLOBAL&page=1&limit=50`, { headers }).then((r) => {
          if (!r.ok) return { data: [] };
          return r.json();
        }),
        sprintId
          ? fetch(
              `${API_BASE}/kpi-values?scope_type=SPRINT&sprint_id=${sprintId}&page=1&limit=50`,
              { headers }
            ).then((r) => {
              if (!r.ok) return { data: [] };
              return r.json();
            })
          : Promise.resolve({ data: [] }),
      ]);

      const projectList = projectsPag.data || [];
      const healthResults = await Promise.all(
        projectList.map((p) =>
          fetch(`${API_BASE}/projects/${p.projectId}/health`, { headers })
            .then((r) => (r.ok ? r.json() : null))
            .catch(() => null)
        )
      );
      const merged = projectList.map((p, i) => ({ ...p, health: healthResults[i] }));

      const sprintKpi = latestKpiByName(kpiSprintPag.data, KPI_SPRINT);
      const deployKpi = latestKpiByName(kpiGlobalPag.data, KPI_DEPLOY);

      setDashboard(dash);
      setProjectsWithHealth(merged);
      setIncidents(incidentsPag.data || []);
      setKpiSprintValue(sprintKpi);
      setKpiDeployValue(deployKpi);
    } catch (e) {
      setError(e.message || 'No se pudo cargar el panel.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const greeting = dashboard?.greeting_summary || {};
  const user = dashboard?.user || {};
  const currentSprint = dashboard?.current_sprint || {};
  const myTasks = dashboard?.my_tasks_next5 || [];

  const sprintPercent = useMemo(() => {
    if (kpiSprintValue != null && kpiSprintValue.value != null) {
      const n = Number(kpiSprintValue.value);
      return Number.isFinite(n) ? Math.min(100, Math.max(0, Math.round(n))) : null;
    }
    const v = currentSprint.velocity_percent;
    if (v == null) return null;
    return Math.min(100, Math.max(0, Number(v)));
  }, [kpiSprintValue, currentSprint.velocity_percent]);

  const sprintOnTrack = currentSprint.on_track !== false && (sprintPercent == null || sprintPercent >= 50);

  const deployPercent = useMemo(() => {
    if (!kpiDeployValue || kpiDeployValue.value == null) return null;
    const n = Number(kpiDeployValue.value);
    return Number.isFinite(n) ? Math.min(100, Math.max(0, Math.round(n))) : null;
  }, [kpiDeployValue]);

  const risk = useMemo(() => riskSummary(incidents, projectsWithHealth), [incidents, projectsWithHealth]);

  const criticalCount = useMemo(
    () => (incidents || []).filter((i) => i.severity === 'CRITICAL').length,
    [incidents]
  );

  const pendingReview = greeting.pending_review_count ?? 0;
  const highPri = greeting.high_priority_task_count ?? 0;

  if (loading && !dashboard) {
    return (
      <PageLayout>
        <div className="mdash-loading">Cargando panel…</div>
      </PageLayout>
    );
  }

  if (error && !dashboard) {
    return (
      <PageLayout>
        <div className="mdash-error">
          <p>{error}</p>
          <button type="button" className="btn btn--primary" onClick={load}>
            Reintentar
          </button>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <div className="mdash">
        {/* Hero — Resumen del espacio */}
        <section className="mdash-hero">
          <div className="mdash-hero__main">
            <h1 className="mdash-hero__title">
              Resumen del espacio — {user.full_name || 'Usuario'}
            </h1>
            <p className="mdash-hero__subtitle">
              Panel del manager. Tienes{' '}
              <strong className="mdash-hero__accent">{pendingReview} tareas pendientes</strong> de revisión
              globales {greeting.current_sprint_name ? `para ${greeting.current_sprint_name}.` : 'para el ciclo actual.'}
              {highPri > 0 && (
                <>
                  {' '}
                  Además, <strong className="mdash-hero__accent">{highPri}</strong> con prioridad alta entre tus
                  tareas asignadas.
                </>
              )}
            </p>
          </div>
        </section>

        {error && (
          <div className="mdash-banner" role="alert">
            {error}
          </div>
        )}

        {/* KPIs */}
        <div className={`mdash-kpi-grid ${deployPercent != null ? 'mdash-kpi-grid--with-deploy' : ''}`}>
          <div className="mdash-kpi-sprint">
            <p className="mdash-label">Cumplimiento del sprint</p>
            <div className="mdash-kpi-sprint__row">
              <span className="mdash-kpi-sprint__value">{sprintPercent != null ? `${sprintPercent}%` : '—'}</span>
              {sprintPercent != null && (
                <span className={`mdash-pill ${sprintOnTrack ? 'mdash-pill--ok' : 'mdash-pill--risk'}`}>
                  {sprintOnTrack ? 'En camino' : 'En riesgo'}
                </span>
              )}
            </div>
            {sprintPercent != null && (
              <div className="mdash-progress">
                <div className="mdash-progress__fill" style={{ width: `${sprintPercent}%` }} />
              </div>
            )}
            <span className="material-icons mdash-kpi-sprint__deco" aria-hidden>
              architecture
            </span>
          </div>

          {deployPercent != null && (
            <div className="mdash-kpi-deploy">
              <p className="mdash-label mdash-kpi-deploy__label">Tasa de éxito de despliegues</p>
              <span className="mdash-kpi-deploy__value">{deployPercent}%</span>
              <p className="mdash-kpi-deploy__hint">
                Indicador global registrado en el sistema. Objetivo de desempeño del trimestre.
              </p>
              <div className="mdash-kpi-deploy__footer">
                <span className="mdash-dot" />
                <span className="mdash-label mdash-kpi-deploy__monitor">Monitoreo activo</span>
              </div>
            </div>
          )}

          <div className="mdash-kpi-risk">
            <span className="material-icons mdash-kpi-risk__icon" aria-hidden>
              analytics
            </span>
            <div>
              <p className="mdash-label">Factor de riesgo</p>
              <p className="mdash-kpi-risk__value">{risk.label}</p>
              <p className="mdash-kpi-risk__detail">{risk.detail}</p>
            </div>
          </div>
        </div>

        {/* Mis proyectos */}
        <section className="mdash-section">
          <div className="mdash-section__head">
            <h2 className="mdash-section__title">Mis proyectos</h2>
            <button type="button" className="mdash-link" onClick={() => history.push('/backlog')}>
              Ver todos los proyectos
            </button>
          </div>
          {projectsWithHealth.length === 0 ? (
            <p className="mdash-empty">No hay proyectos donde seas manager.</p>
          ) : (
            <div className="mdash-portfolio">
              {projectsWithHealth.map((p) => {
                const hl = p.health?.project_health_label;
                const overdue = p.health?.overdue_tasks_this_week ?? 0;
                return (
                  <article
                    key={p.projectId}
                    className={`mdash-portfolio-card ${healthBorderClass(hl)}`}
                  >
                    <div className="mdash-portfolio-card__head">
                      <div className="mdash-portfolio-card__intro">
                        <p className="mdash-portfolio-card__status">
                          Estado: {PROJECT_STATUS_ES[p.status] || p.status || '—'}
                        </p>
                        <h3 className="mdash-portfolio-card__name">{p.name}</h3>
                      </div>
                      {p.activeSprintName && (
                        <span className="mdash-tag mdash-tag--sprint">{p.activeSprintName}</span>
                      )}
                    </div>
                    <dl className="mdash-portfolio-card__stats">
                      <div className="mdash-stat-row">
                        <dt>Salud</dt>
                        <dd className={healthTextClass(hl)}>{HEALTH_LABEL_ES[hl] || hl || '—'}</dd>
                      </div>
                      <div className="mdash-stat-row">
                        <dt>Miembros</dt>
                        <dd>
                          <strong>{p.memberCount ?? 0}</strong> activos
                        </dd>
                      </div>
                      <div className="mdash-stat-row">
                        <dt>Atrasadas</dt>
                        <dd className={overdue > 0 ? 'mdash-text-red' : ''}>
                          <strong>{overdue}</strong>
                          {overdue === 1 ? ' tarea' : overdue > 1 ? ' tareas' : ''}
                        </dd>
                      </div>
                    </dl>
                  </article>
                );
              })}
            </div>
          )}
        </section>

        {/* Incidentes + cola personal */}
        <div className="mdash-bottom">
          <section className="mdash-incidents">
            <div className="mdash-section__head">
              <h2 className="mdash-section__title">Incidentes abiertos</h2>
              {criticalCount > 0 && (
                <span className="mdash-badge-critical">
                  {criticalCount} {criticalCount === 1 ? 'CRÍTICO' : 'CRÍTICOS'}
                </span>
              )}
            </div>
            {incidents.length === 0 ? (
              <p className="mdash-empty">Sin incidentes abiertos.</p>
            ) : (
              <ul className="mdash-incident-list">
                {incidents.map((inc) => (
                  <li key={inc.incidentId} className="mdash-incident-row">
                    <div
                      className={`mdash-incident-icon ${
                        inc.severity === 'CRITICAL' ? 'mdash-incident-icon--critical' : ''
                      }`}
                    >
                      <span className="material-icons" aria-hidden>
                        {incidentIconName(inc.type)}
                      </span>
                    </div>
                    <div className="mdash-incident-body">
                      <h3 className="mdash-incident-title">{inc.type || 'Incidente'}</h3>
                      <p className="mdash-incident-meta">
                        {inc.projectName || 'Proyecto'} · {inc.description || 'Sin descripción'}
                      </p>
                    </div>
                    <span
                      className={`mdash-severity mdash-severity--${(inc.severity || 'MEDIUM').toLowerCase()}`}
                    >
                      {SEVERITY_LABEL_ES[inc.severity] || inc.severity}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="mdash-queue">
            <h2 className="mdash-section__title mdash-queue__title">Cola personal</h2>
            <div className="mdash-queue__box">
              {myTasks.length === 0 ? (
                <p className="mdash-empty mdash-queue__empty">No hay tareas asignadas próximas.</p>
              ) : (
                <ul className="mdash-task-list">
                  {myTasks.map((t, idx) => (
                    <li key={t.task_id || idx} className={idx > 0 ? 'mdash-task-row mdash-task-row--border' : 'mdash-task-row'}>
                      <div className="mdash-task-row__top">
                        <h3 className="mdash-task-title">{t.title}</h3>
                        {t.is_today && <span className="mdash-due mdash-due--today">Hoy</span>}
                        {!t.is_today && t.is_tomorrow && (
                          <span className="mdash-due mdash-due--tomorrow">Mañana</span>
                        )}
                        {!t.is_today && !t.is_tomorrow && t.is_overdue && (
                          <span className="mdash-due mdash-due--overdue">Atrasada</span>
                        )}
                        {!t.is_today && !t.is_tomorrow && !t.is_overdue && t.due_date && (
                          <span className="mdash-due mdash-due--muted">{t.due_date}</span>
                        )}
                      </div>
                      <p className="mdash-task-meta">
                        {t.project_name || 'Proyecto'} · {t.status}
                        {t.priority ? ` · ${t.priority}` : ''}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
              <button type="button" className="mdash-queue__cta" onClick={() => history.push('/backlog')}>
                Ver lista maestra de tareas
              </button>
            </div>
          </section>
        </div>
      </div>
    </PageLayout>
  );
}

export default ManagerDashboard;
