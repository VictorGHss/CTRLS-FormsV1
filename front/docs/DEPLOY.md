# 🚀 Guia de Deploy - CTRLS-Forms Frontend

Guia completo para deploy do frontend no Google Cloud Run.

---

## 📋 Pré-Requisitos

### Ferramentas Necessárias

✅ **Docker Desktop**
```powershell
# Verificar instalação
docker --version
docker ps
```

✅ **Google Cloud SDK**
```powershell
# Verificar instalação
gcloud --version

# Autenticar
gcloud auth login

# Configurar projeto
gcloud config set project ctrls-forms-api
```

✅ **Backend API Deployado**
- Você precisará da URL do backend
- Exemplo: `https://ctrls-forms-api-xyz.a.run.app`

---

## 🚀 Deploy Rápido

### Comando Único

```powershell
.\deploy-frontend.ps1 -BackendUrl "https://ctrls-forms-api-xyz.a.run.app"
```

### Modo Interativo

```powershell
.\deploy-frontend.ps1
```

O script irá solicitar a URL do backend durante execução.

---

## 🏗️ Arquitetura

### Build Multi-Stage (Dockerfile)

```
┌─────────────────────────────────┐
│ Stage 1: Build (Node.js 20)    │
│ - npm ci (instala deps)         │
│ - VITE_API_BASE_URL injetado    │
│ - npm run build → dist/         │
└─────────────────────────────────┘
              ↓
┌─────────────────────────────────┐
│ Stage 2: Serve (Nginx Alpine)  │
│ - Copia dist/ para nginx        │
│ - Porta 8080 (Cloud Run)        │
│ - SPA routing configurado       │
└─────────────────────────────────┘
```

### Nginx Configuration

**Características**:
- ✅ Porta **8080** (obrigatório para Cloud Run)
- ✅ SPA Routing: `try_files $uri /index.html`
- ✅ Gzip compression
- ✅ Cache otimizado (1 ano para assets, sem cache para index.html)
- ✅ Health check em `/health`

---

## 🌍 Variáveis de Ambiente

### ⚠️ Importante: Build Time vs Runtime

Vite exige variáveis em **BUILD TIME**, não runtime.

### Desenvolvimento Local

```bash
# 1. Copiar template
cp .env.example .env.local

# 2. Editar se necessário (padrão: http://localhost:8080)
# VITE_API_BASE_URL=http://localhost:8080

# 3. Rodar dev server
npm run dev
```

### Produção (Docker)

A variável é injetada via `--build-arg`:

```powershell
docker build --build-arg VITE_API_BASE_URL=https://api-url.a.run.app ...
```

O script `deploy-frontend.ps1` faz isso automaticamente! ✅

---

## 🧪 Teste Local (Recomendado)

Antes de fazer deploy, teste o container localmente:

```powershell
# 1. Build
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080 -t ctrls-web:test .

# 2. Run
docker run -p 8080:8080 --name web-test ctrls-web:test

# 3. Acessar
# http://localhost:8080

# 4. Testes
# - Página inicial carrega
# - Rotas funcionam (/login, /dashboard)
# - F5 não dá 404
# - Health check: http://localhost:8080/health

# 5. Limpar
docker stop web-test
docker rm web-test
```

---

## ⚙️ Configuração Cloud Run

O script configura automaticamente:

| Propriedade | Valor | Descrição |
|-------------|-------|-----------|
| **Serviço** | `ctrls-forms-web` | Nome do serviço |
| **Região** | `us-central1` | Região GCP |
| **Porta** | `8080` | Porta do container |
| **Memória** | `256Mi` | RAM alocada |
| **CPU** | `1 vCPU` | CPU alocada |
| **Min Instances** | `0` | Escala para zero quando sem uso |
| **Max Instances** | `10` | Limite de auto-scaling |
| **Timeout** | `60s` | Timeout de requisição |
| **Acesso** | Público | Sem autenticação |

---

## 🔍 Comandos Úteis Pós-Deploy

### Ver Logs em Tempo Real

```powershell
gcloud run services logs tail ctrls-forms-web --region=us-central1
```

### Obter URL Pública

```powershell
gcloud run services describe ctrls-forms-web --region=us-central1 --format="value(status.url)"
```

### Ver Status do Serviço

```powershell
gcloud run services describe ctrls-forms-web --region=us-central1
```

### Listar Revisões

```powershell
gcloud run revisions list --service=ctrls-forms-web --region=us-central1
```

### Fazer Rollback

```powershell
# 1. Listar revisões
gcloud run revisions list --service=ctrls-forms-web --region=us-central1

# 2. Voltar para revisão específica
gcloud run services update-traffic ctrls-forms-web \
  --to-revisions=NOME_DA_REVISAO=100 \
  --region=us-central1
```

