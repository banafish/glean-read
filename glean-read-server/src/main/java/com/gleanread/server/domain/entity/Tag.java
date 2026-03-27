package com.gleanread.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 标签管理实体类
 * 标签和碎片是多对多关系
 */
@Data
@TableName("tags")
public class Tag {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 标签本身字符内容，如"性能优化"
    private String tagName;
    
    // 当前热度值(随着使用频次递增)
    private Integer heatWeight;
    
    private LocalDateTime createTime;
}
