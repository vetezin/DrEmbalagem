const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const sourcePath=path.join(__dirname,'../compartilhado/mock.js');
function fixture(){
 const bag=new Map([['outra-chave','preservar']]);
 const storage={getItem:k=>bag.get(k)??null,setItem:(k,v)=>bag.set(k,v),removeItem:k=>bag.delete(k)};
 const context={window:{},console};
 if(fs.existsSync(sourcePath)) vm.runInNewContext(fs.readFileSync(sourcePath,'utf8'),context);
 return {api:context.window.Demo,storage,bag};
}
test('cadastro no dashboard fica disponível em uma segunda instância e não duplica a empresa',()=>{
 const {api,storage}=fixture();assert.ok(api,'Base compartilhada ainda não implementada');
 const a=api.createStore(storage),b=api.createStore(storage);
 assert.equal(b.read().company,null);
 a.saveCompany(api.companyExample);a.saveCompany(api.companyExample);
 assert.equal(b.read().company.name,'Cooperativa Horizonte');
 assert.equal(b.read().company.id,'horizonte');
});
test('envio exige coleta, preserva vínculo e é idempotente; recarga preserva catálogo e quantidades',()=>{
 const {api,storage}=fixture();assert.ok(api,'Base compartilhada ainda não implementada');
 const store=api.createStore(storage);
 assert.throws(()=>store.createVisit(),/empresa/i);
 store.saveCompany(api.companyExample);store.createVisit();
 assert.throws(()=>store.sendVisit(),/equipamento/i);
 const state=store.read();state.visit.rooms.push({name:'Recepção',items:[{catalog:0,quantity:3}]});store.save(state);
 store.sendVisit();store.sendVisit();
 const fresh=api.createStore(storage).read();
 assert.equal(fresh.visit.companyId,'horizonte');assert.equal(fresh.visit.status,'sent');
 assert.equal(api.summarize(fresh).equipment,3);assert.equal(api.summarize(fresh).rooms,1);
 assert.equal(store.createVisit().id,fresh.visit.id);assert.equal(store.read().visit.status,'sent');
});
test('cálculos conciliam com Resultado Final da planilha sem arredondar subtotais antes da soma',()=>{
 const {api}=fixture();assert.ok(api,'Base compartilhada ainda não implementada');
 const a=api.analyze();
 assert.ok(Math.abs(a.current-22208.852)<1e-8);
 assert.ok(Math.abs(a.proposed-12184.295)<1e-8);
 assert.equal(a.saving.toFixed(2),'10024.56');
 const physical=a.categories.map(c=>[c.currentUse,c.proposedUse]);
 assert.equal(JSON.stringify(physical),JSON.stringify([[672.36,244.64],[60,20],[287000,200889],[69120,62500]]));
});
test('reiniciar limpa apenas a demonstração e não cria empresa automaticamente',()=>{
 const {api,storage,bag}=fixture();assert.ok(api,'Base compartilhada ainda não implementada');
 const store=api.createStore(storage);store.saveCompany(api.companyExample);store.createVisit();store.reset();
 assert.equal(store.read().company,null);assert.equal(store.read().visit,null);assert.equal(bag.get('outra-chave'),'preservar');
});
test('armazenamento indisponível ou corrompido não vira sucesso silencioso',()=>{
 const {api,storage}=fixture();assert.ok(api,'Base compartilhada ainda não implementada');
 const store=api.createStore(storage);storage.setItem(api.key,'{inválido');
 assert.throws(()=>store.read(),/reinici/i);
 const broken=api.createStore({getItem(){throw Error('blocked');},setItem(){throw Error('blocked');}});
 assert.throws(()=>broken.read(),/armazenamento/i);
});
