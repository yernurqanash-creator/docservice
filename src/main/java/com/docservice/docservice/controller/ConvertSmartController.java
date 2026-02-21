package com.docservice.docservice.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docservice.docservice.service.SmartExtractService;

@RestController
public class ConvertSmartController {

    private final SmartExtractService smart;

    public ConvertSmartController(SmartExtractService smart) {
        this.smart = smart;
    }

    @PostMapping(value = "/convert-smart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "text/plain; charset=utf-8")
    public String convertSmart(@RequestParam("file") MultipartFile file) throws Exception {
        return smart.extractSmart(file.getBytes());
    }
}
