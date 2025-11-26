/// <reference types="vite/client" />

// Definição de tipos para variáveis de ambiente customizadas do Vite
// Extende a interface ImportMetaEnv padrão do Vite
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

