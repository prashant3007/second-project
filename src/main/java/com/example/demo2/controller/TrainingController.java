package com.example.demo2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrainingController {

    @GetMapping ("/")
    public String index() {
        return "Prashant Kumar says Hello!!!!";
    }
}
