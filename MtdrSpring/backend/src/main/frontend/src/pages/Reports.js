import React, { useState, useEffect, useRef, useMemo } from 'react';
import PageLayout from '../components/layout/PageLayout';
import ProgressBar from '../components/common/ProgressBar';
import { API_BASE } from '../config/apiBase';
import { useProject } from '../context/ProjectContext';
import './Reports.css';

function getToken() {
  return localStorage.getItem('authToken');
}

async function apiFetch(resourceQuery) {
  const token = getToken();
  const q = resourceQuery.startsWith('/') ? resourceQuery.slice(1) : resourceQuery;
  const url = `${String(API_BASE).replace(/\/$/, '')}/${q}`;
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!res.ok) throw new Error(`Error del servidor (${res.status})`);
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
  TAREAS_COMPLETADAS_SPRINT: { label: 'Tareas Completadas', icon: 'task_alt' },
  CUMPLIMIENTO_SPRINT: { label: 'Cumplimiento Sprint', icon: 'verified' },
  TAREAS_REABIERTAS: { label: 'Tareas Reabiertas', icon: 'restart_alt' },
  ACTUALIZACION_TAREAS_DIA: { label: 'Actualización Diaria', icon: 'update' },
};

/** Orden visual del dashboard (solo estas 4 tarjetas). */
const DASHBOARD_KPI_ORDER = [
  'CUMPLIMIENTO_SPRINT',
  'TAREAS_COMPLETADAS_SPRINT',
  'TAREAS_REABIERTAS',
  'ACTUALIZACION_TAREAS_DIA',
];

/** Valor del selector para métricas agregadas del proyecto (sin sprint concreto). */
const PROJECT_SCOPE_ALL = 'PROJECT_ALL';

function dashboardKpiPlaceholder(kpiName) {
  const categoryByName = {
    CUMPLIMIENTO_SPRINT: 'DELIVERY',
    TAREAS_COMPLETADAS_SPRINT: 'DELIVERY',
    TAREAS_REABIERTAS: 'QUALITY',
    ACTUALIZACION_TAREAS_DIA: 'ACTIVITY',
  };
  const unitByName = {
    CUMPLIMIENTO_SPRINT: 'percent',
    ACTUALIZACION_TAREAS_DIA: 'percent',
    TAREAS_COMPLETADAS_SPRINT: 'count',
    TAREAS_REABIERTAS: 'count',
  };
  return {
    kpiValueId: null,
    kpiName,
    value: null,
    unit: unitByName[kpiName] || 'count',
    category: categoryByName[kpiName] || 'DELIVERY',
  };
}

/** Un registro por kpiName: el más reciente según recordedAt (evita mezclar historial). */
function latestKpiByNameMap(rows) {
  const byName = new Map();
  (rows || []).forEach((kv) => {
    if (!kv || !kv.kpiName) return;
    const prev = byName.get(kv.kpiName);
    if (!prev) {
      byName.set(kv.kpiName, kv);
      return;
    }
    const pt = prev.recordedAt ? new Date(prev.recordedAt).getTime() : 0;
    const ct = kv.recordedAt ? new Date(kv.recordedAt).getTime() : 0;
    if (ct >= pt) byName.set(kv.kpiName, kv);
  });
  return byName;
}

/**
 * Mezcla KPI de proyecto y de sprint. Gana el sprint si existe el mismo kpiName;
 * si un KPI solo está guardado a nivel proyecto (p. ej. reabiertas), se sigue mostrando.
 */
function mergeSprintAndProjectKpiRows(sprintRows, projectRows) {
  const projectByName = latestKpiByNameMap(projectRows);
  const sprintByName = latestKpiByNameMap(sprintRows);
  const merged = new Map(projectByName);
  sprintByName.forEach((v, k) => merged.set(k, v));
  return Array.from(merged.values());
}

