package com.cb.fts.sample.entities;

import com.couchbase.client.java.repository.annotation.Field;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Country {

    @Field("iso_3166_1")
    private String IsoCode;
    private String name;
}
