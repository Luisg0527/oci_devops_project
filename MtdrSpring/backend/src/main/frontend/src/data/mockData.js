// ─── USERS (joined with ROLES) ──────────────────────────────
export const teamMembers = [
  { userId: 1, fullName: 'Jordan Davila', email: 'jordan.davila@corp.com', telegramId: null, roleId: 1, roleName: 'Gerente de Proyecto Sr.', teamId: 1, status: 'ACTIVE', workload: 85 },
  { userId: 2, fullName: 'Sarah Chen', email: 'sarah.chen@corp.com', telegramId: null, roleId: 2, roleName: 'Arquitecta UX', teamId: 1, status: 'ACTIVE', workload: 42 },
  { userId: 3, fullName: 'Marcus Rodriguez', email: 'marcus.r@corp.com', telegramId: null, roleId: 3, roleName: 'Ingeniero Sr.', teamId: 1, status: 'ACTIVE', workload: 92 },
  { userId: 4, fullName: 'Elena Kozlova', email: 'elena.k@corp.com', telegramId: null, roleId: 3, roleName: 'Desarrolladora Full Stack', teamId: 1, status: 'ACTIVE', workload: 67 },
];

// ─── ROLES ──────────────────────────────────────────────────
export const roles = [
  { roleId: 1, roleName: 'Gerente', description: 'Gerente de Proyecto' },
  { roleId: 2, roleName: 'Diseñador', description: 'Diseñador UX/UI' },
  { roleId: 3, roleName: 'Desarrollador', description: 'Desarrollador de Software' },
];

export function getRoleTag(roleId) {
  const map = { 1: 'PM', 2: 'Diseño', 3: 'Dev' };
  return map[roleId] || 'Miembro';
}

// ─── TASKS (BACKLOG view) ───────────────────────────────────
export const backlogTasks = [
  { taskId: 1, projectId: 1, sprintId: null, title: 'Análisis estructural de cimentación', description: '', taskStage: 'SPRINT', status: 'IN_PROGRESS', priority: 'HIGH', createdBy: 1, assignedTo: 5, assignedToName: 'Sarah Jenkins', dueDate: '24 Oct, 2023', createdAt: '2023-10-10T08:00:00Z' },
  { taskId: 2, projectId: 1, sprintId: null, title: 'Revisión de retroalimentación del cliente - Fase 2', description: '', taskStage: 'BACKLOG', status: 'PENDING', priority: 'MEDIUM', createdBy: 1, assignedTo: 6, assignedToName: 'David Chen', dueDate: '28 Oct, 2023', createdAt: '2023-10-11T09:00:00Z' },
  { taskId: 3, projectId: 1, sprintId: 1, title: 'Mapeo del sistema HVAC', description: '', taskStage: 'COMPLETED', status: 'DONE', priority: 'LOW', createdBy: 2, assignedTo: 4, assignedToName: 'Elena Rodriguez', dueDate: '20 Oct, 2023', createdAt: '2023-10-05T10:00:00Z' },
  { taskId: 4, projectId: 1, sprintId: null, title: 'Optimización de la red eléctrica', description: '', taskStage: 'SPRINT', status: 'IN_PROGRESS', priority: 'HIGH', createdBy: 1, assignedTo: 3, assignedToName: 'Marcus Rodriguez', dueDate: '25 Oct, 2023', createdAt: '2023-10-12T08:00:00Z' },
  { taskId: 5, projectId: 1, sprintId: null, title: 'Propuestas de diseño interior', description: '', taskStage: 'BACKLOG', status: 'PENDING', priority: 'MEDIUM', createdBy: 2, assignedTo: 1, assignedToName: 'Jordan Davila', dueDate: '30 Oct, 2023', createdAt: '2023-10-13T08:00:00Z' },
  { taskId: 6, projectId: 1, sprintId: 1, title: 'Revisión del sistema de plomería', description: '', taskStage: 'COMPLETED', status: 'DONE', priority: 'LOW', createdBy: 1, assignedTo: 2, assignedToName: 'Sarah Chen', dueDate: '18 Oct, 2023', createdAt: '2023-10-04T10:00:00Z' },
  { taskId: 7, projectId: 1, sprintId: null, title: 'Auditoría de cumplimiento de seguridad', description: '', taskStage: 'BACKLOG', status: 'PENDING', priority: 'HIGH', createdBy: 1, assignedTo: 4, assignedToName: 'Elena Kozlova', dueDate: '1 Nov, 2023', createdAt: '2023-10-14T08:00:00Z' },
  { taskId: 8, projectId: 1, sprintId: null, title: 'Estimación de costos de materiales', description: '', taskStage: 'SPRINT', status: 'IN_PROGRESS', priority: 'MEDIUM', createdBy: 2, assignedTo: 6, assignedToName: 'David Chen', dueDate: '27 Oct, 2023', createdAt: '2023-10-09T08:00:00Z' },
  { taskId: 9, projectId: 1, sprintId: null, title: 'Diseño de estructura de estacionamiento', description: '', taskStage: 'BACKLOG', status: 'PENDING', priority: 'LOW', createdBy: 1, assignedTo: 5, assignedToName: 'Sarah Jenkins', dueDate: '5 Nov, 2023', createdAt: '2023-10-15T08:00:00Z' },
  { taskId: 10, projectId: 1, sprintId: null, title: 'Reporte de impacto ambiental', description: '', taskStage: 'SPRINT', status: 'IN_PROGRESS', priority: 'HIGH', createdBy: 1, assignedTo: 3, assignedToName: 'Marcus Rodriguez', dueDate: '29 Oct, 2023', createdAt: '2023-10-08T08:00:00Z' },
];

