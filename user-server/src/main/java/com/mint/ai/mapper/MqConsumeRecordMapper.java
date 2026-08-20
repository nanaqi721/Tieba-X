package com.mint.ai.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MqConsumeRecordMapper {

    @Insert("""
            INSERT IGNORE INTO mq_consume_record
                (id, consumer_group, event_id, topic, consumed_at)
            VALUES
                (#{id}, #{consumerGroup}, #{eventId}, #{topic}, NOW())
            """)
    int insertIgnore(@Param("id") String id,
                     @Param("consumerGroup") String consumerGroup,
                     @Param("eventId") String eventId,
                     @Param("topic") String topic);
}
