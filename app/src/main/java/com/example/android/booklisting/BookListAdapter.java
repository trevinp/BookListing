package com.example.android.booklisting;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class BookListAdapter extends ArrayAdapter<BookResult> {
    public BookListAdapter(@NonNull Context context, ArrayList<BookResult> books) {
        super(context, 0, books);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;

        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item, parent, false);
        }

        BookResult currentBook = getItem(position);

        TextView title = listItemView.findViewById(R.id.book_title);
        title.setText(currentBook.title);

        TextView author = listItemView.findViewById(R.id.book_author);
        author.setText(currentBook.author);

        TextView pubDate = listItemView.findViewById(R.id.book_published);
        pubDate.setText(currentBook.datePublished);

        return listItemView;
    }
}
