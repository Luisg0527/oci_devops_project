import React, { useState } from 'react';
import PageLayout from '../components/layout/PageLayout';
import ProgressBar from '../components/common/ProgressBar';
import { sprintVelocityData, taskCompletionData, sprintInfo } from '../data/mockData';
import './Reports.css';

const burndownIdeal = [100, 83, 67, 50, 33, 17, 0];
const burndownActual = [100, 88, 72, 55, 40, 28, null];
const days = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
const cumulativeWeeks = ['S1', 'S2', 'S3', 'S4', 'S5', 'S6', 'S7'];

function Reports() {
  const [velocityView, setVelocityView] = useState('recent');

  const maxPoints = Math.max(...sprintVelocityData.map(d => d.points));

  const idealPath = burndownIdeal.map((v, i) => {
    const x = 20 + i * (460 / 6);
    const y = 20 + (100 - v) * 1.6;
    return `${x},${y}`;
  }).join(' ');

  const actualPoints = burndownActual.filter(v => v !== null);
  const actualPath = actualPoints.map((v, i) => {
    const x = 20 + i * (460 / 6);
    const y = 20 + (100 - v) * 1.6;
    return `${x},${y}`;
  }).join(' ');

  return (
    <PageLayout searchPlaceholder="Buscar analíticas...">
      {/* Header */}
      <section className="reports-header">
        <div>
          <span className="section-label">Motor de Análisis</span>
          <h2 className="section-title" style={{ fontSize: 36 }}>Reportes de Rendimiento</h2>
        </div>
        <div className="reports-header__actions">
          <button className="btn btn--white">
            <span className="material-icons" style={{ fontSize: 18 }}>calendar_today</span>
            Sprint Actual
          </button>
          <button className="btn btn--primary">
            <span className="material-icons" style={{ fontSize: 18 }}>file_download</span>
            Exportar PDF
          </button>
        </div>
      </section>

      {/* Top row: Burndown + Sidebar metrics */}
      <div className="reports-top mt-24">
        {/* Burndown Chart */}
        <div className="card reports-burndown">
          <div className="flex justify-between items-center mb-24">
            <div>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Gráfica Burndown</h3>
              <span className="text-sm text-muted">Sprint 24: Infraestructura Base</span>
            </div>
            <span className="reports-status-pill">En Curso</span>
          </div>
          <svg viewBox="0 0 500 200" className="reports-chart-svg">
            <polyline points={idealPath} fill="none" stroke="var(--color-divider)" strokeWidth="2" strokeDasharray="6 4" />
            <polyline points={actualPath} fill="none" stroke="var(--color-primary)" strokeWidth="2.5" />
            {actualPoints.map((v, i) => {
              const x = 20 + i * (460 / 6);
              const y = 20 + (100 - v) * 1.6;
              return <circle key={i} cx={x} cy={y} r="4" fill="var(--color-primary)" />;
            })}
          </svg>
          <div className="reports-chart-labels">
            {days.map(d => <span key={d} className="text-sm text-muted">{d}</span>)}
          </div>
        </div>

        {/* Metrics sidebar */}
        <div className="reports-metrics-sidebar">
          <div className="card card--dark reports-velocity-card">
            <span className="material-icons" style={{ fontSize: 24, opacity: 0.7 }}>speed</span>
            <div>
              <h4 style={{ margin: 0, fontSize: 14 }}>Velocidad del Sprint</h4>
              <div className="reports-velocity-val">
                <span style={{ fontSize: 36, fontWeight: 700 }}>{sprintInfo.currentVelocity}</span>
                <span className="text-sm" style={{ opacity: 0.7 }}>{sprintInfo.velocityChange}</span>
              </div>
            </div>
          </div>
          <div className="card reports-cumulative">
            <div className="flex justify-between items-center">
              <h4 style={{ margin: 0, fontSize: 14 }}>Flujo Acumulado</h4>
              <span className="material-icons text-muted" style={{ fontSize: 18 }}>open_in_new</span>
            </div>
            <div className="reports-cumulative-bars mt-8">
              {[40, 55, 60, 70, 65, 75, 80].map((h, i) => (
                <div key={i} className="reports-cum-bar-wrap">
                  <div className="reports-cum-bar" style={{ height: `${h}%` }} />
                </div>
              ))}
            </div>
            <div className="reports-chart-labels mt-8">
              {cumulativeWeeks.map(w => <span key={w} className="text-sm text-muted">{w}</span>)}
            </div>
          </div>
        </div>
      </div>

      {/* Sprint Velocity Bar Chart */}
      <div className="card mt-24">
        <div className="flex justify-between items-center mb-24">
          <div>
            <h3 style={{ margin: 0, fontSize: 20, fontWeight: 700 }}>Velocidad del Sprint</h3>
            <span className="text-sm text-muted">Puntos entregados por iteración de sprint</span>
          </div>
          <div className="reports-toggle">
            <button
              className={`reports-toggle__btn ${velocityView === 'recent' ? 'reports-toggle__btn--active' : ''}`}
              onClick={() => setVelocityView('recent')}
            >
              Últimos 6 Sprints
            </button>
            <button
              className={`reports-toggle__btn ${velocityView === 'all' ? 'reports-toggle__btn--active' : ''}`}
              onClick={() => setVelocityView('all')}
            >
              Histórico
            </button>
          </div>
        </div>
        <div className="reports-bar-chart">
          {sprintVelocityData.map((d) => (
            <div key={d.sprintId} className="reports-bar-col">
              <span className="reports-bar-value">{d.points}</span>
              <div className="reports-bar" style={{ height: `${(d.points / maxPoints) * 100}%` }} />
              <span className="text-sm text-muted">{d.sprintName}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Task Completion Rate Table */}
      <div className="card mt-24" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="reports-table-header">
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Tasa de Completado de Tareas</h3>
          <span className="reports-table-badge">Por Miembro del Equipo</span>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Miembro</th>
              <th>Asignadas</th>
              <th>Completadas</th>
              <th>Eficiencia</th>
              <th>Tendencia</th>
              <th>Acción</th>
            </tr>
          </thead>
          <tbody>
            {taskCompletionData.map(row => (
              <tr key={row.userId}>
                <td>
                  <div className="flex items-center gap-12">
                    <div className="backlog-assignee__avatar">
                      {row.fullName.split(' ').map(n => n[0]).join('')}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600 }}>{row.fullName}</div>
                      <div className="text-sm text-muted">{row.roleName}</div>
                    </div>
                  </div>
                </td>
                <td>{row.assignedTasks} Tareas</td>
                <td>{row.completedTasks} Tareas</td>
                <td>
                  <div className="flex items-center gap-8">
                    <ProgressBar value={row.efficiency} height={6} />
                    <span className="text-sm font-bold">{row.efficiency}%</span>
                  </div>
                </td>
                <td>
                  <span className="material-icons" style={{ color: row.efficiency >= 90 ? 'var(--color-green)' : 'var(--color-gold)', fontSize: 20 }}>
                    {row.efficiency >= 90 ? 'trending_up' : 'trending_flat'}
                  </span>
                </td>
                <td>
                  <button className="btn btn--ghost btn--small">Ver Detalles</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </PageLayout>
  );
}

export default Reports;
