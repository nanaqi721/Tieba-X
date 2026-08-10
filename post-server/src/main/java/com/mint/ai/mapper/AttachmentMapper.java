package com.mint.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mint.ai.mapper.entity.AttachmentDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 图片数据层
 */
public interface AttachmentMapper extends BaseMapper<AttachmentDO> {
    void batchInsert(@Param("attachmentDOS") List<AttachmentDO> attachmentDOS);
}
