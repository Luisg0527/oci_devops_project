import React, { useState } from 'react';
import PageLayout from '../components/layout/PageLayout';
import TeamMemberCard from '../components/common/TeamMemberCard';
import ProgressBar from '../components/common/ProgressBar';
import { teamMembers } from '../data/mockData';
import './TeamManagement.css';

function TeamManagement() {
  const [members, setMembers] = useState(teamMembers);
  const [showInvite, setShowInvite] = useState(false);
  const [invite, setInvite] = useState({ fullName: '', roleName: '', roleId: 3 });

  const handleInvite = (e) => {
    e.preventDefault();
    if (!invite.fullName.trim()) return;
    const newMember = {
      userId: Date.now(),
      fullName: invite.fullName,
      email: '',
      telegramId: null,
      roleId: invite.roleId,
      roleName: invite.roleName || 'Desarrollador',
      teamId: 1,
      status: 'ACTIVE',
      workload: 0,
    };
    setMembers(prev => [...prev, newMember]);
    setInvite({ fullName: '', roleName: '', roleId: 3 });
    setShowInvite(false);
  };

  const networkHealth = Math.round(
    members.reduce((sum, m) => sum + m.workload, 0) / members.length
  );

  return (
    <PageLayout searchPlaceholder="Buscar equipo...">
      {/* Header */}
      <section className="team-header">
        <div>
          <h2 className="section-title" style={{ fontSize: 36 }}>Gestión de Equipo</h2>
          <p className="section-subtitle">
            Coordina recursos en los pipelines arquitectónicos activos.
          </p>
        </div>
        <button className="btn btn--primary" onClick={() => setShowInvite(!showInvite)}>
          <span className="material-icons" style={{ fontSize: 18 }}>person_add</span>
          Invitar Miembro
        </button>
      </section>

      {/* Invite Form */}
      {showInvite && (
        <form className="team-invite card mt-16" onSubmit={handleInvite}>
          <input
            type="text"
            className="backlog-input"
            placeholder="Nombre completo..."
            value={invite.fullName}
            onChange={e => setInvite(p => ({ ...p, fullName: e.target.value }))}
            autoFocus
          />
          <input
            type="text"
            className="backlog-input"
            placeholder="Título del rol..."
            value={invite.roleName}
            onChange={e => setInvite(p => ({ ...p, roleName: e.target.value }))}
          />
          <select
            className="backlog-select"
            value={invite.roleId}
            onChange={e => setInvite(p => ({ ...p, roleId: Number(e.target.value) }))}
          >
            <option value={3}>Desarrollador</option>
            <option value={2}>Diseñador</option>
            <option value={1}>Gerente</option>
          </select>
          <button type="submit" className="btn btn--primary btn--small">Invitar</button>
          <button type="button" className="btn btn--ghost btn--small" onClick={() => setShowInvite(false)}>Cancelar</button>
        </form>
      )}

      {/* Stats Row */}
      <div className="team-stats mt-24">
        <div className="card card--dark team-ai-tip">
          <span className="material-icons team-ai-icon">psychology</span>
          <div>
            <h3 className="team-ai-tip__title">Consejo de IA</h3>
            <p className="team-ai-tip__text">
              El análisis de capacidad muestra un cuello de botella en Front-end.
              Considera mover a Alex al Proyecto Zenith.
            </p>
          </div>
        </div>
        <div className="card team-health">
          <div className="team-health__top">
            <div>
              <span className="text-sm text-muted font-bold">Salud de Red</span>
              <div className="team-health__value">
                <span className="team-health__number">{networkHealth}%</span>
                <span className="team-health__label">Utilización</span>
              </div>
            </div>
          </div>
          <p className="text-sm text-muted mt-8">
            Tu equipo opera a máxima eficiencia. {members.length} miembros están
            cerca del ancho de banda máximo para el Sprint 14.
          </p>
          <ProgressBar value={networkHealth} color="var(--color-primary)" height={8} />
        </div>
      </div>

      {/* Team Grid */}
      <div className="team-grid mt-24">
        {members.map(member => (
          <TeamMemberCard key={member.userId} member={member} />
        ))}
      </div>
    </PageLayout>
  );
}

export default TeamManagement;
