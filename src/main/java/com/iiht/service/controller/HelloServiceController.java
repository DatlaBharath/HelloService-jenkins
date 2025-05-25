package com.iiht.service.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

@RestController
public class HelloServiceController {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+");
    private static final int MAX_NUMBER_FOR_ADDITION = 10000; // Set a sensible limit for addition
    private static final int MAX_FACTORIAL_INPUT = 20; // Limit factorial calculation to reasonable bounds

    @GetMapping
    public String hello() {
        String htmlContent = "<!DOCTYPE html>" +
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
                "<h1> Congratulations! The app is Deployed for first time 😁 </h1>" +
                "<p>Your application is up and running successfully!</p>" +
                "</div>" +
                "</body>" +
                "</html>";
        return HtmlUtils.htmlEscape(htmlContent);
    }

    @GetMapping("/greet")
    public String greet() {
        return "Good Morning, Welcome To Demo Project";
    }

    @GetMapping("/add/{a}/{b}")
    public ResponseEntity<String> add(@PathVariable String a, @PathVariable String b) {
        if (!INTEGER_PATTERN.matcher(a).matches() || !INTEGER_PATTERN.matcher(b).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: both arguments must be integers.");
        }

        try {
            int num1 = Integer.parseInt(a);
            int num2 = Integer.parseInt(b);
            if (Math.abs(num1) > MAX_NUMBER_FOR_ADDITION || Math.abs(num2) > MAX_NUMBER_FOR_ADDITION) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: integers should be within acceptable range.");
            }
            
            return ResponseEntity.ok(String.valueOf(num1 + num2));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: mathematical operation failed.");
        }
    }

    @GetMapping("/fact/{a}")
    public ResponseEntity<String> fact(@RequestHeader HttpHeaders header, @PathVariable String a) {
        if (!INTEGER_PATTERN.matcher(a).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: argument must be an integer.");
        }

        try {
            int num = Integer.parseInt(a);
            if (num < 0 || num > MAX_FACTORIAL_INPUT) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: integer should be between 0 and " + MAX_FACTORIAL_INPUT);
            }

            int fact = 1;
            for (int i = 1; i <= num; i++) {
                fact *= i;
            }
            return ResponseEntity.ok(String.valueOf(fact));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: mathematical operation failed.");
        }
    }
}