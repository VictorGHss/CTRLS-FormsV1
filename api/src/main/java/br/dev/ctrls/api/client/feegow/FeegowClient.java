package br.dev.ctrls.api.client.feegow;

import br.dev.ctrls.api.client.feegow.dto.FeegowPatientRequest;
import br.dev.ctrls.api.client.feegow.dto.FeegowPatientResponse;
import br.dev.ctrls.api.client.feegow.dto.UploadFileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign para operações primárias com o Feegow.
 */
@FeignClient(name = "feegowClient", url = "${integration.feegow.base-url}")
public interface FeegowClient {

    @GetMapping(value = "/patient/list")
    FeegowPatientResponse listPatients(@RequestHeader("x-access-token") String accessToken,
                                       @RequestParam("cpf") String cpf);

    @PostMapping(value = "/patient")
    Long createPatient(@RequestHeader("x-access-token") String accessToken,
                       @RequestBody FeegowPatientRequest request);

    @PostMapping(value = "/patient/files")
    Long uploadPatientFile(@RequestHeader("x-access-token") String accessToken,
                           @RequestBody UploadFileRequest request);
}
