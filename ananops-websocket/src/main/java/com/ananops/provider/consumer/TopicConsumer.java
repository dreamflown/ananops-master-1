package com.ananops.provider.consumer;

import com.ananops.JacksonUtil;
import com.ananops.base.constant.AliyunMqTopicConstants;
import com.ananops.base.dto.MqSendMsgDto;
import com.ananops.base.enums.ErrorCodeEnum;
import com.ananops.base.exception.BusinessException;
import com.ananops.core.mq.MqMessage;
import com.ananops.provider.model.dto.WebSocketMsgDto;
import com.ananops.provider.model.dto.mqDto.ImcSendTaskStatusDto;
import com.ananops.provider.service.WebSocketPushMsgService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Created by rongshuai on 2020/2/25 11:56
 */
@Slf4j
@Service
public class TopicConsumer {
    @Resource
    WebSocketPushMsgService webSocketPushMsgService;

    public void handlerSendMqMsg(String body, String topicName, String tags, String keys){
        MqMessage.checkMessage(body, keys, topicName);
        System.out.println("IMC_TASK_TOPIC:" + body);
        MqSendMsgDto mqSendMsgDto = new MqSendMsgDto();
        try {
            mqSendMsgDto = JacksonUtil.parseJson(body, MqSendMsgDto.class);
        } catch (IOException e) {
            log.error("发送短信MQ出现异常={}", e.getMessage(), e);
            throw new IllegalArgumentException("JSON转换异常", e);
        }
        if(mqSendMsgDto==null){
            throw new BusinessException(ErrorCodeEnum.WEBSOCKET10100000);
        }
        String userId = String.valueOf(mqSendMsgDto.getUserId());
        WebSocketMsgDto<MqSendMsgDto> webSocketMsgDto = new WebSocketMsgDto<>();
        webSocketMsgDto.setTopic(AliyunMqTopicConstants.MqTopicEnum.IMC_TOPIC.getTopic());
        webSocketMsgDto.setTag(AliyunMqTopicConstants.MqTagEnum.IMC_TASK_STATUS_CHANGED.getTag());
        webSocketMsgDto.setContent(mqSendMsgDto);
        log.info("webSocketMsgDto = {}",webSocketMsgDto);
        log.info("userId = {}",userId);
        webSocketPushMsgService.SendMessageToWebSocketClient(webSocketMsgDto,userId);
    }
}
