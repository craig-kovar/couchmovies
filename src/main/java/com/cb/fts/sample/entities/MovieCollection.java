package com.cb.fts.sample.entities;

import com.couchbase.client.java.repository.annotation.Field;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieCollection {


    private Long id;
    @Field("backdrop_path")
    private String backdropPath;
    private String name;

    @Field("poster_path")
    private String posterPath;
}
