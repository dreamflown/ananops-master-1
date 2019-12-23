package com.ananops.provider.web.frontend;


import com.ananops.base.dto.LoginAuthDto;
import com.ananops.core.support.BaseController;

import com.ananops.provider.model.domain.MdmcTask;
import com.ananops.provider.model.domain.MdmcTaskLog;
import com.ananops.provider.model.dto.MdmcAddTaskDto;
import com.ananops.provider.model.dto.MdmcChangeStatusDto;
import com.ananops.provider.model.dto.MdmcStatusDto;
import com.ananops.provider.service.MdmcTaskLogService;
import com.ananops.provider.service.MdmcTaskService;
import com.ananops.wrapper.WrapMapper;
import com.ananops.wrapper.Wrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(value = "/mdmcTask",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "WEB - MdmcTask",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class MdmcTaskController extends BaseController {
    @Resource
    MdmcTaskService taskService;

    @Resource
    MdmcTaskLogService taskLogService;

    @PostMapping(value = "/save")
    @ApiOperation(httpMethod = "POST",value = "编辑维修任务记录")

    public Wrapper<MdmcAddTaskDto> saveTask(@ApiParam(name = "saveTask",value = "添加维修任务记录")@RequestBody MdmcAddTaskDto mdmcAddTaskDto){
        LoginAuthDto loginAuthDto = getLoginAuthDto();
        return WrapMapper.ok(taskService.saveTask(mdmcAddTaskDto,loginAuthDto));
    }

    @GetMapping(value = "/getTaskLogs/{taskId}")
    @ApiOperation(httpMethod = "GET",value = "根据任务ID查询任务的日志")
    public Wrapper<List<MdmcTaskLog>> getTaskLogs(@PathVariable Long taskId){
        List<MdmcTaskLog> taskLogList=taskLogService.getTaskLogsByTaskId(taskId);
        return WrapMapper.ok(taskLogList);
    }

    @PostMapping(value = "/modifyTaskStatusByTaskId/{taskId}")
    @ApiOperation(httpMethod = "POST",value = "更改任务的状态")

    public Wrapper<MdmcChangeStatusDto> modifyTaskStatusByTaskId(@ApiParam(name = "modifyTaskStatus",value = "根据任务的ID修改任务的状态")@RequestBody MdmcChangeStatusDto changeStatusDto){
        LoginAuthDto loginAuthDto = getLoginAuthDto();
        taskService.modifyTaskStatus(changeStatusDto,loginAuthDto);
        return WrapMapper.ok(changeStatusDto);
    }

    @PostMapping(value = "/getTaskByTaskId")
    @ApiOperation(httpMethod = "POST",value = "根据任务的状态，获取工单列表")
    public Wrapper<List<MdmcTask>> getTaskByStatus(@RequestBody MdmcStatusDto statusDto){
        List<MdmcTask> taskList = taskService.getTaskListByStatus(statusDto);
        return WrapMapper.ok(taskList);
    }

    @GetMapping(value = "/getTaskByTaskId/{taskId}")
    @ApiOperation(httpMethod = "GET",value = "根据任务的ID，获取当前的任务详情")
    public Wrapper<MdmcTask> getTaskByTaskId(@PathVariable Long taskId){
        MdmcTask task = taskService.getTaskByTaskId(taskId);
        return WrapMapper.ok(task);
    }

    @PostMapping(value = "/getTaskListByUserId")
    @ApiOperation(httpMethod = "POST",value = "根据用户ID查询工单列表")
    public Wrapper<List<MdmcTask>> getTaskListByUserId(@RequestBody MdmcStatusDto statusDto){
        List<MdmcTask> taskList=taskService.getTaskListByUserId(statusDto);
        return WrapMapper.ok(taskList);
    }

    @PostMapping(value = "/getTaskListByFacilitatorId")
    @ApiOperation(httpMethod = "POST",value = "根据服务商ID查询工单列表")
    public Wrapper<List<MdmcTask>> getTaskListByFacilitatorId(@RequestBody MdmcStatusDto statusDto){
        List<MdmcTask> taskList=taskService.getTaskListByFacilitatorId(statusDto);
        return WrapMapper.ok(taskList);
    }

    @PostMapping(value = "/getTaskListByPrincipalId")
    @ApiOperation(httpMethod = "POST",value = "根据甲方ID查询工单列表")
    public Wrapper<List<MdmcTask>> getTaskListByPrincipalId(@RequestBody MdmcStatusDto statusDto){
        List<MdmcTask> taskList=taskService.getTaskListByPrincipalId(statusDto);
        return WrapMapper.ok(taskList);
    }
    @PostMapping(value = "/getTaskListByMaintainerId")
    @ApiOperation(httpMethod = "POST",value = "根据维修工ID查询工单列表")
    public Wrapper<List<MdmcTask>> getTaskListByMaintainerId(@RequestBody MdmcStatusDto statusDto){
        List<MdmcTask> taskList=taskService.getTaskListByMaintainerId(statusDto);
        return WrapMapper.ok(taskList);
    }
    @PostMapping(value = "/getTaskList")
    @ApiOperation(httpMethod = "POST",value = "返回全部工单列表")
    public Wrapper<List<MdmcTask>> getTaskList(@RequestBody MdmcStatusDto statusDto){
        List<MdmcTask> taskList=taskService.getTaskList(statusDto);
        return WrapMapper.ok(taskList);
    }
}