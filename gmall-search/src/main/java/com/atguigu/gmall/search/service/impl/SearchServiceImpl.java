package com.atguigu.gmall.search.service.impl;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void search(SearchParamVo paramVo) {

        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("goods");
            searchRequest.source(buildDSL(paramVo));
            this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SearchSourceBuilder buildDSL(SearchParamVo paramVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();


        //构建关键子查询
        String keyword = paramVo.getKeyword();
        if (StringUtils.isEmpty(keyword)){
            return null;
        }

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        //构建条件过滤
        //品牌过滤
        //分类过滤
        //价格区间过滤
        //库存过滤
        //规格参数过滤

        //排序

        //分页

        //高亮

        //聚合
        //品牌聚合
        //分类聚合
        //参数聚合

        return sourceBuilder;
    }
}
