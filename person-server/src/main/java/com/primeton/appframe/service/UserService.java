package com.primeton.appframe.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.primeton.appframe.common.Constant;
import com.primeton.appframe.common.annotation.ServiceLog;
import com.primeton.appframe.common.pojo.AjaxResult;
import com.primeton.appframe.common.support.DataCache;
import com.primeton.appframe.common.utils.AppUtil;
import com.primeton.appframe.common.utils.CookieUtil;
import com.primeton.appframe.common.utils.DateUtil;
import com.primeton.appframe.common.utils.IPUtil;
import com.primeton.appframe.mapper.AuthRoleMapper;
import com.primeton.appframe.mapper.AuthUserMapper;
import com.primeton.appframe.model.AuthRole;
import com.primeton.appframe.model.AuthUser;

/**
 * 用户管理
 * ClassName: UserService <br/>
 * Function: TODO ADD FUNCTION. <br/>
 * Reason: TODO ADD REASON(可选). <br/>
 * date: 2017年10月17日 下午1:40:08 <br/>
 *
 * @author Jin.He (mailto:hejin@primeton.com)
 * @version
 */
@Service
public class UserService extends AbstratService<AuthUser> {

	@Autowired
	private AuthUserMapper userMapper;
	@Autowired
	private AuthRoleMapper roleMapper;
	@Autowired
	private DataCache dataCache;

	@Override
	@ServiceLog("查询用户列表")
	public List<AuthUser> queryPage(AuthUser user) {
		List<AuthUser> list = userMapper.queryList(user);
		return list;
	}

	@ServiceLog("添加用户")
	public AjaxResult saveUser(AuthUser user) {
		String result = null;
		AuthUser $user = userMapper.queryByUsername(user.getUsername());
		if (null == $user) {
			user.setAddtime(DateUtil.getCurDateTime());
			save(user);
		} else {
			result = "用户名已存在";
		}
		return AppUtil.returnObj(result);
	}

	@ServiceLog("修改用户")
	public AjaxResult updateUser(AuthUser user) {
		String result = null;
		AuthUser $user = userMapper.queryByUsername(user.getUsername());
		if (null != $user && $user.getId() != user.getId()) {
			result = "用户名已存在";
		} else {
			updateByID(user);
		}
		return AppUtil.returnObj(result);
	}

	@ServiceLog("修改个人密码")
	public AjaxResult updatePwd(HttpServletResponse response, HttpServletRequest request, int id, String oldPwd, String newPwd) {
		AuthUser user = null;
		if(StringUtils.isNotEmpty(oldPwd)){
			user = queryByID(id);
			if(!oldPwd.equals(user.getPassword())){
				return new AjaxResult(0, "旧密码不正确");
			}
		}
		user = new AuthUser();
		user.setId(id);
		user.setPassword(newPwd);
		int ret = userMapper.updateByPrimaryKeySelective(user);
		if(ret > 0){
			String sessionId = CookieUtil.get(Constant.SESSION_IDENTITY_KEY, request);
			if (StringUtils.isNotEmpty(sessionId)) {
				dataCache.remove(sessionId);
				String userName = (String) dataCache.getValue(sessionId);
				if (StringUtils.isNotEmpty(userName)) {
					dataCache.remove(userName + IPUtil.getIpAdd(request));
				}
				CookieUtil.delete(Constant.SESSION_IDENTITY_KEY, request, response);
			}
		}
		return new AjaxResult(1, "修改成功");
	}

	@ServiceLog("查询用户列表")
	public List<AuthUser> queryRoleUsers(int roleid) {
		AuthUser user = new AuthUser();
		user.setRoleid(roleid);
		return userMapper.select(user);
	}

	@ServiceLog("角色绑定用户")
	public AjaxResult bindUser(int roleid, Integer[] ids) {
		AuthRole role = roleMapper.queryByRolename(Constant.ROLE_ANONYMOUS);
		//剔除的用户
		List<Map<String, Integer>> userList = new ArrayList<Map<String, Integer>>();
		Map<String, Integer> user = null;
		//该角色原拥有的用户
		List<Integer> userids = userMapper.queryRoleUids(roleid);
		for(int id: userids){
			if(!Arrays.asList(ids).contains(id)){
				user = new HashMap<String, Integer>();
				user.put("id", id);
				user.put("roleid", role.getRoleid());
				userList.add(user);
			}
		}
		dao.batchUpdate("AuthUserMapper.bindUser", userList);
		//新增的用户
		userList = new ArrayList<Map<String, Integer>>();
		for(int id: ids){
			user = new HashMap<String, Integer>();
			user.put("id", id);
			user.put("roleid", roleid);
			userList.add(user);
		}
		dao.batchUpdate("AuthUserMapper.bindUser", userList);
		return AppUtil.returnObj(null);
	}

}
