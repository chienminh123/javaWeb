package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ChatBotResponse;
import com.example.demo.service.ChatBotService;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @PostMapping("/message")
    public ChatBotResponse handleMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null) {
            message = "";
        }
        return chatBotService.processMessage(message);
    }
}

