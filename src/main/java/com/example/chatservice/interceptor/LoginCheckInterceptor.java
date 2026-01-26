package com.example.chatservice.interceptor;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.chatservice.jwt.JWTProvider;
import com.example.chatservice.user.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;

@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        // 어떤 토큰인지 구분 하는 것 !!
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authHeader.substring(7);

        try {
            DecodedJWT decodedJWT = JWTProvider.verifyAccessToken(token);

            Long userId = decodedJWT.getClaim("userId").asLong();
            String username = decodedJWT.getClaim("username").asString();

            UserPrincipal userPrincipal = new UserPrincipal(userId, username);
            request.setAttribute("userPrincipal", userPrincipal);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);


            return true;

        } catch (JWTVerificationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
// TODO 사진 s3
// TODO 3 : 이미지 저장
// S3 flow
/**
 * 1. s3Service.uploadFile()
 * 2. String imageUrls = s3Service.uploadFile("posts", images);
 * 3. Message.builder()
 *           .sender(senderUser)
 *           .chatRoom(chatRoom)
 *           .message(messageRequest.getMessage())
 *           .imageUrl(imageUrls)
 *           .build();
 * 4. 메시지 조회 시 url 나오면 이건 -> 클라이언트가 s3에서 받아오는 것!
 *
 */




// send message
// with image



// 1. file upload server
// 2. client url 획득
// 3. (photo)send message(url)