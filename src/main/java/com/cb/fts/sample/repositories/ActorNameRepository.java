package com.cb.fts.sample.repositories;


import com.cb.fts.sample.entities.ActorName;
import com.cb.fts.sample.entities.Movie;
import org.springframework.data.couchbase.core.query.N1qlPrimaryIndexed;
import org.springframework.data.couchbase.core.query.ViewIndexed;
import org.springframework.data.couchbase.repository.CouchbasePagingAndSortingRepository;

@N1qlPrimaryIndexed
public interface ActorNameRepository extends CouchbasePagingAndSortingRepository<ActorName, String> {
}