// ─── TASKS (developer "My Tasks" view) ──────────────────────
export const devTasks = [
  { taskId: 401, projectId: 2, sprintId: 10, title: 'API Endpoint: Telemetría de Usuario', description: 'Infraestructura Base • Issue #402', taskStage: 'SPRINT', status: 'IN_PROGRESS', priority: 'HIGH', assignedTo: 7, dueDate: '2023-10-10', dueLabel: 'Hoy' },
  { taskId: 402, projectId: 2, sprintId: 10, title: 'Fix: Timeout de Handshake WebSocket', description: 'Corrección de Bug • Issue #388', taskStage: 'SPRINT', status: 'PENDING', priority: 'MEDIUM', assignedTo: 7, dueDate: '2023-10-11', dueLabel: 'Mañana' },
  { taskId: 403, projectId: 2, sprintId: 10, title: 'Pruebas Unitarias: Gateway de Facturación', description: 'Pruebas • Issue #415', taskStage: 'SPRINT', status: 'PENDING', priority: 'MEDIUM', assignedTo: 7, dueDate: '2023-10-12', dueLabel: '12 Oct' },
  { taskId: 404, projectId: 2, sprintId: 10, title: 'Documentación: Config Service Mesh', description: 'Docs • Issue #422', taskStage: 'SPRINT', status: 'PENDING', priority: 'LOW', assignedTo: 7, dueDate: '2023-10-14', dueLabel: '14 Oct' },
  { taskId: 405, projectId: 2, sprintId: 10, title: 'Optimización del Asset Bundler', description: 'Rendimiento • Issue #428', taskStage: 'SPRINT', status: 'PENDING', priority: 'LOW', assignedTo: 7, dueDate: '2023-10-15', dueLabel: '15 Oct' },
];

// ─── PROJECTS ───────────────────────────────────────────────
export const projects = [
  { projectId: 1, name: 'Project Meridian', description: 'Campus Urbano Sustentable', managerId: 1, status: 'ACTIVE' },
  { projectId: 2, name: 'Project Nexus', description: 'Plataforma de Infraestructura Base', managerId: 1, status: 'ACTIVE' },
];

// ─── KPI_VALUES (dashboard-level aggregates) ────────────────
export const dashboardStats = {
  projectHealth: 'Excelente',
  teamCapacity: 84,
  budget: { value: '$428,500', percent: 68, label: '68% del presupuesto trimestral utilizado', status: 'EN CURSO' },
  velocity: { value: 54.2, unit: 'pts/sem', change: '+12% incremento vs sprint anterior' },
  quality: { value: '9.8/10', label: 'Basado en auditoría de código y revisión de diseño', stars: 5 },
};

