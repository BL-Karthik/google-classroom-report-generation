package com.bridgelabz.handler;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestController
public class OAuth2ErrorController {

    @GetMapping("/oauth2/error")
    public String handleOAuthError(OAuth2AuthenticationException exception) {
        // Log the error or show a custom error message
        System.out.println("OAuth2 Error: " + exception.getMessage());
        return "oauth-error"; // You can show a custom error page here
    }
}