const KPI_CATEGORY_LABELS = {
  DELIVERY: 'Entrega',
  QUALITY: 'Calidad',
  ACTIVITY: 'Actividad',
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
    <div className={`kpi-card card ${isPercent ? 'kpi-card--gauge' : 'kpi-card--stat'}`}>
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
        <div className="kpi-card__stat-block">
          <div className="kpi-card__big-val" style={{ color }}>{displayVal}</div>
        </div>
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
  const { projectId, projectName } = useProject();
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [sprints, setSprints] = useState([]);
  const [selectedSprintId, setSelectedSprintId] = useState(null);
  const [insightsLoading, setInsightsLoading] = useState(false);
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

  // ── Data fetching: proyecto + lista de sprints ─────────────────────────────
  useEffect(() => {
    if (!projectId) {
      setLoading(false);
      setSprints([]);
      setSelectedSprintId(null);
      return;
    }

    setLoading(true);
    setError(null);

    (async () => {
      try {
        let sprintsList = [];
        try {
          const sprintRes = await apiFetch(
            `sprints?project_id=${projectId}&page=1&limit=50`
          );
          sprintsList = sprintRes.data || [];
        } catch (_) {
          sprintsList = [];
        }
        setSprints(sprintsList);

        const active = sprintsList.find((s) => s.isActive) || null;
        const defaultSprint = active || sprintsList[0] || null;
        const defaultId = defaultSprint
          ? String(defaultSprint.sprintId)
          : PROJECT_SCOPE_ALL;
        setSelectedSprintId(defaultId);

        const [vel, cum, comp, ms, devSp, hoursSp] = await Promise.allSettled([
          apiFetch(`reports/sprint-velocity?project_id=${projectId}&last_n=6`),
          apiFetch(`reports/cumulative-flow?project_id=${projectId}&weeks=7`),
          apiFetch(`reports/task-completion?project_id=${projectId}`),
          apiFetch(`reports/milestones?project_id=${projectId}`),
          apiFetch(`reports/tasks-by-developer-sprint?project_id=${projectId}`),
          apiFetch(`reports/hours-by-user-sprint?project_id=${projectId}`),
        ]);

        if (vel.status === 'fulfilled') setVelocity(vel.value);
        if (cum.status === 'fulfilled') setCumulative(cum.value);
        if (comp.status === 'fulfilled') setCompletion(comp.value?.data || []);
        if (ms.status === 'fulfilled') setMilestones(ms.value?.milestones || []);
        if (devSp.status === 'fulfilled') setDevSprint(devSp.value);
        if (hoursSp.status === 'fulfilled') setHoursSprint(hoursSp.value);
      } catch (err) {
        setError(err.message || 'Error al cargar los reportes.');
      } finally {
        setLoading(false);
      }
    })();
  }, [projectId]);

  // ── KPI del sprint elegido o del proyecto + burndown (solo por sprint) ─────
  useEffect(() => {
    if (!projectId || !selectedSprintId) {
      setKpiValues([]);
      setBurndown(null);
      setInsightsLoading(false);
      return;
    }

    const isProjectScope = selectedSprintId === PROJECT_SCOPE_ALL;

    let cancelled = false;
    setInsightsLoading(true);
    setKpiValues([]);
    setBurndown(null);

    (async () => {
      try {
        let rows = [];
        if (isProjectScope) {
          const pRes = await apiFetch(
            `kpi-values?scope_type=PROJECT&project_id=${projectId}&limit=100`
          );
          rows = Array.from(latestKpiByNameMap(pRes.data || []).values());
        } else {
          let sprintRows = [];
          let projectRows = [];
          const [sRes, pRes] = await Promise.allSettled([
            apiFetch(
              `kpi-values?scope_type=SPRINT&sprint_id=${selectedSprintId}&limit=100`
            ),
            apiFetch(`kpi-values?project_id=${projectId}&limit=100`),
          ]);
          if (sRes.status === 'fulfilled') sprintRows = sRes.value.data || [];
          if (pRes.status === 'fulfilled') projectRows = pRes.value.data || [];
          rows = mergeSprintAndProjectKpiRows(sprintRows, projectRows);
        }

        let bd = null;
        if (!isProjectScope) {
          try {
            bd = await apiFetch(`reports/burndown?sprint_id=${selectedSprintId}`);
          } catch (_) {
            bd = null;
          }
        }

        if (!cancelled) {
          setKpiValues(rows);
          setBurndown(bd);
        }
      } catch (_) {
        if (!cancelled) {
          setKpiValues([]);
          setBurndown(null);
        }
      } finally {
        if (!cancelled) setInsightsLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [projectId, selectedSprintId]);

  const isProjectScope = selectedSprintId === PROJECT_SCOPE_ALL;

  const selectedSprint = useMemo(
    () =>
      isProjectScope
        ? null
        : sprints.find((s) => String(s.sprintId) === String(selectedSprintId)) || null,
    [sprints, selectedSprintId, isProjectScope]
  );

  const sprintLabel = useMemo(() => {
    if (isProjectScope) {
      return projectName ? `Todo el proyecto — ${projectName}` : 'Todo el proyecto';
    }
    if (selectedSprint) {
      if (selectedSprint.sprintNumber != null) {
        return `Sprint ${selectedSprint.sprintNumber} — ${selectedSprint.name || ''}`.trim();
      }
      return selectedSprint.name || 'Sprint';
    }
    if (loading) return 'Cargando…';
    return sprints.length === 0 ? 'Sin sprints' : 'Elige un sprint';
  }, [isProjectScope, projectName, selectedSprint, loading, sprints.length]);

  const insightsBusy = loading || insightsLoading;

  // ── Export PDF (client-side — backend placeholder no genera PDF válido) ───
  const handleExport = () => {
    if (exporting) return;
    setExportError(null);
    setExporting(true);

    try {
      const exportSprintTitle = isProjectScope
        ? (projectName ? `Todo el proyecto — ${projectName}` : 'Todo el proyecto')
        : selectedSprint?.sprintNumber != null
          ? `Sprint ${selectedSprint.sprintNumber} — ${selectedSprint.name || ''}`.trim()
          : selectedSprint?.name || 'Sin sprint seleccionado';
      const date = new Date().toLocaleDateString('es-MX', { dateStyle: 'long' });

      const latestKpiByName = {};
      kpiValues.forEach((kv) => {
        if (!DASHBOARD_KPI_ORDER.includes(kv.kpiName)) return;
        latestKpiByName[kv.kpiName] = kv;
      });

      const kpiRows = DASHBOARD_KPI_ORDER.map((name) => {
        const kv = latestKpiByName[name] || dashboardKpiPlaceholder(name);
        const meta = KPI_META[kv.kpiName] || { label: kv.kpiName };
        const cat = kv.category || 'OTHER';
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
      }).join('');

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
  <title>Reporte — ${exportSprintTitle}</title>
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
  <div class="report-meta">${projectName} &nbsp;·&nbsp; ${exportSprintTitle} &nbsp;·&nbsp; Generado el ${date}</div>

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

  ${milestoneRows ? `<h2>Hitos del proyecto</h2>
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
    } finally {
      setExporting(false);
    }
  };

  // ── Chart data builders ───────────────────────────────────────────────────
  const burndownChartData = burndown
    ? {
        labels: (burndown.ideal_line || []).map((d) => d.day),
        datasets: [
          {
            label: 'Línea ideal',
            data: (burndown.ideal_line || []).map((d) => d.ideal_remaining),
            borderColor: C.slate,
            borderDash: [6, 4],
            borderWidth: 2,
            pointRadius: 0,
            tension: 0,
            fill: false,
          },
          {
            label: 'Avance real',
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
            label: 'Completadas',
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
            label: 'Pendientes',
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

  // ── Dashboard KPI: último valor por nombre, solo las 5 tarjetas definidas ───
  const latestByName = {};
  kpiValues.forEach((kv) => {
    if (!DASHBOARD_KPI_ORDER.includes(kv.kpiName)) return;
    latestByName[kv.kpiName] = kv;
  });

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
          <div className="reports-sprint-select-wrap">
            <span className="material-icons reports-sprint-select__icon" aria-hidden="true">
              calendar_today
            </span>
            <select
              id="reports-sprint-select"
              className="reports-sprint-select"
              aria-label="Alcance de KPI y burndown"
              value={selectedSprintId ?? ''}
              onChange={(e) => setSelectedSprintId(e.target.value ? String(e.target.value) : null)}
              disabled={loading}
            >
              {loading ? (
                <option value="">Cargando sprints…</option>
              ) : (
                <>
                  <option value={PROJECT_SCOPE_ALL}>
                    {projectName ? `Todo el proyecto — ${projectName}` : 'Todo el proyecto'}
                  </option>
                  {sprints.length === 0 ? (
                    <option value="" disabled>
                      Sin sprints registrados
                    </option>
                  ) : (
                    sprints.map((s) => (
                      <option key={s.sprintId} value={String(s.sprintId)}>
                        {s.sprintNumber != null
                          ? `Sprint ${s.sprintNumber} — ${s.name || ''}`.trim()
                          : s.name || `Sprint ${s.sprintId}`}
                      </option>
                    ))
                  )}
                </>
              )}
            </select>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
            <button
              className="btn btn--primary"
              onClick={handleExport}
              disabled={insightsBusy || !projectId || exporting}
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
          <span className="text-sm text-muted">
            {isProjectScope
              ? 'Métricas agregadas de todos los sprints del proyecto'
              : 'Métricas clave del proyecto'}
          </span>
        </div>

        {insightsBusy ? (
          <div className="kpi-grid mt-16">
            {[...Array(4)].map((_, i) => (
              <Skeleton key={i} h={110} className="card" />
            ))}
          </div>
        ) : (
          <div className="kpi-grid mt-16">
            {DASHBOARD_KPI_ORDER.map((name) => {
              const kv = latestByName[name] || dashboardKpiPlaceholder(name);
              const cat = kv.category || 'OTHER';
              return <KpiCard key={name} kpi={kv} category={cat} />;
            })}
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
                  : selectedSprint?.name || sprintLabel}
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

          {insightsBusy ? (
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
            <NoData
              text={
                isProjectScope
                  ? 'El burndown aplica por sprint. Selecciona uno en el listado.'
                  : 'Sin datos de burndown para este sprint'
              }
            />
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
                  puntos/sprint
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
            <span className="text-sm text-muted">Total de horas registradas por sprint</span>
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
            Hitos del proyecto
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
          <NoData text="Sin hitos registrados" />
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
