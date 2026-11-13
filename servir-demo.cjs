// Servidor estático local. Não instala dependências nem publica arquivos na rede.
'use strict';
const http=require('node:http');
const fs=require('node:fs');
const path=require('node:path');
const root=__dirname;
const port=Number(process.env.PORT||8081);
const publicFolders=new Set(['prototipo']);
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8'};
if(!Number.isInteger(port)||port<1024||port>65535)throw Error('PORT deve ser um número entre 1024 e 65535.');
const server=http.createServer((req,res)=>{
 res.setHeader('X-Content-Type-Options','nosniff');
 res.setHeader('Cache-Control','no-store');
 res.setHeader('Content-Security-Policy',"default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'");
 if(!['GET','HEAD'].includes(req.method)){res.writeHead(405,{Allow:'GET, HEAD'});return res.end();}
 let pathname;
 try{pathname=decodeURIComponent(new URL(req.url,'http://127.0.0.1').pathname);}catch{res.writeHead(400);return res.end('Endereço inválido.');}
 if(pathname==='/'){res.writeHead(302,{Location:'/prototipo/dashboard/index.html'});return res.end();}
 const parts=pathname.split('/').filter(Boolean);
 if(!publicFolders.has(parts[0])||parts.some(p=>p==='..'||p.includes('\\')||p.includes('\0'))){res.writeHead(404);return res.end('Página não encontrada.');}
 if(pathname.endsWith('/'))parts.push('index.html');
 const file=path.resolve(root,...parts);
 if(!file.startsWith(root+path.sep)||!types[path.extname(file)]){res.writeHead(404);return res.end('Página não encontrada.');}
 fs.readFile(file,(error,data)=>{
  if(error){res.writeHead(404);return res.end('Página não encontrada.');}
  res.setHeader('Content-Type',types[path.extname(file)]);
  res.end(req.method==='HEAD'?undefined:data);
 });
});
server.on('error',error=>{console.error(error.code==='EADDRINUSE'?`A porta ${port} está ocupada. Use o servidor já aberto ou escolha outra com a variável PORT.`:error.message);process.exitCode=1;});
server.listen(port,'127.0.0.1',()=>{
 console.log(`Dashboard: http://127.0.0.1:${port}/prototipo/dashboard/index.html`);
 console.log(`Mobile:    http://127.0.0.1:${port}/prototipo/mobile/index.html`);
 console.log('Mantenha este terminal aberto. Ctrl+C encerra a demonstração.');
});
