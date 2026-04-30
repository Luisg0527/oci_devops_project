import React, { useState, useEffect, useRef } from 'react';
import PageLayout from '../components/layout/PageLayout';
import ProgressBar from '../components/common/ProgressBar';
import './Reports.css';

// ── API setup ─────────────────────────────────────────────────────────────────
const API_BASE = process.env.NODE_ENV === 'production' ? '' : 'http://localhost:8080';

function getToken() {
  return localStorage.getItem('token') || localStorage.getItem('authToken');
}

async function apiFetch(path) {
  const token = getToken();
  const res = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

// ── Chart.js CDN loader ───────────────────────────────────────────────────────
let _chartJsPromise = null;
function loadChartJs() {
  if (_chartJsPromise) return _chartJsPromise;
  if (window.Chart) return (_chartJsPromise = Promise.resolve(window.Chart));
  _chartJsPromise = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js';
    s.onload = () => resolve(window.Chart);
    s.onerror = reject;
    document.head.appendChild(s);
  });
  return _chartJsPromise;
}

// ── Color palette (concrete values — CSS vars don't work inside Chart.js) ─────
const C = {
  primary:  '#4F46E5',
  primaryA: 'rgba(79,70,229,0.15)',
  green:    '#10B981',
  greenA:   'rgba(16,185,129,0.22)',
  gold:     '#F59E0B',
  red:      '#EF4444',
  slate:    '#94A3B8',
  slateA:   'rgba(148,163,184,0.18)',
  teal:     '#14B8A6',
  tealA:    'rgba(20,184,166,0.18)',
};

// ── KPI metadata ──────────────────────────────────────────────────────────────
const KPI_META = {
  TAREAS_COMPLETADAS_SPRINT:     { label: 'Tareas Completadas',         icon: 'task_alt' },
  CUMPLIMIENTO_SPRINT:           { label: 'Cumplimiento Sprint',         icon: 'verified' },
  TAREAS_REABIERTAS:             { label: 'Tareas Reabiertas',           icon: 'restart_alt' },
  ACTUALIZACION_TAREAS_DIA:      { label: 'Actualización Diaria',        icon: 'update' },
  TRAZABILIDAD_TAREAS:           { label: 'Trazabilidad Tareas',         icon: 'timeline' },
  DESPLIEGUES_PRODUCCION_SPRINT: { label: 'Despliegues a Producción',    icon: 'rocket_launch' },
  MTTR:                          { label: 'MTTR (Recuperación)',         icon: 'schedule' },
  TASA_EXITO_DESPLIEGUES:        { label: 'Éxito en Despliegues',        icon: 'check_circle' },
  INTERACCIONES_BOT:             { label: 'Interacciones del Bot',       icon: 'smart_toy' },
  DISPONIBILIDAD_SERVICIO:       { label: 'Disponibilidad del Servicio', icon: 'cloud_done' },
  USO_CPU:                       { label: 'Uso de CPU',                  icon: 'memory' },
  USO_MEMORIA:                   { label: 'Uso de Memoria RAM',          icon: 'storage' },
  INCIDENTES_SEGURIDAD:          { label: 'Incidentes de Seguridad',     icon: 'security' },
};

const KPI_CATEGORY_LABELS = {
  DELIVERY:    'Entrega',
  QUALITY:     'Calidad',
  ACTIVITY:    'Actividad',
  DEVOPS:      'DevOps',
  ENGAGEMENT:  'Engagement',
  RELIABILITY: 'Confiabilidad',
  INFRA:       'Infraestructura',
  SECURITY:    'Seguridad',
};

// ── Semicircular Gauge SVG ────────────────────────────────────────────────────
function GaugeChart({ value, color }) {
  const r = 30;
  const sw = 7;
  // Semicircle arc length
  const arcLen = Math.PI * r;
  const clamped = Math.min(100, Math.max(0, value || 0));
  const progress = (clamped / 100) * arcLen;

  return (
    <svg className="kpi-gauge-svg" viewBox="0 0 80 46" aria-hidden="true">
      {/* Track */}
      <path
        d="M 10 43 A 30 30 0 0 1 70 43"
        fill="none"
        stroke="rgba(0,0,0,0.08)"
        strokeWidth={sw}
        strokeLinecap="round"
      />
      {/* Progress */}
      <path
        d="M 10 43 A 30 30 0 0 1 70 43"
        fill="none"
        stroke={color}
        strokeWidth={sw}
        strokeLinecap="round"
        strokeDasharray={`${progress} ${arcLen}`}
      />
    </svg>
  );
}

