package com.gitguild.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.common.BusinessException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class PasswordResetCodeServiceTest {

    private static final String EMAIL = "alice@example.com";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PasswordResetCodeService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new PasswordResetCodeService(redisTemplate);
    }

    @Test
    void issueShouldGenerateSixDigitCodeAndStoreIt() {
        when(valueOperations.setIfAbsent(startsWith("pwd:reset:gap:"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        String code = service.issue(EMAIL);

        assertThat(code).matches("\\d{6}");
        verify(valueOperations).set(eq("pwd:reset:code:" + EMAIL), eq(code), any(Duration.class));
    }

    @Test
    void issueShouldRejectWhenWithinResendGap() {
        when(valueOperations.setIfAbsent(startsWith("pwd:reset:gap:"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.issue(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("TOO_FREQUENT");
    }

    @Test
    void verifyShouldReturnFalseWhenNoCodeStored() {
        when(valueOperations.get("pwd:reset:code:" + EMAIL)).thenReturn(null);

        assertThat(service.verifyAndConsume(EMAIL, "123456")).isFalse();
    }

    @Test
    void verifyShouldConsumeCodeWhenMatched() {
        when(valueOperations.get("pwd:reset:code:" + EMAIL)).thenReturn("123456");
        when(valueOperations.increment("pwd:reset:attempt:" + EMAIL)).thenReturn(1L);

        boolean result = service.verifyAndConsume(EMAIL, "123456");

        assertThat(result).isTrue();
        verify(redisTemplate).delete("pwd:reset:code:" + EMAIL);
    }

    @Test
    void verifyShouldReturnFalseWhenCodeMismatch() {
        when(valueOperations.get("pwd:reset:code:" + EMAIL)).thenReturn("123456");
        when(valueOperations.increment("pwd:reset:attempt:" + EMAIL)).thenReturn(1L);

        assertThat(service.verifyAndConsume(EMAIL, "000000")).isFalse();
    }

    @Test
    void verifyShouldInvalidateCodeAfterTooManyAttempts() {
        when(valueOperations.get("pwd:reset:code:" + EMAIL)).thenReturn("123456");
        when(valueOperations.increment("pwd:reset:attempt:" + EMAIL)).thenReturn(6L);

        boolean result = service.verifyAndConsume(EMAIL, "123456");

        assertThat(result).isFalse();
        // 超过尝试上限后，验证码被强制作废。
        verify(redisTemplate).delete("pwd:reset:code:" + EMAIL);
    }
}
