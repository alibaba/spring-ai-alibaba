package com.alibaba.cloud.ai.memory.mapper;

import com.alibaba.cloud.ai.memory.entity.ConversationMemoryForMySQL;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Title mysql conversation store dao.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Mapper
public interface ConversationMemoryMapper extends BaseMapper<ConversationMemoryForMySQL> {

}
