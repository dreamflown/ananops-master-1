/*
 * Copyright (c) 2018. ananops.net All Rights Reserved.
 * 项目名称：ananops快速搭建企业级分布式微服务平台
 * 类名称：MdcAddressQueryFeignClient.java
 * 创建人：刘兆明
 * 联系方式：ananops.net@gmail.com
 * 开源地址: https://github.com/ananops
 * 博客地址: http://blog.ananops.net
 * 项目官网: http://ananops.net
 */

package com.ananops.provider.web.rpc;

import com.ananops.PublicUtil;
import com.ananops.core.support.BaseController;
import com.ananops.provider.model.domain.MdcAddress;
import com.ananops.provider.model.dto.AddressDTO;
import com.ananops.provider.service.MdcAddressQueryFeignApi;
import com.ananops.provider.service.MdcAddressService;
import com.ananops.wrapper.WrapMapper;
import com.ananops.wrapper.Wrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * The class Mdc product query feign client.
 *
 * @author ananops.net @gmail.com
 */
@RefreshScope
@RestController
@Api(value = "API - MdcAddressQueryFeignClient", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class MdcAddressQueryFeignClient extends BaseController implements MdcAddressQueryFeignApi {

	@Resource
	private MdcAddressService mdcAddressService;

	/**
	 * 根据ID获取地址信息.
	 *
	 * @param addressId the address id
	 *
	 * @return the by id
	 */
	@Override
	@ApiOperation(httpMethod = "POST", value = "根据ID获取地址信息")
	public Wrapper<AddressDTO> getById(@PathVariable("addressId") Long addressId) {
		logger.info("根据ID获取地址信息 addressId={}", addressId);
		AddressDTO addressDTO = null;
		MdcAddress mdcAddress = mdcAddressService.selectByKey(addressId);
		if (PublicUtil.isNotEmpty(mdcAddress)) {
			addressDTO = new AddressDTO();
			BeanUtils.copyProperties(mdcAddress, addressDTO);
		}
		return WrapMapper.ok(addressDTO);
	}
}
