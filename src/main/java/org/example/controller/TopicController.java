package org.example.controller;

import org.example.dto.TopicResponse;
import org.example.service.TopicService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<TopicResponse> listTopics(@RequestParam(required = false) String search) {
        return topicService.listTopics(search);
    }
}