// ── KPI Card ──────────────────────────────────────────────────────────────────
function KpiCard({ kpi, category }) {
  const meta = KPI_META[kpi.kpiName] || { label: kpi.kpiName, icon: 'analytics' };
  const catLabel = KPI_CATEGORY_LABELS[category] || category;
  const isPercent = kpi.unit === 'percent';
  const isMinutes = kpi.unit === 'minutes';
  const numVal = kpi.value != null ? Number(kpi.value) : null;

  const color =
    isPercent && numVal != null
      ? numVal >= 80 ? C.green : numVal >= 50 ? C.gold : C.red
      : C.primary;

  const displayVal =
    numVal != null
      ? isPercent
        ? `${Math.round(numVal)}%`
        : isMinutes
        ? `${Math.round(numVal)} min`
        : String(Math.round(numVal))
      : '—';

  return (
    <div className="kpi-card card">
      <div className="kpi-card__top">
        <span className="material-icons kpi-card__icon" style={{ color }}>
          {meta.icon}
        </span>
        <span className="kpi-card__label">{meta.label}</span>
      </div>
      {isPercent ? (
        <div className="kpi-card__gauge-wrap">
          <GaugeChart value={numVal} color={color} />
          <span className="kpi-card__gauge-val" style={{ color }}>
            {displayVal}
          </span>
        </div>
      ) : (
        <div className="kpi-card__big-val" style={{ color }}>{displayVal}</div>
      )}
      <span className="kpi-card__category">{catLabel}</span>
    </div>
  );
}

// ── Chart.js canvas wrapper ───────────────────────────────────────────────────
function ChartBox({ type, data, options, height }) {
  const canvasRef = useRef(null);
  const instanceRef = useRef(null);

  useEffect(() => {
    if (!data) return;
    let cancelled = false;
    loadChartJs()
      .then((Chart) => {
        if (cancelled || !canvasRef.current) return;
        if (instanceRef.current) {
          instanceRef.current.destroy();
          instanceRef.current = null;
        }
        instanceRef.current = new Chart(canvasRef.current, {
          type,
          data,
          options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 500 },
            plugins: {
              legend: {
                display: (data.datasets || []).length > 1,
                position: 'bottom',
                labels: { boxWidth: 12, padding: 16, font: { size: 11 } },
              },
              tooltip: { mode: 'index', intersect: false },
            },
            ...options,
          },
        });
      })
      .catch(() => {});
    return () => {
      cancelled = true;
      if (instanceRef.current) {
        instanceRef.current.destroy();
        instanceRef.current = null;
      }
    };
  }, [data]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="chart-box" style={{ height: height || 200 }}>
      {data ? <canvas ref={canvasRef} /> : <div className="chart-skeleton" />}
    </div>
  );
}

// ── Skeleton placeholder ──────────────────────────────────────────────────────
function Skeleton({ h, className }) {
  return (
    <div
      className={`reports-skeleton${className ? ' ' + className : ''}`}
      style={{ height: h || 120 }}
    />
  );
}

// ── No-data placeholder ───────────────────────────────────────────────────────
function NoData({ text }) {
  return (
    <div className="reports-no-data">
      <span className="material-icons" style={{ fontSize: 32, opacity: 0.3 }}>
        bar_chart
      </span>
      <span>{text || 'Sin datos disponibles'}</span>
    </div>
  );
}

