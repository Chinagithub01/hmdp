package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;//BeanUtil和BeanUtils不同
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service // 使用mybatis-plus需要继承ServiceImpl<Mapper, Entity>
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送短信验证码
     * @param phone
     * @param session
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 验证手机号是否规范(boolean和Boolean不同)
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail(ExceptionConstant.PHONE_INVALID_EXCEPTION);
        }
        // 生成验证码
        String code = RandomUtil.randomNumbers(SystemConstants.USER_CODE_LENGTH);
        // 保存验证码到redis，需要设置有效期
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY+phone, code,
                RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 发送验证码（控制台模拟）
        log.debug("验证码发送成功："+ code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 验证手机号格式
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail(ExceptionConstant.PHONE_INVALID_EXCEPTION);
        }
        // 校验验证码的格式
        if(RegexUtils.isCodeInvalid(loginForm.getCode())){
            return Result.fail(ExceptionConstant.CODE_INVALID_EXCEPTION);
        }

        // 获取redis中该手机号对应的验证码
        String code = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if(code==null || !code.equals(loginForm.getCode())){
            return Result.fail(ExceptionConstant.PHONE_OR_CODE_FALSE_EXCEPTION);
        }
        // 从数据库查询该用户是否存在
        User user = query().eq("phone", phone).one();
        if(user == null){// 不存在则创建该用户
            user = createUserWithPhone(phone);
        }


        // 3.将用户存到redis
        // 3.1将用户信息放到map，使用lambda表达式将userDTO的id转为String类型
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().
                        setIgnoreNullValue(true)
                        .setFieldValueEditor(
                                (fieldName, fieldValue)-> fieldValue.toString()
                        ));
        // 3.2随机生成token作为登录令牌
        String token = String.valueOf(UUID.randomUUID());

        // 3.3保存用户登录信息到redis，设置有效期
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, map);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);

        // 将该用户登录token返回给客户端
        log.info("登录成功");
        return Result.ok(token);
    }

    @Override
    public Result logout() {
        UserDTO userDTO = UserHolder.getUser();
        return Result.ok();
    }

    /**
     * 创建用户
     */
    private User createUserWithPhone(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX
                + RandomUtil.randomString(SystemConstants.USER_NICK_NAME_LENGTH));
        save(user);

        return user;
    }


}
