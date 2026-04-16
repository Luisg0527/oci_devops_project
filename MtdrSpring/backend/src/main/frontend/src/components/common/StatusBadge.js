import React from 'react';
import './StatusBadge.css';

const statusConfig = {
  'PENDING':     { icon: 'radio_button_unchecked', className: 'status-badge--todo',     label: 'Pendiente' },
  'IN_PROGRESS': { icon: 'autorenew',              className: 'status-badge--progress',  label: 'En Progreso' },
  'DONE':        { icon: 'check_circle',           className: 'status-badge--done',      label: 'Completado' },
  'CANCELLED':   { icon: 'cancel',                 className: 'status-badge--cancelled', label: 'Cancelado' },
  'REOPENED':    { icon: 'replay',                 className: 'status-badge--reopened',  label: 'Reabierto' },
};

function StatusBadge({ status }) {
  const config = statusConfig[status] || statusConfig['PENDING'];

  return (
    <span className={`status-badge ${config.className}`}>
      <span className="material-icons status-badge__icon">{config.icon}</span>
      {config.label}
    </span>
  );
}

export default StatusBadge;
