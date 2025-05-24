package com.iiht.service.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

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
               "<h1> Congratulations! The app  is Deployed for first time  😁 </h1>" +
               "<p>Your application is up and running successfully!</p>" +
               "</div>" +
               "</body>" +
               "</html>";
    }

    @GetMapping("/greet")
    public String greet() 
    {
        return "Good Morning, Welcome To Demo Project";
    }
    
    @GetMapping("/add/{a}/{b}")
    public ResponseEntity<String> add(@PathVariable String a,@PathVariable String b) {
        try {
            int num1 = Integer.parseInt(a);
            int num2 = Integer.parseInt(b);
            return ResponseEntity.ok((num1 + num2) + "");
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: both arguments must be integers.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
    
    @GetMapping("/fact/{a}")
    public ResponseEntity<String> fact(@RequestHeader HttpHeaders header,@PathVariable String a) {
        try {
            int num = Integer.parseInt(a);
            int fact = 1;
            for(int i = 1; i <= num; i++) {
                fact *= i;
            }
            return ResponseEntity.ok(fact + "");
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input: argument must be an integer.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
}