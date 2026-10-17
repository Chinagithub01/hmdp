package com.hmdp.utils;

public class SystemConstants {
    public static final String IMAGE_UPLOAD_DIR = "D:\\lesson\\nginx-1.18.0\\html\\hmdp\\imgs\\";
    public static final String USER_NICK_NAME_PREFIX = "user_";
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10;

    public static final int USER_CODE_LENGTH = 6;//验证码长度
    public static final int USER_NICK_NAME_LENGTH = 10;//用户昵称随机字符串长度
    public static final String TOKEN_KEY = "authorization";//前端传来的请求头中token的key
}
