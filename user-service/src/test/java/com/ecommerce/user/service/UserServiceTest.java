package com.ecommerce.user.service;

import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.DuplicateResourceException;
import com.ecommerce.user.exception.ResourceNotFoundException;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .role(Role.CUSTOMER)
                .build();

        registrationRequest = UserRegistrationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .build();
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserResponse result = userService.registerUser(registrationRequest);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            userService.registerUser(registrationRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(1L);
        });
    }

    @Test
    void getUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        UserResponse result = userService.getUserByEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void updateUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserRegistrationRequest updateRequest = UserRegistrationRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .phone("+9876543210")
                .build();

        // Act
        UserResponse result = userService.updateUser(1L, updateRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(any(User.class));

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository, times(1)).delete(testUser);
    }
}
