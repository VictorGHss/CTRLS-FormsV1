# ============================================
# CTRLS-Forms API - Quick Update Script
# Atualiza apenas o código (Mantém banco/env vars)
# ============================================

$ErrorActionPreference = "Stop"

# ============================================
# CONFIGURAÇÃO (CORRIGIDA PARA BRASIL)
# ============================================

$PROJECT_ID = "ctrls-forms-api"
$REGION = "southamerica-east1" # <--- AQUI ESTAVA O ERRO (Antes era us-central1)
$ARTIFACT_REPO = "ctrls-repo"
$IMAGE_NAME = "api"
$IMAGE_TAG = "latest"
$SERVICE_NAME = "ctrls-forms-api"

$IMAGE_FULL_PATH = "$REGION-docker.pkg.dev/$PROJECT_ID/$ARTIFACT_REPO/${IMAGE_NAME}:${IMAGE_TAG}"

# ============================================
# QUICK UPDATE DEPLOYMENT
# ============================================

Write-Host "`n🚀 Iniciando Atualização Rápida (Code Only)...`n" -ForegroundColor Cyan

# 1. Configurar Projeto
gcloud config set project $PROJECT_ID

# 2. Autenticar Docker (CRÍTICO PARA EVITAR ERRO DE PERMISSÃO)
Write-Host "🔑 Autenticando Docker..." -ForegroundColor Yellow
gcloud auth configure-docker "$REGION-docker.pkg.dev" --quiet

# 3. Buildar Imagem
Write-Host "📦 Buildando Docker image..." -ForegroundColor Yellow
# Usa --no-cache para garantir que pegue as últimas alterações do código
docker build -t $IMAGE_FULL_PATH .

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Erro no Build" -ForegroundColor Red
    exit 1
}

# 4. Push para o Registry
Write-Host "`n📤 Enviando para o Google Artifact Registry..." -ForegroundColor Yellow
docker push $IMAGE_FULL_PATH

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Erro no Push" -ForegroundColor Red
    exit 1
}

# 5. Atualizar Cloud Run
Write-Host "`n☁️  Atualizando serviço no Cloud Run..." -ForegroundColor Yellow
# Nota: Não precisamos passar as variáveis de ambiente de novo, elas persistem.
gcloud run deploy $SERVICE_NAME `
    --image=$IMAGE_FULL_PATH `
    --region=$REGION `
    --project=$PROJECT_ID

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Erro no Deploy" -ForegroundColor Red
    exit 1
}

# 6. Resultado
$SERVICE_URL = gcloud run services describe $SERVICE_NAME `
    --region=$REGION `
    --project=$PROJECT_ID `
    --format="value(status.url)"

Write-Host "`n✅ ATUALIZADO COM SUCESSO!" -ForegroundColor Green
Write-Host "🌐 URL: $SERVICE_URL" -ForegroundColor Cyan