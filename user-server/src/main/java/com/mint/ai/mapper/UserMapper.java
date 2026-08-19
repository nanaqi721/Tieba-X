package com.mint.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mint.ai.mapper.entity.UserDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @param
 * @return
 */
public interface UserMapper extends BaseMapper<UserDO> {
    void getUserInfo(String userId);

    @Update("""
            UPDATE `user`
            SET post_count = GREATEST(0, post_count + #{delta})
            WHERE id = #{userId} AND deleted = 0
            """)
    int incrementPostCount(@Param("userId") String userId,
                           @Param("delta") int delta);

    @Update("""
            UPDATE `user`
            SET comment_count = GREATEST(0, comment_count + #{delta})
            WHERE id = #{userId} AND deleted = 0
            """)
    int incrementCommentCount(@Param("userId") String userId,
                              @Param("delta") int delta);
}
