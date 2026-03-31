package com.gleanread.server.domain.model.fragment;

import java.util.Collection;
import java.util.List;

public interface FragmentRepository {
    
    Fragment save(Fragment fragment);
    
    Fragment findById(Long id);
    
    List<Fragment> listByIds(Collection<Long> fragmentIds);
    
    void update(Fragment fragment);
}
