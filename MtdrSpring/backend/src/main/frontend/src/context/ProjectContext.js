import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

const ProjectContext = createContext(null);

export function ProjectProvider({ children }) {
  const [projectId, setProjectId] = useState(
    () => localStorage.getItem('currentProjectId') || ''
  );
  const [projectName, setProjectName] = useState(
    () => localStorage.getItem('currentProjectName') || 'Selecciona proyecto'
  );

  const setProject = useCallback((id, name) => {
    const nextId = id ? String(id) : '';
    const nextName = name || (nextId ? 'Proyecto' : 'Selecciona proyecto');
    setProjectId(nextId);
    setProjectName(nextName);
    if (nextId) {
      localStorage.setItem('currentProjectId', nextId);
      localStorage.setItem('currentProjectName', nextName);
    } else {
      localStorage.removeItem('currentProjectId');
      localStorage.removeItem('currentProjectName');
    }
  }, []);

  const value = useMemo(
    () => ({ projectId, projectName, setProject }),
    [projectId, projectName, setProject]
  );

  return <ProjectContext.Provider value={value}>{children}</ProjectContext.Provider>;
}

export function useProject() {
  const ctx = useContext(ProjectContext);
  if (!ctx) {
    throw new Error('useProject debe usarse dentro de ProjectProvider');
  }
  return ctx;
}
