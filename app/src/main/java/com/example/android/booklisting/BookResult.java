package com.example.android.booklisting;


public class BookResult {

    public final String title;
    public final String author;
    public final String datePublished;

    public BookResult(String bookTitle, String bookAuthor, String publishDate)
    {
        title = bookTitle;
        author = bookAuthor;
        datePublished = publishDate;
    }

    @Override
    public String toString() {
        return "BookResult{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", datePublished='" + datePublished + '\'' +
                '}';
    }
}
