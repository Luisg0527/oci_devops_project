import React from 'react';
import './MilestoneTimeline.css';

function MilestoneTimeline({ milestones }) {
  return (
    <div className="milestone-timeline">
      <div className="milestone-timeline__line" />
      <div className="milestone-timeline__items">
        {milestones.map((ms) => (
          <div key={ms.id} className={`milestone-timeline__item milestone-timeline__item--${ms.status}`}>
            <div className="milestone-timeline__dot">
              {ms.status === 'done' && <span className="material-icons">check</span>}
              {ms.status === 'current' && <div className="milestone-timeline__ring" />}
            </div>
            <span className="milestone-timeline__name">{ms.name}</span>
            <span className="milestone-timeline__date">{ms.date}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default MilestoneTimeline;
