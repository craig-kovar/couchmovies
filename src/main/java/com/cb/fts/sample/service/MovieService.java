package com.cb.fts.sample.service;

import java.util.List;

import com.cb.fts.sample.entities.vo.Result;

public interface MovieService {

    Result searchQuery(String query, String genres, Boolean fuzzy);
    List<String> autocomplete(String query);

}
