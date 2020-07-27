package com.atguigu.gmall.search.service.impl;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

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
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termQuery("brandId",brandId));
        }
        //分类过滤
        List<Long> cid = paramVo.getCid();
        if (!CollectionUtils.isEmpty(cid)){
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryId",cid));
        }
        //价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceFrom != null || priceTo != null ){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            if (priceFrom != null){
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null){
                rangeQuery.lte(priceTo);
            }
        }
        //库存过滤
        Boolean store = paramVo.getStore();
        if (store != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }
        //规格参数过滤
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)){
            props.forEach(prop -> {
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2 ){
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",attr[0]));
                    String[] attrValues = StringUtils.split(attr[1], "-");
                    if (attrValues != null && attrValues.length > 0){
                        boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",attrValues));
                    }
                    NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None);
                    boolQueryBuilder.filter(nestedQuery);
                }
            });
        }
        //排序
        Integer sort = paramVo.getSort();
        if (sort != null) {
            switch (sort){
                case 1: sourceBuilder.sort("price", SortOrder.ASC); break;
                case 2: sourceBuilder.sort("price", SortOrder.DESC); break;
                case 3: sourceBuilder.sort("sale", SortOrder.DESC); break;
                case 4: sourceBuilder.sort("createTime", SortOrder.DESC); break;
            }
        }
        //分页
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        //高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red'>").postTags("</font>"));
        //聚合
        //品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoAgg").field("logo")));
        //分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        //参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId"))
                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")));
        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
    }
}
