import React from 'react';
import { currentUser } from '../../data/mockData';
import './TopNavBar.css';

function TopNavBar({ searchPlaceholder = 'Buscar recursos...' }) {
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
        <div className="topnav__user">
          <div className="topnav__user-info">
            <span className="topnav__user-name">{currentUser.fullName}</span>
            <span className="topnav__user-role">{currentUser.roleName}</span>
          </div>
          <div className="topnav__avatar">
            <span className="material-icons">person</span>
          </div>
        </div>
      </div>
    </header>
  );
}

export default TopNavBar;
