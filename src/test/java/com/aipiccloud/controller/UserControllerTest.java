package com.aipiccloud.controller;

import com.aipiccloud.common.BaseResponse;
import com.aipiccloud.exception.BusinessException;
import com.aipiccloud.exception.ErrorCode;
import com.aipiccloud.model.dto.user.UserLoginRequest;
import com.aipiccloud.model.vo.LoginUserVO;
import com.aipiccloud.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class UserControllerTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        request = new MockHttpServletRequest();
    }

    @Test
    void userLogin_success() {
        // Arrange
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUserAccount("jiang1");
        loginRequest.setUserPassword("test1234");

        LoginUserVO mockVO = new LoginUserVO();
        mockVO.setUserName("jiang1");

        when(userService.userLogin("jiang1", "test1234", request)).thenReturn(mockVO);

        // Act
        BaseResponse<LoginUserVO> response = userController.userLogin(loginRequest, request);

        // Assert
        assertNotNull(response);
        assertEquals(mockVO, response.getData());
        verify(userService, times(1)).userLogin("jiang1", "test1234", request);
    }

    private static Stream<Arguments> invalidUserAccounts() {
        return Stream.of(
                Arguments.of("", "参数为空"),
                Arguments.of(null, "参数为空"),
                Arguments.of("!@#$%^&*", "账号错误"),
                Arguments.of("a".repeat(100), "账号错误"),
                Arguments.of("nonexistent", "用户不存在或密码错误")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUserAccounts")
    void userLogin_invalidAccount(String userAccount, String expectedMessage) {
        // Arrange
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUserAccount(userAccount);
        loginRequest.setUserPassword("test1234");

        when(userService.userLogin(userAccount, "test1234", request))
                .thenThrow(new BusinessException(ErrorCode.PARAMS_ERROR, expectedMessage));

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userController.userLogin(loginRequest, request)
        );

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals(expectedMessage, exception.getMessage());

        verify(userService, times(1)).userLogin(userAccount, "test1234", request);
    }
}
