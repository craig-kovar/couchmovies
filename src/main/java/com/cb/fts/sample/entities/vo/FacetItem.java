package com.cb.fts.sample.entities.vo;

import com.couchbase.client.java.search.result.facets.FacetResult;
import com.couchbase.client.java.search.result.facets.TermFacetResult;
import com.couchbase.client.java.search.result.facets.TermRange;
import lombok.Data;

import java.io.Serializable;

@Data
public class FacetItem implements Serializable {

    private String name;
    private Long total;

    public FacetItem(TermRange termRange) {
        this.name = termRange.name();
        this.total = termRange.count();
    }
}
