package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {

    private String keyword;
    private List<Long> brandId;
    private List<Long> cid;
    private List<String> props;

    private Integer sort;

    private Double priceFrom;
    private Double priceTo;

    private Integer pageNum = 1;
    private final Integer pageSize = 20;

    private Boolean store;
}
