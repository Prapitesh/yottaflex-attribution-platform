import {useEffect,useMemo,useState} from "react";
import {Activity,ArrowUpRight,BarChart3,CheckCircle2,Database,Download,GitBranch,Layers,Play,RefreshCw,ShieldAlert,Sparkles,Target,Wallet,Workflow} from "lucide-react";
import {Bar,BarChart,CartesianGrid,ResponsiveContainer,Tooltip,XAxis,YAxis} from "recharts";
import {generate,getCampaigns,getIssues,getSummary,getDatasetRuns,recalculate,validateData,type AttributionModel} from "../services/api";
import { MetricCard } from "../components/MetricCard";
import { ModelOption } from "../components/ModelOption";

type Row=Record<string,any>;
const money=(n:number)=>new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR",maximumFractionDigits:0}).format(n||0);
const number=(n:number)=>new Intl.NumberFormat("en-IN").format(n||0);

export default function DashboardPage(){
 const [summary,setSummary]=useState<Row|null>(null);
 const [campaigns,setCampaigns]=useState<Row[]>([]);
 const [issues,setIssues]=useState<Row[]>([]);
 const [runs,setRuns]=useState<Row[]>([]);
 const [runId,setRunId]=useState("");
 const [model,setModel]=useState<AttributionModel>("LINEAR");
 const [loading,setLoading]=useState(false);
 const [message,setMessage]=useState("");
 const [error,setError]=useState("");
 const counts=summary?.counts||{};
 const load=async()=>{
  try{
   setError("");
   const selectedRun=runId || undefined;
   const [s,c,i,r]=await Promise.all([getSummary(selectedRun),getCampaigns(model,selectedRun),getIssues(selectedRun),getDatasetRuns()]);
   setSummary(s);setCampaigns(c);setIssues(i);setRuns(r);
   if(runId && !r.some((entry:Row)=>entry.id===runId)) setRunId("");
  }
  catch(e:any){setError(e?.response?.data?.message||"Cannot reach the API. Start Spring Boot and PostgreSQL, then refresh.");}
 };
 useEffect(()=>{load();},[model,runId]);
 const topCampaigns=useMemo(()=>campaigns.slice(0,8).map(c=>({...c,shortName:String(c.campaign_name||"").replace("Campaign ","C")})),[campaigns]);
 const run=async(action:string)=>{
  setLoading(true);setMessage("");setError("");
  try{
   let result:any;
   let generatedRunId:string|undefined;
   if(action==="starter"||action==="full"){
    result=await generate(action==="full"?"FULL":"STARTER");
    generatedRunId=result.runId;
    setMessage("Synthetic data generated: "+JSON.stringify(result));
   }
   else if(action==="calculate"){result=await recalculate(runId||undefined);setMessage("Attribution calculated: "+JSON.stringify(result));}
   else{result=await validateData(runId||undefined);setMessage("Validation complete: "+JSON.stringify(result));}
   if(generatedRunId) setRunId(generatedRunId);
   else await load();
  }catch(e:any){setError(e?.response?.data?.message||e.message||"Action failed");}
  finally{setLoading(false);}
 };
 const issuesCount=issues.length;
 return <div className="shell">
  <aside className="sidebar">
   <div className="brand"><div className="brand-mark"><Workflow size={21}/></div><div><strong>YottaFlex</strong><small>ATTRIBUTION INTELLIGENCE</small></div></div>
   <div className="nav-label">WORKSPACE</div>
   <a className="nav active" href="#overview"><BarChart3 size={17}/> Overview</a>
   <a className="nav" href="#campaign-analytics"><Layers size={17}/> Campaign analytics</a>
   <a className="nav" href="#attribution-models"><GitBranch size={17}/> Attribution paths</a>
   <a className="nav" href="#data-quality"><ShieldAlert size={17}/> Data quality <span className="nav-count">{issuesCount}</span></a>
   <div className="sidebar-bottom"><div className="status-dot"/> Synthetic environment <small>SEED · 20261022</small></div>
  </aside>
  <main className="main" id="overview">
   <header className="topbar"><div><div className="eyebrow">MARKETING INTELLIGENCE / OVERVIEW</div><h1>Attribution overview</h1></div><div className="top-actions"><select aria-label="Dataset run" value={runId} onChange={e=>setRunId(e.target.value)}><option value="">All dataset runs</option>{runs.map((r:Row)=><option key={r.id} value={r.id}>{r.id.slice(0,12)} · {r.status}</option>)}</select><span className="live"><i/> LOCAL ENVIRONMENT</span><button className="icon-btn" aria-label="Refresh dashboard" onClick={load}><RefreshCw size={16}/></button><div className="avatar">PA</div></div></header>
   {error&&<div className="alert error"><ShieldAlert size={18}/>{error}</div>}
   {message&&<div className="alert success"><CheckCircle2 size={18}/>{message}</div>}
   <section className="hero"><div><div className="hero-tag"><Sparkles size={14}/> PERFORMANCE WORKSPACE</div><h2>Know what drives<br/><em>real revenue.</em></h2><p>Connect every campaign touch to pipeline and closed revenue. Compare attribution models with transparent, traceable decisions.</p><div className="hero-actions"><button className="primary" disabled={loading} onClick={()=>run("calculate")}><Play size={15} fill="currentColor"/> Run attribution</button><button className="secondary" disabled={loading} onClick={()=>run("starter")}><Database size={15}/> Generate starter data</button></div></div><div className="hero-graphic"><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="orbit-core"><Target size={36}/></div><div className="floating f1">+38.4%<small>ATTRIBUTED ROI</small></div><div className="floating f2"><Activity size={14}/> Journey mapped</div></div></section>
   <section className="section-head"><div><div className="eyebrow">BUSINESS SNAPSHOT</div><h3>Performance at a glance</h3></div><span className="period">SYNTHETIC DATASET · ALL PERIODS</span></section>
   <div className="metrics">
    <MetricCard icon={<Wallet/>} label="Recognized revenue" value={money(summary?.recognizedRevenue)} note="Closed-won orders" trend="REALIZED"/>
    <MetricCard icon={<Activity/>} label="Reconciled spend" value={money(summary?.reconciledSpend)} note="Matching campaign currency" trend="VERIFIED"/>
    <MetricCard icon={<Layers/>} label="Opportunities" value={number(counts.opportunities)} note="Pipeline conversion records" trend="TRACKED"/>
    <MetricCard icon={<ShieldAlert/>} label="Quality exceptions" value={number(counts.data_quality_issues)} note="Exceptions requiring review" trend={issuesCount?"REVIEW":"CLEAR"}/>
   </div>
   <section className="charts-grid">
    <div className="panel wide"><div className="panel-head"><div><div className="eyebrow">CAMPAIGN PERFORMANCE</div><h3>Attributed revenue by campaign</h3></div><select aria-label="Attribution model" value={model} onChange={e=>setModel(e.target.value as AttributionModel)}><option value="FIRST_TOUCH">First touch</option><option value="LAST_TOUCH">Last touch</option><option value="LINEAR">Linear multi-touch</option></select></div>
     <div className="chart"><ResponsiveContainer width="100%" height="100%"><BarChart data={topCampaigns} margin={{top:10,right:8,left:0,bottom:35}}><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#28364b"/><XAxis dataKey="shortName" tick={{fill:"#8996aa",fontSize:11}} axisLine={false} tickLine={false} angle={-25} textAnchor="end"/><YAxis tick={{fill:"#8996aa",fontSize:11}} axisLine={false} tickLine={false} tickFormatter={v=>"₹"+Math.round(v/1000)+"k"}/><Tooltip contentStyle={{background:"#111e32",border:"1px solid #34445d",borderRadius:10}} formatter={(v:any)=>money(Number(v))}/><Bar dataKey="attributed_revenue" name="Attributed revenue" radius={[5,5,0,0]} fill="#51d8c4"/></BarChart></ResponsiveContainer></div>
    </div>
    <div className="panel" id="attribution-models"><div className="panel-head"><div><div className="eyebrow">MODEL COMPARISON</div><h3>Attribution logic</h3></div><span className="mini-badge">3 MODELS</span></div>
     <div className="model-list"><ModelOption name="First touch" description="Rewards the journey opener" color="mint" active={model==="FIRST_TOUCH"} onClick={()=>setModel("FIRST_TOUCH")}/><ModelOption name="Last touch" description="Rewards the final interaction" color="blue" active={model==="LAST_TOUCH"} onClick={()=>setModel("LAST_TOUCH")}/><ModelOption name="Linear" description="Shares credit across eligible touches" color="purple" active={model==="LINEAR"} onClick={()=>setModel("LINEAR")}/></div>
     <div className="model-note"><CheckCircle2 size={15}/> Only touches before conversion count. 180-day lookback.</div>
    </div>
   </section>
   <section className="charts-grid lower">
    <div className="panel"><div className="panel-head"><div><div className="eyebrow">DATA COVERAGE</div><h3>Connected records</h3></div><Database size={18} className="muted"/></div><div className="coverage">{[["Campaigns",counts.campaigns],["Contacts",counts.contacts],["Touchpoints",counts.touchpoints],["Web sessions",counts.web_sessions],["Leads",counts.leads],["Orders",counts.orders]].map(([n,v]:any)=><div className="coverage-row" key={n}><span>{n}</span><strong>{number(v)}</strong></div>)}</div></div>
    <div className="panel" id="data-quality"><div className="panel-head"><div><div className="eyebrow">DATA GOVERNANCE</div><h3>Quality exceptions</h3></div><span className="mini-badge warning">{issuesCount} OPEN</span></div>
     <div className="issue-list">{issues.length?issues.slice(0,4).map((x,i)=><div className="issue" key={x.id||i}><span className={"severity "+String(x.severity||"warning").toLowerCase()}>{x.severity||"WARN"}</span><div><strong>{String(x.type||"").replaceAll("_"," ")}</strong><small>{x.description}</small></div></div>):<div className="empty">No validation results yet. Run data validation to inspect the dataset.</div>}</div>
     <button className="text-btn" onClick={()=>run("validate")} disabled={loading}>Run data quality checks <ArrowUpRight size={14}/></button>
    </div>
   </section>
   <section className="panel campaign-panel" id="campaign-analytics"><div className="panel-head"><div><div className="eyebrow">RETURN ON INVESTMENT</div><h3>Campaign performance table</h3></div><button className="secondary small" onClick={()=>run("full")} disabled={loading}><Download size={14}/> Generate full dataset</button></div>
    <div className="table-wrap"><table><thead><tr><th>CAMPAIGN</th><th>SPEND</th><th>ATTRIBUTED REVENUE</th><th>ROI</th><th>STATUS</th></tr></thead><tbody>{campaigns.slice(0,10).map((c,i)=><tr key={c.campaign_id||i}><td><strong>{c.campaign_name}</strong><small>{c.campaign_id}</small></td><td>{money(c.spend)}</td><td>{money(c.attributed_revenue)}</td><td className={c.roi_percent>=0?"positive":"negative"}>{c.roi_percent==null?"—":Number(c.roi_percent).toFixed(1)+"%"}</td><td><span className={"table-status "+String(c.roi_status).toLowerCase()}>{String(c.roi_status).replaceAll("_"," ")}</span></td></tr>)}</tbody></table>{!campaigns.length&&<div className="empty">Generate synthetic data and run attribution to populate campaign metrics.</div>}</div>
   </section>
   <footer>YOTTAFLEX ATTRIBUTION INTELLIGENCE <span>EXPLAINABLE · REPRODUCIBLE · AUDITABLE</span></footer>
  </main>
 </div>;
}
