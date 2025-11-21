package br.dev.ctrls.api.infrastructure.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converte strings para o formato criptografado usando o StringEncryptor configurado.
 */
@Component
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

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
        return encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        return encryptor.decrypt(dbData);
    }
}

