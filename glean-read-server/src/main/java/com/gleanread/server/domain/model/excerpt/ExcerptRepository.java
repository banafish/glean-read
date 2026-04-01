package com.gleanread.server.domain.model.excerpt;

import java.util.Collection;
import java.util.List;

public interface ExcerptRepository {
    
    Excerpt save(Excerpt excerpt);
    
    Excerpt findById(Long id);
    
    List<Excerpt> listByIds(Collection<Long> excerptIds);
    
    void update(Excerpt excerpt);
}
