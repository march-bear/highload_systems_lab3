import org.itmo.user.accounter.model.dto.JwtTokenDto;
import org.itmo.user.accounter.model.dto.UserAuthDto;
import org.itmo.user.accounter.model.entities.User;
import org.itmo.user.accounter.model.entities.enums.UserRole;
import org.itmo.user.accounter.services.AuthenticationService;
import org.itmo.user.accounter.services.JwtService;
import org.itmo.user.accounter.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReactiveAuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void signUp_ShouldReturnToken_WhenSuccessful() {
        // Arrange
        UserAuthDto request = new UserAuthDto("testUser", "password123");
        User user = User.builder()
                .id(1L)
                .username("testUser")
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();
        String token = "jwt.token.here";

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userService.create(any(User.class))).thenReturn(Mono.just(user));
        when(jwtService.generateToken(any(User.class))).thenReturn(token);

        // Act & Assert
        StepVerifier.create(authenticationService.signUp(request))
                .expectNextMatches(jwtTokenDto ->
                        jwtTokenDto.token().equals(token)
                )
                .verifyComplete();
    }

    @Test
    void signIn_ShouldReturnToken_WhenSuccessful() {
        // Arrange
        UserAuthDto request = new UserAuthDto("testUser", "password123");
        User user = User.builder()
                .id(1L)
                .username("testUser")
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();
        String token = "jwt.token.here";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mono.just(new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                )));
        when(userService.findByUsername("testUser")).thenReturn(Mono.just(user));
        when(jwtService.generateToken(any(User.class))).thenReturn(token);

        // Act & Assert
        StepVerifier.create(authenticationService.signIn(request))
                .expectNextMatches(jwtTokenDto ->
                        jwtTokenDto.token().equals(token)
                )
                .verifyComplete();
    }

    @Test
    void signIn_ShouldPropagateError_WhenAuthenticationFails() {
        // Arrange
        UserAuthDto request = new UserAuthDto("testUser", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mono.error(new RuntimeException("Authentication failed")));

        // Act & Assert
        StepVerifier.create(authenticationService.signIn(request))
                .expectError(RuntimeException.class)
                .verify();
    }
}