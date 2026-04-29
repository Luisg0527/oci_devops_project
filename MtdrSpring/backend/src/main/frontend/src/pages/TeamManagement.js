import React, { useCallback, useEffect, useMemo, useState } from 'react';
import PageLayout from '../components/layout/PageLayout';
import TeamMemberCard from '../components/common/TeamMemberCard';
import ProgressBar from '../components/common/ProgressBar';
import { API_BASE } from '../config/apiBase';
import './TeamManagement.css';

function authHeaders() {
  const token = localStorage.getItem('authToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function resolveTeamId(headers) {
  let tid = localStorage.getItem('userTeamId');
  if (tid) return tid;
  const res = await fetch(`${API_BASE}/teams`, { headers });
  if (!res.ok) return null;
  const body = await res.json().catch(() => ({}));
  const teams = Array.isArray(body.data) ? body.data : [];
  if (teams.length === 1) {
    const id = String(teams[0].teamId);
    localStorage.setItem('userTeamId', id);
    return id;
  }
  return null;
}

function TeamManagement() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [noTeam, setNoTeam] = useState(false);
  const [teamName, setTeamName] = useState('');
  const [teamDescription, setTeamDescription] = useState('');
  const [members, setMembers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [showInvite, setShowInvite] = useState(false);
  const [invite, setInvite] = useState({
    fullName: '',
    email: '',
    username: '',
    password: '',
    roleId: '',
  });
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    const token = localStorage.getItem('authToken');
    if (!token) {
      setError('Inicia sesión para ver el equipo.');
      setLoading(false);
      return;
    }

    const headers = authHeaders();
    setLoading(true);
    setError('');
    setNoTeam(false);

    try {
      const teamId = await resolveTeamId(headers);
      if (!teamId) {
        setNoTeam(true);
        setMembers([]);
        setTeamName('');
        setTeamDescription('');
        setLoading(false);
        return;
      }

      const [teamRes, rolesRes, usersRes] = await Promise.all([
        fetch(`${API_BASE}/teams/${teamId}`, { headers }),
        fetch(`${API_BASE}/roles`, { headers }),
        fetch(`${API_BASE}/users?teamId=${encodeURIComponent(teamId)}&page=1&limit=200`, { headers }),
      ]);

      if (teamRes.status === 401 || rolesRes.status === 401 || usersRes.status === 401) {
        localStorage.removeItem('authToken');
        window.location.assign('/login');
        return;
      }

      if (!teamRes.ok) {
        const err = await teamRes.json().catch(() => ({}));
        throw new Error(err.message || 'No se pudo cargar el equipo.');
      }
      if (!rolesRes.ok) {
        const err = await rolesRes.json().catch(() => ({}));
        throw new Error(err.message || 'No se pudieron cargar los roles.');
      }
      if (!usersRes.ok) {
        const err = await usersRes.json().catch(() => ({}));
        throw new Error(err.message || 'No se pudieron cargar los miembros.');
      }

      const teamBody = await teamRes.json();
      const rolesBody = await rolesRes.json();
      const usersBody = await usersRes.json();

      setTeamName(teamBody.name || '');
      setTeamDescription(teamBody.description || '');

      const roleList = Array.isArray(rolesBody.data) ? rolesBody.data : [];
      setRoles(roleList);
      setInvite((prev) => {
        if (prev.roleId && roleList.some((r) => String(r.roleId) === prev.roleId)) {
          return prev;
        }
        if (!roleList.length) return prev;
        return { ...prev, roleId: String(roleList[0].roleId) };
      });

      const userRows = Array.isArray(usersBody.data) ? usersBody.data : [];

      const withWorkload = await Promise.all(
        userRows.map(async (u) => {
          try {
            const wr = await fetch(`${API_BASE}/users/${u.userId}/workload`, { headers });
            if (!wr.ok) return { ...u, workload: 0 };
            const w = await wr.json();
            return { ...u, workload: w.workloadPercent ?? 0 };
          } catch {
            return { ...u, workload: 0 };
          }
        })
      );

      setMembers(withWorkload);
    } catch (e) {
      setError(e.message || 'Error al cargar el equipo.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const networkHealth = useMemo(() => {
    if (!members.length) return 0;
    return Math.round(
      members.reduce((sum, m) => sum + (typeof m.workload === 'number' ? m.workload : 0), 0) /
        members.length
    );
  }, [members]);

  const insightText = useMemo(() => {
    const high = members.filter((m) => (m.workload || 0) >= 70).length;
    const low = members.filter((m) => (m.workload || 0) <= 30).length;
    if (!members.length) {
      return 'No hay miembros en este equipo todavía.';
    }
    if (high >= 2) {
      return `${high} miembros tienen carga alta (≥70%). Considera repriorizar tareas o redistribuir trabajo.`;
    }
    if (low === members.length) {
      return 'La carga está distribuida con margen; puedes asignar trabajo adicional si el sprint lo requiere.';
    }
    return 'Revisa el tablero de backlog para equilibrar tareas entre roles.';
  }, [members]);

  const handleInvite = async (e) => {
    e.preventDefault();
    setFormError('');
    const teamId = localStorage.getItem('userTeamId');
    if (!teamId) {
      setFormError('No hay equipo seleccionado.');
      return;
    }
    if (!invite.fullName.trim()) {
      setFormError('Indica el nombre completo.');
      return;
    }
    if (!invite.email.trim()) {
      setFormError('Indica el correo.');
      return;
    }
    if (!invite.username.trim()) {
      setFormError('Indica el nombre de usuario.');
      return;
    }
    if (!invite.password || invite.password.length < 6) {
      setFormError('La contraseña debe tener al menos 6 caracteres.');
      return;
    }
    if (!invite.roleId) {
      setFormError('Selecciona un rol.');
      return;
    }

    const token = localStorage.getItem('authToken');
    if (!token) {
      window.location.assign('/login');
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch(`${API_BASE}/users`, {
        method: 'POST',
        headers: {
          ...authHeaders(),
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          fullName: invite.fullName.trim(),
          email: invite.email.trim(),
          username: invite.username.trim(),
          password: invite.password,
          roleId: Number(invite.roleId),
          teamId: Number(teamId),
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (response.status === 401) {
        localStorage.removeItem('authToken');
        window.location.assign('/login');
        return;
      }
      if (!response.ok) {
        throw new Error(payload.message || payload.error || `Error ${response.status}`);
      }
      setInvite((prev) => ({
        fullName: '',
        email: '',
        username: '',
        password: '',
        roleId: prev.roleId,
      }));
      setShowInvite(false);
      await loadData();
    } catch (err) {
      setFormError(err.message || 'No se pudo crear el usuario.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewMember = (member) => {
    const bits = [
      member.fullName,
      member.email ? `Correo: ${member.email}` : null,
      member.status ? `Estado: ${member.status}` : null,
    ].filter(Boolean);
    window.alert(bits.join('\n'));
  };

  if (loading) {
    return (
      <PageLayout>
        <p className="section-subtitle">Cargando equipo…</p>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <section className="team-header">
        <div>
          <h2 className="section-title" style={{ fontSize: 36 }}>
            {teamName ? `Equipo: ${teamName}` : 'Gestión de equipo'}
          </h2>
          <p className="section-subtitle">
            {teamDescription ||
              'Coordina miembros y carga de trabajo según los datos del sistema.'}
          </p>
        </div>
        <button
          type="button"
          className="btn btn--primary"
          disabled={noTeam}
          onClick={() => {
            setFormError('');
            setShowInvite((v) => !v);
          }}
        >
          <span className="material-icons" style={{ fontSize: 18 }}>
            person_add
          </span>
          Invitar miembro
        </button>
      </section>

      {error && (
        <div className="card mt-16" style={{ borderLeft: '4px solid var(--color-red)' }}>
          <p style={{ margin: 0 }}>{error}</p>
          <button type="button" className="btn btn--ghost btn--small mt-8" onClick={loadData}>
            Reintentar
          </button>
        </div>
      )}

      {noTeam && !error && (
        <div className="card mt-16">
          <p className="section-subtitle" style={{ margin: 0 }}>
            Tu usuario no tiene equipo asignado en la base de datos, o hay varios equipos y debes
            elegir uno. Vuelve a iniciar sesión tras asignar equipo al usuario, o contacta a un
            administrador.
          </p>
        </div>
      )}

      {showInvite && !noTeam && (
        <form className="team-invite card mt-16" onSubmit={handleInvite}>
          <input
            type="text"
            className="backlog-input"
            placeholder="Nombre completo"
            value={invite.fullName}
            onChange={(e) => setInvite((p) => ({ ...p, fullName: e.target.value }))}
            autoComplete="name"
            autoFocus
          />
          <input
            type="email"
            className="backlog-input"
            placeholder="Correo electrónico"
            value={invite.email}
            onChange={(e) => setInvite((p) => ({ ...p, email: e.target.value }))}
            autoComplete="email"
          />
          <input
            type="text"
            className="backlog-input"
            placeholder="Nombre de usuario (login)"
            value={invite.username}
            onChange={(e) => setInvite((p) => ({ ...p, username: e.target.value }))}
            autoComplete="username"
          />
          <input
            type="password"
            className="backlog-input"
            placeholder="Contraseña inicial"
            value={invite.password}
            onChange={(e) => setInvite((p) => ({ ...p, password: e.target.value }))}
            autoComplete="new-password"
          />
          <select
            className="backlog-select"
            value={invite.roleId}
            onChange={(e) => setInvite((p) => ({ ...p, roleId: e.target.value }))}
          >
            {roles.map((r) => (
              <option key={r.roleId} value={String(r.roleId)}>
                {r.roleName}
              </option>
            ))}
          </select>
          <button type="submit" className="btn btn--primary btn--small" disabled={submitting}>
            {submitting ? 'Creando…' : 'Crear usuario'}
          </button>
          <button
            type="button"
            className="btn btn--ghost btn--small"
            onClick={() => {
              setShowInvite(false);
              setFormError('');
            }}
          >
            Cancelar
          </button>
          {formError && (
            <p className="login-form__feedback login-form__feedback--error" style={{ gridColumn: '1 / -1' }}>
              {formError}
            </p>
          )}
        </form>
      )}

      {!noTeam && (
        <div className="team-stats mt-24">
          <div className="card card--dark team-ai-tip">
            <span className="material-icons team-ai-icon">psychology</span>
            <div>
              <h3 className="team-ai-tip__title">Resumen de capacidad</h3>
              <p className="team-ai-tip__text">{insightText}</p>
            </div>
          </div>
          <div className="card team-health">
            <div className="team-health__top">
              <div>
                <span className="text-sm text-muted font-bold">Salud de red</span>
                <div className="team-health__value">
                  <span className="team-health__number">{networkHealth}%</span>
                  <span className="team-health__label">Utilización media</span>
                </div>
              </div>
            </div>
            <p className="text-sm text-muted mt-8">
              {members.length} miembro{members.length !== 1 ? 's' : ''} en el equipo
              {teamName ? ` «${teamName}»` : ''}. La utilización promedia se calcula con tareas
              activas y pendientes por persona.
            </p>
            <ProgressBar value={networkHealth} color="var(--color-primary)" height={8} />
          </div>
        </div>
      )}

      {!noTeam && (
        <div className="team-grid mt-24">
          {members.map((member) => (
            <TeamMemberCard
              key={member.userId}
              member={member}
              onViewDetails={handleViewMember}
            />
          ))}
        </div>
      )}

      {!noTeam && !members.length && !error && (
        <p className="section-subtitle mt-24">No hay miembros en este equipo.</p>
      )}
    </PageLayout>
  );
}

export default TeamManagement;
