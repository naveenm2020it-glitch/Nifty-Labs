import { useCallback, useEffect, useRef, useState } from 'react';
import {
  SlidersHorizontal, LayoutDashboard, PanelsTopLeft, Calculator, History, BookOpen,
} from 'lucide-react';

type PageKey = 'session' | 'overview' | 'contracts' | 'analytics' | 'historical-sr' | 'guide';
type ApiStatus = 'checking' | 'online' | 'offline';

const NAV_ITEMS: Array<{key:PageKey;label:string;short:string;description:string;icon:typeof SlidersHorizontal}> = [
  {key:'session',label:'Data session',short:'Data',description:'Historical date, expiry and strike range',icon:SlidersHorizontal},
  {key:'overview',label:'Market Levels',short:'Levels',description:'Session snapshot and calculated levels',icon:LayoutDashboard},
  {key:'contracts',label:'Contracts',short:'Contracts',description:'HLC ATM OHLC and selected option contracts',icon:PanelsTopLeft},
  {key:'analytics',label:'BEP analysis',short:'BEP',description:'Call/put closing premium calculations',icon:Calculator},
  {key:'historical-sr',label:'Historical S&R',short:'History',description:'Historical level reactions and event analysis',icon:History},
  {key:'guide',label:'Formula guide',short:'Guide',description:'Existing formulas and calculation notes',icon:BookOpen},
];

export default function App() {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const [activePage,setActivePage] = useState<PageKey>('session');
  const [apiStatus,setApiStatus] = useState<ApiStatus>('checking');

  const syncWorkspace = useCallback((page:PageKey) => {
    const frame=iframeRef.current;
    const doc=frame?.contentDocument;
    if(!doc)return;
    try{
      const nav=doc.querySelector<HTMLButtonElement>(`button[data-page-target="${page}"]`);
      if(nav && !nav.classList.contains('active')) nav.click();
      const win=frame.contentWindow;
      if(win){win.dispatchEvent(new Event('resize'));win.scrollTo({top:0,behavior:'auto'});}
    }catch{/* The iframe will retry navigation after it finishes loading. */}
  },[]);

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
  useEffect(()=>{syncWorkspace(activePage);},[activePage,syncWorkspace]);

  const choosePage=(page:PageKey)=>{
    setActivePage(page);
    syncWorkspace(page);
  };

  return (
    <main className="reference-app-shell">
      <section className="reference-workspace" aria-label="NIFTY LAB workspace">
        <iframe
          ref={iframeRef}
          className="workspace-frame"
          title="NIFTY LAB feature workspace"
          src="/workspace"
          onLoad={()=>syncWorkspace(activePage)}
        />
        <div className={`reference-api-indicator ${apiStatus}`} aria-label={apiStatus==='online'?'Backend connected':apiStatus==='checking'?'Checking backend':'Backend unavailable'}>
          <span />
        </div>
      </section>
      <nav className="reference-bottom-dock" aria-label="NIFTY LAB sections">
        {NAV_ITEMS.map(item=>{
          const Icon=item.icon;
          const active=activePage===item.key;
          return <button key={item.key} type="button" className={`reference-dock-item ${active?'active':''}`} onClick={()=>choosePage(item.key)} aria-current={active?'page':undefined} aria-label={item.label}>
            <span className="reference-dock-icon"><Icon size={21} strokeWidth={1.8}/></span>
            <span className="reference-dock-label">{item.short}</span>
          </button>;
        })}
      </nav>
    </main>
  );
}
