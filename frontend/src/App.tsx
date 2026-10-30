import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Activity,
  BookOpen,
  Calculator,
  ChevronRight,
  CircleAlert,
  Database,
  History,
  LayoutDashboard,
  Menu,
  Moon,
  PanelLeftClose,
  PanelLeftOpen,
  PanelsTopLeft,
  RefreshCw,
  ShieldCheck,
  SlidersHorizontal,
  Sun,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';

type PageKey = 'session' | 'overview' | 'contracts' | 'analytics' | 'historical-sr' | 'guide';
type ApiStatus = 'checking' | 'online' | 'offline';

const NAV_ITEMS: Array<{
  key: PageKey;
  label: string;
  description: string;
  icon: typeof LayoutDashboard;
  group: 'workspace' | 'analysis';
}> = [
  { key: 'session', label: 'Data session', description: 'Historical date, expiry and strike range', icon: SlidersHorizontal, group: 'workspace' },
  { key: 'overview', label: 'Market Levels', description: 'Session snapshot and calculated levels', icon: LayoutDashboard, group: 'workspace' },
  { key: 'contracts', label: 'Contracts', description: 'HLC ATM OHLC and selected option contracts', icon: PanelsTopLeft, group: 'workspace' },
  { key: 'analytics', label: 'BEP analysis', description: 'Call/put closing premium calculations', icon: Calculator, group: 'analysis' },
  { key: 'historical-sr', label: 'Historical S&R', description: 'Historical level reactions and event analysis', icon: History, group: 'analysis' },
  { key: 'guide', label: 'Formula guide', description: 'Existing formulas and calculation notes', icon: BookOpen, group: 'analysis' },
];

