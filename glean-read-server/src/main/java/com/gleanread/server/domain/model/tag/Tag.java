package com.gleanread.server.domain.model.tag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签实体（充血模型）
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@TableName("tag")
public class Tag {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 标签名（例如 '#架构设计'）
    private String tagName;
    
    // 记录该词汇的热度（引用它就会增加指数）
    private Integer heatWeight;
    
    private LocalDateTime createTime;

    /**
     * 工厂方法：创建一个新标签，默认热度为 1
     */
    public static Tag createNew(String tagName) {
        Tag tag = new Tag();
        tag.tagName = tagName;
        tag.heatWeight = 1;
        tag.createTime = LocalDateTime.now();
        return tag;
    }

    /**
     * 领域行为：增加标签的使用热度
     * 调用该方法会使当前标签被提及次数自增
     */
    public void incrementHeatWeight() {
        if (this.heatWeight == null) {
            this.heatWeight = 0;
        }
        this.heatWeight++;
    }
}
