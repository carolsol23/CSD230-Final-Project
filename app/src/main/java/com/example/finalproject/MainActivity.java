/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.finalproject;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The WhoWroteIt app queries the Book Search API for books based
 * on a user's search.  It uses an AsyncTask to run the search task in
 * the background.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.searchButton.setOnClickListener(this::searchBooks);
    }

    /**
     * onClick handler for the "Search Books" button.
     *
     * @param view The view (Button) that was clicked.
     */
    public void searchBooks(View view) {
        // Get the search string from the input field.
        String queryString = binding.bookInput.getText().toString();

        // Hide the keyboard when the button is pushed.
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }

        // Check the status of the network connection.
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }

        // If the network is available, connected, and the search field
        // is not empty, start a CompletableFuture.
        if (networkInfo != null && networkInfo.isConnected()
                && queryString.length() != 0) {
//            new FetchBook(binding.titleText, binding.authorText).execute(queryString);
            // Use the getBookInfo() method in the NetworkUtils class to make
            // the connection in the background.
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    ()-> NetworkUtils.getBookInfo(queryString));
            binding.authorText.setText("");
            binding.titleText.setText(R.string.loading);
            /*
             * @return Returns the JSON string from the Books API, or
             * null if the connection failed.
             */
            String json = null;
            try {
                json = future.get();
            } catch (ExecutionException | InterruptedException ignored) {}
            onPostExecute(json);
        }
        // Otherwise update the TextView to tell the user there is no
        // connection, or no search term.
        else {
            if (queryString.length() == 0) {
                binding.authorText.setText("");
                binding.titleText.setText(R.string.no_search_term);
            } else {
                binding.authorText.setText("");
                binding.titleText.setText(R.string.no_network);
            }
        }
    }

    /**
     * Handles the results on the UI thread. Gets the information from
     * the JSON result and updates the views.
     *
     * @param s Result from the doInBackground() method containing the raw
     *          JSON response, or null if it failed.
     */
    protected void onPostExecute(String s) {
//        super.onPostExecute(s);

        try {
            // Convert the response into a JSON object.
            JSONObject jsonObject = new JSONObject(s);
            // Get the JSONArray of book items.
            JSONArray itemsArray = jsonObject.getJSONArray("items");

            // Initialize iterator and results fields.
            int i = 0;
            String title = null;
            String authors = null;

            // Look for results in the items array, exiting when both the
            // title and author are found or when all items have been checked.
            while (i < itemsArray.length() &&
                    (authors == null && title == null)) {
                // Get the current item information.
                JSONObject book = itemsArray.getJSONObject(i);
                JSONObject volumeInfo = book.getJSONObject("volumeInfo");

                // Try to get the author and title from the current item,
                // catch if either field is empty and move on.
                try {
                    title = volumeInfo.getString("title");
                    authors = volumeInfo.getString("authors");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Move to the next item.
                i++;
            }

            // If both are found, display the result.
            if (title != null && authors != null) {
                binding.titleText.setText(title);
                binding.authorText.setText(authors);
            } else {
                // If none are found, update the UI to show failed results.
                binding.titleText.setText(R.string.no_results);
                binding.authorText.setText("");
            }

        } catch (Exception e) {
            // If onPostExecute() does not receive a proper JSON string,
            // update the UI to show failed results.
            binding.titleText.setText(R.string.no_results);
            binding.authorText.setText("");
            e.printStackTrace();
        }

    }
}
