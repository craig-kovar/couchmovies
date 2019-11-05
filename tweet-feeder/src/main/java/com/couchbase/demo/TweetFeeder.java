package com.couchbase.demo;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;


/**
 * Hello world!
 */
public final class TweetFeeder {
    private TweetFeeder() {
    }

    /**
     * Says hello to the world.
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        Logger clientLogger = Logger.getLogger("com.couchbase.client");
        clientLogger.setLevel(Level.WARNING);

        System.out.println("Starting Demo Tweet Feeder");

        Cluster cluster = CouchbaseCluster.create("127.0.0.1");
        try {
            Bucket sourceBucket = cluster.openBucket("tweetsource", "password");
            System.out.println("Connected to tweetsource bucket");
            Bucket targetBucket = cluster.openBucket("tweettarget", "password");
            System.out.println("Connected to tweettarget bucket");

            N1qlQuery sourceQuery = N1qlQuery.simple("SELECT * from `tweetsource` where latitude is not null LIMIT 1000");
            N1qlQueryResult sourceResults = sourceBucket.query(sourceQuery);

            System.out.println("Requested source data. Success: " + sourceResults.finalSuccess() + " Row count:" + sourceResults.info().resultCount());

         Iterator<N1qlQueryRow> tweets = sourceResults.rows();
         int count = 0;
         while(tweets.hasNext()) {
             JsonObject tweet = tweets.next().value().getObject("tweetsource");
                Long id = tweet.getLong("tweetId");
                JsonDocument doc = JsonDocument.create("tweet::" + id, tweet);
                targetBucket.upsert(doc);
                count = count + 1;
                System.out.println("Inserted tweet id: " + id + " total: " + count );
                TimeUnit.MILLISECONDS.sleep(250); 
         }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cluster.disconnect();
            System.out.println("Disconnected from cluster");
        }
     



    }


}
