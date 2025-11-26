# 📚 Documentação - CTRLS-Forms Frontend

Documentação técnica organizada do projeto.

---

## 📖 Arquivos Disponíveis

### [`DEPLOY.md`](DEPLOY.md)
Guia completo de deployment no Google Cloud Run.

**Conteúdo**:
- Pré-requisitos e configuração
- Processo de deploy passo a passo
- Arquitetura Docker + Nginx
- Variáveis de ambiente
- Testes locais
- Troubleshooting
- Custos estimados
- CI/CD e próximos passos

**Quando usar**: Primeira vez fazendo deploy ou quando precisar de detalhes técnicos completos.

---

### [`COMMANDS.md`](COMMANDS.md)
Referência rápida de comandos úteis.

**Conteúdo**:
- Comandos de desenvolvimento local
- Docker: build, run, debug
- Google Cloud: autenticação, deploy, logs
- Cloud Run: gerenciamento de serviços e revisões
- Troubleshooting e debug
- Aliases úteis

**Quando usar**: Referência rápida durante o dia a dia de desenvolvimento e deploy.

---

## 🎯 Quick Start

### Desenvolvimento Local

```bash
npm install
npm run dev
```

### Deploy

```powershell
.\deploy-frontend.ps1 -BackendUrl "https://sua-api.a.run.app"
```

---

## 📁 Estrutura de Documentação

```
front/
├── README.md              # Overview do projeto (COMECE AQUI)
├── docs/
│   ├── README.md         # Este arquivo
│   ├── DEPLOY.md         # Guia completo de deploy
│   └── COMMANDS.md       # Referência de comandos
├── .env.example          # Template de variáveis de ambiente
└── deploy-frontend.ps1   # Script de deploy automatizado
```

---

## 🔗 Links Úteis

- **README Principal**: [`../README.md`](../README.md)
- **Script de Deploy**: [`../deploy-frontend.ps1`](../deploy-frontend.ps1)
- **Environment Template**: [`../.env.example`](../.env.example)

---

**Última atualização**: 2025-11-25

