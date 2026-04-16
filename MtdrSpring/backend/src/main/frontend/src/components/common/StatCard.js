import React from 'react';
import './StatCard.css';

function StatCard({ icon, title, value, unit, subtitle, status, statusColor, children }) {
  return (
    <div className="stat-card card">
      <div className="stat-card__header">
        <div className="stat-card__icon-wrap">
          <span className="material-icons">{icon}</span>
        </div>
        {status && (
          <span className="stat-card__status" style={{ background: statusColor || 'var(--color-green)' }}>
            {status}
          </span>
        )}
      </div>
      <h3 className="stat-card__title">{title}</h3>
      <div className="stat-card__value">
        <span className="stat-card__number">{value}</span>
        {unit && <span className="stat-card__unit">{unit}</span>}
      </div>
      {children}
      {subtitle && <p className="stat-card__subtitle">{subtitle}</p>}
    </div>
  );
}

export default StatCard;
