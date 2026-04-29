import React from 'react';
import Sidebar from './Sidebar';
import TopNavBar from './TopNavBar';

function PageLayout({ children }) {
  return (
    <div className="page-layout">
      <Sidebar />
      <div className="page-layout__main">
        <TopNavBar />
        <div className="page-layout__content">
          {children}
        </div>
      </div>
    </div>
  );
}

export default PageLayout;
