package mapper.entiy;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 帖子实体类
 */
@TableName("post")
public class Post {

    /**
     * 帖子id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     *用户id
     */
    private String user_id;

    /**
     * 帖子id
     */
    private String bar_id;

    /**
     * 帖子标题(最少5字 最多30字）
     */
    private String title;

    /**
     * 封面图片存储地址
     */
    private String coverImage;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 浏览量(目前不冗余但是后续要是扩展新增用户浏览帖子表，这个数据就是冗余的，但留着有用处)
     */
    private Integer viewCount;

    /**
     * 评论数(冗余)
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer favoriteCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 最后评论时间
     */
    private LocalDateTime lastReplyTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标识 0 未删除 1已经删除
     */
    @TableLogic
    private Integer deleted;

}
