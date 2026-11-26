/* Base única do protótipo. Sem serviços externos ou dados reais de clientes. */
'use strict';
window.Demo = (() => {
 const key = 'doutor-embalagem.demo.v1';
 const companyExample = {id:'horizonte',name:'Cooperativa Horizonte',unit:'Centro',address:'Rua das Flores, 120',contact:'Marina · Administração',consultant:'Ana Lima'};
 const catalog = [
  {name:'Lixeira com pedal',detail:'20 litros · Plástico',icon:'box',mouth:38,height:42},
  {name:'Lixeira com pedal',detail:'60 litros · Plástico',icon:'box',mouth:68,height:76},
  {name:'Lixeira de escritório',detail:'12 litros · Sem tampa',icon:'box',mouth:38,height:42},
  {name:'Dispensador de sabonete',detail:'800 ml · Parede',icon:'drop'},
  {name:'Dispensador de papel toalha',detail:'Interfolhado · Parede',icon:'paper'},
  {name:'Dispensador de papel higiênico',detail:'Rolão · Parede',icon:'paper'}
 ];
 const roomNames = ['Recepção','Copa','Sanitário feminino','Sanitário masculino','Sala de reuniões','Área administrativa'];
 // Quantidade anual × preço unitário (milésimos de real), e quantidade × consumo unitário.
 // Referência: abas de consumo da planilha fornecida. As medidas/quantidades de campo
 // são independentes deste cenário anual, que ainda precisa ser validado na consultoria real.
 const scenario = [
  {id:'sacos',name:'Sacos de lixo',unit:'kg de plástico',source:'Sacos de Lixo',
   current:[{name:'Saco 20 L',quantity:66,price:25000,use:2.46},{name:'Saco 60 L',quantity:60,price:65900,use:5},{name:'Saco 100 L',quantity:42,price:65900,use:5}],
   proposed:[{name:'Saco 12 L',quantity:66,price:9000,use:.68},{name:'Saco 30 L',quantity:22,price:14000,use:1.1},{name:'Saco 40 L',quantity:28,price:19000,use:1.46},{name:'Saco 60 L',quantity:10,price:36000,use:3.1},{name:'Saco 100 L',quantity:18,price:65300,use:5.76}],package:'pacotes'},
  {id:'sabonete',name:'Sabonete líquido',unit:'litros',source:'Sabonete Líquido',
   current:[{name:'Sabonete cremoso · 5 L',quantity:12,price:35000,use:5}],proposed:[{name:'Sabonete espuma · 5 L',quantity:4,price:31250,use:5}],package:'galões'},
  {id:'toalha',name:'Papel toalha',unit:'folhas',source:'Papel Toalha',
   current:[{name:'Interfolhado · 1.000 folhas',quantity:287,price:27940,use:1000}],proposed:[{name:'Bobina · 909 folhas equivalentes',quantity:221,price:20995,use:909}],package:'pacotes / bobinas'},
  {id:'higienico',name:'Papel higiênico',unit:'metros',source:'Papel Higiênico',
   current:[{name:'Rolinho · 30 m',quantity:2304,price:2343,use:30}],proposed:[{name:'Rolão · 250 m',quantity:250,price:17800,use:250}],package:'rolos'}
 ];
 function analyze() {
  const categories=scenario.map(c=>{
   const sum=(rows,field)=>rows.reduce((s,r)=>s+r.quantity*r[field],0);
   const current=sum(c.current,'price')/1000,proposed=sum(c.proposed,'price')/1000;
   return {...c,currentCost:current,proposedCost:proposed,saving:current-proposed,currentUse:Number(sum(c.current,'use').toFixed(6)),proposedUse:Number(sum(c.proposed,'use').toFixed(6))};
  });
  const current=categories.reduce((s,c)=>s+c.currentCost,0),proposed=categories.reduce((s,c)=>s+c.proposedCost,0);
  return {categories,current,proposed,saving:current-proposed,percent:(current-proposed)/current*100};
 }
 const blank=()=>({version:1,company:null,visit:null,catalog:catalog.map(c=>({...c})),roomNames:[...roomNames]});
 function validate(state) {
  if(!state || state.version!==1 || !Array.isArray(state.catalog) || !Array.isArray(state.roomNames)) throw Error('Dados da demonstração inválidos. Use Reiniciar demonstração.');
  if(state.company && (state.company.id!=='horizonte' || ['name','unit','address','contact','consultant'].some(k=>typeof state.company[k]!=='string'||!state.company[k].trim()))) throw Error('Empresa inválida. Use Reiniciar demonstração.');
  if(state.catalog.some(c=>!c || typeof c.name!=='string' || typeof c.detail!=='string' || !['box','drop','paper','rooms'].includes(c.icon))) throw Error('Catálogo inválido. Use Reiniciar demonstração.');
  if(state.visit && (!state.company || state.visit.companyId!==state.company.id || !['draft','sent'].includes(state.visit.status) || !Array.isArray(state.visit.rooms) || state.visit.rooms.some(r=>typeof r.name!=='string'||!Array.isArray(r.items)||r.items.some(i=>!Number.isInteger(i.quantity)||i.quantity<1||!Number.isInteger(i.catalog)||!state.catalog[i.catalog])))) throw Error('Coleta inválida. Use Reiniciar demonstração.');
  return state;
 }
 function createStore(storage) {
  function read() {
   let raw;
   try {raw=storage.getItem(key);} catch {throw Error('O armazenamento local está indisponível. Abra pelo endereço HTTP local e permita o armazenamento no navegador.');}
   if(!raw) return blank();
   let state;try {state=JSON.parse(raw);} catch {throw Error('Dados da demonstração inválidos. Use Reiniciar demonstração.');}
   return validate(state);
  }
  function save(state) {
   validate(state);
   try {storage.setItem(key,JSON.stringify(state));} catch {throw Error('Não foi possível salvar. Verifique o armazenamento local do navegador.');}
  }
  return {read,save,
   saveCompany(input) {const state=read();state.company={...companyExample,...input,id:'horizonte'};save(state);return state.company;},
   createVisit() {const state=read();if(!state.company) throw Error('Cadastre a empresa no dashboard primeiro.');if(!state.visit){state.visit={id:'visita-001',companyId:state.company.id,status:'draft',date:'21/10/2026',rooms:[]};save(state);}return state.visit;},
   sendVisit() {const state=read();if(!state.visit || !state.visit.rooms.length || state.visit.rooms.some(r=>!r.items.length)) throw Error('Adicione pelo menos um equipamento em cada ambiente antes de enviar.');state.visit.status='sent';save(state);return state.visit;},
   reset() {try {storage.removeItem(key);} catch {throw Error('Não foi possível reiniciar o armazenamento local.');}}
  };
 }
 function summarize(state) {const rooms=state.visit?.rooms||[];return {rooms:rooms.length,equipment:rooms.reduce((s,r)=>s+r.items.reduce((q,i)=>q+i.quantity,0),0)};}
 const escape=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const money=value=>new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(value);
 const number=value=>new Intl.NumberFormat('pt-BR',{maximumFractionDigits:2}).format(value);
 return {key,companyExample,createStore,analyze,summarize,escape,money,number};
})();
