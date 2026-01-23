package com.iiht.service.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
public class HelloServiceController {

    @GetMapping
    public String hello() {
        return "<!DOCTYPE html>" +
               "<html lang='en'>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<title>Application Deployed</title>" +
               "<style>" +
               "body {" +
               "  font-family: Arial, sans-serif;" +
               "  background-color: #f4f4f4;" +
               "  text-align: center;" +
               "  margin: 0;" +
               "  padding: 0;" +
               "}" +
               "h1 {" +
               "  color: #4CAF50;" +
               "  font-size: 50px;" +
               "  margin-top: 20%;" +
               "}" +
               ".container {" +
               "  padding: 20px;" +
               "  background-color: white;" +
               "  border-radius: 10px;" +
               "  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);" +
               "  display: inline-block;" +
               "}" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='container'>" +
               "<h1> Congratulations! The app is Deployed for the first time!!  😁 </h1>" +
               "<p>Your application is up and running successfully!</p>" +
               "</div>" +
               "</body>" +
               "</html>";
    }

    @GetMapping("/greet")
    public String greet() {
        return "Good Morning, Welcome To Demo Project";
    }

    @GetMapping("/add/{a}/{b}")
    public String add(@PathVariable int a, @PathVariable int b) {
        if (a < 0 || b < 0) {
            return "Inputs must be non-negative integers.";
        }
        return (a + b) + "";
    }

    @GetMapping("/fact/{a}")
    public ResponseEntity<String> factorial(@RequestHeader HttpHeaders headers, @PathVariable int a) {
        // Validate HttpHeaders for malicious input
        if (headers != null) {
            for (String headerName : headers.keySet()) {
                String headerValue = headers.getFirst(headerName);
                if (!isValidHeaderValue(headerValue)) {
                    return ResponseEntity.badRequest().body("Invalid header value detected.");
                }
            }
        }

        // Validate input for factorial calculation
        if (a < 0) {
            return ResponseEntity.badRequest().body("Input must be a non-negative integer.");
        }

        int fact = 1;
        for (int i = 1; i <= a; i++) {
            fact *= i;
        }
        return ResponseEntity.ok(String.valueOf(fact));
    }

    private boolean isValidHeaderValue(String value) {
        // Allow only alphanumeric characters and safe symbols in headers
        String safePattern = "^[a-zA-Z0-9-_:;,.]*$";
        return value != null && Pattern.matches(safePattern, value);
    }
}