# ==============================================================================
# CTRLS-Forms API - Deploy Final (Production - Brasil)
# ==============================================================================
$ErrorActionPreference = "Stop"

# ============================================
# 1. CONFIGURAÇÃO & SEGREDOS
# ============================================

# Load environment variables from .env file
$envFile = Join-Path $PSScriptRoot ".env"
if (!(Test-Path $envFile)) {
    Write-Host "✗ Arquivo .env nao encontrado!" -ForegroundColor Red
    Write-Host "  Copie .env.example para .env e preencha com seus valores" -ForegroundColor Yellow
    exit 1
}

Write-Host "📋 Carregando variaveis do arquivo .env..." -ForegroundColor Gray
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+?)\s*=\s*(.+?)\s*$') {
        $name = $matches[1]
        $value = $matches[2]
        Set-Variable -Name $name -Value $value -Scope Script
    }
}

# ============================================
# 2. PREPARAÇÃO E APIs
# ============================================

Write-Host "`n🚀 Iniciando Deploy Completo no GCP (Brasil)...`n" -ForegroundColor Cyan

# Validações Básicas
if (!(Get-Command gcloud -ErrorAction SilentlyContinue)) { Write-Host "✗ gcloud não encontrado" -ForegroundColor Red; exit 1 }
if (!(Get-Command docker -ErrorAction SilentlyContinue)) { Write-Host "✗ docker não encontrado" -ForegroundColor Red; exit 1 }

# Configurar Projeto
gcloud config set project $PROJECT_ID

# Habilitar APIs
Write-Host "📋 Habilitando APIs necessárias..." -ForegroundColor Yellow
$APIS = "run.googleapis.com","sqladmin.googleapis.com","redis.googleapis.com","vpcaccess.googleapis.com","servicenetworking.googleapis.com","artifactregistry.googleapis.com","compute.googleapis.com"
foreach ($api in $APIS) { gcloud services enable $api --project=$PROJECT_ID 2>$null }

# ============================================
# 3. REDE & VPC (CRITICAL: Ordem Correta!)
# ============================================
Write-Host "🔗 Configurando Rede VPC..." -ForegroundColor Yellow

# PASSO 1: Private Services Access (OBRIGATORIO fazer ANTES de criar SQL/Redis)
Write-Host "   Verificando VPC Peering..." -ForegroundColor Gray
$rangeExists = gcloud compute addresses list --global --filter="name:google-managed-services-default" --format="value(name)" 2>$null
if (!$rangeExists) {
    Write-Host "   Criando IP range para Private Services..." -ForegroundColor Gray
    gcloud compute addresses create google-managed-services-default `
        --global `
        --purpose=VPC_PEERING `
        --prefix-length=16 `
        --network=default

    Write-Host "   Conectando VPC Peering (aguarde 60s)..." -ForegroundColor Gray
    gcloud services vpc-peerings connect `
        --service=servicenetworking.googleapis.com `
        --ranges=google-managed-services-default `
        --network=default

    Start-Sleep 60
    Write-Host "   Peering configurado!" -ForegroundColor Green
} else {
    Write-Host "   IP range ja existe, verificando conexao..." -ForegroundColor Gray
    # CRITICAL FIX: Forcar update da conexao mesmo se ja existir
    Write-Host "   Atualizando conexao do peering..." -ForegroundColor Gray
    gcloud services vpc-peerings update `
        --service=servicenetworking.googleapis.com `
        --ranges=google-managed-services-default `
        --network=default `
        --force 2>$null

    Start-Sleep 30
    Write-Host "   Peering atualizado!" -ForegroundColor Green
}

# PASSO 2: VPC Connector (para Cloud Run acessar private IPs)
Write-Host "   Verificando VPC Connector..." -ForegroundColor Gray
$connExists = gcloud compute networks vpc-access connectors list --region=$REGION --filter="name:$VPC_CONNECTOR_NAME" --format="value(name)" 2>$null
if (!$connExists) {
    Write-Host "   Criando VPC Connector..." -ForegroundColor Gray
    gcloud compute networks vpc-access connectors create $VPC_CONNECTOR_NAME `
        --region=$REGION `
        --network=default `
        --range=10.8.0.0/28 `
        --min-instances=2 `
        --max-instances=3 `
        --machine-type=e2-micro
    Write-Host "   Connector criado!" -ForegroundColor Green
} else {
    Write-Host "   Connector ja existe" -ForegroundColor Green
}

# ============================================
# 4. BANCO DE DADOS & REDIS
# ============================================
Write-Host "🐘 Configurando Cloud SQL (Postgres)..." -ForegroundColor Yellow
$sqlExists = gcloud sql instances list --filter="name:$DB_INSTANCE_NAME" --format="value(name)" 2>$null
if (!$sqlExists) {
    Write-Host "   Criando instancia SQL (aguarde 3-5 minutos)..."
    # CRITICAL: Usar --network com formato completo projects/{PROJECT}/global/networks/default
    gcloud sql instances create $DB_INSTANCE_NAME `
        --database-version=POSTGRES_15 `
        --tier=db-f1-micro `
        --region=$REGION `
        --network="projects/$PROJECT_ID/global/networks/default" `
        --no-assign-ip `
        --storage-size=10GB `
        --storage-auto-increase

    Write-Host "   Aguardando instancia ficar pronta..."
    Start-Sleep 20

    Write-Host "   Configurando usuarios e banco..."
    gcloud sql users set-password postgres --instance=$DB_INSTANCE_NAME --password=$DB_PASSWORD
    gcloud sql users create $DB_USER --instance=$DB_INSTANCE_NAME --password=$DB_PASSWORD
    gcloud sql databases create $DB_NAME --instance=$DB_INSTANCE_NAME
} else {
    Write-Host "   Instancia SQL ja existe"
}
$SQL_CONN = gcloud sql instances describe $DB_INSTANCE_NAME --format="value(connectionName)"

