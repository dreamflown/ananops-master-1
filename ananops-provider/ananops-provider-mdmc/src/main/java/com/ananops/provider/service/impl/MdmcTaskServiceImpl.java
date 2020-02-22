package com.ananops.provider.service.impl;


import com.ananops.PublicUtil;
import com.ananops.base.constant.GlobalConstant;
import com.ananops.base.dto.LoginAuthDto;
import com.ananops.base.enums.ErrorCodeEnum;
import com.ananops.base.exception.BusinessException;
import com.ananops.core.support.BaseService;

import com.ananops.provider.mapper.*;
import com.ananops.provider.model.domain.*;
import com.ananops.provider.model.dto.*;
import com.ananops.provider.model.dto.attachment.OptAttachmentUpdateReqDto;
import com.ananops.provider.model.dto.attachment.OptUploadFileByteInfoReqDto;
import com.ananops.provider.model.dto.oss.*;
import com.ananops.provider.model.dto.user.UserInfoDto;
import com.ananops.provider.model.enums.*;
import com.ananops.provider.model.service.UacGroupBindUserFeignApi;
import com.ananops.provider.model.service.UacUserFeignApi;
import com.ananops.provider.model.vo.CompanyVo;
import com.ananops.provider.service.*;
import com.github.pagehelper.PageHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.xiaoleilu.hutool.io.FileTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class MdmcTaskServiceImpl extends BaseService<MdmcTask> implements MdmcTaskService {
    @Resource
    MdmcTaskMapper taskMapper;

    @Resource
    MdmcTaskLogMapper taskLogMapper;

    @Resource
    MdmcTaskItemService taskItemService;

    @Resource
    ImcItemFeignApi imcItemFeignApi;

    @Resource
    OpcOssFeignApi opcOssFeignApi;

    @Resource
    UacGroupBindUserFeignApi uacGroupBindUserFeignApi;

    @Resource
    MdmcFileTaskStatusMapper fileTaskStatusMapper;

    @Resource
    MdmcTroubleTypeMapper troubleTypeMapper;

    @Resource
    MdmcTroubleAddressMapper troubleAddressMapper;

    @Resource
    UacUserFeignApi uacUserFeignApi;

    @Resource
    SpcCompanyFeignApi spcCompanyFeignApi;

    @Resource
    SpcEngineerFeignApi spcEngineerFeignApi;

    @Resource
    PmcProjectFeignApi pmcProjectFeignApi;

    @Override
    public MdmcTask getTaskByTaskId(Long taskId) {
        return taskMapper.selectByPrimaryKey(taskId);
    }

    @Override
    public MdmcTaskDetailDto getTaskDetail(Long taskId) {
        MdmcTaskDetailDto taskDetailDto=new MdmcTaskDetailDto();
        MdmcTask mdmcTask=taskMapper.selectByPrimaryKey(taskId);
        taskDetailDto.setMdmcTask(mdmcTask);
        UserInfoDto userInfoDto=uacUserFeignApi.getUacUserById(mdmcTask.getUserId()).getResult();
        UserInfoDto principalInfoDto=uacUserFeignApi.getUacUserById(mdmcTask.getPrincipalId()).getResult();
        CompanyVo companyVo=spcCompanyFeignApi.getCompanyDetailsById(mdmcTask.getFacilitatorId()).getResult();
        EngineerDto engineerDto=spcEngineerFeignApi.getEngineerById(mdmcTask.getMaintainerId()).getResult();
        PmcProjectDto pmcProjectDto=pmcProjectFeignApi.getProjectByProjectId(mdmcTask.getProjectId()).getResult();
        taskDetailDto.setCompanyVo(companyVo);
        taskDetailDto.setEngineerDto(engineerDto);
        taskDetailDto.setPmcProjectDto(pmcProjectDto);
        taskDetailDto.setPrincipalInfoDto(principalInfoDto);
        taskDetailDto.setUserInfoDto(userInfoDto);

        return taskDetailDto;
    }

    @Override
    public MdmcAddTaskDto saveTask(MdmcAddTaskDto mdmcAddTaskDto, LoginAuthDto loginAuthDto) {

        MdmcTask task = new MdmcTask();
        copyPropertiesWithIgnoreNullProperties(mdmcAddTaskDto,task);
        task.setUpdateInfo(loginAuthDto);
        List<Long> attachmentIdList=mdmcAddTaskDto.getAttachmentIdList();

        if(mdmcAddTaskDto.getId()==null){
            logger.info("创建一条维修工单记录... CrateTaskInfo = {}", mdmcAddTaskDto);

            Long taskId = super.generateId();
            task.setId(taskId);
            task.setStatus(2);
            taskMapper.insert(task);
            logger.info("新创建一条维修任务记录成功[OK], 创建维修任务子项中...");
            if (attachmentIdList != null && !attachmentIdList.isEmpty()){
                Long refNo=super.generateId();
                for(Long attachmentId:attachmentIdList){
                    OptAttachmentUpdateReqDto optAttachmentUpdateReqDto=new OptAttachmentUpdateReqDto();
                    optAttachmentUpdateReqDto.setId(attachmentId);
                    optAttachmentUpdateReqDto.setLoginAuthDto(loginAuthDto);
                    optAttachmentUpdateReqDto.setRefNo(String.valueOf(refNo));
                    opcOssFeignApi.updateAttachmentInfo(optAttachmentUpdateReqDto);
                }

                MdmcFileTaskStatus fileTaskStatus = new MdmcFileTaskStatus();
                fileTaskStatus.setId(refNo);
                fileTaskStatus.setStatus(2);
                fileTaskStatus.setTaskId(taskId);
                fileTaskStatus.setUpdateInfo(loginAuthDto);
                fileTaskStatusMapper.insert(fileTaskStatus);

            }

            //获取所有的巡检任务子项
            List<MdmcAddTaskItemDto> mdmcAddTaskItemDtoList = mdmcAddTaskDto.getMdmcAddTaskItemDtoList();
            mdmcAddTaskItemDtoList.forEach(taskItem->{
                //设置任务子项对应的任务id
                taskItem.setTaskId(taskId);
                //创建新的任务子项，并更新返回结果
                MdmcTaskItem item = taskItemService.saveItem(taskItem,loginAuthDto);
                BeanUtils.copyProperties(item ,taskItem);
            });

            //更新返回结果
            BeanUtils.copyProperties(task,mdmcAddTaskDto);
            BeanUtils.copyProperties(mdmcAddTaskItemDtoList,mdmcAddTaskDto);

            logger.info("创建维修工单成功[OK] TaskDetail = {}", mdmcAddTaskDto);

        } else {
            logger.info("编辑/修改维修工单详情... UpdateInfo = {}", mdmcAddTaskDto);

            Long taskId = mdmcAddTaskDto.getId();
            MdmcTask t =taskMapper.selectByPrimaryKey(taskId);
            if (t == null) {//如果没有此任务
                throw new BusinessException(ErrorCodeEnum.MDMC9998098,taskId);
            }

            // 更新工单信息和状态
            taskMapper.updateByPrimaryKeySelective(task);

            Integer status = task.getStatus();
            Integer objectType = task.getObjectType();

            if (!attachmentIdList.isEmpty()){
                Long refNo1=super.generateId();

                for (Long attachmentId:attachmentIdList){
                    OptAttachmentUpdateReqDto attachmentUpdateReqDto=new OptAttachmentUpdateReqDto();
                    attachmentUpdateReqDto.setId(attachmentId);
                    attachmentUpdateReqDto.setLoginAuthDto(loginAuthDto);
                    attachmentUpdateReqDto.setRefNo(String.valueOf(refNo1));
                    opcOssFeignApi.updateAttachmentInfo(attachmentUpdateReqDto);
                }

                MdmcFileTaskStatus mdmcFileTaskStatus = new MdmcFileTaskStatus();
                mdmcFileTaskStatus.setId(refNo1);
                mdmcFileTaskStatus.setStatus(task.getStatus());
                mdmcFileTaskStatus.setTaskId(taskId);
                mdmcFileTaskStatus.setUpdateInfo(loginAuthDto);
                fileTaskStatusMapper.insert(mdmcFileTaskStatus);
            }

            // 维修工单完成(无需评价)时，如果是巡检发起的维修任务，更新巡检工单状态
            if(status == MdmcTaskStatusEnum.DaiPingJia.getStatusNum() && objectType == MdmcObjectTypeEnum.IMC.getCode()) {
                Long imcItemId = task.getObjectId();
                if(imcItemId != null) {

                    ImcItemChangeStatusDto imcItemChangeStatusDto = new ImcItemChangeStatusDto();
                    imcItemChangeStatusDto.setLoginAuthDto(loginAuthDto);
                    imcItemChangeStatusDto.setStatus(4);
                    imcItemChangeStatusDto.setItemId(imcItemId);
                    imcItemFeignApi.modifyImcItemStatus(imcItemChangeStatusDto);
                    logger.info("用户负责人支付完成，更新巡检状态");

                }
            }

            // 更新返回结果
            BeanUtils.copyProperties(task,mdmcAddTaskDto);

            logger.info("编辑/修改维修工单成功[OK] Task = {}", task);

        }

        MdmcTaskLog taskLog=new MdmcTaskLog();
        Long taskLogId = super.generateId();
        taskLog.setUpdateInfo(loginAuthDto);
        taskLog.setId(taskLogId);
        taskLog.setTaskId(task.getId());
        taskLog.setStatus(task.getStatus());
        taskLog.setMovement(MdmcTaskStatusEnum.getStatusMsg(task.getStatus()));
        taskLogMapper.insert(taskLog);

        logger.info("记录维修工单操作日志[OK] TaskLog = {}", taskLog);

        return mdmcAddTaskDto;
    }

    @Override
    public MdmcAddTroubleInfoDto saveTroubleList(MdmcAddTroubleInfoDto addTroubleInfoDto, LoginAuthDto loginAuthDto) {
        Long userId=addTroubleInfoDto.getUserId();
        Long groupId=uacGroupBindUserFeignApi.getGroupIdByUserId(userId).getResult();

        if (groupId==null){
            throw new BusinessException(ErrorCodeEnum.UAC10015010,groupId);
        }
        List<String> troubleTypeList=addTroubleInfoDto.getTroubleTypeList();
        List<MdmcTroubleAddressDto> troubleAddrssList=addTroubleInfoDto.getTroubleAddressList();
        if (troubleAddrssList.isEmpty() && troubleTypeList.isEmpty()){
            throw  new BusinessException(ErrorCodeEnum.MDMC99980005);
        }


        if (!troubleTypeList.isEmpty()){
            for (String troubleType:troubleTypeList){
                MdmcTroubleType mdmcTroubleType=new MdmcTroubleType();
                Long typeId=super.generateId();
                mdmcTroubleType.setId(typeId);
                mdmcTroubleType.setGroupId(groupId);
                mdmcTroubleType.setTroubleType(troubleType);
                troubleTypeMapper.insert(mdmcTroubleType);
            }
        }
        if (!troubleAddrssList.isEmpty()){
            for (MdmcTroubleAddressDto troubleAddressDto:troubleAddrssList){
                MdmcTroubleAddress mdmcTroubleAddress=new MdmcTroubleAddress();
                Long addressId=super.generateId();
                mdmcTroubleAddress.setId(addressId);
                mdmcTroubleAddress.setGroupId(groupId);
                copyPropertiesWithIgnoreNullProperties(troubleAddressDto,mdmcTroubleAddress);
                troubleAddressMapper.insert(mdmcTroubleAddress);
            }
        }

        return addTroubleInfoDto;
    }

    @Override
    public MdmcAddTroubleInfoDto getTroubleList(Long userId) {
        Long groupId=uacGroupBindUserFeignApi.getGroupIdByUserId(userId).getResult();
        MdmcAddTroubleInfoDto mdmcAddTroubleInfoDto=new MdmcAddTroubleInfoDto();
        mdmcAddTroubleInfoDto.setUserId(userId);
        Example example=new Example(MdmcTroubleAddress.class);
        Example.Criteria criteria=example.createCriteria();
        criteria.andEqualTo("groupId",groupId);
        List<MdmcTroubleAddressDto> troubleAddressDtoList=new ArrayList<>();
        List<MdmcTroubleAddress> troubleAddressList=troubleAddressMapper.selectByExample(example);
        if (!troubleAddressList.isEmpty()){
        for (MdmcTroubleAddress troubleAddress:troubleAddressList){
            MdmcTroubleAddressDto troubleAddressDto=new MdmcTroubleAddressDto();
            BeanUtils.copyProperties(troubleAddress,troubleAddressDto);
            troubleAddressDtoList.add(troubleAddressDto);
        }
        }
      //else{添加默认列表}

        mdmcAddTroubleInfoDto.setTroubleAddressList(troubleAddressDtoList);
        Example example1=new Example(MdmcTroubleType.class);
        Example.Criteria criteria1=example1.createCriteria();
        criteria1.andEqualTo("groupId",groupId);
        List<String> troubleTypeList=new ArrayList<>();
        List<MdmcTroubleType> mdmcTroubleTypeList=troubleTypeMapper.selectByExample(example1);
        if (!mdmcTroubleTypeList.isEmpty()){
            for (MdmcTroubleType troubleType:mdmcTroubleTypeList){
                troubleTypeList.add(troubleType.getTroubleType());
            }
        }
        //else{添加默认列表}
        mdmcAddTroubleInfoDto.setTroubleTypeList(troubleTypeList);
        return mdmcAddTroubleInfoDto;

    }

    @Override
    public MdmcTaskDto modifyTask(MdmcTaskDto mdmcTaskDto) {
        logger.info("修改维修工单详情... UpdateInfo = {}", mdmcTaskDto);

        MdmcTask task = new MdmcTask();
        copyPropertiesWithIgnoreNullProperties(mdmcTaskDto,task);
        Long taskId = mdmcTaskDto.getId();
        if (taskId==null){
            throw new BusinessException(ErrorCodeEnum.MDMC9998098,taskId);
        }
        Example example = new Example(MdmcTask.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("id",taskId);
        List<MdmcTask> taskList =taskMapper.selectByExample(example);
        if(taskList.size()==0){//如果没有此任务
            throw new BusinessException(ErrorCodeEnum.MDMC9998098,taskId);
        }
        //如果当前是更新一条记录
//            String key = RedisKeyUtil.createMqKey(topic,tag,String.valueOf(task.getId()),body);
//            mqMessageData = new MqMessageData(body, topic, tag, key);
//            taskManager.saveTask(mqMessageData,task,false);
        taskMapper.updateByPrimaryKeySelective(task);
        //更新返回结果
        BeanUtils.copyProperties(task,mdmcTaskDto);

        logger.info("修改维修工单详情成功[OK] Task = {}", task);
        return mdmcTaskDto;
    }


    @Override
    public MdmcTask modifyTaskStatus(MdmcChangeStatusDto changeStatusDto,LoginAuthDto loginAuthDto) {
        logger.info("更新维修工单状态... UpdateInfo = {}", changeStatusDto);

        if(loginAuthDto == null) {
            logger.error("警告：匿名用户正在操作！");
        }

        Long taskId = changeStatusDto.getTaskId();
        MdmcTask task = getTaskByTaskId(taskId);
        if(task == null){//如果当前任务不存在
            throw new BusinessException(ErrorCodeEnum.MDMC9998098,taskId);
        }

        Integer status = changeStatusDto.getStatus();
        if (status == MdmcTaskStatusEnum.QuXiao.getStatusNum()){          // 工单终止
            logger.info("当前维修工单已被终止[Terminal] Task = {}", task);
            taskMapper.deleteByPrimaryKey(taskId);
        } else {
            task.setStatus(status);
            task.setUpdateInfo(loginAuthDto);
//        // 可靠服务
//        MqMessageData mqMessageData;
//        String body = JSON.toJSONString(changeStatusDto);
//        String topic = AliyunMqTopicConstants.MqTagEnum.MODIFY_INSPECTION_TASK_STATUS.getTopic();
//        String tag = AliyunMqTopicConstants.MqTagEnum.MODIFY_INSPECTION_TASK_STATUS.getTag();
//        String key = RedisKeyUtil.createMqKey(topic,tag,String.valueOf(task.getId()),body);
//        mqMessageData = new MqMessageData(body, topic, tag, key);
//        taskManager.modifyTaskStatus(mqMessageData,task);
            taskMapper.updateByPrimaryKey(task);

            // 如果是巡检发起的维修任务，更新巡检工单状态
            Integer objectType = task.getObjectType();
            if (status == MdmcTaskStatusEnum.WanCheng.getStatusNum() && objectType == MdmcObjectTypeEnum.IMC.getCode()) {
                Long imcItemId = task.getObjectId();
                if (imcItemId != null) {

                    ImcItemChangeStatusDto imcItemChangeStatusDto = new ImcItemChangeStatusDto();
                    imcItemChangeStatusDto.setLoginAuthDto(loginAuthDto);
                    imcItemChangeStatusDto.setStatus(4);
                    imcItemChangeStatusDto.setItemId(imcItemId);
                    imcItemFeignApi.modifyImcItemStatus(imcItemChangeStatusDto);
                    logger.info("维修工单完成，更新巡检状态");

                }
            }
            logger.info("更新维修工单状态成功[OK] Task = {}", task);
        }

        // 记录维修工单状态更新
        MdmcTaskLog taskLog=new MdmcTaskLog();
        Long taskLogId = super.generateId();
        taskLog.setUpdateInfo(loginAuthDto);
        taskLog.setId(taskLogId);
        taskLog.setTaskId(taskId);
        taskLog.setStatus(status);
        taskLog.setMovement(MdmcTaskStatusEnum.getStatusMsg(status));
        taskLogMapper.insert(taskLog);

        logger.info("记录维修工单操作日志[OK] TaskLog = {}", taskLog);

        return task;
    }

    @Override
    public List<MdmcTask> getTaskListByStatus(MdmcStatusDto statusDto) {
        Example example = new Example(MdmcTask.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("status",statusDto.getStatus());
        if(taskMapper.selectCountByExample(example)==0){
            throw new BusinessException(ErrorCodeEnum.MDMC9998098);
        }
        PageHelper.startPage(statusDto.getPageNum(),statusDto.getPageSize());
        return taskMapper.selectByExample(example);
    }

    @Override
    public List<MdmcTask> getTaskList(MdmcStatusDto statusDto) {

        PageHelper.startPage(statusDto.getPageNum(),statusDto.getPageSize());
        return taskMapper.selectAll();
    }

    @Override
    public List<MdmcTask> getTaskListByIdAndStatus(MdmcQueryDto queryDto) {
        // todo 分页查询
//        String roleCode=queryDto.getRoleCode();
        Long id=queryDto.getId();
        Integer status=queryDto.getStatus();
//        Example example = new Example(MdmcTask.class);
//        Example.Criteria criteria = example.createCriteria();
//        if (status!=null){
//        criteria.andEqualTo("status",queryDto.getStatus());
//        }
//
//        switch (roleCode){
//            case "user_watcher":criteria.andEqualTo("userId",id);break;
//            case "user_manager":criteria.andEqualTo("principalId",id);break;
//            case "engineer":criteria.andEqualTo("maintainerId",id);break;
//            case "fac_service":criteria.andEqualTo("facilitatorId",id);break;
//            case "fac_manager":criteria.andEqualTo("facilitatorId",id);break;
//            default: throw new BusinessException(ErrorCodeEnum.UAC10012008,roleCode);
//        }
//        if(taskMapper.selectCountByExample(example)==0){
//            throw new BusinessException(ErrorCodeEnum.MDMC9998098);
//        }
//        PageHelper.startPage(queryDto.getPageNum(),queryDto.getPageSize());

//        return taskMapper.selectByExample(example);

        return getTaskListByUserIdAndStatusOptional(id, status);
    }

    @Override
    public List<MdmcListDto> getTaskListByIdAndStatusArrary(MdmcStatusArrayDto statusArrayDto) {
//        String roleCode=statusArrayDto.getRoleCode();
        Long id=statusArrayDto.getId();
        Integer[] status=statusArrayDto.getStatus();
//        Example example = new Example(MdmcTask.class);
//        Example.Criteria criteria = example.createCriteria();
        List<MdmcListDto> listDtoList=new ArrayList<>();
//        switch (roleCode){
//            case "user_watcher":criteria.andEqualTo("userId",id);break;
//            case "user_manager":criteria.andEqualTo("principalId",id);break;
//            case "engineer":criteria.andEqualTo("maintainerId",id);break;
//            case "fac_service":criteria.andEqualTo("facilitatorId",id);break;
//            case "fac_manager":criteria.andEqualTo("facilitatorId",id);break;
//            default: throw new BusinessException(ErrorCodeEnum.UAC10012008,roleCode);
//        }
        if (status!=null){
            for (Integer i:status){
//                Example example1 = new Example(MdmcTask.class);
//                Example.Criteria criteria1 = example1.createCriteria();
//                criteria1.andEqualTo("status",i);
//                switch (roleCode){
//                    case "user_watcher":criteria1.andEqualTo("userId",id);break;
//                    case "user_manager":criteria1.andEqualTo("principalId",id);break;
//                    case "engineer":criteria1.andEqualTo("maintainerId",id);break;
//                    case "fac_service":criteria1.andEqualTo("facilitatorId",id);break;
//                    case "fac_manager":criteria.andEqualTo("facilitatorId",id);break;
//                    default: throw new BusinessException(ErrorCodeEnum.UAC10012008,roleCode);
//                }
                MdmcListDto listDto=new MdmcListDto();
                listDto.setStatus(i);
//                listDto.setTaskList(taskMapper.selectByExample(example1));
                listDto.setTaskList(getTaskListByUserIdAndStatusOptional(id, i));
                listDtoList.add(listDto);
            }

        } else{
            MdmcListDto listDto1=new MdmcListDto();
//            listDto1.setTaskList(taskMapper.selectByExample(example));
            listDto1.setTaskList(getTaskListByUserIdAndStatusOptional(id, null));
            listDtoList.add(listDto1);
        }

        PageHelper.startPage(statusArrayDto.getPageNum(),statusArrayDto.getPageSize());
        return listDtoList;
    }

    @Override
    public MdmcPageDto getTaskListByPage(MdmcQueryDto queryDto) {
//        String roleCode=queryDto.getRoleCode();
        Long id=queryDto.getId();
        Integer status=queryDto.getStatus();
//        Example example = new Example(MdmcTask.class);
//        Example.Criteria criteria = example.createCriteria();
//        if (status!=null){
//            criteria.andEqualTo("status",queryDto.getStatus());
//        }
//        switch (roleCode){
//            case "user_watcher":criteria.andEqualTo("userId",id);break;
//            case "user_manager":criteria.andEqualTo("principalId",id);break;
//            case "engineer":criteria.andEqualTo("maintainerId",id);break;
//            case "fac_service":criteria.andEqualTo("facilitatorId",id);break;
//            case "fac_manager":criteria.andEqualTo("facilitatorId",id);break;
//            default: throw new BusinessException(ErrorCodeEnum.UAC10012008,roleCode);
//        }
//        if(taskMapper.selectCountByExample(example)==0){
//            throw new BusinessException(ErrorCodeEnum.MDMC9998098);
//        }
        PageHelper.startPage(queryDto.getPageNum(),queryDto.getPageSize());
        MdmcPageDto pageDto=new MdmcPageDto();
//        pageDto.setTaskList(taskMapper.selectByExample(example));
        pageDto.setTaskList(getTaskListByUserIdAndStatusOptional(id, status));
        pageDto.setPageNum(queryDto.getPageNum());
        pageDto.setPageSize(queryDto.getPageSize());

        return pageDto;

    }

    public List<MdmcTask> getTaskListByUserIdAndStatusOptional(Long userId, Integer status) {
        String roleCode=uacUserFeignApi.getUacUserById(userId).getResult().getRoleCode();
        if(status == null) {
            List<MdmcTask> taskList=taskMapper.selectBySomeoneId(userId);
            List<MdmcTask> taskList1=new ArrayList<>();
            if (roleCode.equals("fac_service")||roleCode.equals("fac_manager")||roleCode.equals("fac_leader")){
                for (MdmcTask task:taskList){
                    if (task.getStatus()>2)
                        taskList1.add(task);
                }
                return taskList1;
            }
            else if(roleCode.equals("engineer")){
                for (MdmcTask task:taskList){
                    if (task.getStatus()>4 && task.getStatus()!=14)
                        taskList1.add(task);
                }
                return  taskList1;
            }
            return taskList;
        } else {
            return taskMapper.selectBySomeoneIdAndStatus(userId, status);
        }
    }


    @Override
    public MdmcTask modifyMaintainer(MdmcChangeMaintainerDto mdmcChangeMaintainerDto){
        LoginAuthDto loginAuthDto = mdmcChangeMaintainerDto.getLoginAuthDto();
        Long taskId = mdmcChangeMaintainerDto.getTaskId();
        Long maintainerId = mdmcChangeMaintainerDto.getMaintainerId();
        Example example = new Example(MdmcTask.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("id",taskId);
        if(taskMapper.selectCountByExample(example) == 0){//如果当前任务不存在
            throw new BusinessException(ErrorCodeEnum.MDMC9998098);
        }
        MdmcTask mdmcTask = taskMapper.selectByExample(example).get(0);
        mdmcTask.setMaintainerId(maintainerId);
        mdmcTask.setUpdateInfo(loginAuthDto);
        taskMapper.updateByPrimaryKey(mdmcTask);
        return mdmcTask;
    }


    @Override
    public MdmcChangeStatusDto refuseTaskByMaintainer(RefuseMdmcTaskDto refuseMdmcTaskDto){
        Long taskId = refuseMdmcTaskDto.getTaskId();
        LoginAuthDto loginAuthDto = refuseMdmcTaskDto.getLoginAuthDto();
        MdmcChangeStatusDto mdmcChangeStatusDto = new MdmcChangeStatusDto();
        Example example = new Example(MdmcTask.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("id",taskId);
        if(taskMapper.selectCountByExample(example) == 0){//如果当前任务不存在
            throw new BusinessException(ErrorCodeEnum.MDMC9998098);
        }
        MdmcTask mdmcTask = taskMapper.selectByExample(example).get(0);
        if(mdmcTask.getStatus()==MdmcTaskStatusEnum.JieDan3.getStatusNum()){
            //如果当前任务的状态处于“已分配维修工，待维修工接单”，工程师才能拒单
            mdmcChangeStatusDto.setLoginAuthDto(loginAuthDto);
            mdmcChangeStatusDto.setStatus(MdmcTaskStatusEnum.Reject2.getStatusNum());
            mdmcChangeStatusDto.setStatusMsg(MdmcTaskStatusEnum.Reject2.getStatusMsg());
            this.modifyTaskStatus(mdmcChangeStatusDto,loginAuthDto);

        }else{
            throw new BusinessException(ErrorCodeEnum.MDMC9998087);
        }
        return mdmcChangeStatusDto;
    }


    @Override
    public MdmcChangeStatusDto refuseTaskByFacilitator(RefuseMdmcTaskDto refuseMdmcTaskDto){
        Long taskId = refuseMdmcTaskDto.getTaskId();
        LoginAuthDto loginAuthDto = refuseMdmcTaskDto.getLoginAuthDto();
        MdmcChangeStatusDto mdmcChangeStatusDto = new MdmcChangeStatusDto();
        Example example = new Example(MdmcTask.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("id",taskId);
        if(taskMapper.selectCountByExample(example) == 0){//如果当前任务不存在
            throw new BusinessException(ErrorCodeEnum.MDMC9998098);
        }
        MdmcTask mdmcTask = taskMapper.selectByExample(example).get(0);
        if(mdmcTask.getStatus()==MdmcTaskStatusEnum.JieDan1.getStatusNum()){
            //如果当前任务的状态处于“审核通过，待服务商接单”，服务商才能拒单
            mdmcChangeStatusDto.setLoginAuthDto(loginAuthDto);
            mdmcChangeStatusDto.setStatus(MdmcTaskStatusEnum.Reject1.getStatusNum());
            mdmcChangeStatusDto.setStatusMsg(MdmcTaskStatusEnum.Reject1.getStatusMsg());
            this.modifyTaskStatus(mdmcChangeStatusDto,loginAuthDto);

        }else{
            throw new BusinessException(ErrorCodeEnum.MDMC9998087);
        }
        return mdmcChangeStatusDto;
    }


    public String[] getNullPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    public void copyPropertiesWithIgnoreNullProperties(Object source, Object target){
        String[] ignore = getNullPropertyNames(source);
        BeanUtils.copyProperties(source, target, ignore);
    }

    @Override
    public List<OptUploadFileRespDto> uploadTaskFile(MultipartHttpServletRequest multipartRequest,OptUploadFileReqDto optUploadFileReqDto, LoginAuthDto loginAuthDto) {
        String filePath = optUploadFileReqDto.getFilePath();
        Long userId = loginAuthDto.getUserId();
        String userName = loginAuthDto.getUserName();
        List<OptUploadFileRespDto> result = Lists.newArrayList();
        try {
            List<MultipartFile> fileList = multipartRequest.getFiles("file");
            if (fileList.isEmpty()) {
                return result;
            }

            for (MultipartFile multipartFile : fileList) {
                String fileName = multipartFile.getOriginalFilename();
                if (PublicUtil.isEmpty(fileName)) {
                    continue;
                }
                Preconditions.checkArgument(multipartFile.getSize() <= GlobalConstant.FILE_MAX_SIZE, "上传文件不能大于5M");
                InputStream inputStream = multipartFile.getInputStream();

                String inputStreamFileType = FileTypeUtil.getType(inputStream);
                // 将上传文件的字节流封装到到Dto对象中
                OptUploadFileByteInfoReqDto optUploadFileByteInfoReqDto = new OptUploadFileByteInfoReqDto();
                optUploadFileByteInfoReqDto.setFileByteArray(multipartFile.getBytes());
                optUploadFileByteInfoReqDto.setFileName(fileName);
                optUploadFileByteInfoReqDto.setFileType(inputStreamFileType);
                optUploadFileReqDto.setUploadFileByteInfoReqDto(optUploadFileByteInfoReqDto);
                // 设置不同文件路径来区分图片
                optUploadFileReqDto.setFilePath("ananops/mdmc/" + userId + "/" + filePath+ "/");
                optUploadFileReqDto.setUserId(userId);
                optUploadFileReqDto.setUserName(userName);
                OptUploadFileRespDto optUploadFileRespDto = opcOssFeignApi.uploadFile(optUploadFileReqDto).getResult();
                result.add(optUploadFileRespDto);
            }
        } catch (IOException e) {
            logger.error("上传文件失败={}", e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<ElementImgUrlDto> getFileByTaskIdAndStatus(MdmcFileReqDto mdmcFileReqDto) {
        Long id=mdmcFileReqDto.getTaskId();
        Integer status=mdmcFileReqDto.getStatus();
        Example example = new Example(MdmcFileTaskStatus.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("taskId",mdmcFileReqDto.getTaskId());
        criteria.andEqualTo("status",mdmcFileReqDto.getStatus());
        if(fileTaskStatusMapper.selectCountByExample(example) == 0){//如果当前查看的图片不存在
            throw new BusinessException(ErrorCodeEnum.OPC10040008,id);
        }
        MdmcFileTaskStatus fileTaskStatus=fileTaskStatusMapper.selectByExample(example).get(0);
        OptBatchGetUrlRequest optBatchGetUrlRequest=new OptBatchGetUrlRequest();
        optBatchGetUrlRequest.setRefNo(String.valueOf(fileTaskStatus.getId()));

        return opcOssFeignApi.listFileUrl(optBatchGetUrlRequest).getResult();
    }
}
