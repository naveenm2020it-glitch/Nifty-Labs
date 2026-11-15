import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Activity, BarChart3, PanelsTopLeft, Calculator, History, BookOpen,
  Sun, Moon, Wifi, WifiOff, LoaderCircle,
} from 'lucide-react';

type PageKey = 'session' | 'overview' | 'contracts' | 'analytics' | 'historical-sr' | 'guide';
type ApiStatus = 'checking' | 'online' | 'offline';
type Theme = 'light' | 'dark';

const NAV_ITEMS: Array<{key:PageKey;label:string;short:string;icon:typeof Activity}> = [
  {key:'session',label:'Data session',short:'Data',icon:Activity},
  {key:'overview',label:'Market levels',short:'Levels',icon:BarChart3},
  {key:'contracts',label:'Contracts',short:'Chain',icon:PanelsTopLeft},
  {key:'analytics',label:'BEP analysis',short:'BEP',icon:Calculator},
  {key:'historical-sr',label:'Historical S&R',short:'History',icon:History},
  {key:'guide',label:'Formula guide',short:'Guide',icon:BookOpen},
];

export default function App() {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const [activePage,setActivePage] = useState<PageKey>('session');
  const [apiStatus,setApiStatus] = useState<ApiStatus>('checking');
  const [theme,setTheme] = useState<Theme>(() => {
    try { return window.localStorage.getItem('niftylab-mobile-theme') === 'dark' ? 'dark' : 'light'; }
    catch { return 'light'; }
  });

  const syncWorkspace = useCallback((page:PageKey) => {
    const frame=iframeRef.current;
    const doc=frame?.contentDocument;
    if(!doc)return;
    try{
      doc.documentElement.dataset.theme=theme;
      const nav=doc.querySelector<HTMLButtonElement>(`button[data-page-target="${page}"]`);
      if(nav && !nav.classList.contains('active')) nav.click();
      const win=frame.contentWindow;
      if(win){const resizeEvent=doc.createEvent('Event');resizeEvent.initEvent('resize',true,false);win.dispatchEvent(resizeEvent);win.scrollTo({top:0,behavior:'auto'});}
    }catch{/* The workspace will synchronize after its next load. */}
  },[theme]);

  const checkApi=useCallback(async()=>{
    const controller=new AbortController();
    const timeout=window.setTimeout(()=>controller.abort(),4500);
    try{
      const response=await fetch('/api/health',{signal:controller.signal,cache:'no-store'});
      if(!response.ok)throw new Error('API unavailable');
      const payload:unknown=await response.json();
      setApiStatus(payload && typeof payload==='object' && 'status' in payload && payload.status==='ok'?'online':'offline');
    }catch{setApiStatus('offline');}
    finally{window.clearTimeout(timeout);}
  },[]);

  useEffect(()=>{void checkApi();},[checkApi]);
  useEffect(()=>{try{window.localStorage.setItem('niftylab-mobile-theme',theme);}catch{/* Storage may be disabled. */} syncWorkspace(activePage);},[theme,activePage,syncWorkspace]);

  const choosePage=(page:PageKey)=>{setActivePage(page);syncWorkspace(page);};
  const statusLabel=apiStatus==='online'?'Engine connected':apiStatus==='checking'?'Connecting to engine':'Engine offline';
  const StatusIcon=apiStatus==='online'?Wifi:apiStatus==='checking'?LoaderCircle:WifiOff;

  return (
    <main className={`nifty-shell ${theme}`}>
      <header className="nifty-topbar">
        <div className="nifty-brand">
          <span className="nifty-brand-mark"><Activity size={19} strokeWidth={2.2}/></span>
          <span className="nifty-brand-copy"><strong>NIFTY LAB</strong><small>MARKET INTELLIGENCE</small></span>
        </div>
        <div className="nifty-top-actions">
          <span className={`nifty-engine-status ${apiStatus}`}><StatusIcon size={13}/><span>{statusLabel}</span></span>
          <button className="nifty-theme-toggle" type="button" onClick={()=>setTheme(v=>v==='light'?'dark':'light')} aria-label={theme==='light'?'Switch to dark theme':'Switch to light theme'} title={theme==='light'?'Dark appearance':'Light appearance'}>
            {theme==='light'?<Moon size={18}/>:<Sun size={18}/>}
          </button>
        </div>
      </header>
      <section className="nifty-workspace" aria-label="NIFTY LAB workspace">
        <iframe ref={iframeRef} className="workspace-frame" title="NIFTY LAB feature workspace" src="/workspace" onLoad={()=>syncWorkspace(activePage)}/>
      </section>
      <nav className="nifty-bottom-dock" aria-label="NIFTY LAB sections">
        {NAV_ITEMS.map(item=>{const Icon=item.icon;const active=activePage===item.key;return <button key={item.key} type="button" className={`nifty-dock-item ${active?'active':''}`} onClick={()=>choosePage(item.key)} aria-current={active?'page':undefined} aria-label={item.label}><span className="nifty-dock-icon"><Icon size={19} strokeWidth={1.85}/></span><span className="nifty-dock-label">{item.short}</span></button>;})}
      </nav>
    </main>
  );
}
