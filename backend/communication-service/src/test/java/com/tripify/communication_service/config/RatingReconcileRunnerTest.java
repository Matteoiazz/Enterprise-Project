package com.tripify.communication_service.config;

import com.tripify.communication_service.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RatingReconcileRunnerTest {

    @Mock ReviewService reviewService;

    @InjectMocks RatingReconcileRunner runner;

    @Test
    void run_triggersReconcileOnStartup() {
        runner.run();

        verify(reviewService).reconcileAllRatings();
    }

    @Test
    void run_swallowsExceptionsSoStartupIsNotBlocked() {
        doThrow(new RuntimeException("db down")).when(reviewService).reconcileAllRatings();

        assertThatCode(() -> runner.run()).doesNotThrowAnyException();
    }
}
