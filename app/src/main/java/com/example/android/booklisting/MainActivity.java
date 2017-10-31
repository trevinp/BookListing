package com.example.android.booklisting;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String GOOGLE_BOOK_REQUEST_URL1 =
            "https://www.googleapis.com/books/v1/volumes?q=android&maxResults=3";

    private static final String GOOGLE_BOOK_REQUEST_URL =
            "https://www.googleapis.com/books/v1/volumes?q=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void doSearch(View v) {
        BookAsyncTask task = new BookAsyncTask();
        task.execute();
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
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
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

    private class BookAsyncTask extends AsyncTask<URL, Void, List<BookResult>> {

        @Override
        protected List<BookResult> doInBackground(URL... urls) {
            EditText search = findViewById(R.id.txtSearch);
            String searchTerm = search.getText().toString();
            URL url = createUrl(GOOGLE_BOOK_REQUEST_URL + searchTerm + "&maxResults=10");

            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem making the HTTP request.", e);
            }

            List<BookResult> books = extractBookFromJson(jsonResponse);
            return books;
        }

        @Override
        protected void onPostExecute(List<BookResult> data) {
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    Log.d("onPostExecute", data.get(i).toString());
                }

                TextView view = findViewById(R.id.txtResults);
                view.setText("There were " + data.size() + " results.");
            }
            else {
                TextView view = findViewById(R.id.txtResults);
                view.setText("No results found.");
            }
        }
    }

    private List<BookResult> extractBookFromJson(String bookJSON) {
        if (TextUtils.isEmpty(bookJSON)) {
            return null;
        }

        List<BookResult> books = new ArrayList<>();
        try {
            JSONObject baseJsonResponse = new JSONObject(bookJSON);
            // Get array holding the book items
            JSONArray itemArray = baseJsonResponse.getJSONArray("items");

            for (int i = 0; i < itemArray.length(); i++) {
                if (itemArray.length() > 0) {
                    // Get book information and add to book list
                    JSONObject bookItem = itemArray.getJSONObject(i);
                    JSONObject volumeInfo = bookItem.getJSONObject("volumeInfo");
                    String title = volumeInfo.getString("title");
                    JSONArray authors = volumeInfo.getJSONArray("authors");
                    String publishedDate = volumeInfo.getString("publishedDate");

                    BookResult book = new BookResult(title, authors.getString(0), publishedDate);
                    books.add(book);
                    //test
                }
            }
            return books;
        }
         catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the book JSON results", e);
        }
        return null;
    }
}

