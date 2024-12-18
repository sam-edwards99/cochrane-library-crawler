package org.sam.vantage.labs.interview.file;

import org.sam.vantage.labs.interview.dto.ReviewDTO;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

// handles writing to file
public class ReviewFileWriter {
    public void writeReviewsToFile(List<ReviewDTO> reviews){
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("reviews.txt");
        } catch (IOException e) {
            System.out.println("Error opening output file for write.");
        } finally {
            for (ReviewDTO review : reviews) {
                try {
                    fileWriter.write(review.toString());
                    fileWriter.write("\n");
                } catch (IOException e) {
                    System.out.println("Error writing review to file.");
                }
            }
            try {
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error closing file.");
            }
        }

    }
}
