package com.mehmetemineker.bitcoinlivetrading;

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pusher.android.PusherAndroid;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    private OkHttpClient clientGdax;
    private OkHttpClient clientBitfinex;

    private ProgressBar progressBar;
    private LinearLayout main_view;
    private TextView textViewPrice;
    private TextView textViewChange;
    private TextView textViewChannel;

    private DecimalFormat formatterPrice;
    private DecimalFormat formatterRate;

    private double lastPriceValue;

    private boolean isFisrtLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isFisrtLoad = true;

        formatterPrice = new DecimalFormat("#0.0");
        formatterRate = new DecimalFormat("%#0.0000");

        clientGdax = new OkHttpClient();
        clientBitfinex = new OkHttpClient();

        main_view = (LinearLayout)findViewById(R.id.main_view);
        textViewPrice = (TextView)findViewById(R.id.textViewPrice);
        textViewChange = (TextView)findViewById(R.id.textViewChange);
        textViewChannel = (TextView)findViewById(R.id.textViewChannel);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        textViewPrice.setVisibility(View.GONE);
        textViewChange.setVisibility(View.GONE);
        textViewChannel.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        BitstampPusherConnect();
        BitfinexWebSocketConnect();
        GdaxWebSocketConnect();
    }

    private void SetTextViewsVisible(){
        textViewPrice.setVisibility(View.VISIBLE);
        textViewChange.setVisibility(View.VISIBLE);
        textViewChannel.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void SetPriceChangeValue(double newPriceValue){
        if(isFisrtLoad){
            lastPriceValue = newPriceValue;
            isFisrtLoad = false;
        }

        Log.w("newPriceValue: ", String.valueOf(newPriceValue));
        Log.w("lastPriceValue: ", String.valueOf(lastPriceValue));


        double rate = (newPriceValue - lastPriceValue) * 100 / lastPriceValue;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if(rate > 0){
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDarkGreen));
            }else if(rate < 0){
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDarkRed));
            }else{
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            }
        }

        if(rate > 0){
            main_view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryGreen));
        }else if(rate < 0){
            main_view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryRed));
        }else{
            main_view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }




        Log.w("rate: ", String.valueOf(rate));

        rate = Math.abs(rate);
        textViewChange.setText(formatterRate.format(rate));

        lastPriceValue = newPriceValue;
    }

    private void BitfinexWebSocketConnect(){
        Request request = new Request.Builder().url("wss://api.bitfinex.com/ws/2").build();

        WebSocket ws = clientBitfinex.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send("{\"event\": \"subscribe\",\"channel\": \"trades\",\"symbol\": \"tBTCUSD\"}");
            }

            @Override
            public void onMessage(WebSocket webSocket, final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(text.indexOf("\"tu\"") != -1){
                            try {
                                JSONArray jsonArray = new JSONArray(text);
                                JSONArray data = jsonArray.getJSONArray(2);
                                double price = data.getDouble(3);

                                textViewPrice.setText(formatterPrice.format(price));
                                textViewChannel.setText(getString(R.string.bitfinex));

                                SetPriceChangeValue(price);
                                SetTextViewsVisible();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
            }
        });

        clientBitfinex.dispatcher().executorService().shutdown();
    }

    private void GdaxWebSocketConnect(){
        Request request = new Request.Builder().url("wss://ws-feed.gdax.com").build();

        WebSocket ws = clientGdax.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send("{\"type\": \"subscribe\",\"product_id\": \"BTC-USD\"}");
            }

            @Override
            public void onMessage(WebSocket webSocket, final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(text.indexOf("match") != -1) {
                            try {
                                JSONObject jsonObject = new JSONObject(text);
                                double price = jsonObject.getDouble("price");

                                textViewPrice.setText(formatterPrice.format(price));
                                textViewChannel.setText(getString(R.string.gdax));

                                SetPriceChangeValue(price);
                                SetTextViewsVisible();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
            }
        });

        clientGdax.dispatcher().executorService().shutdown();
    }

    private void BitstampPusherConnect(){
        PusherAndroid pusher = new PusherAndroid("de504dc5763aeef9ff52");
        Channel channel = pusher.subscribe("live_trades");

        channel.bind("trade", new SubscriptionEventListener() {
            @Override
            public void onEvent(String channel, String event,final String data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            double price = jsonObject.getDouble("price_str");

                            textViewPrice.setText(formatterPrice.format(price));
                            textViewChannel.setText(getString(R.string.bitstamp));

                            SetPriceChangeValue(price);
                            SetTextViewsVisible();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        pusher.connect();
    }
}
