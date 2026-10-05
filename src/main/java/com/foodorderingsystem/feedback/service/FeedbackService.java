package com.foodorderingsystem.feedback.service;

import com.foodorderingsystem.feedback.entity.Feedback;
import com.foodorderingsystem.feedback.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback save(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    public Optional<Feedback> findByOrderId(Long orderId) {
        return feedbackRepository.findByOrder_OrderId(orderId);
    }

    public List<Feedback> getAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Feedback> getByCustomer(Long customerId) {
        return feedbackRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId);
    }

    public double getAverageRating() {
        List<Feedback> all = feedbackRepository.findAll();
        if (all.isEmpty()) return 0;
        return all.stream().mapToInt(Feedback::getRating).average().orElse(0);
    }

    public Feedback getById(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + id));
    }

    public Feedback update(Long id, Integer rating, String comment) {
        Feedback feedback = getById(id);
        feedback.setRating(rating);
        feedback.setComment(comment);
        return feedbackRepository.save(feedback);
    }

    public void delete(Long id) {
        feedbackRepository.deleteById(id);
    }
}
