package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import org.springframework.stereotype.Service;

@Service
public interface SearchService {
    void search(SearchParamVo paramVo);
}
