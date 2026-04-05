package com.hubble.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    /**
     * Simple health check endpoint for the API
     */
    @GetMapping("/")
    public String health() {
        return "OK";
    }
}
