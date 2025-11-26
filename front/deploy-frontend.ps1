# ============================================================
# SCRIPT DE DEPLOY - FRONTEND CTRLS-FORMS (GCP Cloud Run)
# ============================================================
# Este script realiza o build, push e deploy do frontend React
# no Google Cloud Run utilizando Container Registry.
# ============================================================

param(
    [Parameter(Mandatory=$false)]
    [string]$BackendUrl = ""
)

# Configurações do projeto GCP
$PROJECT_ID = "ctrls-forms-api"
$REGION = "southamerica-east1"
$REPOSITORY = "ctrls-repo"
$SERVICE_NAME = "ctrls-forms-web"
$IMAGE_NAME = "web"
$IMAGE_TAG = "latest"

# Cores para output
function Write-Success { param($msg) Write-Host "✓ $msg" -ForegroundColor Green }
function Write-Info { param($msg) Write-Host "→ $msg" -ForegroundColor Cyan }
function Write-Error { param($msg) Write-Host "✗ $msg" -ForegroundColor Red }
function Write-Warning { param($msg) Write-Host "⚠ $msg" -ForegroundColor Yellow }

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  DEPLOY FRONTEND - CTRLS-FORMS WEB    " -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# ============================================================
# 1. VALIDAÇÃO DE PRÉ-REQUISITOS
# ============================================================
Write-Info "Verificando pré-requisitos..."

# Verifica se gcloud está instalado
if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    Write-Error "Google Cloud SDK (gcloud) não encontrado. Instale em: https://cloud.google.com/sdk/docs/install"
    exit 1
}

# Verifica se Docker está instalado e rodando
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker não encontrado. Instale em: https://www.docker.com/products/docker-desktop"
    exit 1
}

try {
    docker ps | Out-Null
} catch {
    Write-Error "Docker não está rodando. Inicie o Docker Desktop e tente novamente."
    exit 1
}

Write-Success "Pré-requisitos verificados."

# ============================================================
# 2. SOLICITAR URL DO BACKEND (SE NÃO FORNECIDA)
# ============================================================
if ([string]::IsNullOrWhiteSpace($BackendUrl)) {
    Write-Host ""
    Write-Warning "A URL do Backend API é necessária para configurar o Frontend."
    Write-Info "Exemplo: https://ctrls-forms-api-123456.a.run.app"
    Write-Host ""
    $BackendUrl = Read-Host "Digite a URL completa do Backend API"

    if ([string]::IsNullOrWhiteSpace($BackendUrl)) {
        Write-Error "URL do Backend é obrigatória. Abortando deploy."
        exit 1
    }
}

# Remove trailing slash se existir
$BackendUrl = $BackendUrl.TrimEnd('/')

Write-Success "URL do Backend configurada: $BackendUrl"

# ============================================================
# 3. CONFIGURAR PROJETO GCP
# ============================================================
Write-Host ""
Write-Info "Configurando projeto GCP: $PROJECT_ID"

gcloud config set project $PROJECT_ID
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha ao configurar projeto GCP. Verifique se você tem acesso ao projeto."
    exit 1
}

Write-Success "Projeto configurado."

# ============================================================
# 4. AUTENTICAR DOCKER COM ARTIFACT REGISTRY
# ============================================================
Write-Host ""
Write-Info "Autenticando Docker com Artifact Registry..."

gcloud auth configure-docker "$REGION-docker.pkg.dev" --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha na autenticação do Docker."
    exit 1
}

Write-Success "Docker autenticado."

# ============================================================
# 5. BUILD DA IMAGEM DOCKER
# ============================================================
Write-Host ""
Write-Info "Iniciando build da imagem Docker..."
Write-Info "VITE_API_BASE_URL será configurado para: $BackendUrl"

$FULL_IMAGE_NAME = "$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/$IMAGE_NAME`:$IMAGE_TAG"

Write-Host ""
Write-Info "Executando: docker build --build-arg VITE_API_BASE_URL=$BackendUrl -t $FULL_IMAGE_NAME ."

docker build `
    --build-arg VITE_API_BASE_URL=$BackendUrl `
    -t $FULL_IMAGE_NAME `
    .

if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha no build da imagem Docker."
    exit 1
}

Write-Success "Imagem Docker criada com sucesso."

# ============================================================
# 6. PUSH DA IMAGEM PARA ARTIFACT REGISTRY
# ============================================================
Write-Host ""
Write-Info "Enviando imagem para Artifact Registry..."
Write-Info "Destino: $FULL_IMAGE_NAME"

docker push $FULL_IMAGE_NAME

if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha ao enviar imagem para Artifact Registry."
    exit 1
}

Write-Success "Imagem enviada com sucesso."

# ============================================================
# 7. DEPLOY NO CLOUD RUN
# ============================================================
Write-Host ""
Write-Info "Realizando deploy no Cloud Run..."
Write-Info "Serviço: $SERVICE_NAME"
Write-Info "Região: $REGION"

gcloud run deploy $SERVICE_NAME `
    --image=$FULL_IMAGE_NAME `
    --platform=managed `
    --region=$REGION `
    --allow-unauthenticated `
    --port=8080 `
    --memory=256Mi `
    --cpu=1 `
    --min-instances=0 `
    --max-instances=10 `
    --timeout=60s

if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha no deploy do Cloud Run."
    exit 1
}

Write-Success "Deploy realizado com sucesso!"

# ============================================================
# 8. OBTER URL DO SERVIÇO
# ============================================================
Write-Host ""
Write-Info "Obtendo URL do serviço..."

$SERVICE_URL = gcloud run services describe $SERVICE_NAME `
    --platform=managed `
    --region=$REGION `
    --format="value(status.url)"

if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($SERVICE_URL)) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Success "DEPLOY CONCLUÍDO COM SUCESSO!"
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "URL do Frontend: " -NoNewline
    Write-Host $SERVICE_URL -ForegroundColor Yellow
    Write-Host ""
    Write-Info "Você pode acessar a aplicação através da URL acima."
    Write-Host ""
} else {
    Write-Warning "Deploy concluído, mas não foi possível obter a URL do serviço."
    Write-Info "Verifique manualmente em: https://console.cloud.google.com/run"
}

# ============================================================
# 9. INFORMAÇÕES ADICIONAIS
# ============================================================
Write-Host ""
Write-Info "Comandos úteis:"
Write-Host "  - Ver logs:        " -NoNewline; Write-Host "gcloud run services logs tail $SERVICE_NAME --region=$REGION" -ForegroundColor White
Write-Host "  - Ver detalhes:    " -NoNewline; Write-Host "gcloud run services describe $SERVICE_NAME --region=$REGION" -ForegroundColor White
Write-Host "  - Deletar serviço: " -NoNewline; Write-Host "gcloud run services delete $SERVICE_NAME --region=$REGION" -ForegroundColor White
Write-Host ""

# ============================================================
# FIM DO SCRIPT
# ============================================================

