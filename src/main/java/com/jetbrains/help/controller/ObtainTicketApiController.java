package com.jetbrains.help.controller;

import com.jetbrains.help.context.LicenseContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/rpc")
public class ObtainTicketApiController {

    @GetMapping(value = "/obtainTicket.action", produces = {"application/xml;charset=utf-8"})
    @ResponseBody
    public ResponseEntity<String> obtainTicket(LicenseContextHolder.ObtainTicketRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(LicenseContextHolder.obtainTicket(request));
    }
}
