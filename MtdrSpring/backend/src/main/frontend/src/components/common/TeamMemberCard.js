import React from 'react';
import ProgressBar from './ProgressBar';
import { getRoleTag } from '../../data/mockData';
import './TeamMemberCard.css';

function TeamMemberCard({ member, onViewDetails }) {
  const initials = member.fullName.split(' ').map(n => n[0]).join('');

  return (
    <div className="member-card card">
      <div className="member-card__top">
        <div className="member-card__avatar-wrap">
          <div className="member-card__avatar">
            <span>{initials}</span>
          </div>
          <span className="member-card__role-tag">{getRoleTag(member.roleId)}</span>
        </div>
      </div>

      <div className="member-card__info">
        <h4 className="member-card__name">{member.fullName}</h4>
        <p className="member-card__role">{member.roleName}</p>
      </div>

      <div className="member-card__bottom">
        <ProgressBar value={member.workload} showLabel />
        <button className="member-card__btn" onClick={() => onViewDetails && onViewDetails(member)}>
          Ver Detalles
        </button>
      </div>
    </div>
  );
}

export default TeamMemberCard;
