package com.gleanread.server.domain.model.fragment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识碎片实体类 (充血模型)
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 限制无参构造供MyBatis或JPA使用
@TableName("fragment")
public class Fragment {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 摘录的高亮文本/正文
    private String content;
    
    // 来源链接
    private String url;
    
    // 用户的随手想法
    private String userThought;
    
    // 归属的体系树节点ID，未处理的碎片该值为null(即存在于Inbox中)
    // 充血模型：屏蔽 setter，通过显式行为操作
    @Setter(AccessLevel.NONE)
    private Long treeNodeId;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;

    /**
     * 工厂方法，创建新的知识碎片
     */
    public static Fragment create(String content, String url, String userThought) {
        Fragment fragment = new Fragment();
        fragment.content = content;
        fragment.url = url;
        fragment.userThought = userThought;
        fragment.treeNodeId = null; // 默认在 Inbox
        fragment.createTime = LocalDateTime.now();
        fragment.updateTime = LocalDateTime.now();
        return fragment;
    }

    /**
     * 领域行为：将碎片归档/挂载到知识树的某个节点下
     * 业务规则：只能挂载当前在 Inbox 中的，或者将它挪到新的节点下。此处只做关联。
     */
    public void mountToNode(Long targetTreeNodeId) {
        if (targetTreeNodeId == null) {
            throw new IllegalArgumentException("树节点ID不能为空");
        }
        this.treeNodeId = targetTreeNodeId;
        this.updateTime = LocalDateTime.now();
    }
}
