package com.example.readingrewards.unit.message;

import com.example.readingrewards.domain.repo.message.MessageRepository;
import com.example.readingrewards.domain.service.message.PayoutReminderNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayoutReminderEmailPreferenceTest {

    @Test
    void emailDefaultsToEnabledAndSupportsOptOut() {
        MessageRepository messageRepository = mock(MessageRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<com.example.readingrewards.auth.service.VerificationEmailService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<com.example.readingrewards.auth.service.VerificationEmailService> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        PayoutReminderNotificationService service = new PayoutReminderNotificationService(messageRepository, provider);

        assertThat(service.isEmailEnabled(null)).isTrue();
        assertThat(service.isEmailEnabled(true)).isTrue();
        assertThat(service.isEmailEnabled(false)).isFalse();
    }
}
