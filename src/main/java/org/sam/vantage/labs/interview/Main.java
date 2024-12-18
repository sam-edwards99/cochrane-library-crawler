package org.sam.vantage.labs.interview;

import org.sam.vantage.labs.interview.crawler.LibraryCrawler;
import org.sam.vantage.labs.interview.dto.ReviewDTO;
import org.sam.vantage.labs.interview.dto.TopicDTO;
import org.sam.vantage.labs.interview.file.ReviewFileWriter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        LibraryCrawler libraryCrawler = new LibraryCrawler();

        // get all topics
        List<TopicDTO> topics = libraryCrawler.getTopicList();

        // get all reviews by topic
        List<ReviewDTO> reviews = libraryCrawler.getReviewsInAllTopics(topics);
        System.out.println("Total reviews collected:" + reviews.size());

        ReviewFileWriter reviewFileWriter = new ReviewFileWriter();
        reviewFileWriter.writeReviewsToFile(reviews);
    }
}