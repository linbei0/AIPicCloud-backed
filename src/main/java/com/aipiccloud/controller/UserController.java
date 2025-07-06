package com.aipiccloud.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.aipiccloud.annotation.AuthCheck;
import com.aipiccloud.common.BaseResponse;
import com.aipiccloud.common.DeleteRequest;
import com.aipiccloud.common.ResultUtils;
import com.aipiccloud.constant.UserConstant;
import com.aipiccloud.exception.BusinessException;
import com.aipiccloud.exception.ErrorCode;
import com.aipiccloud.exception.ThrowUtils;
import com.aipiccloud.utils.EmailUtils;
import com.aipiccloud.manager.upload.FilePictureUpload;
import com.aipiccloud.manager.upload.PictureUploadTemplate;
import com.aipiccloud.model.dto.file.UploadPictureResult;
import com.aipiccloud.model.dto.user.*;
import com.aipiccloud.model.entity.User;
import com.aipiccloud.model.vo.LoginUserVO;
import com.aipiccloud.model.vo.UserVO;
import com.aipiccloud.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EmailUtils emailUtils;

    /**
     * 发送邮箱验证码
     * 60秒内只能获取一次，有效期为5分钟
     */
    @GetMapping("/send-email-code")
    public BaseResponse<Boolean> sendEmailCode(@RequestParam String email, HttpServletRequest request) {
        // 参数校验
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        }
        if (!email.contains("@")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }

        // 60秒内限制重复发送
        String lastSendKey = "email_code_last_send:" + email;
        String lastSendTime = stringRedisTemplate.opsForValue().get(lastSendKey);
        if (lastSendTime != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿频繁发送验证码");
        }

        // 生成6位数字验证码
        String code = RandomUtil.randomNumbers(6);

        // 存储验证码到Redis（5分钟过期）
        String codeKey = "email_code:" + email;
        stringRedisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(5));

        // 设置60秒限制标记
        stringRedisTemplate.opsForValue().set(lastSendKey, "sent", Duration.ofSeconds(60));

        // 发送邮件
        emailUtils.sendVerificationCode(email, code);

        return ResultUtils.success(true);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String emailCode = userRegisterRequest.getEmailCode();
        String codeKey = "email_code:" + userAccount;
        String correctCode = stringRedisTemplate.opsForValue().get(codeKey);

        // 验证验证码
        if (correctCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期或未发送");
        }
        if (!correctCode.equals(emailCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        stringRedisTemplate.delete(codeKey);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 修改密码
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updateUserPassword(String userPassword, HttpServletRequest request) {
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        User loginUser = userService.getLoginUser(request);
        loginUser.setUserPassword(userPassword);
        boolean result = userService.updateById(loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑用户(用户本人)
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        //  仅用户本人有权限，仅能修改用户名和简介
        User loginUser = userService.getLoginUser(request);
        if (user.getId().longValue() != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        user.setUserRole(loginUser.getUserRole());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户头像
     */
    @PostMapping("/update/avatar")
    public BaseResponse<Boolean> updateUserAvatar(@RequestPart("file") MultipartFile multipartFile, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String uploadPathPrefix = String.format("userAvatar/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(multipartFile, uploadPathPrefix);
        String avatar = uploadPictureResult.getUrl();
        User user = new User();
        user.setId(loginUser.getId());
        user.setUserAvatar(avatar);
        boolean result = userService.updateById(user);
        return ResultUtils.success(result);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
}