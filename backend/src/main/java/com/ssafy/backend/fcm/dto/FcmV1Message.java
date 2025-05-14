package com.ssafy.backend.fcm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class FcmV1Message {

    private Message message;

    @Data
    @AllArgsConstructor
    public static class Message {
        private Notification notification;
        private String token;
        private Map<String, String> data;
    }

    @Data
    @AllArgsConstructor
    public static class Notification {
        private String title;
        private String body;
    }
}
