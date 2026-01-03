package com.example.chatservice.sharding;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ShardingAspect {

    private static final ThreadLocal<Long> threadLocalChatId = new ThreadLocal<>();

    @Around("@annotation(sharding)")
    public Object handleSharding(ProceedingJoinPoint joinPoint, Sharding sharding) throws Throwable {
        Long chatId = resolveShardKey(joinPoint, sharding.key());

        threadLocalChatId.set(chatId);
        try {
            return joinPoint.proceed();
        } finally {
            threadLocalChatId.remove();
        }
    }

    private Long resolveShardKey(ProceedingJoinPoint joinPoint, String spel) {
        // Build evaluation context from method parameters
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        // Fallback: if no SpEL provided, use first argument
        if (spel == null || spel.isBlank()) {
            if (args.length > 0) {
                return toLongSafe(args[0]);
            }
            return 0L;
        }

        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(spel);
        Object value = expression.getValue(context);
        return toLongSafe(value);
    }

    private Long toLongSafe(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static Long getCurrentThreadChatId() {
        return threadLocalChatId.get();
    }


}
