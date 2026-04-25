/**
 * Base URL del API REST (logout y demás fetch).
 * En producción, mismo origen que Spring: /api/v1.
 * En desarrollo, Spring en 8080 por defecto (evita depender solo del proxy).
 * Sobreescribe con REACT_APP_API_BASE en .env si hace falta.
 */
function getApiBase() {
  const fromEnv = process.env.REACT_APP_API_BASE;
  if (fromEnv && fromEnv.trim()) {
    return fromEnv.trim().replace(/\/$/, '');
  }
  if (process.env.NODE_ENV === 'development') {
    return 'http://127.0.0.1:8080/api/v1';
  }
  return '/api/v1';
}

export const API_BASE = getApiBase();