// ─── Milestones (UI concept for project timeline) ───────────
export const milestones = [
  { id: 1, name: 'Concepto', date: '12 May', status: 'done' },
  { id: 2, name: 'Wireframes', date: '24 May', status: 'done' },
  { id: 3, name: 'Fase UI', date: 'En Progreso', status: 'current' },
  { id: 4, name: 'Prototipo', date: '10 Jun', status: 'upcoming' },
  { id: 5, name: 'Entrega', date: '30 Jun', status: 'upcoming' },
];

// ─── Resource allocation (KPI aggregate) ────────────────────
export const resourceAllocation = [
  { name: 'Ingeniería', percent: 40, color: 'var(--color-primary)' },
  { name: 'Diseño', percent: 30, color: 'var(--color-teal)' },
  { name: 'Gestión', percent: 30, color: 'var(--color-bg-badge)' },
];

// ─── INCIDENTS / critical activity ──────────────────────────
export const criticalActivity = [
  { incidentId: 1, title: 'Conflicto de Recursos Detectado', description: "Marcus asignado a 3 tareas superpuestas en el Proyecto 'Nexus'.", occurredAt: 'Hace 2 horas', severity: 'HIGH' },
  { incidentId: 2, title: 'Hito de Presupuesto Alcanzado', description: 'Facturación de fase de diseño aprobada por Cliente: Enterprise Corp.', occurredAt: 'Hace 4 horas', severity: 'MEDIUM' },
  { incidentId: 3, title: 'Nuevo Protocolo de Seguridad', description: "Controles de acceso actualizados para el repositorio interno 'Horizon'.", occurredAt: 'Ayer', severity: 'LOW' },
];

// ─── AUDIT_LOG style activity feed ──────────────────────────
export const activityFeed = [
  { auditId: 1, fullName: 'Sarah Chen', entityName: 'PR #882', actionDate: 'Hace 24m', description: "Creo que podemos optimizar la consulta DB en la línea 142 indexando el campo 'project_id'. ¡Buen trabajo!" },
  { auditId: 2, fullName: 'Marcus Thorne', entityName: 'PR #879', actionDate: 'Hace 2h', description: 'Necesitamos asegurar que los headers de seguridad estén incluidos en la respuesta. Revisa la config del middleware.' },
  { auditId: 3, fullName: 'Elena Rodriguez', entityName: 'Issue #402', actionDate: 'Hace 5h', description: '@dev-alpha ¿Puedes verificar el cronograma de despliegue para esta tarea? El cliente lo quiere para el viernes.' },
];

// ─── KPI_VALUES (sprint velocity per sprint) ────────────────
export const sprintVelocityData = [
  { sprintId: 19, sprintName: 'SPR 19', points: 38 },
  { sprintId: 20, sprintName: 'SPR 20', points: 45 },
  { sprintId: 21, sprintName: 'SPR 21', points: 41 },
  { sprintId: 22, sprintName: 'SPR 22', points: 52 },
  { sprintId: 23, sprintName: 'SPR 23', points: 39 },
  { sprintId: 24, sprintName: 'ACTUAL', points: 42 },
];

// ─── KPI_VALUES (task completion per user) ──────────────────
export const taskCompletionData = [
  { userId: 1, fullName: 'Jordan Driskell', roleName: 'Ingeniero Líder', assignedTasks: 24, completedTasks: 22, efficiency: 92 },
  { userId: 2, fullName: 'Sarah Chen', roleName: 'UI/UX Sr.', assignedTasks: 18, completedTasks: 17, efficiency: 94 },
  { userId: 3, fullName: 'Marcus Tallow', roleName: 'Desarrollador Fullstack', assignedTasks: 32, completedTasks: 25, efficiency: 78 },
];

// ─── SPRINTS (active sprint info) ───────────────────────────
export const sprintInfo = {
  sprintId: 12,
  name: 'Sprint 12 Alpha',
  startDate: '2023-10-01',
  endDate: '2023-10-14',
  status: 'ACTIVE',
  velocityPercent: 72,
  daysLeft: 4,
  pointsCompleted: 18,
  pointsTotal: 25,
  currentVelocity: 42.8,
  velocityChange: '+12% vs anterior',
};

// ─── Current logged-in user (USERS + ROLES join) ────────────
export const currentUser = {
  userId: 7,
  fullName: 'Alex Sterling',
  email: 'alex.sterling@corp.com',
  roleName: 'Arquitecto Líder',
  roleId: 3,
  teamId: 1,
  status: 'ACTIVE',
};
