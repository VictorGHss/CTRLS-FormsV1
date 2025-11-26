import {clsx, type ClassValue} from "clsx";
import {twMerge} from "tailwind-merge";

// Utilitário para combinar classes do Tailwind
export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

/**
 * Converte cor hexadecimal para formato HSL para uso em variáveis CSS
 */
export function hexToHSL(hex: string): string {
    // Remove a hashtag se existir
    hex = hex.replace(/^#/, '');

    // Analisa os valores hexadecimais
    const r = parseInt(hex.substring(0, 2), 16) / 255;
    const g = parseInt(hex.substring(2, 4), 16) / 255;
    const b = parseInt(hex.substring(4, 6), 16) / 255;

    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h = 0;
    let s = 0;
    const l = (max + min) / 2;

    if (max !== min) {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

        switch (max) {
            case r:
                h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
                break;
            case g:
                h = ((b - r) / d + 2) / 6;
                break;
            case b:
                h = ((r - g) / d + 4) / 6;
                break;
        }
    }

    h = Math.round(h * 360);
    s = Math.round(s * 100);
    const lValue = Math.round(l * 100);

    return `${h} ${s}% ${lValue}%`;
}

/**
 * Formata data ISO para o padrão brasileiro (DD/MM/YYYY) para exibição visual
 */
export function formatDateBR(isoDate: string): string {
    if (!isoDate) return '';
    const date = new Date(isoDate);
    return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
    }).format(date);
}

/**
 * Formata data e hora para o padrão brasileiro
 */
export function formatDateTimeBR(isoDate: string): string {
    if (!isoDate) return '';
    const date = new Date(isoDate);
    return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(date);
}

/**
 * Converte Data ISO (yyyy-MM-dd) vinda do input HTML para formato BR (dd/MM/yyyy)
 * Essencial para enviar ao Backend sem problemas de fuso horário.
 */
export function formatIsoDateToBr(isoDate: string): string {
    if (!isoDate) return '';
    // Divide "2025-11-04" em partes
    const parts = isoDate.split('-');
    if (parts.length !== 3) return isoDate;

    const [year, month, day] = parts;
    // Retorna "04/11/2025"
    return `${day}/${month}/${year}`;
}