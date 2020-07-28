package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.springframework.stereotype.Service;

@Service
public interface SearchService {
    SearchResponseVo search(SearchParamVo paramVo);
}
