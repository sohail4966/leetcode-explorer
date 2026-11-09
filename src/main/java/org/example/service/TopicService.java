package org.example.service;

import org.example.dto.TopicResponse;
import org.example.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public List<TopicResponse> listTopics(String search) {
        return topicRepository.findAllWithProblemCount(search);
    }
}
