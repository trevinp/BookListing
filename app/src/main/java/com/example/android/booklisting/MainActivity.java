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
                TextView tv = view.findViewById(R.id.bookTitle);
                Toast.makeText(getApplicationContext(), "Title: " + tv.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void doSearch(View v) {
        if (isNetworkAvailable()) {
            BookAsyncTask task = new BookAsyncTask();
            task.execute();
        }
        else
        {
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
            URL url = createUrl(GOOGLE_BOOK_REQUEST_URL + searchTerm + "&maxResults=10");

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
            }
            else {
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

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}

