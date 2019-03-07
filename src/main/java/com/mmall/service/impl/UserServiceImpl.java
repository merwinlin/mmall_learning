package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by mlin4 on 2019/2/13.
 */
@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        //md5 encode
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username,md5Password);
        if(user == null){
            return ServerResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功",user);
    }

    public ServerResponse<String> register(User user){

        ServerResponse validResponse = this.checkVaild(user.getUsername(),Const.USERNAME);
        if(!validResponse.isSuccess()){
            return validResponse;
        }
        validResponse = this.checkVaild(user.getEmail(),Const.EMAIL);
        if(!validResponse.isSuccess()){
            return validResponse;
        }

        user.setRole(Const.Role.ROLE_CUSTOMER);
        //MD5加密（需要写一个MD5工具类MD5Util）
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        int resultCount = userMapper.insert(user);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("register fail");
        }

        return ServerResponse.createBySuccessMessage("register success");
    }

    public ServerResponse<String> checkVaild(String str, String type){
        if(StringUtils.isNoneBlank(type)){
            //start check
            if(Const.USERNAME.equals(type)){
                int resultCount = userMapper.checkUsername(str);
                if(resultCount > 0){
                    return ServerResponse.createByErrorMessage("username exist");
                }
            }
            if(Const.EMAIL.equals(type)){
                int resultCount = userMapper.checkEmail(str);
                if(resultCount > 0){
                    return ServerResponse.createByErrorMessage("email exist");
                }
            }
        } else {
            return ServerResponse.createByErrorMessage("param error");
        }
        return ServerResponse.createBySuccessMessage("check success");
    }

    public ServerResponse<String> selectQuestion(String username){
        ServerResponse validResponse = this.checkVaild(username,Const.USERNAME);
        if(validResponse.isSuccess()){
            //用户不存在
            return ServerResponse.createByErrorMessage("user not exist");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if(StringUtils.isNoneBlank(question)){
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    public ServerResponse<String> checkAnswer(String username,String question,String answer){
        int resultCount = userMapper.checkAnswer(username,question,answer);
        if(resultCount > 0){
            //答案正确
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username,forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMessage("answer error");
    }

    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken){
        if(StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("参数错误，需要传递token");
        }
        ServerResponse validResponse = this.checkVaild(username,Const.USERNAME);
        if(validResponse.isSuccess()){
            //用户不存在
            return ServerResponse.createByErrorMessage("user not exist");
        }

        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        if(StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("token invalid or 过期");
        }

        //这种equal写法好
        if(StringUtils.equals(forgetToken,token)){
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.updatePasswordByUsername(username,md5Password);
            //生效行数
            if(rowCount > 0){
                return ServerResponse.createBySuccessMessage("update password success");
            }
        }else{
            return ServerResponse.createByErrorMessage("token error,please re-get token");
        }
        return ServerResponse.createByErrorMessage("update password fail");
    }

    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,User user){
        //防止横向越权，要校验一下这个用户的旧密码，一定要指定是这个用户，因为我们会查询一个count(1),
        // 如果不指定id，那么查询结果就是true
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("old password error");
        }

        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0){
            return ServerResponse.createBySuccessMessage("update password success");
        }
        return ServerResponse.createByErrorMessage("update password fail");
    }

    public ServerResponse<User> updateInformation(User user){
        //username是不能被更新的
        //email也要校验，校验新的email是否已存在
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if(resultCount > 0){
            return ServerResponse.createByErrorMessage("email exist,please change the email");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0){
            return ServerResponse.createBySuccess("update information success",updateUser);
        }
        return ServerResponse.createByErrorMessage("update information fail");
    }

    public ServerResponse<User> getInformation(Integer userId){
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null){
            return ServerResponse.createByErrorMessage("找不到用户");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    //backend

    /**
     * 校验是否为管理员
     * @param user
     * @return
     */
    public ServerResponse checkAdminRole(User user){
        if(user != null && user.getRole().equals(Const.Role.ROLE_ADMIN)){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
}


