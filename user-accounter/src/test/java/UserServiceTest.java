import org.itmo.user.accounter.model.entities.User;
import org.itmo.user.accounter.model.entities.enums.UserRole;
import org.itmo.user.accounter.repositories.UserRepository;
import org.itmo.user.accounter.services.UserService;
import org.itmo.user.accounter.utils.exceptions.AssigningAdminViaAPIException;
import org.itmo.user.accounter.utils.exceptions.DataIntegrityViolationException;
import org.itmo.user.accounter.utils.exceptions.ItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    private User testUser;
    private User adminUser;
    private User existingUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("TestUser")
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("AdminUser")
                .password("encodedPassword")
                .role(UserRole.ADMIN)
                .build();

        existingUser = User.builder()
                .id(3L)
                .username("ExistingUser")
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();
    }

    @Test
    void create_ShouldSaveUser_WhenUserDoesNotExist() {
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Mono.empty());
        when(userRepository.save(testUser)).thenReturn(Mono.just(testUser));

        StepVerifier.create(userService.create(testUser))
                .expectNextMatches(user ->
                        user.getId().equals(1L) &&
                                user.getUsername().equals("TestUser")
                )
                .verifyComplete();

        verify(userRepository).findByUsername(testUser.getUsername());
        verify(userRepository).save(testUser);
    }

    @Test
    void create_ShouldFail_WhenUserWithSameNameExists() {
        when(userRepository.findByUsername(testUser.getUsername()))
                .thenReturn(Mono.just(existingUser));

        StepVerifier.create(userService.create(testUser))
                .expectError(DataIntegrityViolationException.class)
                .verify();

        verify(userRepository).findByUsername(testUser.getUsername());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateRole_ShouldUpdate_WhenUserExistsAndNotAdmin() {
        when(userRepository.findById(existingUser.getId()))
                .thenReturn(Mono.just(existingUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(existingUser));

        StepVerifier.create(userService.updateRole(existingUser.getId(), UserRole.USER))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void updateRole_ShouldFail_WhenUserNotFound() {
        when(userRepository.findById(999L))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.updateRole(999L, UserRole.USER))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void updateRole_ShouldFail_WhenTryingToAssignAdmin() {
        when(userRepository.findById(existingUser.getId()))
                .thenReturn(Mono.just(existingUser));

        StepVerifier.create(userService.updateRole(existingUser.getId(), UserRole.ADMIN))
                .expectError(AssigningAdminViaAPIException.class)
                .verify();
    }

    @Test
    void updateRole_ShouldFail_WhenTryingToUnassignAdminFromAdmin() {
        when(userRepository.findById(adminUser.getId()))
                .thenReturn(Mono.just(adminUser));

        StepVerifier.create(userService.updateRole(adminUser.getId(), UserRole.USER))
                .expectError(AssigningAdminViaAPIException.class)
                .verify();
    }

    @Test
    void deleteById_ShouldComplete_WhenUserExistsAndNotAdmin() {
        when(userRepository.findById(existingUser.getId()))
                .thenReturn(Mono.just(existingUser));
        when(userRepository.deleteById(existingUser.getId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.deleteById(existingUser.getId()))
                .verifyComplete();
    }

    @Test
    void deleteById_ShouldFail_WhenUserNotFound() {
        when(userRepository.findById(999L))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.deleteById(999L))
                .expectError(ItemNotFoundException.class)
                .verify();
    }

    @Test
    void deleteById_ShouldFail_WhenTryingToDeleteAdmin() {
        when(userRepository.findById(adminUser.getId()))
                .thenReturn(Mono.just(adminUser));

        StepVerifier.create(userService.deleteById(adminUser.getId()))
                .expectError(AssigningAdminViaAPIException.class)
                .verify();
    }

    @Test
    void findById_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Mono.just(testUser));

        StepVerifier.create(userService.findById(1L))
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    void findById_ShouldReturnEmpty() {
        when(userRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(userService.findById(1L))
                .verifyComplete();
    }

    @Test
    void findByUsername_ShouldReturnUser() {
        when(userRepository.findByUsername("TestUser"))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(userService.findUserByUsername("TestUser"))
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    void findByUsername_ShouldReturnEmpty() {
        when(userRepository.findByUsername("Unknown"))
                .thenReturn(Mono.empty());

        StepVerifier.create(userService.findUserByUsername("Unknown"))
                .verifyComplete();
    }

    @Test
    void getCurrentUser_ShouldReturnUser_WhenAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("TestUser");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);

        when(userRepository.findByUsername("TestUser"))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(
                        userService.getCurrentUser()
                                .contextWrite(
                                        ReactiveSecurityContextHolder.withSecurityContext(
                                                Mono.just(securityContext)
                                        )
                                )
                )
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    void getCurrentUser_ShouldFail_WhenNotAuthenticated() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);

        StepVerifier.create(
                        userService.getCurrentUser()
                                .contextWrite(
                                        ReactiveSecurityContextHolder.withSecurityContext(
                                                Mono.just(securityContext)
                                        )
                                )
                )
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    void findByUsernameForUserDetails_ShouldReturnUserDetails() {
        when(userRepository.findByUsername("TestUser"))
                .thenReturn(Mono.just(testUser));

        StepVerifier.create(userService.findByUsername("TestUser"))
                .expectNextMatches(userDetails ->
                        userDetails.getUsername().equals("TestUser") &&
                                userDetails.getAuthorities().size() == 1
                )
                .verifyComplete();
    }
}