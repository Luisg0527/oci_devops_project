import React from 'react';
import { NavLink, useHistory } from 'react-router-dom';
import './Sidebar.css';

const navItems = [
  { to: '/dashboard', icon: 'grid_view', label: 'Panel' },
  { to: '/backlog', icon: 'list_alt', label: 'Backlog' },
  { to: '/team', icon: 'group', label: 'Equipo' },
  { to: '/reports', icon: 'bar_chart', label: 'Reportes' },
];

function Sidebar() {
  const history = useHistory();
  const activeProject = localStorage.getItem('currentProjectName') || 'Sin proyecto';

  return (
    <aside className="sidebar">
      <div className="sidebar__brand" onClick={() => history.push('/dashboard')}>
        <div>
          <h1 className="sidebar__title">Project Studio</h1>
          <span className="sidebar__subtitle">Proyecto activo: {activeProject}</span>
        </div>
      </div>

      <nav className="sidebar__nav">
        {navItems.map(item => (
          <NavLink
            key={item.to}
            to={item.to}
            className="sidebar__link"
            activeClassName="sidebar__link--active"
          >
            <span className="material-icons">{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}

export default Sidebar;
