package com.ananops.provider.mapper;

import com.ananops.core.mybatis.MyMapper;
import com.ananops.provider.model.domain.UacRoleUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The interface Uac role user mapper.
 *
 * @author ananops.net@gmail.com
 */
@Mapper
@Component
public interface UacRoleUserMapper extends MyMapper<UacRoleUser> {

	/**
	 * Delete exclude super mng int.
	 *
	 * @param currentRoleId the current role id
	 * @param superRoleId   the super role id
	 *
	 * @return the int
	 */
	int deleteExcludeSuperMng(@Param("currentRoleId") Long currentRoleId, @Param("superRoleId") Long superRoleId);

	/**
	 * Gets by user id and role id.
	 *
	 * @param userId the user id
	 * @param roleId the role id
	 *
	 * @return the by user id and role id
	 */
	UacRoleUser getByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

	/**
	 * List by user id list.
	 *
	 * @param userId the user id
	 *
	 * @return the list
	 */
	List<UacRoleUser> listByUserId(@Param("userId") Long userId);

	/**
	 * List by role id list.
	 *
	 * @param roleId the role id
	 *
	 * @return the list
	 */
	List<UacRoleUser> listByRoleId(@Param("roleId") Long roleId);

	/**
	 * List super user list.
	 *
	 * @param roleId the role id
	 *
	 * @return the list
	 */
	List<Long> listSuperUser(@Param("roleId") Long roleId);

	/**
	 * List by role id list list.
	 *
	 * @param idList the id list
	 *
	 * @return the list
	 */
	List<UacRoleUser> listByRoleIdList(@Param("roleIds") List<Long> idList);

	/**
	 * Delete by role id int.
	 *
	 * @param roleId the role id
	 *
	 * @return the int
	 */
	int deleteByRoleId(@Param("roleId") Long roleId);

	/**
	 * Delete by role id list int.
	 *
	 * @param roleIdList the role id list
	 *
	 * @return the int
	 */
	int deleteByRoleIdList(@Param("roleIdList") List<Long> roleIdList);
}