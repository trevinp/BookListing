package com.example.android.booklisting;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String GOOGLE_BOOK_REQUEST_URL =
            "https://www.googleapis.com/books/v1/volumes?q=";
    public static final int READ_TIMEOUT = 10000;
    public static final int CONNECT_TIMEOUT = 15000;
    public static final String MAX_RESULTS_20 = "&maxResults=20";

    private static ArrayList<BookResult> mBookResults;
    private static ListView mListView;
    private static ArrayAdapter<BookResult> mAdapter;
    private static Parcelable mListState = null;
    private final String LIST_STATE = "listState";

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mListState = state.getParcelable(LIST_STATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mListState != null) {
            mListView.setAdapter(mAdapter);
            mListView.onRestoreInstanceState(mListState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        mListState = mListView.onSaveInstanceState();
        state.putParcelable(LIST_STATE, mListState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView view = findViewById(R.id.textNoResults);
        view.setVisibility(View.GONE);
        ListView lv = findViewById(R.id.list);
        mListView = lv;
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView tv = view.findViewById(R.id.book_title);
                Toast.makeText(getApplicationContext(), "Title: " + tv.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void doSearch(View v) {
        if (isNetworkAvailable()) {
            BookAsyncTask task = new BookAsyncTask();
            task.execute();
        } else {
            ListView listView = findViewById(R.id.list);
            mAdapter = null;
            listView.setAdapter(null);
            TextView view = findViewById(R.id.textNoResults);
            view.setVisibility(View.VISIBLE);
            view.setText(R.string.no_internet);
            mBookResults = null;
        }
    }

    private class BookAsyncTask extends AsyncTask<URL, Void, ArrayList<BookResult>> {

        @Override
        protected ArrayList<BookResult> doInBackground(URL... urls) {
            EditText search = findViewById(R.id.txtSearch);
            String searchTerm = search.getText().toString();
            URL url = createUrl(GOOGLE_BOOK_REQUEST_URL + searchTerm + MAX_RESULTS_20);

            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem making the HTTP request.", e);
            }

            ArrayList<BookResult> books = extractBookFromJson(jsonResponse);
            return books;
        }

        @Override
        protected void onPostExecute(ArrayList<BookResult> data) {
            if (data != null) {
                TextView view = findViewById(R.id.textNoResults);
                view.setVisibility(View.GONE);
                BookListAdapter adapter = new BookListAdapter(MainActivity.this, data);
                mAdapter = adapter;
                ListView listView = findViewById(R.id.list);
                listView.setAdapter(adapter);
                mBookResults = data;
            } else {
                ListView listView = findViewById(R.id.list);
                mAdapter = null;
                listView.setAdapter(null);
                TextView view = findViewById(R.id.textNoResults);
                view.setVisibility(View.VISIBLE);
                mBookResults = null;
            }
        }
    }

    private ArrayList<BookResult> extractBookFromJson(String bookJSON) {
        if (TextUtils.isEmpty(bookJSON)) {
            return null;
        }

        ArrayList<BookResult> books = new ArrayList<>();
        try {
            JSONObject baseJsonResponse = new JSONObject(bookJSON);
            JSONArray itemArray = null;
            // Get array holding the book items
            if (baseJsonResponse.has("items")) {
                itemArray = baseJsonResponse.getJSONArray("items");
            }
            if (itemArray == null) {
                return null;
            }

            for (int i = 0; i < itemArray.length(); i++) {
                if (itemArray.length() > 0) {
                    // Get book information and add to book list
                    JSONObject volumeInfo = null;
                    String title = null;
                    JSONArray authors = null;
                    String publishedDate = null;
                    JSONObject bookItem = itemArray.getJSONObject(i);

                    if (bookItem.has("volumeInfo")) {
                        volumeInfo = bookItem.getJSONObject("volumeInfo");
                        if (volumeInfo.has("title")) {
                            title = volumeInfo.getString("title");
                        }
                        if (volumeInfo.has("authors")) {
                            authors = volumeInfo.getJSONArray("authors");
                        }
                        if (volumeInfo.has("publishedDate")) {
                            publishedDate = volumeInfo.getString("publishedDate");
                        }
                    }

                    String allAuthors = null;
                    if (authors != null) {
                        for (int j = 0; j < authors.length(); j++) {
                            String author = authors.getString(j);
                            if (!author.isEmpty()) {
                                allAuthors = author;
                            } else if (j == authors.length() - 1) {
                                allAuthors = allAuthors + " and " + author;
                            } else {
                                allAuthors = allAuthors + ", " + author;
                            }
                        }
                    } else {
                        allAuthors = "Unknown";
                    }
                    BookResult book = new BookResult(title, allAuthors, publishedDate);
                    books.add(book);
                }
            }
            return books;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the book JSON results", e);
        }
        return null;
    }

    private URL createUrl(String stringUrl) {
        URL url;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException exception) {
            Log.e(LOG_TAG, "Error with creating URL", exception);
            return null;
        }
        return url;
    }

    private String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(READ_TIMEOUT /* milliseconds */);
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT /* milliseconds */);
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // function must handle java.io.IOException here
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    private String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}

