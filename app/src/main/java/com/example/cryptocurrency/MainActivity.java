/**
 * This is the Main class activity
 *
 * Project description: This Application allows tracking Cryptocurrencies value change.
 * Cryptocurrencies are loaded using https://min-api.cryptocompare.com/ API. They are arranged by
 * 24H Top Tier Volume. First 20 Cryptocurrencies are shown initially. After scrolling to the end of
 * list, another 20 Cryptocurrencies it would be loaded. Every Cryptocurrency is made of an image as
 * a logo, name and symbol. The data is also entered into the database. It is convenient when users
 * do not have internet access, the data can be loaded from the database. After selecting a
 * cryptocurrency, a new form will open. It is a form with a tab menu and two tabs. The first tab
 * shows a complete list of information for selected Criptocurrency and compares its value with
 * some important values. The second tab shows graphical changes for selected one. This view
 * consists of 3 charts, the first representing the change from day to day, the second from hour to
 * hour and the third from minute to minute. A default comparison is provided with Bitcoin(BTC).
 * The user can add a new comparison and delete what has already been shown.
 *
 * @author  Milos Cabrilo
 * @version 1.0
 * @since   12/4/2019
 */
package com.example.cryptocurrency;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String ARG_NAME_FROM_MAIN = "name_coin";
    private static final String ARG_IMAGE_FROM_MAIN = "image_coin";
    private static final String ARG_SYMBOL_FROM_MAIN = "symbol_coin";
    private static final String ARG_LIST_SYMBOL = "list_symbol";

    private static final String CRYPTOCOMPARE_URL = "https://www.cryptocompare.com";
    private static final String API_CRYPTOCOMPARE_URL = "https://min-api.cryptocompare.com/data/top/totaltoptiervolfull?tsym=USD&limit=";
    private List<String> listSymbol;
    private ListView listView;
    private CoinArrayAdapter coinArrayAdapter;
    public static RequestQueue mQueue;
    public static DatabaseHandler db;
    private final int numberDataPerPages = 20;   // number of Cryptocurrencies returned


    // Main Activity create
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialization parameters.
        mQueue = Volley.newRequestQueue(this);
        db = new DatabaseHandler(this);

        listView = (ListView) findViewById(R.id.listView);
        coinArrayAdapter = new CoinArrayAdapter(getApplicationContext(), R.layout.list_of_coins);
        listView.setAdapter(coinArrayAdapter);
        listView.setFocusable(false);
        listSymbol = new ArrayList<>();
        listSymbol.add("Select");


        // If users don't have an internet access, data from the Database is loaded. In the second
        // case, the old data from the Database will be wiped out and refreshed with a new one.
        if(!isNetworkConnected()) {
            Cursor res = db.readCoinsFromDB();
            while(res.moveToNext()) {
                Coin coin = new Coin( res.getString(2), res.getString(0), res.getString(1));
                listSymbol.add(res.getString(1));
                coinArrayAdapter.add(coin);
            }
        }
        else {
            // If network is connected, delete all previous tables and create new ones according to the activities in the app.
            db.deleteAllTables();
            // Use readCryptocurrencies method to get Cryptocurrencies.
            if (coinArrayAdapter.getCount() == 0) {
                try {
                    readCryptocurrenciesFromUrl(coinArrayAdapter.getCount());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // OnClick Listener. Opens a new layout with general information about the Cryptocurrency clicked.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, TabMenuForSelectedCoin.class);
                intent.putExtra(ARG_NAME_FROM_MAIN, coinArrayAdapter.getItem(position).getNameCoin());
                intent.putExtra(ARG_SYMBOL_FROM_MAIN, coinArrayAdapter.getItem(position).getSymbolCoin());
                intent.putExtra(ARG_IMAGE_FROM_MAIN, coinArrayAdapter.getItem(position).getImageCoin());
                intent.putStringArrayListExtra(ARG_LIST_SYMBOL, (ArrayList<String>)listSymbol);
                startActivity(intent);
            }
        });

        // Scroll Listener. Expands the list of loaded Cryptocurrencies.
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int firstItem = -1;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            // Load new Cryptocurrencies only if user scrolled to bottom of list.
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount < totalItemCount && (firstVisibleItem + visibleItemCount == totalItemCount)) {
                    if (firstVisibleItem != firstItem) {
                        firstItem = firstVisibleItem;
                        try {
                            readCryptocurrenciesFromUrl(coinArrayAdapter.getCount());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    /**
     * JSON deserialize method. Cryptocurrencies are arranged by 24H Top Tier Volume.
     *
     * @param countCoins    - Number of shown elements
     */
    private void readCryptocurrenciesFromUrl(int countCoins) throws JSONException {
        // Read data from URL
        String url = API_CRYPTOCOMPARE_URL + numberDataPerPages + "&page=" + countCoins / numberDataPerPages;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray data = response.getJSONArray("Data");     // find Array with name DATA
                    for(int i=0; i<data.length();i++) {
                        // Get important element from Array DATA
                        JSONObject objectData = data.getJSONObject(i);
                        JSONObject coinInfo = objectData.getJSONObject("CoinInfo");
                        String symbol = coinInfo.getString("Name");
                        String coinName = coinInfo.getString("FullName");
                        String imageUrl = CRYPTOCOMPARE_URL + coinInfo.getString("ImageUrl");
                        Coin coin = new Coin(imageUrl, coinName, symbol);
                        coinArrayAdapter.add(coin);
                        listSymbol.add(symbol);

                        // Insert loaded Coin into database
                        db.writeCoinIntoDB(coin);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        mQueue.add(request);
    }

    // Checking network access.
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

}