Write-Host "🔴 Configurando Redis (Memorystore)..." -ForegroundColor Yellow
$redisExists = gcloud redis instances list --region=$REGION --filter="name:$REDIS_INSTANCE_NAME" --format="value(name)" 2>$null
if (!$redisExists) {
    gcloud redis instances create $REDIS_INSTANCE_NAME --size=1 --region=$REGION --tier=basic --redis-version=redis_7_0 --network=default
}
# Pegar IP do Redis
$REDIS_HOST = gcloud redis instances describe $REDIS_INSTANCE_NAME --region=$REGION --format="value(host)"

# ============================================
# 5. BUILD & DEPLOY API
# ============================================
Write-Host "📦 Build & Push Docker Image..." -ForegroundColor Yellow
$IMAGE_PATH = "$REGION-docker.pkg.dev/$PROJECT_ID/$ARTIFACT_REPO/api:latest"

# Criar Repositório no Artifact Registry se não existir
$repoExists = gcloud artifacts repositories list --location=$REGION --filter="name:$ARTIFACT_REPO" --format="value(name)" 2>$null
if (!$repoExists) {
    gcloud artifacts repositories create $ARTIFACT_REPO --repository-format=docker --location=$REGION
}
gcloud auth configure-docker "$REGION-docker.pkg.dev" --quiet

# Build e Push
docker build -t $IMAGE_PATH .
docker push $IMAGE_PATH

Write-Host "☁️  Deploying to Cloud Run..." -ForegroundColor Yellow

# Montar string de conexão JDBC com Socket Factory
$JDBC_URL = "jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${SQL_CONN}&socketFactory=com.google.cloud.sql.postgres.SocketFactory&user=${DB_USER}&password=${DB_PASSWORD}"

# Deploy com TODAS as variaveis (FIX: usar formato key=value unico)
# CRITICAL FIX: --set-env-vars aceita apenas UM argumento com todas as vars separadas por virgula
# CORS: Incluir AMBAS as URLs (localhost para dev + Cloud Run para prod)
$CORS_ORIGINS_FULL = "https://ctrls-forms-web-vnbavqye5a-rj.a.run.app,http://localhost:3000,https://*.run.app"
$ENV_VARS = "SPRING_PROFILES_ACTIVE=prod,SPRING_DATASOURCE_URL=$JDBC_URL,SPRING_DATASOURCE_USERNAME=$DB_USER,SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD,SPRING_DATA_REDIS_HOST=$REDIS_HOST,SPRING_DATA_REDIS_PORT=6379,JWT_SECRET=$JWT_SECRET,JWT_EXPIRATION=$JWT_EXPIRATION,CORS_ALLOWED_ORIGINS=$CORS_ORIGINS_FULL,SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=$GOOGLE_CLIENT_ID,SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=$GOOGLE_CLIENT_SECRET,CLOUDINARY_URL=$CLOUDINARY_URL,BREVO_API_KEY=$BREVO_API_KEY,JASYPT_ENCRYPTOR_PASSWORD=$JASYPT_PASSWORD,FEEGOW_BASE_URL=$FEEGOW_BASE_URL"

gcloud run deploy $SERVICE_NAME `
    --image=$IMAGE_PATH `
    --region=$REGION `
    --allow-unauthenticated `
    --port=8080 `
    --memory=1024Mi `
    --cpu=1 `
    --timeout=300 `
    --vpc-connector=$VPC_CONNECTOR_NAME `
    --vpc-egress=private-ranges-only `
    --add-cloudsql-instances=$SQL_CONN `
    --set-env-vars=$ENV_VARS

# Obter URL Final
$FINAL_URL = gcloud run services describe $SERVICE_NAME --region=$REGION --format="value(status.url)"

Write-Host "`n✅ DEPLOY FINALIZADO COM SUCESSO!" -ForegroundColor Green
Write-Host "🌐 API URL: $FINAL_URL" -ForegroundColor Cyan
Write-Host "🏥 Health: $FINAL_URL/actuator/health" -ForegroundColor Cyan
Write-Host "📚 Swagger Docs: $FINAL_URL/swagger-ui.html" -ForegroundColor Cyan
Write-Host "`n⚠️ IMPORTANTE: Adicione esta URL ($FINAL_URL/login/oauth2/code/google) no Google Cloud Console > Credentials > Redirect URIs" -ForegroundColor Yellow