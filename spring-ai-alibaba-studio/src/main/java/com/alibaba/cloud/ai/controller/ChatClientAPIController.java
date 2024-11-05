package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.ChatClientAPI;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("studio/api/chat-clients")
public class ChatClientAPIController implements ChatClientAPI {

}
