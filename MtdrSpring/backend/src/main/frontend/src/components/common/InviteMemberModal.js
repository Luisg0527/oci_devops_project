import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { API_BASE } from '../../config/apiBase';
import { labelRole } from '../../utils/labelsEs';
import './DetailModal.css';
import './TaskEditModal.css';
import './InviteMemberModal.css';

const PROJECT_ROLE_OPTIONS = [
  { value: 'DEVELOPER', label: 'Desarrollador' },
  { value: 'MANAGER', label: 'Manager' },
  { value: 'SCRUM_MASTER', label: 'Scrum Master' },
];

function authHeaders() {
  const token = localStorage.getItem('authToken');
  return token
    ? { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' };
}

function userInitials(name) {
  return (name || 'U')
    .split(' ')
    .filter(Boolean)
    .map((word) => word[0])
    .join('')
    .slice(0, 3)
    .toUpperCase();
}

function InviteMemberModal({
  projectId,
  memberUserIds = [],
  roles = [],
  onClose,
  onSuccess,
}) {
  const [step, setStep] = useState('choice');
  const [allUsers, setAllUsers] = useState([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState('');
  const [userSearch, setUserSearch] = useState('');
  const [selectedUserId, setSelectedUserId] = useState('');
  const [projectRole, setProjectRole] = useState('DEVELOPER');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  const [invite, setInvite] = useState({
    fullName: '',
    email: '',
    username: '',
    password: '',
    roleId: '',
  });

  useEffect(() => {
    if (!roles.length) return;
    setInvite((prev) => {
      if (prev.roleId && roles.some((r) => String(r.roleId) === prev.roleId)) {
        return prev;
      }
      return { ...prev, roleId: String(roles[0].roleId) };
    });
  }, [roles]);

  const memberIdSet = useMemo(() => new Set(memberUserIds.map(String)), [memberUserIds]);

  const loadUsers = useCallback(async () => {
    setUsersLoading(true);
    setUsersError('');
    try {
      const response = await fetch(`${API_BASE}/users?page=1&limit=200`, {
        headers: authHeaders(),
      });
      const payload = await response.json().catch(() => ({}));
      if (response.status === 401) {
        localStorage.removeItem('authToken');
        window.location.assign('/login');
        return;
      }
      if (!response.ok) {
        throw new Error(payload.message || payload.error || 'No se pudieron cargar los usuarios.');
      }
      const rows = Array.isArray(payload.data) ? payload.data : [];
      setAllUsers(rows.filter((u) => !u.isDeleted));
    } catch (err) {
      setUsersError(err.message || 'No se pudieron cargar los usuarios.');
    } finally {
      setUsersLoading(false);
    }
  }, []);

  useEffect(() => {
    if (step === 'existing') {
      loadUsers();
    }
  }, [step, loadUsers]);

  const availableUsers = useMemo(() => {
    const term = userSearch.trim().toLowerCase();
    return allUsers
      .filter((u) => !memberIdSet.has(String(u.userId)))
      .filter((u) => {
        if (!term) return true;
        const haystack = `${u.fullName || ''} ${u.email || ''} ${u.roleName || ''}`.toLowerCase();
        return haystack.includes(term);
      });
  }, [allUsers, memberIdSet, userSearch]);

  const handleAddExisting = async (event) => {
    event.preventDefault();
    if (!selectedUserId) {
      setFormError('Selecciona un usuario.');
      return;
    }
    if (!projectId) {
      setFormError('No hay proyecto seleccionado.');
      return;
    }

    setSubmitting(true);
    setFormError('');
    try {
      const response = await fetch(`${API_BASE}/projects/${projectId}/members`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({
          userId: Number(selectedUserId),
          roleInProject: projectRole,
        }),
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.message || payload.error || 'No se pudo añadir al proyecto.');
      }
      onSuccess?.();
      onClose();
    } catch (err) {
      setFormError(err.message || 'No se pudo añadir al proyecto.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateUser = async (event) => {
    event.preventDefault();
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
    if (!projectId) {
      setFormError('No hay proyecto seleccionado.');
      return;
    }

    const selectedRole = roles.find((r) => String(r.roleId) === invite.roleId);
    const roleInProject = selectedRole?.roleName || projectRole;

    setSubmitting(true);
    try {
      const createRes = await fetch(`${API_BASE}/users`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({
          fullName: invite.fullName.trim(),
          email: invite.email.trim(),
          username: invite.username.trim(),
          password: invite.password,
          roleId: Number(invite.roleId),
          teamId: Number(teamId),
        }),
      });
      const created = await createRes.json().catch(() => ({}));
      if (createRes.status === 401) {
        localStorage.removeItem('authToken');
        window.location.assign('/login');
        return;
      }
      if (!createRes.ok) {
        throw new Error(created.message || created.error || 'No se pudo crear el usuario.');
      }

      const addRes = await fetch(`${API_BASE}/projects/${projectId}/members`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({
          userId: created.userId,
          roleInProject: roleInProject,
        }),
      });
      const addPayload = await addRes.json().catch(() => ({}));
      if (!addRes.ok) {
        throw new Error(
          addPayload.message || addPayload.error || 'Usuario creado, pero no se pudo añadir al proyecto.'
        );
      }

      onSuccess?.();
      onClose();
    } catch (err) {
      setFormError(err.message || 'No se pudo crear el integrante.');
    } finally {
      setSubmitting(false);
    }
  };

  const goToChoice = () => {
    setStep('choice');
    setFormError('');
    setSelectedUserId('');
    setUserSearch('');
  };

  return (
    <div className="member-modal-overlay" onClick={onClose}>
      <div
        className={`member-modal member-modal--form task-edit-modal invite-member-modal invite-member-modal--${step}`}
        onClick={(event) => event.stopPropagation()}
        role="dialog"
        aria-labelledby="invite-member-kicker"
        aria-modal="true"
      >
        <button type="button" className="member-modal__close" onClick={onClose}>
          <span className="material-icons">close</span>
        </button>

        {step !== 'choice' && (
          <button type="button" className="invite-member-modal__back" onClick={goToChoice}>
            <span className="material-icons">arrow_back</span>
            Volver
          </button>
        )}

        <header className="task-edit-modal__hero">
          <div className="task-edit-modal__avatar invite-member-modal__avatar">
            {step === 'create' ? userInitials(invite.fullName) : '＋'}
          </div>
        </header>

        {step === 'choice' && (
          <div className="invite-member-modal__choice">
            <p id="invite-member-kicker" className="task-edit-modal__kicker">
              Añadir integrante
            </p>
            <h3 className="invite-member-modal__title">¿Cómo deseas añadir un integrante?</h3>
            <div className="invite-member-modal__options">
              <button
                type="button"
                className="invite-member-modal__option"
                onClick={() => {
                  setFormError('');
                  setStep('existing');
                }}
              >
                <span className="material-icons">group</span>
                <strong>Seleccionar existente</strong>
                <span>Elige un usuario ya registrado en el sistema</span>
              </button>
              <button
                type="button"
                className="invite-member-modal__option"
                onClick={() => {
                  setFormError('');
                  setStep('create');
                }}
              >
                <span className="material-icons">person_add</span>
                <strong>Crear integrante nuevo</strong>
                <span>Registra un usuario y asígnalo al proyecto</span>
              </button>
            </div>
          </div>
        )}

        {step === 'existing' && (
          <form className="task-edit-modal__form" onSubmit={handleAddExisting}>
            <p id="invite-member-kicker" className="task-edit-modal__kicker">
              Integrante existente
            </p>
            <h3 className="invite-member-modal__title">Selecciona un usuario</h3>

            <input
              type="search"
              className="task-edit-modal__input invite-member-modal__search"
              placeholder="Buscar por nombre, correo o rol…"
              value={userSearch}
              onChange={(e) => setUserSearch(e.target.value)}
            />

            <div className="invite-member-modal__user-list" role="listbox" aria-label="Usuarios disponibles">
              {usersLoading && (
                <p className="invite-member-modal__empty">Cargando usuarios…</p>
              )}
              {!usersLoading && usersError && (
                <p className="task-edit-modal__error" role="alert">
                  <span className="material-icons">warning</span>
                  {usersError}
                </p>
              )}
              {!usersLoading && !usersError && availableUsers.length === 0 && (
                <p className="invite-member-modal__empty">
                  {allUsers.length === 0
                    ? 'No hay usuarios registrados.'
                    : 'Todos los usuarios ya pertenecen a este proyecto.'}
                </p>
              )}
              {!usersLoading &&
                !usersError &&
                availableUsers.map((user) => {
                  const isSelected = selectedUserId === String(user.userId);
                  return (
                    <button
                      key={user.userId}
                      type="button"
                      role="option"
                      aria-selected={isSelected}
                      className={`invite-member-modal__user ${isSelected ? 'is-selected' : ''}`}
                      onClick={() => setSelectedUserId(String(user.userId))}
                    >
                      <span className="invite-member-modal__user-avatar">
                        {userInitials(user.fullName)}
                      </span>
                      <span className="invite-member-modal__user-info">
                        <strong>{user.fullName}</strong>
                        <span>{user.email}</span>
                        <em>{labelRole(user.roleName)}</em>
                      </span>
                    </button>
                  );
                })}
            </div>

            <div className="task-edit-modal__field">
              <label className="task-edit-modal__label" htmlFor="invite-existing-role">
                <span className="material-icons task-edit-modal__label-icon">badge</span>
                Rol en el proyecto
              </label>
              <select
                id="invite-existing-role"
                className="task-edit-modal__select"
                value={projectRole}
                onChange={(e) => setProjectRole(e.target.value)}
              >
                {PROJECT_ROLE_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            {formError && (
              <p className="task-edit-modal__error" role="alert">
                <span className="material-icons">warning</span>
                {formError}
              </p>
            )}

            <div className="task-edit-modal__actions">
              <button type="button" className="btn btn--ghost btn--small" onClick={onClose} disabled={submitting}>
                Cancelar
              </button>
              <button type="submit" className="btn btn--primary btn--small" disabled={submitting || !selectedUserId}>
                {submitting ? 'Añadiendo…' : 'Añadir al proyecto'}
              </button>
            </div>
          </form>
        )}

        {step === 'create' && (
          <form className="task-edit-modal__form" onSubmit={handleCreateUser}>
            <p id="invite-member-kicker" className="task-edit-modal__kicker">
              Nuevo integrante
            </p>
            <div className="task-edit-modal__badges">
              <span className="invite-member-modal__status-pill">Usuario nuevo</span>
            </div>

            <div className="task-edit-modal__fields">
              <div className="task-edit-modal__field">
                <label className="task-edit-modal__label" htmlFor="invite-create-name">
                  <span className="material-icons task-edit-modal__label-icon">person</span>
                  Nombre completo
                </label>
                <input
                  id="invite-create-name"
                  type="text"
                  className="task-edit-modal__input"
                  value={invite.fullName}
                  onChange={(e) => setInvite((p) => ({ ...p, fullName: e.target.value }))}
                  required
                  autoFocus
                />
              </div>

              <div className="task-edit-modal__field">
                <label className="task-edit-modal__label" htmlFor="invite-create-email">
                  <span className="material-icons task-edit-modal__label-icon">email</span>
                  Correo electrónico
                </label>
                <input
                  id="invite-create-email"
                  type="email"
                  className="task-edit-modal__input"
                  value={invite.email}
                  onChange={(e) => setInvite((p) => ({ ...p, email: e.target.value }))}
                  required
                />
              </div>

              <div className="task-edit-modal__field">
                <label className="task-edit-modal__label" htmlFor="invite-create-username">
                  <span className="material-icons task-edit-modal__label-icon">account_circle</span>
                  Nombre de usuario
                </label>
                <input
                  id="invite-create-username"
                  type="text"
                  className="task-edit-modal__input"
                  value={invite.username}
                  onChange={(e) => setInvite((p) => ({ ...p, username: e.target.value }))}
                  required
                />
              </div>

              <div className="task-edit-modal__field">
                <label className="task-edit-modal__label" htmlFor="invite-create-password">
                  <span className="material-icons task-edit-modal__label-icon">lock</span>
                  Contraseña inicial
                </label>
                <input
                  id="invite-create-password"
                  type="password"
                  className="task-edit-modal__input"
                  value={invite.password}
                  onChange={(e) => setInvite((p) => ({ ...p, password: e.target.value }))}
                  required
                  minLength={6}
                />
              </div>

              <div className="task-edit-modal__field">
                <label className="task-edit-modal__label" htmlFor="invite-create-role">
                  <span className="material-icons task-edit-modal__label-icon">admin_panel_settings</span>
                  Rol del sistema
                </label>
                <select
                  id="invite-create-role"
                  className="task-edit-modal__select"
                  value={invite.roleId}
                  onChange={(e) => setInvite((p) => ({ ...p, roleId: e.target.value }))}
                >
                  {roles.map((r) => (
                    <option key={r.roleId} value={String(r.roleId)}>
                      {labelRole(r.roleName)}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {formError && (
              <p className="task-edit-modal__error" role="alert">
                <span className="material-icons">warning</span>
                {formError}
              </p>
            )}

            <div className="task-edit-modal__actions">
              <button type="button" className="btn btn--ghost btn--small" onClick={onClose} disabled={submitting}>
                Cancelar
              </button>
              <button type="submit" className="btn btn--primary btn--small" disabled={submitting}>
                {submitting ? 'Creando…' : 'Crear y añadir'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

export default InviteMemberModal;
