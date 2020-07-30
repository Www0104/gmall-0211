package com.atguigu.gmall.search.service.impl;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Override
    public SearchResponseVo search(SearchParamVo paramVo) {

        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices("goods");
            searchRequest.source(buildDSL(paramVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(response.toString());
            SearchResponseVo responseVo = this.parseResult(response);
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SearchSourceBuilder buildDSL(SearchParamVo paramVo){

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //构建关键子查询
        String keyword = paramVo.getKeyword();
        if (StringUtils.isBlank(keyword)){
            return null;
        }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));


        //构建条件过滤
        //品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }
        //分类过滤
        List<Long> cid = paramVo.getCid();
        if (!CollectionUtils.isEmpty(cid)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",cid));
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
                case 3: sourceBuilder.sort("sales", SortOrder.DESC); break;
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

        sourceBuilder.fetchSource(new String[]{"skuId","title","price","image","subTitle"},null);

        System.out.println(sourceBuilder.toString());
        return sourceBuilder;
    }


    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.getTotalHits());


        SearchHit[] hitsHits = hits.getHits();
        if (hitsHits == null || hitsHits.length == 0 ){
            return responseVo;
        }
        List<Goods> goodsList = Stream.of(hitsHits).map(hit -> {
            String source = hit.getSourceAsString();
            Goods goods = null;
            try {
                goods = MAPPER.readValue(source, Goods.class);
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if (!CollectionUtils.isEmpty(highlightFields)) {
                    HighlightField highlightField = highlightFields.get("title");
                    if (highlightField != null) {
                        Text[] fragments = highlightField.getFragments();
                        if (fragments != null && fragments.length > 0) {
                            String title = fragments[0].string();
                            goods.setTitle(title);
                        }

                    }
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        List<BrandEntity> brandEntities = buckets.stream().map(bucket -> {
            BrandEntity brandEntity = new BrandEntity();
            long brandId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
            brandEntity.setId(brandId);
            Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            ParsedStringTerms brandNameAgg = (ParsedStringTerms)subAggregationMap.get("brandNameAgg");
            List<? extends Terms.Bucket> nameBuckets = brandNameAgg.getBuckets();
            if (!CollectionUtils.isEmpty(nameBuckets)){
                Terms.Bucket nameBucket = nameBuckets.get(0);
                if (nameBucket != null){
                    brandEntity.setName(nameBucket.getKeyAsString());
                }
            }
            ParsedStringTerms brandLogoAgg = (ParsedStringTerms) subAggregationMap.get("brandLogoAgg");
            if (brandEntity != null){
                List<? extends Terms.Bucket> logoBuckets = brandLogoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoBuckets)){
                    Terms.Bucket logoBucket = logoBuckets.get(0);
                    if (logoBucket != null){
                        brandEntity.setLogo(logoBucket.getKeyAsString());
                    }
                }
            }
            return brandEntity;
        }).collect(Collectors.toList());
        responseVo.setBrands(brandEntities);


        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        List<CategoryEntity> categoryEntities = categoryBuckets.stream().map(bucket -> {
            CategoryEntity categoryEntity = new CategoryEntity();
            long categoryId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
            categoryEntity.setId(categoryId);
            ParsedStringTerms categoryNameAgg = (ParsedStringTerms) ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
            if (categoryNameAgg != null){
                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)){
                    categoryEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
            }
            return categoryEntity;
        }).collect(Collectors.toList());
        responseVo.setCategories((categoryEntities));

        ParsedNested attrAgg = (ParsedNested)aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        if (attrIdAgg != null) {
            List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
            if (!CollectionUtils.isEmpty(attrIdAggBuckets)){
                List<SearchResponseAttrVo> searchResponseAttrVos = attrIdAggBuckets.stream().map(bucket -> {
                    SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();


                    long attrId = ((Terms.Bucket) bucket).getKeyAsNumber().longValue();
                    searchResponseAttrVo.setAttrId(attrId);


                    Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                    ParsedStringTerms attrNameAgg = (ParsedStringTerms) subAggregationMap.get("attrNameAgg");
                    if (attrNameAgg != null){
                        List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(nameAggBuckets)){
                            Terms.Bucket nameBucket = nameAggBuckets.get(0);
                            if (nameBucket != null) {
                                searchResponseAttrVo.setAttrName(nameBucket.getKeyAsString());
                            }
                        }
                    }

                    ParsedStringTerms attrValueAgg = (ParsedStringTerms)subAggregationMap.get("attrValueAgg");
                    if (attrValueAgg != null){
                        List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                        if (!CollectionUtils.isEmpty(attrValueAggBuckets)){
                            searchResponseAttrVo.setAttrValues(attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                        }
                    }

                    return searchResponseAttrVo;
                }).collect(Collectors.toList());
                responseVo.setFilters(searchResponseAttrVos);
            }
        }
        return responseVo;
    }
}

