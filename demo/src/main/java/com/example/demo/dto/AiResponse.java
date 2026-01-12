package com.example.demo.dto;

import java.util.List;

import lombok.Data;

@Data
public class AiResponse {
    private Integer user_id;
    private List<Integer> recommendations; // Hứng danh sách ID từ Python
}
