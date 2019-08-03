package com.cb.fts.sample.entities.vo;

import com.couchbase.client.java.search.result.SearchQueryRow;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class QueryStats implements Serializable {

    private String id;
    private int position;
    private double score;
    private Map<String, Object> explanation;
    private Map<String, List<String>> fragments;
    private Map<String, String> fields;


    public QueryStats(int position, SearchQueryRow row) {
        this.position = position;
        this.id = row.id();
        this.score = row.score();
        this.fragments = row.fragments();
        this.fields = row.fields();
        this.explanation = row.explanation()!= null ? row.explanation().toMap():null;
    }
}
