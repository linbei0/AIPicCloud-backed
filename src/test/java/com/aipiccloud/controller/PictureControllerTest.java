package com.aipiccloud.controller;

import cn.hutool.core.date.DateUtil;
import com.aipiccloud.exception.BusinessException;
import com.aipiccloud.manager.CosManager;
import com.aipiccloud.model.dto.picture.PictureUploadRequest;
import com.aipiccloud.model.entity.User;
import com.aipiccloud.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import static com.aipiccloud.constant.UserConstant.USER_LOGIN_STATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PictureControllerTest {

    @Autowired
    private PictureController pictureController;

    @Autowired
    private UserService userService;
    @Autowired
    private CosManager cosManager;
    private static File testImageFile;
    private static HttpServletRequest mockRequest;

    /*@BeforeAll
    public static void setup() {
        // 模拟请求对象
        mockRequest = new MockHttpServletRequest();
    }*/
    @BeforeAll
    public static void setup() throws IOException {
        mockRequest = new MockHttpServletRequest();
        // 准备测试图片文件
        testImageFile = new File("src/test/resources/test.jpg");
        assertTrue(testImageFile.exists(), "测试图片不存在，请确认路径");
    }
    /**
     * 模拟用户登录
     */
    private void loginAs(Long userId) {
        User user = userService.getById(userId);
        assertNotNull(user, "用户不存在，请确认测试用户ID是否正确");
        mockRequest.getSession().setAttribute(USER_LOGIN_STATE, user);
    }

    /**
     * 模拟用户登出
     */
    private void logout() {
        mockRequest.getSession().removeAttribute(USER_LOGIN_STATE);
    }

    @Test
    public void testGetPictureVOById_PublicImage_LoggedIn() {
        Long publicImageId = 1909266143474970626L;
        loginAs(1908178403920461825L);

        var response = pictureController.getPictureVOById(publicImageId, mockRequest);
        assertNotNull(response);
        assertNotNull(response.getData());
        System.out.println(response.getData().getPermissionList());
    }

    @Test
    public void testGetPictureVOById_PrivateImage_NoLogin() {
        Long privateImageId = 1918219483571294210L;

        // 用户未登录
        assertThrows(BusinessException.class, () -> {
            pictureController.getPictureVOById(privateImageId, mockRequest);
        });
    }

    @Test
    public void testGetPictureVOById_PrivateImage_WithLogin_ButNoPermission() {
        Long privateImageId = 1918219483571294210L;
        loginAs(1908178425076531201L); // 无空间权限

        assertThrows(BusinessException.class, () -> {
            pictureController.getPictureVOById(privateImageId, mockRequest);
        });
    }

    @Test
    public void testGetPictureVOById_PrivateImage_WithLogin_AndPermission() {
        Long publicImageId = 1909266143474970626L;
        loginAs(1908178403920461825L);

        var response = pictureController.getPictureVOById(publicImageId, mockRequest);
        assertNotNull(response);
        assertNotNull(response.getData());
        System.out.println(response.getData().getPermissionList());
    }

    @Test
    public void testGetPictureVOById_InvalidId() {
        loginAs(1908178403920461825L);

        assertThrows(BusinessException.class, () -> {
            pictureController.getPictureVOById(0L, mockRequest);
        });
    }
    @Test
    @Order(1)
    public void testUploadPublicPicture_Success() throws Exception {
        loginAs(1908178403920461825L);

        // 读取测试文件
        MultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new FileInputStream(testImageFile)
        );

        PictureUploadRequest requestDTO = new PictureUploadRequest();

        // 执行上传
        var response = pictureController.uploadPicture(file, requestDTO, mockRequest);
        assertNotNull(response);
        assertNotNull(response.getData());
//        assertTrue(response.isSuccess());

        // 验证返回值格式
        String expectedPathPrefix = "public/1908178403920461825/";
        assertTrue(response.getData().getUrl().contains(expectedPathPrefix));
        assertNotNull(response.getData().getId());
    }

    @Test
    @Order(2)
    public void testUploadPublicPicture_NotLoggedIn() {
        logout(); // 确保未登录

        MultipartFile file = mock(MultipartFile.class);
        assertThrows(BusinessException.class, () -> {
            pictureController.uploadPicture(file, null, mockRequest);
        });
    }

    @Test
    @Order(3)
    public void testUploadPublicPicture_EmptyFile() {
        loginAs(1908178403920461825L);

        assertThrows(NullPointerException.class, () -> {
            pictureController.uploadPicture(null, null, mockRequest);
        });
    }

    @Test
    @Order(4)
    public void testUploadPublicPicture_InvalidRequest() {
        loginAs(1908178403920461825L);

        MultipartFile file = mock(MultipartFile.class);
        assertThrows(NullPointerException.class, () -> {
            pictureController.uploadPicture(file, null, mockRequest);
        });
    }

    @Test
    @Order(5)
    public void testUploadPublicPicture_DefaultName() throws Exception {
        loginAs(1908178403920461825L);

        MultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new FileInputStream(testImageFile)
        );

        PictureUploadRequest requestDTO = new PictureUploadRequest();
        var response = pictureController.uploadPicture(file, requestDTO, mockRequest);

        assertNotNull(response);
        assertNotNull(response.getData());
//        assertTrue(response.isSuccess());

        // 验证默认名称逻辑
        assertTrue(response.getData().getName().startsWith("test"));
    }

    @Test
    @Order(6)
    public void testUploadPublicPicture_FileTooLarge() {
        loginAs(1908178403920461825L);

        // 创建 20MB+ 的测试文件
        File largeFile = new File("src/test/resources/large_test_file.bin");
        try (FileOutputStream fos = new FileOutputStream(largeFile)) {
            byte[] data = new byte[1024 * 1024 * 20]; // 20MB
            fos.write(data);
        } catch (IOException e) {
            fail("创建大文件失败");
        }

        MultipartFile file = null;
        try {
            file = new MockMultipartFile(
                    "file", "large.jpg", "image/jpeg", new FileInputStream(largeFile)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MultipartFile finalFile = file;
        assertThrows(NullPointerException.class, () -> {
            pictureController.uploadPicture(finalFile, null, mockRequest);
        });

        // 清理大文件
        assertTrue(largeFile.delete());
    }

    @AfterEach
    public void cleanup() {
        logout();
    }
}
