package org.sam.vantage.labs.interview.crawler;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sam.vantage.labs.interview.dto.ReviewDTO;
import org.sam.vantage.labs.interview.dto.TopicDTO;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LibraryCrawler {
    private final CloseableHttpClient httpclient = HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build())
        .build();

    private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36";
    private final String BASE_URL = "https://www.cochranelibrary.com";

    private final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US);
    // fetch all review topics. will use the topic list to get all reviews for each topic
    public List<TopicDTO> getTopicList() throws IOException {
        HttpResponse httpresponse;

        HttpGet httpget = new HttpGet("https://www.cochranelibrary.com/cdsr/reviews/topics");
        httpget.addHeader("User-Agent", USER_AGENT);
        httpresponse = httpclient.execute(httpget);
        System.out.println("Request status:" + httpresponse.getStatusLine().getStatusCode());

        Document doc = Jsoup.parse(IOUtils.toString(httpresponse.getEntity().getContent(), StandardCharsets.UTF_8));
        Elements elements = doc.getElementsByClass("browse-by-list-item");

        List<TopicDTO> topics = new ArrayList<>();
        for (Element element : elements) {
            topics.add(TopicDTO.builder()
                    .url(element.getElementsByTag("a").attr("href"))
                    .name(element.getElementsByTag("a").text())
                    .build()
            );
        }
        System.out.println("Topics:");
        topics.forEach(topic -> System.out.println(topic.getName()));
        System.out.println();

        return topics;
    }

    // loop thru each topic and collect all reviews in the list of topics
    public List<ReviewDTO> getReviewsInAllTopics(List<TopicDTO> topics) throws IOException, InterruptedException, URISyntaxException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<ReviewDTO> reviews = new ArrayList<>();

        // parse all reviews for each topic
        for(TopicDTO topic: topics){
            System.out.println("Starting Topic: " + topic.getName());
            List<ReviewDTO> reviewsForTopic = getReviewsInTopic(topic);
            System.out.println(stopWatch.getTime(TimeUnit.SECONDS) + "s " + reviewsForTopic.size() + " Reviews for Topic " + topic.getName());
            reviews.addAll(reviewsForTopic);
            System.out.println(stopWatch.getTime(TimeUnit.SECONDS) + "s Total Review Count: " + reviews.size());
            System.out.println();
        }
        stopWatch.stop();
        System.out.println("Total Time: " + stopWatch.getTime(TimeUnit.SECONDS) + "s");
        System.out.println("Total Review Count: " + reviews.size());

        return reviews;

    }

    // get all reviews in a single topic
    private List<ReviewDTO> getReviewsInTopic(TopicDTO topic) throws URISyntaxException, IOException, InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        HttpGet httpget = new HttpGet();
        HttpResponse httpresponse;
        List<ReviewDTO> reviews = new ArrayList<>();
        // set resultPerPage to max value to make as few requests as possible
        URI nextPageLink = new URIBuilder(topic.getUrl()).addParameter("resultPerPage", "100").build();
        boolean hasNextPage = true;


        int topicReviewCount = 0;

        // handle pagination
        while(hasNextPage) {
            httpget.setURI(nextPageLink);
            httpget.addHeader("User-Agent", USER_AGENT);
            try {
                httpresponse = httpclient.execute(httpget);
                System.out.println(stopWatch.getTime(TimeUnit.SECONDS) + "s Request status: " + httpresponse.getStatusLine().getStatusCode());

                Document doc = Jsoup.parse(IOUtils.toString(httpresponse.getEntity().getContent(), StandardCharsets.UTF_8));
                Elements reviewElements = doc.getElementsByClass("search-results-item-body");
                List<ReviewDTO> reviewsForPage = parseReviews(reviewElements, topic.getName());

                topicReviewCount += reviewsForPage.size();
                reviews.addAll(reviewsForPage);

                try {
                    nextPageLink = URI.create(doc.getElementsByClass("pagination-next-link")
                            .first()
                            .getElementsByTag("a")
                            .first()
                            .attr("href"));
                } catch (NullPointerException e) {
                    hasNextPage = false;
                }
            } finally {
                httpget.releaseConnection();
            }
        }
        // delay to avoid getting rate limited (Adds 2 second delay between pages)
        Thread.sleep(2000);
        return reviews;

    }

    // map the html response to a list of review DTOs
    private List<ReviewDTO> parseReviews(Elements elements, String topic){
        List<ReviewDTO> reviews = new ArrayList<>();
        for(Element element : elements){
            reviews.add(parseReview(element, topic));
        }
        return reviews;
    }

    // map the html response to a review DTO
    private ReviewDTO parseReview(Element element, String topic){
        ReviewDTO review = ReviewDTO.builder()
                .url(BASE_URL + element.getElementsByClass("result-title").first().getElementsByTag("a").first().attr("href"))
                .topic(topic)
                .title(element.getElementsByClass("result-title").first().getElementsByTag("a").first().text())
                .author(element.getElementsByClass("search-result-authors").first().getElementsByTag("div").first().text())
                .date(LocalDate.parse(element.getElementsByClass("search-result-date")
                        .first()
                        .getElementsByTag("div")
                        .first()
                        .text(), TIME_FORMAT))
                .build();
        return review;
    }

}
