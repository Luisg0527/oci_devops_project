import React from 'react';
import ProgressBar from './ProgressBar';
import './TeamMemberCard.css';

function roleTagFromRoleName(roleName) {
  if (!roleName) return '—';
  const parts = roleName.trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return roleName.substring(0, 3).toUpperCase();
}

function TeamMemberCard({ member, onViewDetails }) {
  const initials = (member.fullName || '?')
    .split(' ')
    .filter(Boolean)
    .map((n) => n[0])
    .join('')
    .slice(0, 3);

  const workload = typeof member.workload === 'number' ? member.workload : 0;

  return (
    <div className="member-card card">
      <div className="member-card__top">
        <div className="member-card__avatar-wrap">
          <div className="member-card__avatar">
            <span>{initials}</span>
          </div>
          <span className="member-card__role-tag">{roleTagFromRoleName(member.roleName)}</span>
        </div>
      </div>

      <div className="member-card__info">
        <h4 className="member-card__name">{member.fullName}</h4>
        <p className="member-card__role">{member.roleName || 'Sin rol'}</p>
      </div>

      <div className="member-card__bottom">
        <ProgressBar value={workload} showLabel />
        <button
          type="button"
          className="member-card__btn"
          onClick={() => onViewDetails && onViewDetails(member)}
        >
          Ver detalles
        </button>
      </div>
    </div>
  );
}

export default TeamMemberCard;
