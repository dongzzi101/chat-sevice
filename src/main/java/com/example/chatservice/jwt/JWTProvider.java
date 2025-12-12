package com.example.chatservice.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.chatservice.common.constants.Constants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JWTProvider {

    @Getter
    private static String secretKey;
    private static String refreshSecretKey;
    private static long tokenTimeForMinute;
    private static long refreshTokenTimeForMinute;

    @Value("${token.secret-key}")
    public void setSecretKey(String secretKey) {
        JWTProvider.secretKey = secretKey;
    }

    @Value("${token.refresh-secret-key}")
    public void setRefreshSecretKey(String refreshSecretKey) {
        JWTProvider.refreshSecretKey = refreshSecretKey;
    }

    @Value("${token.token-time}")
    public void setTokenTime(long tokenTime) {
        JWTProvider.tokenTimeForMinute = tokenTime;
    }

    @Value("${token.refresh-token-time}")
    public void setRefreshTokenTime(long refreshTokenTime) {
        JWTProvider.refreshTokenTimeForMinute = refreshTokenTime;
    }

    public static String generateToken(Long userId, String name) {
        return JWT.create()
                .withSubject(name)
                .withClaim("userId", userId)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + tokenTimeForMinute * Constants.ON_MINUTE_TO_MILLIS))
                .sign(Algorithm.HMAC256(secretKey));
    }

    public static String generateRefreshToken(String name) {
        return JWT.create()
                .withSubject(name)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + tokenTimeForMinute * Constants.ON_MINUTE_TO_MILLIS))
                .sign(Algorithm.HMAC256(refreshSecretKey));
    }

    public static DecodedJWT verifyAccessToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(secretKey))
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            log.error("Access Token verification failed: {}", e.getMessage());
            throw e;
        }
    }

    public static Long getUserIdFromVerifiedToken(String token) {
        DecodedJWT decodedJWT = verifyAccessToken(token);

        if (decodedJWT.getClaim("userId").isNull()) {
            log.warn("UserId claim not found in verified token: {}", token);
            return null;
        }
        return decodedJWT.getClaim("userId").asLong();
    }

    public static DecodedJWT checkTokenForRefresh(String token) {
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC256(secretKey)).build().verify(token);
            log.error("token must be expired : {}", decoded.getSubject());
            throw new RuntimeException();
        } catch (AlgorithmMismatchException | SignatureVerificationException | InvalidClaimException e) {
            throw new RuntimeException();
        } catch (TokenExpiredException e) {
            return JWT.decode(token);
        }
    }


    public static DecodedJWT decodedJWT(String token) {
        return JWT.decode(token);
    }


}
