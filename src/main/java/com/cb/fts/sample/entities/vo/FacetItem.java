package com.cb.fts.sample.entities.vo;


import lombok.Data;

import java.io.Serializable;

@Data
public class FacetItem implements Serializable {

    private String name;
    private Long total;

    public FacetItem(String name, Long total) {
        this.name = name;
        this.total = total;
    }
}
