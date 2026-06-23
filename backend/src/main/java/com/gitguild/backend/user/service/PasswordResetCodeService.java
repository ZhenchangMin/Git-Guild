package com.gitguild.backend.user.service;

import com.gitguild.backend.common.BusinessException;
import java.security.SecureRandom;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 负责密码重置验证码的生成、存储与校验。
 *
 * <p>验证码及其相关计数器全部存放在 Redis，并依赖 Redis 的 TTL 自动过期：
 * <ul>
 *   <li>验证码 5 分钟有效，校验通过后立即删除（一次性）。</li>
 *   <li>同一邮箱 1 分钟内只能请求一次，防止被刷邮件。</li>
 *   <li>同一验证码最多允许尝试 5 次，超过即作废，防止暴力猜码。</li>
 * </ul>
 */
@Service
public class PasswordResetCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_GAP = Duration.ofMinutes(1);
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private static final String CODE_KEY_PREFIX = "pwd:reset:code:";
    private static final String GAP_KEY_PREFIX = "pwd:reset:gap:";
    private static final String ATTEMPT_KEY_PREFIX = "pwd:reset:attempt:";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetCodeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 为指定邮箱生成并存储一个新的验证码。
     *
     * @return 生成的 6 位数字验证码（供调用方发送邮件）
     * @throws BusinessException 距上次请求不足 1 分钟时抛出 TOO_FREQUENT
     */
    public String issue(String email) {
        String gapKey = GAP_KEY_PREFIX + email;
        Boolean firstWrite = redisTemplate.opsForValue().setIfAbsent(gapKey, "1", RESEND_GAP);
        if (!Boolean.TRUE.equals(firstWrite)) {
            throw new BusinessException("TOO_FREQUENT", HttpStatus.TOO_MANY_REQUESTS,
                    "请求过于频繁，请稍后再试", "resend gap for " + email);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, CODE_TTL);
        redisTemplate.delete(ATTEMPT_KEY_PREFIX + email);
        return code;
    }

    /**
     * 校验验证码；通过则立即消费（删除）该验证码。
     *
     * @return 是否校验通过
     */
    public boolean verifyAndConsume(String email, String code) {
        String codeKey = CODE_KEY_PREFIX + email;
        String saved = redisTemplate.opsForValue().get(codeKey);
        if (saved == null) {
            return false;
        }

        if (exceededMaxAttempts(email, codeKey)) {
            return false;
        }

        if (!saved.equals(code)) {
            return false;
        }

        redisTemplate.delete(codeKey);
        redisTemplate.delete(ATTEMPT_KEY_PREFIX + email);
        return true;
    }

    /** 记录一次尝试；超过上限时连同验证码一起作废。 */
    private boolean exceededMaxAttempts(String email, String codeKey) {
        String attemptKey = ATTEMPT_KEY_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1L) {
            // 首次尝试时对计数器设置与验证码一致的过期时间，避免计数器永久残留。
            redisTemplate.expire(attemptKey, CODE_TTL);
        }
        if (attempts != null && attempts > MAX_VERIFY_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptKey);
            return true;
        }
        return false;
    }
}
