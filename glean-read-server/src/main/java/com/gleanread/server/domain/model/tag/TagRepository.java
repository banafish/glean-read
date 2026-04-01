package com.gleanread.server.domain.model.tag;

import java.util.Optional;

public interface TagRepository {
    
    Optional<Tag> findByName(String tagName);
    
    Tag save(Tag tag);
    
    void update(Tag tag);

    void saveExcerptTagRelation(Long excerptId, Long tagId);
}
