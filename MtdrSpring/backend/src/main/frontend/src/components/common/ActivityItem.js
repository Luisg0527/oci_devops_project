import React from 'react';
import './ActivityItem.css';

const severityColorMap = {
  HIGH: 'var(--color-red-dark)',
  CRITICAL: 'var(--color-red)',
  MEDIUM: 'var(--color-primary)',
  LOW: 'var(--color-green)',
};

function ActivityItem({ fullName, entityName, actionDate, description, severity }) {
  const initials = fullName.split(' ').map(n => n[0]).join('');

  return (
    <div className="activity-item">
      {severity ? (
        <div className="activity-item__dot" style={{ background: severityColorMap[severity] || severityColorMap.MEDIUM }} />
      ) : (
        <div className="activity-item__avatar">{initials}</div>
      )}
      <div className="activity-item__content">
        {severity ? (
          <>
            <p className="activity-item__title">{fullName}</p>
            <p className="activity-item__msg">{description}</p>
          </>
        ) : (
          <>
            <div className="activity-item__meta">
              <span className="activity-item__author">{fullName} on {entityName}</span>
              <span className="activity-item__time">{actionDate}</span>
            </div>
            <p className="activity-item__msg activity-item__msg--quote">"{description}"</p>
          </>
        )}
        {severity && <span className="activity-item__time">{actionDate}</span>}
      </div>
    </div>
  );
}

export default ActivityItem;
