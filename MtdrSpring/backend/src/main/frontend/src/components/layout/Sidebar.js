import React from 'react';
import { NavLink, useHistory } from 'react-router-dom';
import './Sidebar.css';

const navItems = [
  { to: '/dashboard', icon: 'grid_view', label: 'Panel' },
  { to: '/backlog', icon: 'list_alt', label: 'Backlog' },
  { to: '/team', icon: 'group', label: 'Equipo' },
  { to: '/reports', icon: 'bar_chart', label: 'Reportes' },
];

const bottomItems = [
  { icon: 'settings', label: 'Configuración' },
  { icon: 'help_outline', label: 'Soporte' },
];

function Sidebar() {
  const history = useHistory();
  const projectName = localStorage.getItem('currentProjectName') || 'Project Studio';

  return (
    <aside className="sidebar">
      <div className="sidebar__brand" onClick={() => history.push('/projects')}>
        <div className="sidebar__logo">
          <span className="material-icons">architecture</span>
        </div>
        <div>
          <h1 className="sidebar__title">{projectName}</h1>
          <span className="sidebar__subtitle">Espacio Empresarial</span>
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

      <button className="sidebar__new-project btn btn--primary" onClick={() => history.push('/projects')}>
        <span className="material-icons" style={{ fontSize: 18 }}>add</span>
        <span>Nuevo Proyecto</span>
      </button>

      <div className="sidebar__bottom">
        {bottomItems.map(item => (
          <button key={item.label} className="sidebar__link sidebar__link--bottom">
            <span className="material-icons">{item.icon}</span>
            <span>{item.label}</span>
          </button>
        ))}
      </div>
    </aside>
  );
}

export default Sidebar;
