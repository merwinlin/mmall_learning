package com.mmall.controller.backend;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.ICategoryService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by mlin4 on 2019/2/15.
 */
@Controller
@RequestMapping("/manage/category")
public class CategoryManageController {

    @Autowired
    private IUserService IUserService;

    @Autowired
    private ICategoryService iCategoryService;

    @RequestMapping(value = "add_category.do")
    @ResponseBody
    public ServerResponse addCategory(HttpSession session,String categoryName,
                                      @RequestParam(value="parentId",defaultValue = "0") int parentId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        //校验是否是管理员
        ServerResponse response = IUserService.checkAdminRole(user);
        if(response.isSuccess()){
            //是管理员，进行添加category逻辑
            return iCategoryService.addCategory(categoryName,parentId);
        }else{
            return ServerResponse.createByErrorMessage("不是管理员，无权限添加");
        }
    }

    @RequestMapping(value = "set_category_name.do")
    @ResponseBody
    public ServerResponse setCategory(HttpSession session,Integer categoryId,String categoryName){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        if(IUserService.checkAdminRole(user).isSuccess()){
            return iCategoryService.updateCategoryName(categoryId,categoryName);
        }else{
            return ServerResponse.createByErrorMessage("不是管理员，无权限设置");
        }
    }

    /**
     * 查询子节点的category的信息，并且无递归，保持平级
     * @param session
     * @param categoryId
     * @return
     */
    @RequestMapping(value = "get_category.do")
    @ResponseBody
    public ServerResponse getChildrenParallelCategory(HttpSession session,@RequestParam(value = "categoryId",defaultValue = "0") Integer categoryId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        if(IUserService.checkAdminRole(user).isSuccess()){
            return iCategoryService.getChildrenParallelCategory(categoryId);
        }else{
            return ServerResponse.createByErrorMessage("不是管理员，无权限设置");
        }
    }

    @RequestMapping(value = "get_deep_category.do")
    @ResponseBody
    public ServerResponse getCategoryAndDeepChildrenCategory(HttpSession session,@RequestParam(value = "categoryId",defaultValue = "0") Integer categoryId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录");
        }
        if(IUserService.checkAdminRole(user).isSuccess()){
            //查询当前节点的id和递归子节点的id
            return iCategoryService.selectCategoryAndChildrenById(categoryId);
        }else{
            return ServerResponse.createByErrorMessage("不是管理员，无权限设置");
        }
    }
}
