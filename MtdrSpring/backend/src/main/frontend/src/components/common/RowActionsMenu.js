import React, { useEffect, useRef, useState } from 'react';
import './RowActionsMenu.css';

function RowActionsMenu({ items, ariaLabel = 'Acciones' }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!open) return undefined;

    const handleOutsideClick = (event) => {
      if (ref.current && !ref.current.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [open]);

  return (
    <div className="row-actions-menu" ref={ref}>
      <button
        type="button"
        className="row-actions-menu__trigger"
        onClick={() => setOpen((prev) => !prev)}
        aria-label={ariaLabel}
        aria-expanded={open}
        aria-haspopup="menu"
      >
        <span className="material-icons">more_horiz</span>
      </button>
      {open && (
        <div className="row-actions-menu__dropdown" role="menu">
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={`row-actions-menu__item ${item.danger ? 'row-actions-menu__item--danger' : ''}`}
              onClick={() => {
                setOpen(false);
                item.onClick();
              }}
            >
              {item.icon && (
                <span className="material-icons row-actions-menu__icon" aria-hidden>
                  {item.icon}
                </span>
              )}
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default RowActionsMenu;
