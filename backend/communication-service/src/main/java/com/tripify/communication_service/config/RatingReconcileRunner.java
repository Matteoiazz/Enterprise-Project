package com.tripify.communication_service.config;

import com.tripify.communication_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RatingReconcileRunner implements CommandLineRunner {

    private final ReviewService reviewService;

    @Override
    public void run(String... args) {
        try {
            reviewService.reconcileAllRatings();
        } catch (Exception e) {
            log.warn("Reconcile dei rating all'avvio non riuscito: {}", e.getMessage());
        }
    }
}
