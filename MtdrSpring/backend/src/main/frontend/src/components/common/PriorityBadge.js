import React from 'react';
import './PriorityBadge.css';

const priorityConfig = {
  'HIGH':   { className: 'priority-badge--high',   label: 'Alta' },
  'MEDIUM': { className: 'priority-badge--medium', label: 'Media' },
  'LOW':    { className: 'priority-badge--low',    label: 'Baja' },
};

function PriorityBadge({ priority }) {
  const config = priorityConfig[priority] || { className: '', label: priority };

  return (
    <span className={`priority-badge ${config.className}`}>
      {config.label}
    </span>
  );
}

export default PriorityBadge;
