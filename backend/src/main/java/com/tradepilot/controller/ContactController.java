package com.tradepilot.controller;

import com.tradepilot.dto.contact.ContactRequest;
import com.tradepilot.dto.contact.ContactResponse;
import com.tradepilot.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactService contactService;

    @PostMapping
    public ContactResponse submit(@Valid @RequestBody ContactRequest request, HttpServletRequest servletRequest) {
        return contactService.submit(request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
    }

    @java.lang.SuppressWarnings("all")
    public ContactController(final ContactService contactService) {
        this.contactService = contactService;
    }
}