function getSavedTheme(): 'light' | 'dark' {
  try {
    const value = localStorage.getItem('nifty-dashboard-theme');
    return value === 'dark' ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

function getSavedCollapsed(): boolean {
  if (typeof window !== 'undefined' && window.matchMedia('(max-width: 900px)').matches) return false;
  try {
    return localStorage.getItem('nifty-lab-sidebar-collapsed') === 'true';
  } catch {
    return false;
  }
}

export default function App() {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const [activePage, setActivePage] = useState<PageKey>('session');
  const [collapsed, setCollapsed] = useState(getSavedCollapsed);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [theme, setTheme] = useState<'light' | 'dark'>(getSavedTheme);
  const [apiStatus, setApiStatus] = useState<ApiStatus>('checking');
  const [frameError, setFrameError] = useState(false);
  const [showIntro, setShowIntro] = useState(() => { try { const shouldShow = sessionStorage.getItem('nifty-lab-intro-seen') !== '1' && !window.matchMedia('(prefers-reduced-motion: reduce)').matches; if (shouldShow) sessionStorage.setItem('nifty-lab-intro-seen', '1'); return shouldShow; } catch { return false; } });
  const activeItem = NAV_ITEMS.find((item) => item.key === activePage) ?? NAV_ITEMS[1];
  const ActivePageIcon = activeItem.icon;

  const syncWorkspace = useCallback((page: PageKey, nextTheme: 'light' | 'dark') => {
    const frame = iframeRef.current;
    const doc = frame?.contentDocument;
    if (!doc) return;
    try {
      doc.documentElement.dataset.theme = nextTheme;
      const nav = doc.querySelector<HTMLButtonElement>(`button[data-page-target="${page}"]`);
      if (nav && !nav.classList.contains('active')) nav.click();
      const frameWindow = frame.contentWindow;
      if (frameWindow) {
        // Re-run the original chart resize/theme listener so custom charts keep
        // their existing dimensions and colour tokens after a theme change.
        const resizeEvent = frameWindow.document.createEvent('Event');
        resizeEvent.initEvent('resize', true, false);
        frameWindow.dispatchEvent(resizeEvent);
        frameWindow.scrollTo({ top: 0, behavior: 'auto' });
      }
      setFrameError(false);
    } catch {
      setFrameError(true);
    }
  }, []);

  const checkApi = useCallback(async () => {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 4500);
    try {
      const response = await fetch('/api/health', { signal: controller.signal, cache: 'no-store' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const payload: unknown = await response.json();
      if (payload && typeof payload === 'object' && 'status' in payload && payload.status === 'ok') {
        setApiStatus('online');
      } else {
        setApiStatus('offline');
      }
    } catch {
      setApiStatus('offline');
    } finally {
      window.clearTimeout(timeout);
    }
  }, []);

  useEffect(() => {
    if (!showIntro) return;
    const timer = window.setTimeout(() => { setShowIntro(false); try { sessionStorage.setItem('nifty-lab-intro-seen', '1'); } catch { /* storage may be disabled */ } }, 3100);
    return () => window.clearTimeout(timer);
  }, [showIntro]);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try { localStorage.setItem('nifty-dashboard-theme', theme); } catch { /* storage may be disabled */ }
  }, [theme]);

  useEffect(() => {
    void checkApi();
  }, [checkApi]);

  useEffect(() => {
    if (mobileOpen) {
      const onKeyDown = (event: KeyboardEvent) => {
        if (event.key === 'Escape') setMobileOpen(false);
      };
      document.addEventListener('keydown', onKeyDown);
      const previousOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.removeEventListener('keydown', onKeyDown);
        document.body.style.overflow = previousOverflow;
      };
    }
  }, [mobileOpen]);

  useEffect(() => {
    syncWorkspace(activePage, theme);
  }, [activePage, theme, syncWorkspace]);

  const choosePage = (page: PageKey) => {
    setActivePage(page);
    syncWorkspace(page, theme);
    setMobileOpen(false);
  };

  const toggleCollapsed = () => {
    setCollapsed((value) => {
      const next = !value;
      try { localStorage.setItem('nifty-lab-sidebar-collapsed', String(next)); } catch { /* storage may be disabled */ }
      return next;
    });
  };

  const toggleTheme = () => setTheme((value) => value === 'light' ? 'dark' : 'light');

  const sidebar = (mobile = false) => (
    <aside className={`terminal-sidebar ${collapsed && !mobile ? 'is-collapsed' : ''} ${mobile ? 'is-mobile' : ''}`} aria-label="Main navigation">
      <div className="sidebar-brand-row">
        <div className="brand-glyph" aria-hidden="true"><span /><span /><span /><span /></div>
        {(!collapsed || mobile) && <div className="brand-copy"><strong>NIFTY <span>LAB</span></strong><small>TRADING RESEARCH TERMINAL</small></div>}
        {mobile && <Button variant="ghost" size="icon" className="mobile-close" aria-label="Close navigation" onClick={() => setMobileOpen(false)}><X size={18} /></Button>}
      </div>

      <div className="nav-scroller">
        <div className="nav-group-label">{(!collapsed || mobile) && 'WORKSPACE'}</div>
        <nav className="terminal-nav" aria-label="Workspace">
          {NAV_ITEMS.filter((item) => item.group === 'workspace').map((item) => {
            const Icon = item.icon;
            const active = activePage === item.key;
            const content = (
              <button key={item.key} type="button" className={`terminal-nav-item ${active ? 'active' : ''}`} onClick={() => choosePage(item.key)} aria-current={active ? 'page' : undefined} aria-label={collapsed && !mobile ? item.label : undefined}>
                <Icon className="nav-item-icon" size={19} strokeWidth={1.8} />
                {(!collapsed || mobile) && <span className="nav-item-copy"><span>{item.label}</span><small>{item.description}</small></span>}
                {(!collapsed || mobile) && active && <span className="nav-active-mark" aria-hidden="true" />}
              </button>
            );
            return collapsed && !mobile ? (
              <Tooltip key={item.key} delayDuration={250}><TooltipTrigger asChild>{content}</TooltipTrigger><TooltipContent side="right">{item.label}</TooltipContent></Tooltip>
            ) : content;
          })}
        </nav>
        <div className="nav-group-label secondary-label">{(!collapsed || mobile) && 'ANALYSIS'}</div>
        <nav className="terminal-nav" aria-label="Analysis">
          {NAV_ITEMS.filter((item) => item.group === 'analysis').map((item) => {
            const Icon = item.icon;
            const active = activePage === item.key;
            const content = (
              <button key={item.key} type="button" className={`terminal-nav-item ${active ? 'active' : ''}`} onClick={() => choosePage(item.key)} aria-current={active ? 'page' : undefined} aria-label={collapsed && !mobile ? item.label : undefined}>
                <Icon className="nav-item-icon" size={19} strokeWidth={1.8} />
                {(!collapsed || mobile) && <span className="nav-item-copy"><span>{item.label}</span><small>{item.description}</small></span>}
                {(!collapsed || mobile) && active && <span className="nav-active-mark" aria-hidden="true" />}
              </button>
            );
            return collapsed && !mobile ? (
              <Tooltip key={item.key} delayDuration={250}><TooltipTrigger asChild>{content}</TooltipTrigger><TooltipContent side="right">{item.label}</TooltipContent></Tooltip>
            ) : content;
          })}
        </nav>
      </div>

      {(!collapsed || mobile) ? (
        <div className="sidebar-bottom-card">
          <div className="sidebar-bottom-icon"><ShieldCheck size={17} /></div>
          <div><strong>Calculation engine protected</strong><p>Values remain sourced from the existing backend.</p></div>
          <span className="integrity-dot" />
        </div>
      ) : (
        <Tooltip delayDuration={250}><TooltipTrigger asChild><div className="sidebar-collapsed-shield"><ShieldCheck size={19} /></div></TooltipTrigger><TooltipContent side="right">Original calculation engine preserved</TooltipContent></Tooltip>
      )}

      <div className="sidebar-footer">
        {(!collapsed || mobile) && <><span className="footer-dot" /><span>LOCAL WORKSPACE</span><span className="footer-version">v2</span></>}
        {!mobile && <Button className="collapse-control" variant="ghost" size="icon" onClick={toggleCollapsed} aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}>{collapsed ? <PanelLeftOpen size={17} /> : <PanelLeftClose size={17} />}</Button>}
      </div>
    </aside>
  );

  return (
    <TooltipProvider delayDuration={200}>
      {showIntro && <div className="cinematic-intro" role="status" aria-label="NIFTY LAB introduction"><div className="cinematic-ribbons"><i/><i/><i/><i/><i/></div><div className="cinematic-logo-card"><div className="cinematic-mark"><span/><span/><span/><span/></div><div><strong>NIFTY <em>LAB</em></strong><small>TRADING RESEARCH TERMINAL</small></div></div><button type="button" className="intro-skip" onClick={() => { setShowIntro(false); try { sessionStorage.setItem('nifty-lab-intro-seen', '1'); } catch { /* storage may be disabled */ } }}>Skip intro ↗</button></div>}
      <div className="terminal-layout">
        <div className="desktop-sidebar-slot">{sidebar(false)}</div>
        {mobileOpen && <button className="drawer-backdrop" aria-label="Close navigation overlay" onClick={() => setMobileOpen(false)} />}
        <div className={`mobile-sidebar-layer ${mobileOpen ? 'open' : ''}`} aria-hidden={!mobileOpen}>
          {sidebar(true)}
        </div>

        <main className="terminal-main">
          <header className="terminal-topbar">
            <div className="topbar-left">
              <Button variant="ghost" size="icon" className="mobile-menu-button" aria-label="Open navigation" onClick={() => setMobileOpen(true)}><Menu size={20} /></Button>
              <div className="breadcrumbs"><span>Terminal</span><ChevronRight size={14} /><strong>{activeItem.label}</strong></div>
              <span className="topbar-divider" />
              <span className="historical-tag"><Activity size={13} /> HISTORICAL RESEARCH</span>
            </div>
            <nav className="top-workspace-tabs" aria-label="Switch workspace screen">
              {NAV_ITEMS.map((item) => {
                const Icon = item.icon;
                return <button key={item.key} type="button" className={`top-workspace-tab ${activePage === item.key ? 'active' : ''}`} onClick={() => choosePage(item.key)} aria-current={activePage === item.key ? 'page' : undefined}><Icon size={15} /><span>{item.label}</span></button>;
              })}
            </nav>
            <div className="topbar-actions">
              <div className={`api-status ${apiStatus}`} title={apiStatus === 'online' ? 'Backend health endpoint responded successfully' : apiStatus === 'checking' ? 'Checking backend health' : 'Backend health endpoint not reachable'}>
                {apiStatus === 'online' ? <Wifi size={14} /> : apiStatus === 'offline' ? <WifiOff size={14} /> : <RefreshCw size={14} className="spin" />}
                <span>{apiStatus === 'online' ? 'API connected' : apiStatus === 'checking' ? 'Checking API' : 'API unavailable'}</span>
              </div>
              <div className="topbar-action-divider" />
              <Button variant="outline" size="icon" onClick={toggleTheme} aria-label={theme === 'light' ? 'Switch to dark theme' : 'Switch to light theme'} title={theme === 'light' ? 'Dark theme' : 'Light theme'} className="theme-button">
                {theme === 'light' ? <Moon size={17} /> : <Sun size={17} />}
              </Button>
            </div>
          </header>

          <div className="content-heading-row">
            <div className="current-page-icon"><ActivePageIcon size={19} /></div>
            <div className="current-page-copy"><h1>{activeItem.label}</h1><p>{activeItem.description}</p></div>
            <div className="source-integrity"><Database size={14} /><span>Live backend logic</span><span className="integrity-separator">/</span><span>No synthetic prices</span></div>
          </div>

          <section className="workspace-frame-wrap" aria-label={`${activeItem.label} workspace`}>
            <iframe
              ref={iframeRef}
              className="workspace-frame"
              title="NIFTY LAB protected feature workspace"
              src="/workspace"
              onLoad={() => syncWorkspace(activePage, theme)}
              onError={() => setFrameError(true)}
            />
            {frameError && <div className="frame-warning" role="status"><CircleAlert size={17} /><div><strong>Workspace connection issue</strong><span>The original feature view could not be reached. Confirm the FastAPI application is running, then refresh this page.</span></div><Button variant="secondary" size="sm" onClick={() => { setFrameError(false); iframeRef.current?.contentWindow?.location.reload(); void checkApi(); }}>Retry</Button></div>}
          </section>

          <footer className="terminal-footer">
            <span><span className="footer-dot" /> Data values and calculations are provided by the original NIFTY LAB engine.</span>
            <span>Designed for source-aware historical research</span>
          </footer>
        </main>
      </div>
    </TooltipProvider>
  );
}
