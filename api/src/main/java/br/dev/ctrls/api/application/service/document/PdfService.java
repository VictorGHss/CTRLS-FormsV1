package br.dev.ctrls.api.application.service.document;

import br.dev.ctrls.api.domain.form.FormTemplate;
import br.dev.ctrls.api.domain.submission.Submission;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * Serviço utilitário para geração de PDFs em memória.
 */
@Slf4j
@Service
public class PdfService {

    public byte[] generateAnamnesisPdf(Submission submission, FormTemplate template) {
        log.debug("Gerando PDF para submissão: {}", submission.getId());

        Document document = null;
        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            document.add(new Paragraph("Anamnese - " + submission.getPatientName(), titleFont));
            document.add(new Paragraph("CPF: " + submission.getPatientCpf()));
            document.add(new Paragraph("Formulário: " + template.getTitle()));
            document.add(new Paragraph(" "));

            // Respostas
            JSONObject json = new JSONObject(submission.getAnswersJson());
            for (String key : json.keySet()) {
                Object value = json.get(key);
                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
                document.add(new Paragraph(key + ": " + value, normalFont));
            }

            log.info("PDF gerado com sucesso para submissão: {}", submission.getId());
            return baos.toByteArray();

        } catch (DocumentException ex) {
            log.error("Erro ao gerar conteúdo do PDF", ex);
            throw new PdfGenerationException("Erro ao gerar PDF", ex);

        } catch (Exception ex) {
            log.error("Erro inesperado ao gerar PDF", ex);
            throw new PdfGenerationException("Erro inesperado na geração do PDF", ex);

        } finally {
            // ✅ CORREÇÃO: Garante que recursos são liberados mesmo com exceção
            if (document != null && document.isOpen()) {
                try {
                    document.close();
                } catch (Exception ex) {
                    log.warn("Erro ao fechar documento PDF", ex);
                }
            }

            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ex) {
                    log.warn("Erro ao fechar ByteArrayOutputStream", ex);
                }
            }
        }
    }
}