// ── Reports page ──────────────────────────────────────────────────────────────
function Reports() {
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [activeSprint, setActiveSprint] = useState(null);
  const [burndown, setBurndown]     = useState(null);
  const [velocity, setVelocity]     = useState(null);
  const [cumulative, setCumulative] = useState(null);
  const [completion, setCompletion] = useState([]);
  const [milestones, setMilestones] = useState([]);
  const [kpiValues, setKpiValues]   = useState([]);
  const [velocityView, setVelocityView] = useState('recent');
  const [exporting, setExporting]       = useState(false);
  const [exportError, setExportError]   = useState(null);
  const [devSprint, setDevSprint]       = useState(null);
  const [hoursSprint, setHoursSprint]   = useState(null);

  const projectId   = localStorage.getItem('currentProjectId');
  const projectName = localStorage.getItem('currentProjectName') || 'Proyecto';

  // ── Data fetching ────────────────────────────────────────────────────────
  useEffect(() => {
    if (!projectId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    (async () => {
      try {
        // 1. Get active sprint for this project
        let active = null;
        try {
          const sprintRes = await apiFetch(
            `/api/v1/sprints?project_id=${projectId}&status=ACTIVE&limit=1`
          );
          active = (sprintRes.data || [])[0] || null;
          setActiveSprint(active);
        } catch (_) {
          // Sprint fetch failed — continue without active sprint
        }

        // 2. Parallel project-level fetches
        const [vel, cum, comp, ms, kpis, devSp, hoursSp] = await Promise.allSettled([
          apiFetch(`/api/v1/reports/sprint-velocity?project_id=${projectId}&last_n=6`),
          apiFetch(`/api/v1/reports/cumulative-flow?project_id=${projectId}&weeks=7`),
          apiFetch(`/api/v1/reports/task-completion?project_id=${projectId}`),
          apiFetch(`/api/v1/reports/milestones?project_id=${projectId}`),
          apiFetch(`/api/v1/kpi-values?project_id=${projectId}&limit=100`),
          apiFetch(`/api/v1/reports/tasks-by-developer-sprint?project_id=${projectId}`),
          apiFetch(`/api/v1/reports/hours-by-user-sprint?project_id=${projectId}`),
        ]);

        if (vel.status === 'fulfilled')    setVelocity(vel.value);
        if (cum.status === 'fulfilled')    setCumulative(cum.value);
        if (comp.status === 'fulfilled')   setCompletion(comp.value?.data || []);
        if (ms.status === 'fulfilled')     setMilestones(ms.value?.milestones || []);
        if (kpis.status === 'fulfilled')   setKpiValues(kpis.value?.data || []);
        if (devSp.status === 'fulfilled')  setDevSprint(devSp.value);
        if (hoursSp.status === 'fulfilled') setHoursSprint(hoursSp.value);

        // 3. Burndown — needs active sprint
        if (active?.sprintId) {
          try {
            const bd = await apiFetch(
              `/api/v1/reports/burndown?sprint_id=${active.sprintId}`
            );
            setBurndown(bd);
          } catch (_) {/* no burndown */}
        }
      } catch (err) {
        setError(err.message || 'Error al cargar los reportes.');
      } finally {
        setLoading(false);
      }
    })();
  }, [projectId]);

  // ── Export PDF (client-side — backend placeholder no genera PDF válido) ───
  const handleExport = () => {
    if (exporting) return;
    setExportError(null);

    try {
      const sprintLabel = activeSprint?.name || 'Sin sprint activo';
      const date = new Date().toLocaleDateString('es-MX', { dateStyle: 'long' });

      const kpiRows = kpiCategories
        .flatMap((cat) =>
          Object.values(kpiByCategory[cat]).map((kv) => {
            const meta = KPI_META[kv.kpiName] || { label: kv.kpiName };
            const catLabel = KPI_CATEGORY_LABELS[cat] || cat;
            const val =
              kv.value != null
                ? kv.unit === 'percent'
                  ? `${Math.round(kv.value)}%`
                  : kv.unit === 'minutes'
                  ? `${Math.round(kv.value)} min`
                  : String(Math.round(kv.value))
                : '—';
            return `<tr><td>${catLabel}</td><td>${meta.label}</td><td><strong>${val}</strong></td></tr>`;
          })
        )
        .join('');

      const completionRows = completion
        .map(
          (row) => `<tr>
            <td>${row.full_name}</td>
            <td>${row.role || '—'}</td>
            <td>${row.assigned}</td>
            <td>${row.completed}</td>
            <td>${row.efficiency_percent}%</td>
          </tr>`
        )
        .join('');

      const milestoneRows = milestones
        .map((m) => {
          const status =
            m.status === 'COMPLETED'
              ? 'Completado'
              : m.is_current
              ? 'Activo'
              : 'Pendiente';
          return `<tr><td>${m.name}</td><td>${m.date || '—'}</td><td>${status}</td></tr>`;
        })
        .join('');

      const html = `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Reporte — ${sprintLabel}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: Arial, sans-serif; color: #1e293b; padding: 36px; font-size: 13px; line-height: 1.5; }
    .report-title { font-size: 26px; font-weight: 700; margin-bottom: 4px; }
    .report-meta { color: #64748b; font-size: 12px; margin-bottom: 32px; }
    h2 { font-size: 14px; font-weight: 700; margin: 28px 0 8px; color: #4F46E5;
         border-bottom: 2px solid #4F46E5; padding-bottom: 4px; text-transform: uppercase; letter-spacing: 0.5px; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #f1f5f9; text-align: left; padding: 8px 12px;
         font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #64748b; }
    td { padding: 8px 12px; border-bottom: 1px solid #e2e8f0; }
    tr:last-child td { border-bottom: none; }
    @media print {
      body { padding: 16px; }
      h2 { break-before: avoid; }
      table { break-inside: avoid; }
    }
  </style>
</head>
<body>
  <div class="report-title">Reporte de Rendimiento</div>
  <div class="report-meta">${projectName} &nbsp;·&nbsp; ${sprintLabel} &nbsp;·&nbsp; Generado el ${date}</div>

  ${kpiRows ? `<h2>KPIs del Proyecto</h2>
  <table>
    <thead><tr><th>Categoría</th><th>KPI</th><th>Valor</th></tr></thead>
    <tbody>${kpiRows}</tbody>
  </table>` : ''}

  ${completionRows ? `<h2>Completado por Miembro</h2>
  <table>
    <thead><tr><th>Miembro</th><th>Rol</th><th>Asignadas</th><th>Completadas</th><th>Eficiencia</th></tr></thead>
    <tbody>${completionRows}</tbody>
  </table>` : ''}

  ${milestoneRows ? `<h2>Milestones</h2>
  <table>
    <thead><tr><th>Sprint</th><th>Fecha</th><th>Estado</th></tr></thead>
    <tbody>${milestoneRows}</tbody>
  </table>` : ''}
</body>
</html>`;

      const win = window.open('', '_blank');
      if (!win) {
        setExportError('Popup bloqueado — permite ventanas emergentes para este sitio.');
        return;
      }
      win.document.write(html);
      win.document.close();
      win.focus();
      // Small delay so the browser renders before print dialog opens
      setTimeout(() => win.print(), 300);
    } catch (_) {
      setExportError('No se pudo generar el reporte.');
    }
  };

  // ── Chart data builders ───────────────────────────────────────────────────
  const burndownChartData = burndown
    ? {
        labels: (burndown.ideal_line || []).map((d) => d.day),
        datasets: [
          {
            label: 'Ideal',
            data: (burndown.ideal_line || []).map((d) => d.ideal_remaining),
            borderColor: C.slate,
            borderDash: [6, 4],
            borderWidth: 2,
            pointRadius: 0,
            tension: 0,
            fill: false,
          },
          {
            label: 'Real',
            data: (burndown.actual_line || []).map((d) => d.actual_remaining),
            borderColor: C.primary,
            backgroundColor: C.primaryA,
            borderWidth: 2.5,
            pointRadius: 4,
            pointBackgroundColor: C.primary,
            tension: 0.3,
            fill: true,
          },
        ],
      }
    : null;

  const velocitySprints = velocity?.sprints || [];
  const velocityChartData = velocitySprints.length
    ? {
        labels: velocitySprints.map((s) => s.label),
        datasets: [
          {
            label: 'Tareas Completadas',
            data: velocitySprints.map((s) => s.points_delivered),
            backgroundColor: velocitySprints.map((s) =>
              s.is_current ? C.primary : 'rgba(79,70,229,0.38)'
            ),
            borderRadius: 6,
            borderSkipped: false,
          },
        ],
      }
    : null;

  const cumulativeWeeks = cumulative?.weeks || [];
  const cumulativeChartData = cumulativeWeeks.length
    ? {
        labels: cumulativeWeeks.map((w) => w.week),
        datasets: [
          {
            label: 'Done',
            data: cumulativeWeeks.map((w) => w.done),
            backgroundColor: C.greenA,
            borderColor: C.green,
            borderWidth: 2,
            fill: true,
            tension: 0.4,
          },
          {
            label: 'En Progreso',
            data: cumulativeWeeks.map((w) => w.in_progress),
            backgroundColor: C.primaryA,
            borderColor: C.primary,
            borderWidth: 2,
            fill: true,
            tension: 0.4,
          },
          {
            label: 'Backlog',
            data: cumulativeWeeks.map((w) => w.backlog),
            backgroundColor: C.slateA,
            borderColor: C.slate,
            borderWidth: 2,
            fill: true,
            tension: 0.4,
          },
        ],
      }
    : null;

  // ── Grouped bar palette ───────────────────────────────────────────────────
  const BAR_PALETTE = [
    '#4F46E5', '#10B981', '#F59E0B', '#EF4444',
    '#8B5CF6', '#14B8A6', '#F97316', '#EC4899',
  ];

  const devSprintChartData =
    devSprint?.sprints?.length && devSprint?.developers?.length
      ? {
          labels: devSprint.sprints,
          datasets: devSprint.developers.map((dev, i) => ({
            label: dev.name,
            data: dev.data,
            backgroundColor: BAR_PALETTE[i % BAR_PALETTE.length],
            borderRadius: 5,
            borderSkipped: false,
          })),
        }
      : null;

  const hoursSprintChartData =
    hoursSprint?.sprints?.length && hoursSprint?.users?.length
      ? {
          labels: hoursSprint.sprints,
          datasets: hoursSprint.users.map((u, i) => ({
            label: u.name,
            data: u.data,
            backgroundColor: BAR_PALETTE[i % BAR_PALETTE.length],
            borderRadius: 5,
            borderSkipped: false,
          })),
        }
      : null;

  // ── KPI grouping: latest value per kpiName, grouped by category ───────────
  const kpiByCategory = {};
  kpiValues.forEach((kv) => {
    const cat = kv.category || 'OTHER';
    if (!kpiByCategory[cat]) kpiByCategory[cat] = {};
    // Overwrite — last in array wins (assume API returns ordered by time asc)
    kpiByCategory[cat][kv.kpiName] = kv;
  });
  const kpiCategories = Object.keys(kpiByCategory);

  // ── Derived labels ────────────────────────────────────────────────────────
  const sprintLabel = activeSprint?.name || (loading ? 'Cargando…' : 'Sin sprint activo');

  // ── No project selected ───────────────────────────────────────────────────
  if (!projectId && !loading) {
    return (
      <PageLayout searchPlaceholder="Buscar analíticas...">
        <div className="reports-empty">
          <span className="material-icons reports-empty__icon">bar_chart</span>
          <h3>Selecciona un proyecto</h3>
          <p className="text-muted">
            Usa el selector de la barra superior para elegir un proyecto y ver
            sus reportes.
          </p>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout searchPlaceholder="Buscar analíticas...">
      {/* ── Header ─────────────────────────────────────────────────────── */}
      <section className="reports-header">
        <div>
          <span className="section-label">Motor de Análisis · {projectName}</span>
          <h2 className="section-title" style={{ fontSize: 36 }}>
            Reportes de Rendimiento
          </h2>
        </div>
        <div className="reports-header__actions">
          <button className="btn btn--white">
            <span className="material-icons" style={{ fontSize: 18 }}>
              calendar_today
            </span>
            {sprintLabel}
          </button>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
            <button
              className="btn btn--primary"
              onClick={handleExport}
              disabled={loading || !projectId || exporting}
            >
              <span className="material-icons" style={{ fontSize: 18 }}>
                {exporting ? 'hourglass_empty' : 'file_download'}
              </span>
              {exporting ? 'Exportando…' : 'Exportar PDF'}
            </button>
            {exportError && (
              <span style={{ fontSize: 12, color: '#EF4444' }}>{exportError}</span>
            )}
          </div>
        </div>
      </section>

      {/* ── Error banner ────────────────────────────────────────────────── */}
      {error && (
        <div className="reports-error mt-24">
          <span className="material-icons" style={{ fontSize: 18 }}>
            error_outline
          </span>
          {error}
        </div>
      )}

      {/* ── KPI Dashboard ───────────────────────────────────────────────── */}
      <section className="mt-24">
        <div className="reports-section-head">
          <h3 className="reports-section-title">Dashboard KPI</h3>
          <span className="text-sm text-muted">Métricas clave del proyecto</span>
        </div>

        {loading ? (
          <div className="kpi-grid mt-16">
            {[...Array(8)].map((_, i) => (
              <Skeleton key={i} h={110} className="card" />
            ))}
          </div>
        ) : kpiCategories.length === 0 ? (
          <div className="kpi-empty mt-16">
            <span className="material-icons" style={{ fontSize: 28, opacity: 0.3 }}>
              analytics
            </span>
            <span className="text-sm text-muted">
              No hay valores KPI registrados para este proyecto.
            </span>
          </div>
        ) : (
          <div className="kpi-grid mt-16">
            {kpiCategories.flatMap((cat) =>
              Object.values(kpiByCategory[cat]).map((kv) => (
                <KpiCard key={kv.kpiValueId} kpi={kv} category={cat} />
              ))
            )}
          </div>
        )}
      </section>

      {/* ── Top row: Burndown + sidebar ─────────────────────────────────── */}
      <div className="reports-top mt-24">
        {/* Burndown chart */}
        <div className="card reports-burndown">
          <div className="flex justify-between items-center mb-24">
            <div>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>
                Gráfica Burndown
              </h3>
              <span className="text-sm text-muted">
                {burndown
                  ? burndown.sprint_name
                  : activeSprint?.name || 'Sprint activo'}
              </span>
            </div>
            {burndown && (
              <span
                className={`reports-status-pill${
                  burndown.on_track ? '' : ' reports-status-pill--warn'
                }`}
              >
                {burndown.on_track ? 'En Curso' : 'Con Retraso'}
              </span>
            )}
          </div>

          {loading ? (
            <Skeleton h={210} />
          ) : burndownChartData ? (
            <ChartBox
              type="line"
              data={burndownChartData}
              height={210}
              options={{
                scales: {
                  x: { grid: { display: false } },
                  y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Tareas Restantes', font: { size: 11 } },
                    grid: { color: 'rgba(0,0,0,0.05)' },
                  },
                },
              }}
            />
          ) : (
            <NoData text="Sin datos de burndown para el sprint activo" />
          )}
        </div>

        {/* Metrics sidebar */}
        <div className="reports-metrics-sidebar">
          {/* Velocity KPI card */}
          <div className="card card--dark reports-velocity-card">
            <span className="material-icons" style={{ fontSize: 26, opacity: 0.7 }}>
              speed
            </span>
            <div>
              <h4 style={{ margin: 0, fontSize: 14 }}>Velocidad Promedio</h4>
              <div className="reports-velocity-val">
                <span style={{ fontSize: 36, fontWeight: 700 }}>
                  {loading ? '…' : velocity ? velocity.current_velocity : '—'}
                </span>
                <span className="text-sm" style={{ opacity: 0.7 }}>
                  pts/sprint
                </span>
              </div>
              {velocity && (
                <span className="text-sm" style={{ opacity: 0.55, marginTop: 4, display: 'block' }}>
                  {velocitySprints.length} sprints analizados
                </span>
              )}
            </div>
          </div>

          {/* Cumulative flow mini chart */}
          <div className="card reports-cumulative">
            <div className="flex justify-between items-center mb-8">
              <h4 style={{ margin: 0, fontSize: 14 }}>Flujo Acumulado</h4>
            </div>
            {loading ? (
              <Skeleton h={110} />
            ) : cumulativeChartData ? (
              <ChartBox
                type="line"
                data={cumulativeChartData}
                height={110}
                options={{
                  scales: {
                    x: {
                      grid: { display: false },
                      ticks: { font: { size: 9 } },
                      stacked: true,
                    },
                    y: {
                      display: false,
                      stacked: true,
                    },
                  },
                  plugins: {
                    legend: { display: false },
                    tooltip: { mode: 'index', intersect: false },
                  },
                }}
              />
            ) : (
              <NoData text="Sin datos" />
            )}
          </div>
        </div>
      </div>

      {/* ── Sprint Velocity bar chart ────────────────────────────────────── */}
      <div className="card mt-24">
        <div className="flex justify-between items-center mb-24">
          <div>
            <h3 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>
              Velocidad del Sprint
            </h3>
            <span className="text-sm text-muted">
              Tareas completadas por iteración
            </span>
          </div>
          <div className="reports-toggle">
            <button
              className={`reports-toggle__btn${
                velocityView === 'recent' ? ' reports-toggle__btn--active' : ''
              }`}
              onClick={() => setVelocityView('recent')}
            >
              Últimos 6 Sprints
            </button>
            <button
              className={`reports-toggle__btn${
                velocityView === 'all' ? ' reports-toggle__btn--active' : ''
              }`}
              onClick={() => setVelocityView('all')}
            >
              Histórico
            </button>
          </div>
        </div>

        {loading ? (
          <Skeleton h={230} />
        ) : velocityChartData ? (
          <ChartBox
            type="bar"
            data={velocityChartData}
            height={230}
            options={{
              scales: {
                x: { grid: { display: false } },
                y: {
                  beginAtZero: true,
                  title: { display: true, text: 'Tareas Completadas', font: { size: 11 } },
                  grid: { color: 'rgba(0,0,0,0.05)' },
                },
              },
              plugins: { legend: { display: false } },
            }}
          />
        ) : (
          <NoData text="Sin datos de velocidad" />
        )}
      </div>

      {/* ── Tasks by Developer per Sprint ───────────────────────────────── */}
      <div className="card mt-24">
        <div className="reports-chart-header mb-24">
          <div className="reports-chart-header__icon" style={{ background: 'rgba(79,70,229,0.1)' }}>
            <span className="material-icons" style={{ color: '#4F46E5', fontSize: 20 }}>group</span>
          </div>
          <div>
            <h3 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>
              Tareas Completadas por Desarrollador
            </h3>
            <span className="text-sm text-muted">Desglose por sprint y miembro del equipo</span>
          </div>
        </div>
        {loading ? (
          <Skeleton h={250} />
        ) : devSprintChartData ? (
          <ChartBox
            type="bar"
            data={devSprintChartData}
            height={250}
            options={{
              scales: {
                x: { grid: { display: false } },
                y: {
                  beginAtZero: true,
                  ticks: { stepSize: 1 },
                  title: { display: true, text: 'Tareas completadas', font: { size: 11 } },
                  grid: { color: 'rgba(0,0,0,0.05)' },
                },
              },
              plugins: {
                legend: { display: true, position: 'bottom', labels: { boxWidth: 12, padding: 16, font: { size: 11 } } },
                tooltip: { mode: 'index', intersect: false },
              },
            }}
          />
        ) : (
          <NoData text="Sin datos de tareas por desarrollador" />
        )}
      </div>

      {/* ── Real Hours by User per Sprint ────────────────────────────────── */}
      <div className="card mt-24">
        <div className="reports-chart-header mb-24">
          <div className="reports-chart-header__icon" style={{ background: 'rgba(16,185,129,0.1)' }}>
            <span className="material-icons" style={{ color: '#10B981', fontSize: 20 }}>schedule</span>
          </div>
          <div>
            <h3 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>
              Horas Reales por Usuario / Sprint
            </h3>
            <span className="text-sm text-muted">Total de horas registradas (ACTUAL_HOURS) por sprint</span>
          </div>
        </div>
        {loading ? (
          <Skeleton h={250} />
        ) : hoursSprintChartData ? (
          <ChartBox
            type="bar"
            data={hoursSprintChartData}
            height={250}
            options={{
              scales: {
                x: { grid: { display: false } },
                y: {
                  beginAtZero: true,
                  title: { display: true, text: 'Horas reales', font: { size: 11 } },
                  grid: { color: 'rgba(0,0,0,0.05)' },
                },
              },
              plugins: {
                legend: { display: true, position: 'bottom', labels: { boxWidth: 12, padding: 16, font: { size: 11 } } },
                tooltip: { mode: 'index', intersect: false },
              },
            }}
          />
        ) : (
          <NoData text="Sin horas reales registradas por sprint" />
        )}
      </div>

      {/* ── Task Completion Table ────────────────────────────────────────── */}
      <div className="card mt-24" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="reports-table-header">
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>
            Tasa de Completado de Tareas
          </h3>
          <span className="reports-table-badge">Por Miembro del Equipo</span>
        </div>

        {loading ? (
          <Skeleton h={160} />
        ) : completion.length === 0 ? (
          <div style={{ padding: '24px 16px' }}>
            <NoData text="Sin datos de completado para el proyecto" />
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Miembro</th>
                <th>Asignadas</th>
                <th>Completadas</th>
                <th>Eficiencia</th>
                <th>Tendencia</th>
              </tr>
            </thead>
            <tbody>
              {completion.map((row) => {
                const trendIcon =
                  row.trend === 'UP'
                    ? 'trending_up'
                    : row.trend === 'DOWN'
                    ? 'trending_down'
                    : 'trending_flat';
                const trendColor =
                  row.trend === 'UP'
                    ? C.green
                    : row.trend === 'DOWN'
                    ? C.red
                    : C.gold;
                const initials = (row.full_name || '?')
                  .split(' ')
                  .map((n) => n[0])
                  .join('')
                  .slice(0, 2)
                  .toUpperCase();
                return (
                  <tr key={row.user_id}>
                    <td>
                      <div className="flex items-center gap-12">
                        <div className="backlog-assignee__avatar">{initials}</div>
                        <div>
                          <div style={{ fontWeight: 600 }}>{row.full_name}</div>
                          <div className="text-sm text-muted">{row.role || '—'}</div>
                        </div>
                      </div>
                    </td>
                    <td>{row.assigned} tareas</td>
                    <td>{row.completed} tareas</td>
                    <td>
                      <div className="flex items-center gap-8">
                        <ProgressBar value={row.efficiency_percent} height={6} />
                        <span className="text-sm font-bold">
                          {row.efficiency_percent}%
                        </span>
                      </div>
                    </td>
                    <td>
                      <span
                        className="material-icons"
                        style={{ color: trendColor, fontSize: 20 }}
                      >
                        {trendIcon}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* ── Milestones ───────────────────────────────────────────────────── */}
      <div className="card mt-24">
        <div className="flex justify-between items-center mb-16">
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>
            Milestones del Proyecto
          </h3>
          {!loading && (
            <span className="reports-table-badge">
              {milestones.length} sprint{milestones.length !== 1 ? 's' : ''}
            </span>
          )}
        </div>

        {loading ? (
          <Skeleton h={90} />
        ) : milestones.length === 0 ? (
          <NoData text="Sin milestones registrados" />
        ) : (
          <div className="milestones-list">
            {milestones.map((m, i) => {
              const isDone    = m.status === 'COMPLETED';
              const isCurrent = m.is_current;
              const statusLabel =
                isDone ? 'Completado' : isCurrent ? 'Activo' : 'Pendiente';
              const statusMod =
                isDone ? 'done' : isCurrent ? 'active' : 'pending';
              return (
                <div
                  key={i}
                  className={`milestone-item${isCurrent ? ' milestone-item--active' : ''}`}
                >
                  <div
                    className={`milestone-dot milestone-dot--${statusMod}`}
                  />
                  <div className="milestone-info">
                    <span className="milestone-name">{m.name}</span>
                    <span className="text-sm text-muted">{m.date || '—'}</span>
                  </div>
                  <span className={`milestone-badge milestone-badge--${statusMod}`}>
                    {statusLabel}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </PageLayout>
  );
}

export default Reports;
