'use strict';
// Mesmo mock e armazenamento local do dashboard. Sem API.
const content = document.querySelector('#content');
const icon = name => `<svg aria-hidden="true"><use href="#${name}"/></svg>`;
let store,state,catalog,roomNames,visits=[];
const escapeHtml = Demo.escape;
const companyName=()=>escapeHtml(state.company?.name||'Empresa da demonstração');
const sent=()=>activeVisit?.status==='sent';
const dashboardLink=(label='Abrir dashboard')=>`<a class="primary" href="../prototipo-dashboard/consultoria.html">${label}${icon('arrow')}</a>`;
let activeVisit, activeRoom;
const total = room => room.items.reduce((sum,item)=>sum+item.quantity,0);
const intro = (eyebrow,title,description) => `<div class="intro"><span class="eyebrow">${eyebrow}</span><h2>${title}</h2><p class="muted">${description}</p></div>`;
const back = (action,label) => `<button class="back" data-action="${action}">${icon('back')}${label}</button>`;
const primary = (action,label) => `<button class="primary" data-action="${action}"><span>${label}</span>${icon('arrow')}</button>`;
const crumb = current => `<div class="breadcrumbs"><span>${companyName()}</span>${icon('arrow')}<span>${escapeHtml(current)}</span></div>`;
const empty = (title,text) => `<div class="empty-state"><div class="empty-icon">${icon('rooms')}</div><h3>${title}</h3><p>${text}</p></div>`;
function home() {
 return `${intro('COLETA EM CAMPO','Suas consultorias.','Tudo começa com uma visita à empresa.')}
 <section class="feature"><span class="eyebrow">${state.visit?'CONTINUAR O ROTEIRO':'UMA NOVA VISITA'}</span><h3>${state.visit?.status==='sent'?'Levantamento<br>enviado.':'Vamos começar<br>o levantamento?'}</h3><p>${state.company?'A empresa cadastrada no dashboard está disponível para a visita.':'Cadastre a empresa no dashboard para começar a coleta.'}</p>${state.company?`<button class="feature-btn" data-action="${state.visit?'open-visit':'new-visit'}" data-index="0">${state.visit?(state.visit.status==='sent'?'Conferir envio':'Continuar consultoria'):'Adicionar nova consultoria'} <span aria-hidden="true">＋</span></button>`:'<a class="feature-btn" href="../prototipo-dashboard/index.html">Cadastrar empresa no dashboard ↗</a>'}</section>
 <div class="section-head"><h3>Consultorias desta demonstração</h3><span>${visits.length}</span></div>
 ${visits.length ? visits.map((visit,index)=>`<button class="visit" data-action="open-visit" data-index="${index}"><div class="visit-top"><span class="company-icon">${icon('rooms')}</span><span class="badge ${visit.status==='sent'?'':'amber'}">${visit.status==='sent'?'Enviada ao dashboard':'Em levantamento'}</span></div><h3>${companyName()}</h3><p>Unidade ${escapeHtml(state.company.unit)} · Visita ${index+1}</p><div class="visit-bottom"><span>${visit.rooms.length} ambientes · ${visit.rooms.reduce((sum,room)=>sum+total(room),0)} equipamentos</span>${icon('arrow')}</div></button>`).join('') : empty('Nenhuma consultoria ainda','Confirme o cadastro no dashboard e inicie a visita por aqui.')}
 <p class="notice">Dados fictícios. A simulação não altera os documentos ou a planilha.</p>`;
}
function company() {
 return `${back('home','Consultorias')}${intro('NOVA CONSULTORIA','Qual empresa<br>vamos visitar?','Selecione uma empresa já cadastrada.')}
 <p class="section-label">Empresa cadastrada no dashboard</p><button class="visit company-choice" data-action="company-details"><div class="company-icon">${icon('rooms')}</div><div><h3>${companyName()}</h3><p>Unidade ${escapeHtml(state.company.unit)} · Cadastro de exemplo</p><span class="badge">Empresa cadastrada</span></div>${icon('arrow')}</button><p class="notice">O cadastro veio do dashboard. A próxima etapa é iniciar a consultoria.</p>`;
}
function companyDetails() {
 return `${back('new-visit','Selecionar empresa')}${intro('EMPRESA SELECIONADA',companyName(),'Confira o local antes de entrar na consultoria.')}
 <div class="company-identity">${icon('rooms')}<span class="badge">Cadastro disponível</span></div><div class="detail">${[['Unidade','unit'],['Local de exemplo','address'],['Contato fictício','contact'],['Consultora','consultant']].map(([label,key])=>`<div class="data-row"><span>${label}</span><strong>${escapeHtml(state.company[key])}</strong></div>`).join('')}</div>
 <div class="next-step"><strong>Uma consultoria em branco</strong><p>Você vai adicionar os ambientes e, dentro deles, os equipamentos encontrados.</p></div>${primary('create-visit','Entrar e iniciar consultoria')}`;
}
function rooms() {
 return `${back('home','Consultorias')}${crumb('Ambientes')}${intro('CONSULTORIA EM ANDAMENTO',companyName(),`Unidade ${escapeHtml(state.company.unit)} · Levantamento em campo`)}
 <div class="section-head"><h3>Ambientes</h3><span>${activeVisit.rooms.length} cadastrados</span></div>
 ${activeVisit.rooms.length ? activeVisit.rooms.map((room,index)=>`<button class="room room-button" data-action="open-room" data-index="${index}"><span class="company-icon">${icon('rooms')}</span><div><h3>${escapeHtml(room.name)}</h3><p>${total(room)} equipamentos · ${room.items.length} tipos</p></div>${icon('arrow')}</button>`).join('') : empty('Nenhum ambiente cadastrado','Comece pela recepção, por exemplo. Depois, adicione os equipamentos que existem lá.')}
 ${primary('new-room','Adicionar ambiente')}${activeVisit.rooms.length?primary('review','Conferir e enviar'):''}<p class="notice">Cada ambiente organiza os equipamentos de um espaço da empresa.</p>`;
}
function roomPicker() {
 return `${back('rooms','Ambientes da consultoria')}${intro('ADICIONAR AMBIENTE','Qual espaço<br>vamos visitar?','Escolha um ambiente para iniciar a coleta.')}
 <div class="section-head"><h3>Ambientes disponíveis</h3><button class="add-room" data-action="room-form" aria-label="Cadastrar novo ambiente" title="Cadastrar novo ambiente">+</button></div>
 ${roomNames.map((name,index)=>`<button class="room room-button" data-action="select-room" data-index="${index}"><span class="company-icon">${icon('rooms')}</span><div><h3>${escapeHtml(name)}</h3><p>Selecionar e entrar no ambiente</p></div>${icon('arrow')}</button>`).join('')}
 <p class="notice">Não encontrou o espaço? Use o botão <strong>+</strong> para cadastrar um novo ambiente.</p>`;
}
function newRoom() {
 return `${back('new-room','Selecionar ambiente')}${crumb('Novo ambiente')}${intro('CADASTRAR AMBIENTE','Qual é o nome<br>deste espaço?','Cadastre um espaço que ainda não está na lista.')}
 <form id="room-form"><label class="field-label" for="room-name">Nome do ambiente</label><input id="room-name" name="name" value="Depósito" placeholder="Ex.: Depósito" maxlength="60" required>
 <div class="next-step"><strong>Um espaço de cada vez</strong><p>Ao cadastrar o ambiente, você já entra nele para registrar os equipamentos.</p></div><button class="primary" type="submit">Cadastrar ambiente e entrar ${icon('arrow')}</button></form><p class="notice">Exemplo preenchido. Os dados ficam salvos neste navegador.</p>`;
}
function equipment() {
 return `${back('rooms','Todos os ambientes')}${crumb(activeRoom.name)}${intro('DENTRO DO AMBIENTE',escapeHtml(activeRoom.name)+'.','Registre os equipamentos que encontrar aqui.')}
 <div class="room-totals"><span>${icon('rooms')} Ambiente em levantamento</span><strong>${total(activeRoom)} <small>equipamentos</small></strong></div>
 <div class="section-head"><h3>Equipamentos</h3><span>${activeRoom.items.length} tipos</span></div>
 ${activeRoom.items.length ? activeRoom.items.map(item=>`<article class="equipment-card"><div class="equipment-head"><span class="supply-icon">${icon(catalog[item.catalog].icon)}</span><div><h3>${escapeHtml(catalog[item.catalog].name)}</h3><p>${escapeHtml(catalog[item.catalog].detail)}</p></div></div><div class="equipment-bottom"><span>Quantidade encontrada</span><div class="stepper"><button data-action="decrease" data-index="${item.catalog}" ${item.quantity===1?'disabled':''} aria-label="Diminuir quantidade de ${escapeHtml(catalog[item.catalog].name)} ${escapeHtml(catalog[item.catalog].detail)}">−</button><output aria-label="Quantidade de ${escapeHtml(catalog[item.catalog].name)} ${escapeHtml(catalog[item.catalog].detail)}" aria-live="polite">${item.quantity}</output><button data-action="increase" data-index="${item.catalog}" aria-label="Aumentar quantidade de ${escapeHtml(catalog[item.catalog].name)} ${escapeHtml(catalog[item.catalog].detail)}">+</button></div></div><button class="remove-item" data-action="remove-item" data-index="${item.catalog}">Remover este item</button></article>`).join('') : empty('O que tem neste ambiente?','Cadastre o primeiro equipamento. Encontrou outro igual? Basta aumentar a quantidade no botão +.')}
 ${primary('new-equipment',activeRoom.items.length ? 'Adicionar novo item' : 'Cadastrar equipamento')}
 ${activeRoom.items.length ? '<p class="notice">Outro igual: use o <strong>+</strong> na quantidade.<br>Modelo diferente: toque em <strong>Adicionar novo item</strong>.</p>' : ''}`;
}
function catalogScreen() {
 return `${back('equipment','Voltar ao ambiente')}${crumb(activeRoom.name)}${intro('ADICIONAR EQUIPAMENTO','O que você<br>encontrou?','Escolha um modelo ou cadastre outro equipamento.')}
 ${primary('equipment-form','Cadastrar novo equipamento')}
 <p class="section-label">Modelos de exemplo</p>${catalog.map((item,index)=>`<button class="supply" data-action="add-item" data-index="${index}"><span class="supply-icon">${icon(item.icon)}</span><div><h3>${escapeHtml(item.name)}</h3><p>${escapeHtml(item.detail)}</p></div><span class="add-sign" aria-hidden="true">+</span></button>`).join('')}
 <p class="notice">Catálogo fictício para apresentação. Ao selecionar, uma unidade será adicionada e você voltará ao ambiente.</p>`;
}
function equipmentForm() {
 return `${back('new-equipment','Voltar aos modelos')}${crumb(activeRoom.name)}${intro('NOVO EQUIPAMENTO','Um cadastro<br>bem simples.','Quatro informações para identificar o equipamento.')}
 <form id="equipment-form"><label class="field-label" for="equipment-name">Nome do equipamento</label><input id="equipment-name" name="name" value="Lixeira da recepção" placeholder="Ex.: Lixeira da recepção" maxlength="80" required>
 <label class="field-label" for="equipment-type">Tipo</label><select id="equipment-type" name="type" required><option value="box" selected>Lixeira</option><option value="drop">Dispensador de sabonete</option><option value="paper">Dispensador de papel</option><option value="rooms">Outro</option></select>
 <label class="field-label" for="equipment-capacity">Capacidade</label><input id="equipment-capacity" name="capacity" value="20 litros" placeholder="Ex.: 20 litros ou 800 ml" maxlength="40" required>
 <label class="field-label" for="equipment-material">Material</label><select id="equipment-material" name="material" required><option selected>Plástico</option><option>Inox</option><option>Metal</option><option>Outro</option></select>
 <button class="primary" type="submit">Cadastrar e adicionar ao ambiente ${icon('arrow')}</button></form><p class="notice">Será adicionada uma unidade. Depois, use + para registrar outras iguais. Campos ilustrativos, sujeitos à validação.</p>`;
}
function review() {
 const count=Demo.summarize(state);
 return `${back(sent()?'home':'rooms',sent()?'Consultorias':'Voltar aos ambientes')}${intro(sent()?'ENVIO CONCLUÍDO':'REVISAR LEVANTAMENTO',sent()?'Pronto para<br>a análise.':'Tudo certo<br>para enviar?',sent()?'O levantamento está disponível no dashboard.':'Confira os dados antes de continuar no dashboard.')}
 <div class="next-step"><strong>${companyName()}</strong><p>Unidade ${escapeHtml(state.company.unit)} · ${escapeHtml(state.company.consultant)}</p></div>
 <div class="section-head"><h3>Resumo da visita</h3><span>${count.equipment} equipamentos</span></div>
 ${activeVisit.rooms.map(room=>`<section class="review-room"><h3>${escapeHtml(room.name)}</h3>${room.items.length?room.items.map(item=>`<p><span>${escapeHtml(catalog[item.catalog].name)}<small>${escapeHtml(catalog[item.catalog].detail)}</small></span><b>${item.quantity} un.</b></p>`).join(''):'<p class="muted">Adicione um equipamento neste ambiente para enviar.</p>'}</section>`).join('')}
 ${sent()?`<div class="next-step"><strong>Levantamento enviado ao dashboard</strong><p>Os dados estão vinculados à empresa. Continue por lá para conferir os parâmetros e apresentar a projeção de economia.</p></div>${dashboardLink('Ver análise no dashboard')}`:`${primary('send','Enviar ao dashboard')}<p class="notice">Envio simulado neste navegador. Consumo e preços já estarão preenchidos no dashboard para apresentação.</p>`}`;
}
function showError(error){
 let alert=content.querySelector('[role="alert"]');
 if(!alert){alert=document.createElement('p');alert.className='mobile-error';alert.setAttribute('role','alert');content.prepend(alert);}
 alert.textContent=error.message;alert.scrollIntoView({block:'nearest'});
}
function load(){
 store=Demo.createStore(window.localStorage);state=store.read();catalog=state.catalog;roomNames=state.roomNames;visits=state.visit?[state.visit]:[];activeVisit=state.visit;activeRoom=null;
}
function render(next, focus = true) {
 if(!state.company && next!=='home') next='home';
 if(sent() && !['home','company','company-details'].includes(next)) next='review';
 const screens = {home,company,'company-details':companyDetails,rooms,'new-room':roomPicker,'room-form':newRoom,equipment,catalog:catalogScreen,'equipment-form':equipmentForm,review};
 content.innerHTML = screens[next](); content.scrollTop=0;
 if(focus) content.focus({preventScroll:true});
}
document.addEventListener('click',event=>{
 const button=event.target.closest('button[data-action]'); if(!button) return;
 const action=button.dataset.action, index=Number(button.dataset.index);
 try {
  if(action==='home') render('home');
  else if(action==='new-visit') render('company');
  else if(action==='company-details') render('company-details');
  else if(action==='create-visit'){store.createVisit();load();render('rooms');}
  else if(action==='open-visit'){activeVisit=visits[index];render('rooms');}
  else if(action==='rooms') render('rooms');
  else if(action==='review') render('review');
  else if(action==='send'){store.sendVisit();load();render('review');}
  else if(sent()) render('review');
  else if(action==='new-room') render('new-room');
  else if(action==='room-form') render('room-form');
  else if(action==='select-room'){
   activeRoom=activeVisit.rooms.find(room=>room.name===roomNames[index]);
   if(!activeRoom){activeRoom={name:roomNames[index],items:[]};activeVisit.rooms.push(activeRoom);}
   store.save(state);render('equipment');
  }
  else if(action==='open-room'){activeRoom=activeVisit.rooms[index];render('equipment');}
  else if(action==='equipment') render('equipment');
  else if(action==='new-equipment') render('catalog');
  else if(action==='equipment-form') render('equipment-form');
  else if(action==='add-item'){
   const existing=activeRoom.items.find(item=>item.catalog===index);
   if(existing)existing.quantity++;else activeRoom.items.push({catalog:index,quantity:1});
   store.save(state);render('equipment');
  }
  else if(['increase','decrease','remove-item'].includes(action)){
   const item=activeRoom.items.find(item=>item.catalog===index);
   if(action==='remove-item')activeRoom.items=activeRoom.items.filter(entry=>entry!==item);
   else item.quantity=Math.max(1,item.quantity+(action==='increase'?1:-1));
   store.save(state);
   const scroll=content.scrollTop;render('equipment',false);content.scrollTop=scroll;
   const restored=content.querySelector(`button[data-action="${action}"][data-index="${index}"]`);
   (restored&&!restored.disabled?restored:content).focus({preventScroll:true});
  }
 } catch(error){try {load();render('home');}catch{}showError(error);}
});
document.addEventListener('input',event=>{
 if(event.target.matches('input')) event.target.setCustomValidity('');
});
document.addEventListener('submit',event=>{
 const form=event.target;
 if(!['room-form','equipment-form'].includes(form.id)) return;
 event.preventDefault();
 for(const input of form.querySelectorAll('input[required]')) {
  if(!input.value.trim()){input.setCustomValidity('Preencha este campo.'); input.reportValidity(); return;}
 }
 if(!form.reportValidity()) return;
 const fields=new FormData(form);
 try {
  if(sent()){render('review');return;}
  if(form.id==='room-form') {
   const name=fields.get('name').trim();
   activeRoom=activeVisit.rooms.find(room=>room.name===name);
   if(!activeRoom){activeRoom={name,items:[]};activeVisit.rooms.push(activeRoom);}
   if(!roomNames.includes(name))roomNames.push(name);
  } else {
   const index=catalog.length;
   catalog.push({name:fields.get('name').trim(),detail:fields.get('capacity').trim()+' · '+fields.get('material'),icon:fields.get('type')});
   activeRoom.items.push({catalog:index,quantity:1});
  }
  store.save(state);render('equipment');
 }catch(error){try {load();render('home');}catch{}showError(error);}
});
function refresh(){
 try {load();render('home',false);}catch(error){content.innerHTML='<h2>A demonstração precisa de atenção.</h2><p class="notice">Abra o dashboard para reiniciar a demonstração ou verifique o armazenamento local.</p>'+dashboardLink();showError(error);}
}
window.addEventListener('storage',event=>{if(event.key===Demo.key||event.key===null)refresh();});
refresh();
