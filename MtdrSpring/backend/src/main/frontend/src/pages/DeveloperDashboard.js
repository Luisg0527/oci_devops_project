import React, { useState, useEffect, useRef } from 'react';
import PageLayout from '../components/layout/PageLayout';
import TaskRow from '../components/common/TaskRow';
import ActivityItem from '../components/common/ActivityItem';
import ProgressBar from '../components/common/ProgressBar';
import { devTasks, activityFeed, sprintInfo } from '../data/mockData';
import './DeveloperDashboard.css';

function DeveloperDashboard() {
  const [tasks, setTasks] = useState(devTasks);
  const [timerRunning, setTimerRunning] = useState(true);
  const [seconds, setSeconds] = useState(4 * 3600 + 22 * 60 + 15);
  const intervalRef = useRef(null);

  useEffect(() => {
    if (timerRunning) {
      intervalRef.current = setInterval(() => setSeconds(s => s + 1), 1000);
    }
    return () => clearInterval(intervalRef.current);
  }, [timerRunning]);

  const formatTime = (s) => {
    const h = Math.floor(s / 3600).toString().padStart(2, '0');
    const m = Math.floor((s % 3600) / 60).toString().padStart(2, '0');
    const sec = (s % 60).toString().padStart(2, '0');
    return { h, m, sec };
  };

  const handleToggleTask = (taskId) => {
    setTasks(prev => prev.map(t =>
      t.taskId === taskId ? { ...t, status: t.status === 'DONE' ? 'PENDING' : 'DONE' } : t
    ));
  };

  const handleComplete = () => {
    setTimerRunning(false);
    setSeconds(0);
  };

  const time = formatTime(seconds);

  return (
    <PageLayout searchPlaceholder="Buscar arquitectura, docs...">
      {/* Welcome */}
      <section className="dev-welcome">
        <span className="section-label" style={{ color: 'var(--color-gold)' }}>Panel del Desarrollador</span>
        <h2 className="section-title">Arquitectura del Sistema: Sesión Activa</h2>
        <p className="section-subtitle">
          Buenos días. Tienes 3 tareas de alta prioridad y 2 revisiones pendientes para el sprint actual.
        </p>
      </section>

      <div className="dev-grid mt-24">
        {/* Left column */}
        <div className="dev-grid__left">
          {/* Time Tracking */}
          <div className="card dev-timer">
            <div className="dev-timer__header">
              <span className="text-sm text-muted">Seguimiento de Tiempo</span>
              <h3 className="dev-timer__title">Sesión Actual</h3>
            </div>
            <div className="dev-timer__display">
              <span className="dev-timer__digits">{time.h}:{time.m}</span>
              <span className="dev-timer__seconds">:{time.sec}</span>
            </div>
            <div className="dev-timer__task">
              <div className="dev-timer__task-name">
                Refactorización Módulo Auth
              </div>
              <div className="dev-timer__actions">
                <button
                  className="btn btn--primary btn--small"
                  onClick={() => setTimerRunning(!timerRunning)}
                >
                  {timerRunning ? 'Pausar' : 'Reanudar'}
                </button>
                <button
                  className="btn btn--white btn--small"
                  onClick={handleComplete}
                >
                  Completar
                </button>
              </div>
            </div>
          </div>

          {/* Sprint Progress */}
          <div className="card dev-sprint">
            <div className="flex justify-between items-center">
              <div>
                <span className="text-sm text-muted">{sprintInfo.name}</span>
                <h4 className="dev-sprint__velocity">{sprintInfo.velocityPercent}% Velocidad</h4>
              </div>
              <span className="dev-sprint__days">{sprintInfo.daysLeft} Días Restantes</span>
            </div>
            <ProgressBar value={sprintInfo.velocityPercent} color="var(--color-primary)" height={8} />
            <span className="text-sm text-muted mt-8" style={{ display: 'block' }}>
              {sprintInfo.pointsCompleted} de {sprintInfo.pointsTotal} puntos de historia completados
            </span>
          </div>
        </div>

        {/* Right column - Tasks */}
        <div className="card dev-tasks">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-8">
              <h3 style={{ margin: 0, fontSize: 20 }}>Mis Tareas</h3>
              <span className="text-muted" style={{ fontSize: 14 }}>(Próximas 5)</span>
            </div>
            <button className="btn btn--ghost btn--small">
              Ver Todas <span className="material-icons" style={{ fontSize: 16 }}>arrow_forward</span>
            </button>
          </div>
          <div className="mt-8">
            {tasks.map(task => (
              <TaskRow
                key={task.taskId}
                task={task}
                onToggle={handleToggleTask}
              />
            ))}
          </div>
        </div>
      </div>

      {/* Activity Feed */}
      <div className="card mt-24">
        <div className="flex items-center gap-8 mb-24">
          <span className="material-icons" style={{ color: 'var(--color-text-secondary)' }}>forum</span>
          <h3 style={{ margin: 0, fontSize: 20 }}>Actividad Reciente</h3>
        </div>
        <div className="dev-feed">
          {activityFeed.map(item => (
            <ActivityItem key={item.auditId} {...item} />
          ))}
        </div>
      </div>
    </PageLayout>
  );
}

export default DeveloperDashboard;
