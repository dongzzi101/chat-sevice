package com.example.chatservice.user.repository;

import com.example.chatservice.user.entity.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("유저이름으로 유저를 찾을 수 있다.")
    void findByUsername() {
        // given
        String givenName = "userA";

        User user1 = User.builder()
                .username(givenName)
                .build();

        userRepository.save(user1);

        // when
        User findUser = userRepository.findByUsername(givenName);

        // then
        assertThat(findUser).isEqualTo(user1);
        assertThat(findUser)
                .extracting("username")
                .isEqualTo(givenName);
    }

    @Test
    @DisplayName("찾는 유저가 없으면 null을 반환한다.")
    void returnNullIfNoFindByUsername() {

        // when
        User findUser = userRepository.findByUsername("userA");

        // then
        assertThat(findUser).isNull();
    }


}