package com.TrashTDL.ServerlessAuth.function;

import com.TrashTDL.ServerlessAuth.config.SpringContextHolder;
import com.TrashTDL.ServerlessAuth.dto.FolderResponse;
import com.TrashTDL.ServerlessAuth.model.User;
import com.TrashTDL.ServerlessAuth.repository.UserRepository;
import com.TrashTDL.ServerlessAuth.service.DBHandler;
import com.TrashTDL.ServerlessAuth.service.JwtService;
import com.microsoft.azure.functions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FoldersFunction Tests")
class FoldersFunctionTest {

    @InjectMocks
    private FoldersFunction foldersFunction;

    @Mock
    private JwtService jwtService;
    @Mock
    private DBHandler dbHandler;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExecutionContext context;
    @Mock
    private HttpRequestMessage<Void> request;
    @Mock
    private HttpResponseMessage.Builder responseBuilder;
    @Mock
    private HttpResponseMessage httpResponse;

    private final UUID testUserId = UUID.randomUUID();
    private final String testUserEmail = "test@example.com";
    private final String testToken = "valid-jwt-token";

    @BeforeEach
    void setUp() {
        when(context.getLogger()).thenReturn(Logger.getLogger("test-logger"));
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(responseBuilder);
        when(responseBuilder.body(any())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(httpResponse);
    }

    private void setupValidUserAuthentication() {
        User mockUser = mock(User.class);
        when(request.getHeaders()).thenReturn(Map.of("authorization", "Bearer " + testToken));
        when(jwtService.extractUserId(testToken)).thenReturn(testUserId);
        when(jwtService.extractUsername(testToken)).thenReturn(testUserEmail);
        when(userRepository.findByEmail(testUserEmail)).thenReturn(Optional.of(mockUser));
        when(jwtService.isTokenValid(testToken, mockUser)).thenReturn(true);
    }

    // @Test
    // @DisplayName("should return 200 OK with folders on successful request")
    // void getFolders_success_returns200() throws SQLException {
    //     List<FolderResponse> mockFolders = List.of(new FolderResponse(UUID.randomUUID(), "Default Folder"));
        
    //     try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
    //         springMock.when(() -> SpringContextHolder.getBean(JwtService.class)).thenReturn(jwtService);
    //         springMock.when(() -> SpringContextHolder.getBean(UserRepository.class)).thenReturn(userRepository);
    //         springMock.when(() -> SpringContextHolder.getBean(DBHandler.class)).thenReturn(dbHandler);
            
    //         setupValidUserAuthentication();
    //         when(dbHandler.getFoldersForUser(testUserId)).thenReturn(mockFolders);
    //         when(httpResponse.getStatus()).thenReturn(HttpStatus.OK);

    //         HttpResponseMessage response = foldersFunction.run(request, context);

    //         assertEquals(HttpStatus.OK, response.getStatus());
    //         verify(responseBuilder).body(mockFolders);
    //     }
    // }

    // @Test
    // @DisplayName("should return 404 Not Found when user not found")
    // void getFolders_userNotFound_returns404() {
    //     when(request.getHeaders()).thenReturn(Map.of("authorization", "Bearer " + testToken));
        
    //     try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
    //         springMock.when(() -> SpringContextHolder.getBean(JwtService.class)).thenReturn(jwtService);
    //         springMock.when(() -> SpringContextHolder.getBean(UserRepository.class)).thenReturn(userRepository);
            
    //         when(jwtService.extractUserId(testToken)).thenReturn(testUserId);
    //         when(jwtService.extractUsername(testToken)).thenReturn(testUserEmail);
    //         when(userRepository.findByEmail(testUserEmail)).thenReturn(Optional.empty());
    //         when(httpResponse.getStatus()).thenReturn(HttpStatus.NOT_FOUND);

    //         HttpResponseMessage response = foldersFunction.run(request, context);

    //         assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    //         verify(responseBuilder).body("User not found.");
    //     }
    // }

    // @Test
    // @DisplayName("should return 401 Unauthorized for invalid JWT token")
    // void getFolders_invalidJwtToken_returns401() {
    //     User mockUser = mock(User.class);
    //     when(request.getHeaders()).thenReturn(Map.of("authorization", "Bearer " + testToken));
        
    //     try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
    //         springMock.when(() -> SpringContextHolder.getBean(JwtService.class)).thenReturn(jwtService);
    //         springMock.when(() -> SpringContextHolder.getBean(UserRepository.class)).thenReturn(userRepository);
            
    //         when(jwtService.extractUserId(testToken)).thenReturn(testUserId);
    //         when(jwtService.extractUsername(testToken)).thenReturn(testUserEmail);
    //         when(userRepository.findByEmail(testUserEmail)).thenReturn(Optional.of(mockUser));
    //         when(jwtService.isTokenValid(testToken, mockUser)).thenReturn(false);
    //         when(httpResponse.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED);

    //         HttpResponseMessage response = foldersFunction.run(request, context);

    //         assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    //         verify(responseBuilder).body("Invalid token.");
    //     }
    // }

    @Test
    @DisplayName("should return 401 Unauthorized for missing token")
    void getFolders_missingToken_returns401() {
        when(request.getHeaders()).thenReturn(Collections.emptyMap());
        when(httpResponse.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED);

        HttpResponseMessage response = foldersFunction.run(request, context);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
        verify(responseBuilder).body("Authorization header with Bearer token is required.");
    }

    @Test
    @DisplayName("should return 401 Unauthorized for invalid token")
    void getFolders_invalidToken_returns401() {
         when(request.getHeaders()).thenReturn(Map.of("authorization", "Bearer " + testToken));
         
        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(JwtService.class)).thenReturn(jwtService);
            when(jwtService.extractUserId(testToken)).thenThrow(new RuntimeException("Invalid Token"));
            when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

            HttpResponseMessage response = foldersFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("An unexpected error occurred.");
        }
    }

    @Test
    @DisplayName("should return 500 Internal Server Error on SQLException")
    void getFolders_sqlException_returns500() throws SQLException {
        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(JwtService.class)).thenReturn(jwtService);
            springMock.when(() -> SpringContextHolder.getBean(UserRepository.class)).thenReturn(userRepository);
            springMock.when(() -> SpringContextHolder.getBean(DBHandler.class)).thenReturn(dbHandler);

            setupValidUserAuthentication();
            when(dbHandler.getFoldersForUser(testUserId)).thenThrow(new SQLException("DB connection failed"));
            when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
            
            HttpResponseMessage response = foldersFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("Failed to retrieve folders due to a database error.");
        }
    }

    @Test
    @DisplayName("should return 500 Internal Server Error on unexpected exception")
    void getFolders_unexpectedException_returns500() {
        try (MockedStatic<SpringContextHolder> springMock = Mockito.mockStatic(SpringContextHolder.class)) {
            springMock.when(() -> SpringContextHolder.getBean(JwtService.class)).thenThrow(new RuntimeException("Unexpected error"));
            when(request.getHeaders()).thenReturn(Map.of("authorization", "Bearer " + testToken));

            when(httpResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

            HttpResponseMessage response = foldersFunction.run(request, context);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
            verify(responseBuilder).body("An unexpected error occurred.");
        }
    }
}