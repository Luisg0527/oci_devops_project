/** Etiquetas visibles en español para enums del backend */

export const TASK_STATUS_ES = {
  PENDING: 'Pendiente',
  IN_PROGRESS: 'En progreso',
  DONE: 'Completado',
  CANCELLED: 'Cancelado',
  REOPENED: 'Reabierto',
};

export const TASK_PRIORITY_ES = {
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baja',
};

export const ROLE_ES = {
  MANAGER: 'Gestor',
  DEVELOPER: 'Desarrollador',
  QA: 'Control de calidad',
  ADMIN: 'Administrador',
  USER: 'Usuario',
  SCRUM_MASTER: 'Scrum Master',
  TECH_LEAD: 'Líder técnico',
  PRODUCT_OWNER: 'Product Owner',
  PROJECT_MANAGER: 'Project Manager',
};

export const USER_STATUS_ES = {
  ACTIVE: 'Activo',
  INACTIVE: 'Inactivo',
  LOCKED: 'Bloqueado',
};

export const EFFICIENCY_RATING_ES = {
  ELITE: 'Élite',
  HIGH: 'Alto',
  MEDIUM: 'Medio',
  LOW: 'Bajo',
};

export const APP_BRAND = 'Estudio';

export function labelTaskStatus(status) {
  return TASK_STATUS_ES[status] || status || '—';
}

export function labelTaskPriority(priority) {
  return TASK_PRIORITY_ES[priority] || priority || '—';
}

function humanizeRoleKey(role) {
  return role
    .trim()
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export const AUTH_ERROR_ES = {
  'Invalid credentials': 'Credenciales inválidas.',
  'Credenciales inválidas.': 'Credenciales inválidas.',
  'Account is locked': 'La cuenta está bloqueada.',
  'La cuenta está bloqueada.': 'La cuenta está bloqueada.',
  'Invalid refresh token': 'Token de actualización inválido.',
  'User not found': 'Usuario no encontrado.',
};

export function labelAuthError(message) {
  if (!message) return '';
  const trimmed = message.trim();
  return AUTH_ERROR_ES[trimmed] || trimmed;
}

export function canManageProjects(role) {
  if (!role) return false;
  return role.trim().toUpperCase() !== 'DEVELOPER';
}

export function canManageSprints(role) {
  return canManageProjects(role);
}

export function canInviteMembers(role) {
  return canManageProjects(role);
}

export function formatProjectRole(role) {
  if (!role) return '—';
  const key = role.trim().toUpperCase();
  if (key === 'MANAGER') return 'Manager';
  return ROLE_ES[key] || humanizeRoleKey(role);
}

export function labelRole(role) {
  if (!role) return 'Sin rol';
  const key = role.trim().toUpperCase();
  return ROLE_ES[key] || humanizeRoleKey(role);
}

export function labelUserStatus(status) {
  return USER_STATUS_ES[status] || status || '—';
}

export function labelEfficiencyRating(rating) {
  return EFFICIENCY_RATING_ES[rating] || rating || '—';
}
