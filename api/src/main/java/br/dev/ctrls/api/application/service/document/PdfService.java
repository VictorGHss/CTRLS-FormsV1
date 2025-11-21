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
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * Serviço utilitário para geração de PDFs em memória.
 */
@Service
public class PdfService {

    public byte[] generateAnamnesisPdf(Submission submission, FormTemplate template) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            document.add(new Paragraph("Anamnese - " + submission.getPatientName(), titleFont));
            document.add(new Paragraph("CPF: " + submission.getPatientCpf()));
            document.add(new Paragraph("Formulário: " + template.getTitle()));
            document.add(new Paragraph(" "));

            JSONObject json = new JSONObject(submission.getAnswersJson());
            for (String key : json.keySet()) {
                Object value = json.get(key);
                document.add(new Paragraph(key + ": " + value, FontFactory.getFont(FontFactory.HELVETICA, 12)));
            }

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erro ao gerar PDF", e);
        }
    }
}
