package org.sam.vantage.labs.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDTO {
    private String url;
    private String topic;
    private String title;
    private String author;
    private LocalDate date;

    @Override
    public String toString(){
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return String.join("|", url, topic, title, author, date.format(dateFormat));
    }
}
