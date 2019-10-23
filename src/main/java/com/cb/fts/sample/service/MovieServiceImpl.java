package com.cb.fts.sample.service;

import com.cb.fts.sample.entities.vo.*;
import com.cb.fts.sample.repositories.MovieRepository;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.facet.NumericRangeFacet;
import com.couchbase.client.java.search.facet.SearchFacet;
import com.couchbase.client.java.search.queries.*;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.SearchQueryRow;
import com.couchbase.client.java.search.result.facets.DefaultNumericRangeFacetResult;
import com.couchbase.client.java.search.result.facets.DefaultTermFacetResult;
import com.couchbase.client.java.search.result.facets.FacetResult;
import com.couchbase.client.java.search.result.facets.TermFacetResult;
import com.couchbase.client.java.search.result.facets.TermRange;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.cb.fts.sample.service.EntityExtractor.EntityType.GENRES;
import static com.cb.fts.sample.service.EntityExtractor.EntityType.PERSON;

@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieQueryParser movieQueryParser;

    @Override
    public Result searchQuery(String phrase, String filters, Boolean fuzzy) {
        System.out.println("Movie Search Request -  phrase:" + phrase + "  filters:" + filters + " fuzzy:" + fuzzy);
         Map<String,List<String>> facets = getFilters(filters);

        return search(phrase, facets, fuzzy);
    }

    @Override
    public List<String> autocomplete(String query) {
        System.out.println("Auto Complete Request -  query:" + query);
        return complete(query);
    }


    private List<String> complete(String words) {
        String indexName = "movies_autocomplete";
        EntityExtractor entityExtractor = movieQueryParser.parse(words);
        DisjunctionQuery ftsQuery = new DisjunctionQuery();

        if(entityExtractor.getWords().trim().length() >0) {
            MatchQuery matchQuery = SearchQuery.match(entityExtractor.getWords()).field("title").analyzer("simple");
            ftsQuery = ftsQuery.or(matchQuery);
            //.fuzziness(1);

        }
        SearchQuery searchQuery =  new SearchQuery(indexName, ftsQuery).limit(20);
        searchQuery.fields("title");
        // searchQuery.fields("title", "cast.character", "cast.name");  //The character names are strange, have lots of special characters and aren't good for autocomplete

        SearchQueryResult result = movieRepository.getCouchbaseOperations().getCouchbaseBucket().query(searchQuery);
        if (result != null && result.errors().isEmpty()) {
            // for (SearchQueryRow row: result.hits()){
            //     System.out.println(row);
            // }
            
        } else {
            System.out.println("Bad autocomple result: " + result.errors());
        }
        List<String> options = new ArrayList<String>();

        if (result != null && result.errors().isEmpty()) {
            Iterator<SearchQueryRow> resultIterator = result.hits().iterator();
            while (resultIterator.hasNext()) {
                SearchQueryRow row = resultIterator.next();
                String title = row.fields().get("title");
                if (title != null) options.add(title);
                // String character = row.fields().get("cast.character");
                // if (character != null) options.add(character);
                // String name = row.fields().get("cast.name");
                // if (name != null) options.add(name);               
             }
        }

        return options;
    }    

    private Result search(String words, Map<String, List<String>> facets, Boolean fuzzy){

        String indexName = "movies_shingle";

        EntityExtractor entityExtractor = movieQueryParser.parse(words);

        DisjunctionQuery ftsQuery = new DisjunctionQuery();
        if(entityExtractor.getWords().trim().length() >0) {
            ftsQuery.or( getSubquery(entityExtractor.getWords(), "title", 1.4, fuzzy));
            ftsQuery.or( getSubquery(entityExtractor.getWords(), "originalTitle", 1.15, fuzzy));
            ftsQuery.or( getSubquery(entityExtractor.getWords(), "collection.name", 1.1, fuzzy));
            ftsQuery.or( getSubquery(entityExtractor.getWords(), "overview", 1.0, fuzzy));
            ftsQuery.or( getSubquery(entityExtractor.getWords(), "cast.name", 1.3, fuzzy));
            ftsQuery.or( getSubquery(entityExtractor.getWords(), "cast.character", 1.3, fuzzy));
        }

        AbstractFtsQuery actors = getActorsDisjunctionAdjusted(words, entityExtractor, fuzzy);
        if(actors!= null) {
            ftsQuery.or(actors);
        }


        ConjunctionQuery conjunctionQuery = SearchQuery.conjuncts(ftsQuery,
                boostReleaseYearQuery(),
                boostRuntime(),
                boostPromoted(),
                boostWeightedRating(),
                boostPopularity()); 

        if(entityExtractor.getEntities().containsKey(GENRES)) {
            addFilters(conjunctionQuery, "genres.name", entityExtractor.getEntities().get(GENRES));
        }

        if(!facets.isEmpty()) {
            conjunctionQuery = addFacetFilters(conjunctionQuery, facets);
        }

        SearchQuery searchQuery =  new SearchQuery(indexName, conjunctionQuery).highlight().limit(20);
        searchQuery.addFacet("genres",  SearchFacet.term("genres.name", 10));
        NumericRangeFacet yearFacet = SearchFacet.numeric("release_year", 10);
        yearFacet = yearFacet.addRange("2019", 2019.0, 2020.0);
        yearFacet = yearFacet.addRange("2018", 2018.0, 2019.0);
        yearFacet = yearFacet.addRange("2017", 2017.0, 2018.0);
        yearFacet = yearFacet.addRange("2016", 2016.0, 2017.0);
        yearFacet = yearFacet.addRange("2000-2015", 2000.0, 2016.0);
        yearFacet = yearFacet.addRange("1990-1999", 1990.0, 2000.0);
        yearFacet = yearFacet.addRange("1980-1989", 1980.0, 1990.0);
        yearFacet = yearFacet.addRange("1970-1979", 1970.0, 1980.0);
        searchQuery.addFacet("year",  yearFacet);

        //searchQuery.explain(true);

        SearchQueryResult result = movieRepository.getCouchbaseOperations().getCouchbaseBucket().query(searchQuery);
        return getSearchResults(result);
    }



    private DisjunctionQuery boostPromoted() {
        BooleanFieldQuery promotedQuery = SearchQuery.booleanField(true).field("promoted").boost(1.5);
        BooleanFieldQuery notPromotedQuery = SearchQuery.booleanField(false).field("promoted").boost(1);
        return SearchQuery.disjuncts(promotedQuery, notPromotedQuery);
    }




    private ConjunctionQuery addFacetFilters(ConjunctionQuery conjunctionQuery , Map<String, List<String>> facets) {

        for(Map.Entry<String, List<String>> entry: facets.entrySet()) {
            if("genres".equals(entry.getKey())) {
                addFilters(conjunctionQuery, "genres.name", entry.getValue());
            } else if("collection".equals(entry.getKey())) {
                addFilters(conjunctionQuery, "collection.name", entry.getValue());
            } else if("year".equals(entry.getKey())) {
                DisjunctionQuery disjunctionQuery = SearchQuery.disjuncts();
                Iterator<String> selected = entry.getValue().iterator();
                while(selected.hasNext()){
                    String option = selected.next();
                    Long min;
                    Long max;
                    if(option.contains("-")) {
                        int index = option.indexOf("-");
                        min = Long.parseLong(option.substring(0,index));
                        max = Long.parseLong(option.substring(index+1, option.length()));
                    } else {
                        min = Long.parseLong(option);
                        max = min + 1;
                    }
                    disjunctionQuery.or(SearchQuery.numericRange().min(min).max(max).field("release_year"));
                }
                conjunctionQuery.and(disjunctionQuery);
            }
        }
        return conjunctionQuery;
    }



    private void addFilters(ConjunctionQuery conjunctionQuery, String fieldName, List<String> values) {
        if(values.size() == 1) {
            conjunctionQuery.and(SearchQuery.term(values.get(0)).field(fieldName));
        } if(values.size() > 1) {
            DisjunctionQuery disjunctionQuery = SearchQuery.disjuncts();
            for(String genre : values) {
                disjunctionQuery.or(SearchQuery.term(genre).field(fieldName));
            }
            conjunctionQuery.and(disjunctionQuery);
        }
    }


    private DisjunctionQuery getActorsDisjunction(String words, boolean fuzzy) {
            AbstractFtsQuery castQuery = getSubquery(words, "cast.name", 1.15, fuzzy);
            MatchQuery character = SearchQuery.match(words).field("cast.character");
            return SearchQuery.disjuncts(castQuery, character);

    }

    private DisjunctionQuery getCrewDisjunction(String words, boolean fuzzy) {
            AbstractFtsQuery nameQuery = getSubquery(words, "crew.name", 1.15, fuzzy);
            MatchQuery jobQuery = SearchQuery.match(words).boost(1.1).field("crew.job");
        return SearchQuery.disjuncts(nameQuery, jobQuery);

    }


    private AbstractFtsQuery getActorsDisjunction(String words, EntityExtractor entityExtractor, boolean fuzzy) {

        if(entityExtractor != null && entityExtractor.getEntities().keySet().contains(PERSON)) {
            List<String> names = entityExtractor.getEntities().get(PERSON);
            if(names.size() ==1) {
                return getSubquery(names.get(0), "cast.name", 1.5, fuzzy );
            } else {
                DisjunctionQuery dq = new DisjunctionQuery();
                for(String name: names){
                    dq.or(getSubquery(name, "cast.name", 1.5, fuzzy ));
                }
                return dq;
            }
        } else {
            if (entityExtractor.getWords().trim().isEmpty()) {
                return null;
            }
            AbstractFtsQuery castQuery = getSubquery(words, "cast.name", 1.15, fuzzy);
            MatchQuery character = SearchQuery.match(words).field("cast.character");
            return SearchQuery.disjuncts(castQuery, character);
        }
    }

    private AbstractFtsQuery getActorsDisjunctionAdjusted(String words, EntityExtractor entityExtractor, boolean fuzzy) {

        if(entityExtractor != null && entityExtractor.getEntities().keySet().contains(PERSON)) {
            List<String> names = entityExtractor.getEntities().get(PERSON);
            if(names.size() ==1) {
                return getSubquery(names.get(0), "cast.name", 1.5, fuzzy );
            } else {
                DisjunctionQuery dq = new DisjunctionQuery();
                for(String name: names){
                    dq.or(getSubquery(name, "cast.name", 1.5, fuzzy ));
                }
                return dq;
            }
        } else {
            if (entityExtractor.getWords().trim().isEmpty()) {
                return null;
            }
            AbstractFtsQuery castQuery = getSubquery(words, "cast.name", 1.15, fuzzy);
            MatchQuery character = SearchQuery.match(words).field("cast.character");
            return SearchQuery.disjuncts(castQuery, character);
        }
    }

    private AbstractFtsQuery getCrewDisjunction(String words, EntityExtractor entityExtractor, boolean fuzzy) {

        if(entityExtractor != null && entityExtractor.getEntities().keySet().contains(PERSON)) {
            List<String> names = entityExtractor.getEntities().get(PERSON);
            if(names.size() ==1) {
                return getSubquery(names.get(0), "crew.name", 1.4, fuzzy);
            } else {
                DisjunctionQuery dq = new DisjunctionQuery();
                for(String name: names){
                    dq.or(getSubquery(name, "crew.name", 1.4, fuzzy));
                }
                return dq;
            }
        } else {
            if (entityExtractor.getWords().trim().isEmpty()) {
                return null;
            }
            AbstractFtsQuery nameQuery = getSubquery(words, "crew.name", 1.15, fuzzy);
            MatchQuery jobQuery = SearchQuery.match(words).boost(1.1).field("crew.job");
            return SearchQuery.disjuncts(nameQuery, jobQuery);
        }
    }

    private DisjunctionQuery boostPopularity() {
        NumericRangeQuery rangeQuery = SearchQuery.numericRange().field("popularity").boost(1.25);
        rangeQuery.max(1000);
        rangeQuery.min(40);

        NumericRangeQuery rangeQuery2 = SearchQuery.numericRange().field("popularity").boost(1.20);
        rangeQuery2.max(39.9999);
        rangeQuery2.min(30);

        NumericRangeQuery rangeQuery3 = SearchQuery.numericRange().field("popularity").boost(1.10);
        rangeQuery3.max(29.9999);
        rangeQuery3.min(10);

        NumericRangeQuery rangeQuery4 = SearchQuery.numericRange().field("popularity").boost(0.90);
        rangeQuery4.max(9.9999);
        rangeQuery4.min(4);

        NumericRangeQuery rangeQuery5 = SearchQuery.numericRange().field("popularity").boost(0.80);
        rangeQuery5.max(3.9999);
        rangeQuery5.min(0);

        DisjunctionQuery popularityDisjunction = SearchQuery.disjuncts(rangeQuery, rangeQuery2, rangeQuery3, rangeQuery4, rangeQuery5 );
        return popularityDisjunction;
    }

    private DisjunctionQuery boostReleaseYearQuery() {

        LocalDateTime now = LocalDateTime.now();
        NumericRangeQuery rangeQuery = SearchQuery.numericRange().field("release_year").boost(1.35);
        rangeQuery.max(now.getYear(), true);
        rangeQuery.min(now.getYear()-4);

        NumericRangeQuery penalizationQuery = SearchQuery.numericRange().field("release_year").boost(1.15);
        penalizationQuery.max(now.getYear()-5);
        penalizationQuery.min(now.getYear()-10);

        NumericRangeQuery penalization1Query = SearchQuery.numericRange().field("release_year").boost(1);
        penalization1Query.max(now.getYear()-9);
        penalization1Query.min(now.getYear()-15);

        NumericRangeQuery penalization2Query = SearchQuery.numericRange().field("release_year").boost(0.92);
        penalization2Query.max(now.getYear()-16);
        penalization2Query.min(now.getYear()-25);

        NumericRangeQuery penalization3Query = SearchQuery.numericRange().field("release_year").boost(0.85);
        penalization3Query.max(now.getYear()-25);
        penalization3Query.min(0);

        DisjunctionQuery yearDisjunction = SearchQuery.disjuncts(rangeQuery, penalizationQuery, penalization1Query, penalization2Query, penalization3Query );

        return yearDisjunction;
    }

    private DisjunctionQuery boostRuntime() {

        NumericRangeQuery runtime1 = SearchQuery.numericRange().field("runtime").boost(1.25);
        runtime1.max(5000);
        runtime1.min(360);

        NumericRangeQuery runtime2 = SearchQuery.numericRange().field("runtime").boost(1.17);
        runtime2.max(359);
        runtime2.min(100);

        NumericRangeQuery runtime3 = SearchQuery.numericRange().field("runtime").boost(0.90);
        runtime3.max(99);
        runtime3.min(40);

        NumericRangeQuery runtime4 = SearchQuery.numericRange().field("runtime").boost(0.75);
        runtime4.max(39);
        runtime4.min(0);

        DisjunctionQuery runtimeDisjunction = SearchQuery.disjuncts(runtime1, runtime2, runtime3, runtime4 );

        return runtimeDisjunction;
    }

    private DisjunctionQuery boostWeightedRating() {

        NumericRangeQuery weightedRating1 = SearchQuery.numericRange().field("weightedRating").boost(1.25);
        weightedRating1.max(10);
        weightedRating1.min(7);

        NumericRangeQuery weightedRating2 = SearchQuery.numericRange().field("weightedRating").boost(1.10);
        weightedRating2.max(6.9999);
        weightedRating2.min(5);

        NumericRangeQuery weightedRating3 = SearchQuery.numericRange().field("weightedRating").boost(1);
        weightedRating3.max(4.999);
        weightedRating3.min(3);

        NumericRangeQuery weightedRating4 = SearchQuery.numericRange().field("weightedRating").boost(0.75);
        weightedRating4.max(2.999);
        weightedRating4.min(0);


        DisjunctionQuery runtimeDisjunction = SearchQuery.disjuncts(weightedRating1, weightedRating2, weightedRating3, weightedRating4 );

        return runtimeDisjunction;
    }



    private AbstractFtsQuery getSubquery(String words, String field, double boost, boolean fuzzy) {
        MatchQuery query = SearchQuery.match(words).boost(boost).field(field);
        if (fuzzy) {
            MatchQuery queryFuzzy = SearchQuery.match(words).boost(boost).field(field)
            .fuzziness(1);

            return SearchQuery.disjuncts(query, queryFuzzy);
        } else {
            return query;
        }

    }


    private Result getSearchResults(SearchQueryResult result){

    
        Result rt = new Result();
        List<SearchResult> movies = new ArrayList<>();
        if (result != null && result.errors().isEmpty()) {
            Iterator<SearchQueryRow> resultIterator = result.iterator();
            int counter = 1;
            while (resultIterator.hasNext()) {
                SearchQueryRow row = resultIterator.next();
                movies.add( new SearchResult(movieRepository.findById(row.id()).get(), new QueryStats(counter, row)));
                counter++;
            }
       

            rt.setResults(movies);

            List<Facet> facets = new ArrayList<>();

            DefaultTermFacetResult genres = (DefaultTermFacetResult) result.facets().get("genres");
            if(genres != null & genres.terms().size() >0){

                List<FacetItem> items =  genres.terms().stream().map(e->new FacetItem(e.name(), e.count())).collect(Collectors.toList());
                facets.add(new Facet("genres", items));            
            }
            DefaultNumericRangeFacetResult years = (DefaultNumericRangeFacetResult) result.facets().get("year");
            if(years != null & years.numericRanges().size() >0){

                List<FacetItem> items =  years.numericRanges().stream().map(e->new FacetItem(e.name(), e.count())).collect(Collectors.toList());
                Collections.sort(items, new Comparator<FacetItem>() {
                    @Override
                    public int compare(FacetItem o1, FacetItem o2) {
                        return o2.getName().compareTo(o1.getName());
                    }
                });
                facets.add(new Facet("year", items));            
            }
    
            rt.setFacets(facets);
        } else {
            rt.setResults(movies);
        }
        return rt;
    }

    private Map<String,List<String>> getFilters(String filters){
        if (filters == null || filters.trim().isEmpty()) {
            return new HashMap<>();
        }
        String[] values = filters.split("::");

        Map<String,List<String>> facets = new HashMap<>();
        for(int i=0;i<values.length; i++) {

            String[] test = values[i].split("=");
            if(test.length > 1) {
                facets.put(test[0], Arrays.asList(test[1].split(",")));
            }
        }
        return facets;
    }
}
