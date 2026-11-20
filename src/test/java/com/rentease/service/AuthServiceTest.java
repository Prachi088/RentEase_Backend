package com.rentease.service;

import com.rentease.dto.auth.AuthRequest;
import com.rentease.dto.auth.AuthResponse;
import com.rentease.dto.auth.RegisterRequest;
import com.rentease.dto.auth.UserDto;
import com.rentease.entity.Role;
import com.rentease.entity.User;
import com.rentease.enums.RoleName;
import com.rentease.exception.BadRequestException;
import com.rentease.repository.RoleRepository;
import com.rentease.repository.UserRepository;
import com.rentease.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = new Role("role_1", RoleName.ROLE_CUSTOMER);
        sampleUser = new User();
        sampleUser.setId("user_123");
        sampleUser.setEmail("rahul.verma@example.com");
        sampleUser.setFullName("Rahul Verma");
        sampleUser.setPhone("+91 9876543210");
        sampleUser.setPasswordHash("hashed_password");
        sampleUser.setAccountStatus("ACTIVE");
        sampleUser.setRoles(Collections.singleton(customerRole));
        sampleUser.setTrustScore(95);
        sampleUser.setKycVerified(true);
    }

    @Test
    @DisplayName("Should successfully authenticate user and return JWT tokens")
    void testAuthenticateUser_Success() {
        AuthRequest request = new AuthRequest("rahul.verma@example.com", "Password@123");
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("mocked.jwt.token");
        when(userRepository.findByEmail("rahul.verma@example.com")).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generateRefreshToken(sampleUser.getId())).thenReturn("mocked.refresh.token");

        AuthResponse response = authService.authenticateUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getRefreshToken()).isEqualTo("mocked.refresh.token");
        assertThat(response.getUserId()).isEqualTo("user_123");
        assertThat(response.getEmail()).isEqualTo("rahul.verma@example.com");
        assertThat(response.getRoles()).contains("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should register a new customer and return JWT response")
    void testRegisterUser_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Rahul");
        request.setLastName("Verma");
        request.setEmail("newuser@example.com");
        request.setPhone("+91 9876543210");
        request.setPassword("Password@123");
        request.setRole("CUSTOMER");

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("jwt.token.here");
        when(tokenProvider.generateRefreshToken(anyString())).thenReturn("refresh.token.here");

        AuthResponse response = authService.registerUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if email is already registered")
    void testRegisterUser_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("rahul.verma@example.com");

        when(userRepository.existsByEmail("rahul.verma@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Email address is already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should retrieve user profile accurately")
    void testGetCurrentUserProfile_Success() {
        when(userRepository.findById("user_123")).thenReturn(Optional.of(sampleUser));

        UserDto profile = authService.getCurrentUserProfile("user_123");

        assertThat(profile).isNotNull();
        assertThat(profile.getId()).isEqualTo("user_123");
        assertThat(profile.getEmail()).isEqualTo("rahul.verma@example.com");
        assertThat(profile.getFullName()).isEqualTo("Rahul Verma");
        assertThat(profile.getRoles()).contains("ROLE_CUSTOMER");
        assertThat(profile.isKycVerified()).isTrue();
    }
}
