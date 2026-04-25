import React, { useEffect, useMemo, useRef, useState } from 'react';
import { API_BASE } from '../../config/apiBase';
import './TopNavBar.css';

function TopNavBar({ searchPlaceholder = 'Buscar recursos...' }) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef(null);

  const userData = useMemo(() => {
    const fullName = localStorage.getItem('userFullName');
    const role = localStorage.getItem('userRole');

    return {
      fullName: fullName || 'Usuario',
      role: role || 'Sin rol',
    };
  }, []);

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  const handleLogout = async () => {
    const token = localStorage.getItem('authToken');

    try {
      if (token) {
        await fetch(`${API_BASE}/auth/logout`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
      }
    } catch (error) {
      // Best-effort logout: local session is always cleared.
    } finally {
      localStorage.removeItem('authToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userId');
      localStorage.removeItem('userFullName');
      localStorage.removeItem('userEmail');
      localStorage.removeItem('userRole');
      window.location.assign('/login');
    }
  };

  return (
    <header className="topnav">
      <div className="topnav__left">
        <span className="topnav__brand">Architect Prime</span>
        <div className="topnav__search">
          <span className="material-icons topnav__search-icon">search</span>
          <input
            type="text"
            className="topnav__search-input"
            placeholder={searchPlaceholder}
          />
        </div>
      </div>

      <div className="topnav__right">
        <button className="topnav__icon-btn">
          <span className="material-icons">notifications_none</span>
        </button>
        <button className="topnav__icon-btn">
          <span className="material-icons">chat_bubble_outline</span>
        </button>
        <div className="topnav__divider" />
        <div className="topnav__user" ref={menuRef}>
          <div className="topnav__user-info">
            <span className="topnav__user-name">{userData.fullName}</span>
            <span className="topnav__user-role">{userData.role}</span>
          </div>
          <button
            type="button"
            className="topnav__avatar topnav__avatar-btn"
            onClick={() => setIsMenuOpen((prev) => !prev)}
            aria-label="Abrir menú de usuario"
            aria-expanded={isMenuOpen}
          >
            <span className="material-icons">person</span>
          </button>
          {isMenuOpen && (
            <div className="topnav__menu">
              <button type="button" className="topnav__menu-item" onClick={handleLogout}>
                Cerrar sesión
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default TopNavBar;
