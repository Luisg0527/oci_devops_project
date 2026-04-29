import React, { Suspense, lazy } from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import App from './App';
import './theme.css';

const ManagerDashboard = lazy(() => import('./pages/ManagerDashboard'));
const Backlog = lazy(() => import('./pages/Backlog'));
const TeamManagement = lazy(() => import('./pages/TeamManagement'));
const Reports = lazy(() => import('./pages/Reports'));
const Login = lazy(() => import('./pages/Login'));

const Loading = () => (
  <div style={{
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    height: '100vh', fontFamily: 'var(--font-family)', color: 'var(--color-text-secondary)'
  }}>
    Loading...
  </div>
);

function Root() {
  return (
    <Router>
      <Suspense fallback={<Loading />}>
        <Switch>
          <Route exact path="/" component={App} />
          <Route path="/dashboard" component={ManagerDashboard} />
          <Route path="/backlog" component={Backlog} />
          <Route path="/team" component={TeamManagement} />
          <Route path="/reports" component={Reports} />
          <Route path="/login" component={Login} />
        </Switch>
      </Suspense>
    </Router>
  );
}

export default Root;
