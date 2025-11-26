# 🎯 Comandos Úteis - CTRLS-Forms Frontend

Referência rápida de comandos essenciais.

---

## 💻 Desenvolvimento Local

### Instalação e Execução

```bash
# Instalar dependências
npm install

# Modo desenvolvimento
npm run dev

# Build de produção
npm run build

# Preview do build
npm run preview
```

### Environment Variables

```bash
# Copiar template
cp .env.example .env.local

# Editar variáveis
# VITE_API_BASE_URL=http://localhost:8080
```

---

## 🐳 Docker

### Build Local

```powershell
# Build com backend local
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080 -t ctrls-web:local .

# Build com backend de produção
docker build --build-arg VITE_API_BASE_URL=https://api.exemplo.com -t ctrls-web:prod .
```

### Run Container

```powershell
# Rodar na porta 8080
docker run -p 8080:8080 --name web-test ctrls-web:local

# Rodar em background
docker run -d -p 8080:8080 --name web-test ctrls-web:local

# Rodar em outra porta (ex: 9090)
docker run -p 9090:8080 --name web-test ctrls-web:local
```

### Gerenciar Containers

```powershell
# Listar containers rodando
docker ps

# Listar todos (incluindo parados)
docker ps -a

# Parar container
docker stop web-test

# Remover container
docker rm web-test

# Parar e remover (comando único)
docker stop web-test; docker rm web-test

# Ver logs
docker logs web-test

# Ver logs em tempo real
docker logs -f web-test

# Entrar no container (debug)
docker exec -it web-test sh
```

### Limpar Docker

```powershell
# Remover containers parados
docker container prune

# Remover imagens não usadas
docker image prune

# Remover tudo não usado
docker system prune -a
```

---

## ☁️ Google Cloud Platform

### Autenticação

```powershell
# Login
gcloud auth login

# Configurar projeto
gcloud config set project ctrls-forms-api

# Ver projeto atual
gcloud config get-value project

# Listar projetos
gcloud projects list
```

### Artifact Registry

```powershell
# Autenticar Docker
gcloud auth configure-docker us-central1-docker.pkg.dev

# Listar imagens
gcloud artifacts docker images list us-central1-docker.pkg.dev/ctrls-forms-api/ctrls-repo

# Ver tags de uma imagem
gcloud artifacts docker images list us-central1-docker.pkg.dev/ctrls-forms-api/ctrls-repo/web
```

### Cloud Run - Deploy

```powershell
# Deploy usando script (recomendado)
.\deploy-frontend.ps1 -BackendUrl "https://api-url.a.run.app"

# Deploy manual
gcloud run deploy ctrls-forms-web \
  --image=us-central1-docker.pkg.dev/ctrls-forms-api/ctrls-repo/web:latest \
  --platform=managed \
  --region=us-central1 \
  --allow-unauthenticated \
  --port=8080 \
  --memory=256Mi \
  --cpu=1
```

### Cloud Run - Gerenciar

```powershell
# Listar serviços
gcloud run services list --region=us-central1

# Descrever serviço
gcloud run services describe ctrls-forms-web --region=us-central1

# Obter URL
gcloud run services describe ctrls-forms-web --region=us-central1 --format="value(status.url)"

# Ver logs em tempo real
gcloud run services logs tail ctrls-forms-web --region=us-central1

# Ver logs históricos
gcloud run services logs read ctrls-forms-web --region=us-central1 --limit=100

# Deletar serviço
gcloud run services delete ctrls-forms-web --region=us-central1
```

### Cloud Run - Revisões

```powershell
# Listar revisões
gcloud run revisions list --service=ctrls-forms-web --region=us-central1

# Descrever revisão específica
gcloud run revisions describe NOME_DA_REVISAO --region=us-central1

# Atualizar tráfego (rollback)
gcloud run services update-traffic ctrls-forms-web \
  --to-revisions=NOME_DA_REVISAO=100 \
  --region=us-central1

# Deletar revisão antiga
gcloud run revisions delete NOME_DA_REVISAO --region=us-central1
```

### Cloud Run - Configuração

```powershell
# Atualizar memória
gcloud run services update ctrls-forms-web --memory=512Mi --region=us-central1

# Atualizar CPU
gcloud run services update ctrls-forms-web --cpu=2 --region=us-central1

# Atualizar instâncias mínimas
gcloud run services update ctrls-forms-web --min-instances=1 --region=us-central1

# Atualizar instâncias máximas
gcloud run services update ctrls-forms-web --max-instances=20 --region=us-central1

# Atualizar timeout
gcloud run services update ctrls-forms-web --timeout=120s --region=us-central1
```

---

## 🔍 Debug e Testes

### Testar Health Check

```powershell
# Container local
curl http://localhost:8080/health

# Cloud Run (substitua pela URL real)
curl https://ctrls-forms-web-xyz.a.run.app/health
```

### Testar Rotas

```powershell
# Página inicial
curl http://localhost:8080/

# Rota do React (deve retornar index.html)
curl http://localhost:8080/login
curl http://localhost:8080/admin/dashboard
```

### Inspecionar Nginx

```powershell
# Entrar no container
docker exec -it web-test sh

# Dentro do container:
# Ver arquivos servidos
ls -la /usr/share/nginx/html

# Ver config do Nginx
cat /etc/nginx/nginx.conf

# Testar config
nginx -t

# Ver processos
ps aux

# Sair
exit
```

---

## 🛠️ Troubleshooting

### Ver Processos na Porta 8080

```powershell
# Windows
netstat -ano | findstr :8080

# Matar processo (substitua PID)
taskkill /PID <PID> /F
```

### Limpar Cache do NPM

```bash
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### Rebuild Completo

```powershell
# Limpar tudo
docker stop web-test; docker rm web-test
docker rmi ctrls-web:local

# Rebuild from scratch
docker build --no-cache --build-arg VITE_API_BASE_URL=http://localhost:8080 -t ctrls-web:local .
```

### Ver Tamanho das Imagens

```powershell
docker images | findstr ctrls-web
```

---

## 📊 Monitoramento

### Cloud Run Metrics

```powershell
# Ver métricas no console
start https://console.cloud.google.com/run/detail/us-central1/ctrls-forms-web/metrics

# Ver logs no console
start https://console.cloud.google.com/logs/query
```

### Query de Logs

```
resource.type="cloud_run_revision"
resource.labels.service_name="ctrls-forms-web"
severity>=ERROR
```

---

## 🔗 Links Rápidos

### Console GCP

```powershell
# Cloud Run
start https://console.cloud.google.com/run

# Artifact Registry
start https://console.cloud.google.com/artifacts

# Logs
start https://console.cloud.google.com/logs

# IAM
start https://console.cloud.google.com/iam-admin
```

---

## 📝 Aliases Úteis (PowerShell Profile)

Adicione ao seu `$PROFILE`:

```powershell
# Abrir profile
notepad $PROFILE

# Adicionar aliases:
function ctrls-dev { npm run dev }
function ctrls-build { npm run build }
function ctrls-deploy { param($url) .\deploy-frontend.ps1 -BackendUrl $url }
function ctrls-logs { gcloud run services logs tail ctrls-forms-web --region=us-central1 }
function ctrls-url { gcloud run services describe ctrls-forms-web --region=us-central1 --format="value(status.url)" }
```

Uso:

```powershell
ctrls-dev                                    # npm run dev
ctrls-deploy "https://api.exemplo.com"      # deploy
ctrls-logs                                   # ver logs
ctrls-url                                    # obter URL
```

---

**Desenvolvido para**: CTRLS-Forms  
**Última atualização**: 2025-11-25

