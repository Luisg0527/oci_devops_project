import React, { useState } from 'react';
import PageLayout from '../components/layout/PageLayout';
import StatCard from '../components/common/StatCard';
import MilestoneTimeline from '../components/common/MilestoneTimeline';
import ActivityItem from '../components/common/ActivityItem';
import {
  dashboardStats, milestones, resourceAllocation, criticalActivity,
} from '../data/mockData';
import './ManagerDashboard.css';

function ManagerDashboard() {
  const [notifications, setNotifications] = useState(criticalActivity);

  return (
    <PageLayout>
      {/* Hero */}
      <section className="mgr-hero">
        <div className="mgr-hero__text">
          <h2 className="section-title">Resumen del Espacio</h2>
          <p className="section-subtitle">
            Tienes 4 proyectos que requieren atención inmediata esta semana.
          </p>
        </div>
        <div className="mgr-hero__stats">
          <div className="mgr-hero__stat-card card">
            <span className="text-sm text-muted">Salud del Proyecto</span>
            <div className="mgr-hero__stat-value">
              <div className="mgr-hero__dot mgr-hero__dot--green" />
              <span>{dashboardStats.projectHealth}</span>
            </div>
          </div>
          <div className="mgr-hero__stat-card card">
            <span className="text-sm text-muted">Capacidad del Equipo</span>
            <div className="mgr-hero__stat-value">
              <span>{dashboardStats.teamCapacity}%</span>
            </div>
          </div>
        </div>
      </section>

      {/* Metric Cards */}
      <div className="bento-grid bento-grid--3col mt-24">
        <StatCard
          icon="account_balance_wallet"
          title="Uso del Presupuesto"
          value={dashboardStats.budget.value}
          subtitle={dashboardStats.budget.label}
          status={dashboardStats.budget.status}
          statusColor="var(--color-green)"
        >
          <div className="mgr-budget-bar">
            <div className="mgr-budget-bar__fill" style={{ width: `${dashboardStats.budget.percent}%` }} />
          </div>
        </StatCard>

        <StatCard
          icon="speed"
          title="Velocidad del Equipo"
          value={dashboardStats.velocity.value}
          unit={dashboardStats.velocity.unit}
          subtitle={dashboardStats.velocity.change}
        >
          <div className="mgr-velocity-bars">
            {[60, 80, 70, 90].map((h, i) => (
              <div key={i} className="mgr-velocity-bar" style={{ height: `${h}%` }} />
            ))}
          </div>
        </StatCard>

        <StatCard
          icon="verified"
          title="Puntuación de Calidad"
          value={dashboardStats.quality.value}
          subtitle={dashboardStats.quality.label}
        >
          <div className="mgr-stars">
            {Array.from({ length: dashboardStats.quality.stars }).map((_, i) => (
              <span key={i} className="material-icons mgr-star">star</span>
            ))}
          </div>
        </StatCard>
      </div>

      {/* Milestones */}
      <div className="card bento-grid--span-full mt-24">
        <div className="flex justify-between items-center mb-24">
          <h3 className="stat-card__title" style={{ fontSize: 18, margin: 0 }}>Próximos Hitos</h3>
          <button className="btn btn--ghost btn--small">Ver Hoja de Ruta</button>
        </div>
        <MilestoneTimeline milestones={milestones} />
      </div>

      {/* Bottom row */}
      <div className="bento-grid bento-grid--2col mt-24">
        {/* Resource Allocation */}
        <div className="card">
          <h3 className="stat-card__title">Asignación de Recursos</h3>
          <div className="mgr-resources">
            <div className="mgr-resources__list">
              {resourceAllocation.map((r) => (
                <div key={r.name} className="mgr-resource-row">
                  <div className="mgr-resource-row__left">
                    <div className="mgr-resource-dot" style={{ background: r.color }} />
                    <span>{r.name}</span>
                  </div>
                  <span className="font-bold">{r.percent}%</span>
                </div>
              ))}
            </div>
            <div className="mgr-donut">
              <svg viewBox="0 0 100 100" className="mgr-donut__svg">
                <circle cx="50" cy="50" r="38" fill="none" stroke="var(--color-bg-badge)" strokeWidth="12" />
                <circle cx="50" cy="50" r="38" fill="none" stroke="var(--color-teal)" strokeWidth="12"
                  strokeDasharray="72 167" strokeDashoffset="0" transform="rotate(-90 50 50)" />
                <circle cx="50" cy="50" r="38" fill="none" stroke="var(--color-primary)" strokeWidth="12"
                  strokeDasharray="96 143" strokeDashoffset="-72" transform="rotate(-90 50 50)" />
              </svg>
              <div className="mgr-donut__center">
                <span className="mgr-donut__number">24</span>
                <span className="mgr-donut__label">Puestos Activos</span>
              </div>
            </div>
          </div>
        </div>

        {/* Critical Activity (INCIDENTS) */}
        <div className="card">
          <h3 className="stat-card__title">Actividad Crítica</h3>
          <div className="mgr-activity">
            {notifications.map((incident) => (
              <ActivityItem
                key={incident.incidentId}
                fullName={incident.title}
                description={incident.description}
                actionDate={incident.occurredAt}
                severity={incident.severity}
              />
            ))}
          </div>
          {notifications.length > 0 && (
            <button
              className="btn btn--ghost btn--small mt-16"
              onClick={() => setNotifications([])}
            >
              Limpiar Notificaciones
            </button>
          )}
        </div>
      </div>

      {/* Featured Project */}
      <div className="mgr-featured mt-24">
        <div className="mgr-featured__overlay">
          <span className="mgr-featured__label">Proyecto Destacado</span>
          <h2 className="mgr-featured__title">Project Meridian: Campus Urbano Sustentable</h2>
          <p className="mgr-featured__desc">
            La fase 2 de ingeniería está 3 días adelantada al cronograma.
          </p>
        </div>
      </div>
    </PageLayout>
  );
}

export default ManagerDashboard;
