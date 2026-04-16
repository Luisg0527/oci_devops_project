import React from 'react';
import './ProgressBar.css';

function ProgressBar({ value, color, height = 6, showLabel = false }) {
  const barColor = color || (
    value >= 80 ? 'var(--color-red)' :
    value >= 60 ? 'var(--color-gold)' :
    'var(--color-teal)'
  );

  return (
    <div className="progress-bar">
      {showLabel && (
        <div className="progress-bar__labels">
          <span className="progress-bar__label">Carga</span>
          <span className="progress-bar__value">{value}%</span>
        </div>
      )}
      <div className="progress-bar__track" style={{ height }}>
        <div
          className="progress-bar__fill"
          style={{ width: `${value}%`, background: barColor }}
        />
      </div>
    </div>
  );
}

export default ProgressBar;
