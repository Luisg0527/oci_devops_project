import React from 'react';
import './TaskRow.css';

function TaskRow({ task, onToggle }) {
  const isUrgent = task.priority === 'HIGH';

  return (
    <div className={`task-row ${isUrgent ? 'task-row--urgent' : ''}`}>
      <div className="task-row__left">
        <button className="task-row__check" onClick={() => onToggle && onToggle(task.taskId)}>
          <span className="material-icons">
            {task.status === 'DONE' ? 'check_circle' : 'radio_button_unchecked'}
          </span>
        </button>
        <div className="task-row__info">
          <h4 className="task-row__title">{task.title}</h4>
          <span className="task-row__meta">{task.description}</span>
        </div>
      </div>
      <div className={`task-row__due ${isUrgent ? 'task-row__due--urgent' : ''}`}>
        {task.dueLabel}
      </div>
    </div>
  );
}

export default TaskRow;
