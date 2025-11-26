# ============================================================
# DOCKERFILE - FRONTEND REACT VITE
# ============================================================
# Build Multi-Stage para aplicação React + Vite
# Stage 1: Build da aplicação (Node.js)
# Stage 2: Servir arquivos estáticos (Nginx Alpine)
# ============================================================

# ============================================================
# STAGE 1: BUILD
# ============================================================
FROM node:20-alpine AS builder

# Define o diretório de trabalho
WORKDIR /app

# Aceita a URL do Backend como argumento de build
# CRITICAL: Vite exige variáveis de ambiente em BUILD TIME
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copia os arquivos de dependências primeiro (melhor cache do Docker)
COPY package.json package-lock.json ./

# Instala as dependências
RUN npm ci --silent

# Copia todo o código fonte
COPY . .

# Executa o build de produção
# O Vite irá gerar os arquivos estáticos na pasta dist/
RUN npm run build

# ============================================================
# STAGE 2: SERVE (NGINX)
# ============================================================
FROM nginx:alpine

# Copia os arquivos estáticos do build anterior
COPY --from=builder /app/dist /usr/share/nginx/html

# Copia a configuração customizada do Nginx
# CRITICAL: Configurado para porta 8080 (Cloud Run) e SPA routing
COPY nginx.conf /etc/nginx/nginx.conf

# Expõe a porta 8080 (exigência do Cloud Run)
EXPOSE 8080

# Health check para Cloud Run
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/health || exit 1

# Comando padrão: iniciar o Nginx em modo foreground
CMD ["nginx", "-g", "daemon off;"]

