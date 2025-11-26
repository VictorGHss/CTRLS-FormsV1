package br.dev.ctrls.api.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converte strings para o formato criptografado usando o StringEncryptor configurado.
 *
 * IMPORTANTE: Se a descriptografia falhar, retorna o valor original do banco.
 * Isso permite que o sistema funcione mesmo com dados não criptografados ou
 * criptografados com senha diferente (útil durante migração).
 */
@Component
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);
    private static StringEncryptor encryptor;

    @Autowired
    public void setEncryptor(StringEncryptor encryptor) {
        EncryptedStringConverter.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        try {
            return encryptor.encrypt(attribute);
        } catch (Exception e) {
            log.error("Erro ao criptografar valor. Salvando em texto plano. Erro: {}", e.getMessage());
            return attribute; // Fallback: salva em texto plano
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        try {
            return encryptor.decrypt(dbData);
        } catch (EncryptionOperationNotPossibleException e) {
            log.warn("Falha ao descriptografar campo. Possíveis causas: " +
                    "1) Senha Jasypt incorreta, " +
                    "2) Dados não estão criptografados, " +
                    "3) Dados criptografados com senha diferente. " +
                    "Retornando valor original do banco.");
            // FALLBACK: Retorna o valor original (pode estar em texto plano)
            return dbData;
        } catch (Exception e) {
            log.error("Erro inesperado ao descriptografar campo: {}", e.getMessage(), e);
            return dbData; // Fallback: retorna valor original
        }
    }
}