### Deletar Serviço

```powershell
gcloud run services delete ctrls-forms-web --region=us-central1
```

---

## 🐛 Troubleshooting

### Problema: Docker não está rodando

**Erro**: `Cannot connect to Docker daemon`

**Solução**:
1. Abrir Docker Desktop
2. Aguardar carregar completamente
3. Testar: `docker ps`

### Problema: Porta 8080 em uso

**Solução 1**: Usar outra porta no host
```powershell
docker run -p 9090:8080 ...
# Acesse: http://localhost:9090
```

**Solução 2**: Parar processo
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Problema: 404 nas rotas do React

**Causa**: `nginx.conf` não configurado corretamente

**Verificar**: Deve ter `try_files $uri /index.html;`

### Problema: CORS Error

**Causa**: Backend não aceita requisições do frontend

**Solução**: Configurar CORS no backend
```java
@CrossOrigin(origins = {"https://ctrls-forms-web-xyz.a.run.app"})
```

### Problema: Alterações não aparecem

**Causa**: Imagem Docker não foi rebuilded

**Solução**:
```powershell
# 1. Parar container
docker stop web-test; docker rm web-test

# 2. Rebuild
docker build --build-arg VITE_API_BASE_URL=... -t ctrls-web:test .

# 3. Rodar novamente
docker run -p 8080:8080 ctrls-web:test
```

---

## 📊 Estimativa de Custos

### Cloud Run - Tier Gratuito (Mensal)

- **2 milhões** de requisições
- **360.000** vCPU-segundos
- **180.000** GiB-segundos

### Tráfego Baixo/Médio: ~R$ 0,00/mês! 🎉

Com `min-instances=0`, o serviço escala para zero quando não há uso.

---

## 🔄 Workflow de Atualização

```powershell
# 1. Fazer alterações no código
# (edit files...)

# 2. Testar localmente
npm run dev

# 3. (Opcional) Build local
npm run build

# 4. (Opcional) Teste Docker local
docker build ... && docker run ...

# 5. Deploy
.\deploy-frontend.ps1 -BackendUrl "https://api-url.a.run.app"
```

Cloud Run faz deploy gradual automaticamente (zero downtime)! ✅

---

## 🎯 Próximos Passos

### 1. Custom Domain

```powershell
gcloud run domain-mappings create \
  --service=ctrls-forms-web \
  --domain=app.seudominio.com.br \
  --region=us-central1
```

### 2. CI/CD

Configurar deploy automático:
- GitHub Actions
- Cloud Build
- GitLab CI/CD

Exemplo GitHub Actions:

```yaml
name: Deploy Frontend

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Cloud SDK
        uses: google-github-actions/setup-gcloud@v1
        
      - name: Build and Deploy
        run: |
          gcloud builds submit \
            --tag us-central1-docker.pkg.dev/ctrls-forms-api/ctrls-repo/web:${{ github.sha }}
          
          gcloud run deploy ctrls-forms-web \
            --image us-central1-docker.pkg.dev/ctrls-forms-api/ctrls-repo/web:${{ github.sha }} \
            --region us-central1
```

### 3. Monitoramento

Configurar alertas no Cloud Monitoring:
- Uptime checks
- Error rate alerts
- Latency monitoring

### 4. CDN

Adicionar Cloud CDN para melhor performance global:

```powershell
gcloud compute backend-services add-backend \
  --global \
  --backend-service=ctrls-web-backend \
  --serverless-backend-service=ctrls-forms-web \
  --serverless-backend-service-region=us-central1
```

---

## 📞 Links Importantes

- **Console GCP**: https://console.cloud.google.com/run
- **Logs**: https://console.cloud.google.com/logs/query
- **Artifacts**: https://console.cloud.google.com/artifacts
- **Cloud Run Docs**: https://cloud.google.com/run/docs
- **Vite Env Docs**: https://vitejs.dev/guide/env-and-mode.html

---

## ✅ Checklist de Validação

Antes de fazer deploy em produção:

- [ ] Build funciona sem erros
- [ ] Container inicia corretamente (teste local)
- [ ] Página inicial carrega
- [ ] Rotas SPA funcionam
- [ ] F5 não dá 404
- [ ] Health check retorna OK
- [ ] Assets carregam (JS, CSS)
- [ ] Backend responde (se disponível)
- [ ] Sem erros no console do navegador
- [ ] Logs do Nginx sem erros

---

**Desenvolvido para**: CTRLS-Forms  
**Última atualização**: 2025-11-25  
**Status**: ✅ Pronto para Produção

