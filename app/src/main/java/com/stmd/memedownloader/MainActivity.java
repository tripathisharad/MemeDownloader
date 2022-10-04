package com.stmd.memedownloader;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

import android.widget.Toast;

import com.stmd.memedownloader.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int START_LEVEL = 1;
    private int mLevel = 0;
    private Button nextB, shareB;
    private InterstitialAd mInterstitialAd;
    private ActivityMainBinding binding;
    private ProgressBar progressBar;
    private ImageView memeImage;
    private String cUrl;
    private AdView bannerAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //binding the views
        progressBar = binding.progressBar;
        shareB = binding.shareButton;
        nextB = binding.nextButton;
        bannerAd = binding.adView;
        memeImage = binding.imageView;
        progressBar.setVisibility(View.INVISIBLE);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });


        //Load the banner Ad........... !
        try {

            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAd.loadAd(adRequest);

        } catch (Exception exception) {
            exception.printStackTrace();
        }


        nextB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mLevel++;
                if (mLevel % 5 == 0) {
                    showInterstitial();
                }


                progressBar.setVisibility(View.VISIBLE);
                try {
                    fetchMeme();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        shareB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, "Checkout this cool meme.. !" + cUrl);
                Intent memeShare = Intent.createChooser(intent, "Share this meme using....");
                startActivity(memeShare);

            }
        });

        // Load the InterstitialAd and set the adUnitId (defined in values/strings.xml).
        try {
            loadInterstitialAd();
        } catch (Exception e) {
            e.printStackTrace();
        }


//         Load the InterstitialAd......!
        try {

            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {

                        if (mInterstitialAd == null) {
                            loadInterstitialAd();
                        }

                        handler.postDelayed(this, 5000);
                    } catch (IllegalStateException ed) {
                        ed.printStackTrace();
                    }
                }
            };
            handler.postDelayed(runnable, 5000);

        } catch (Exception e) {

        }


    }


    public void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, getString(R.string.interstitial_ad_unit_id), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;


                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Called when fullscreen content is dismissed.
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        mInterstitialAd = null;
                                        Log.d(TAG, "The ad was dismissed.");
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        // Called when fullscreen content failed to show.
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        mInterstitialAd = null;
                                        Log.d(TAG, "The ad failed to show.");
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Called when fullscreen content is shown.
                                        Log.d(TAG, "The ad was shown.");
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i(TAG, loadAdError.getMessage());
                        mInterstitialAd = null;

                        String error = String.format(
                                Locale.ENGLISH,
                                "domain: %s, code: %d, message: %s",
                                loadAdError.getDomain(),
                                loadAdError.getCode(),
                                loadAdError.getMessage());
                        Log.d(TAG, error);
                    }
                });
    }

    private void showInterstitial() {
        // Show the ad if it"s ready. Otherwise toast and reload the ad.
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        } else {

            loadInterstitialAd();
        }
    }

    void fetchMeme() {


        String url = " https://meme-api.herokuapp.com/gimme";
        RequestQueue queue = Volley.newRequestQueue(this);
        progressBar.setVisibility(View.VISIBLE);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String ur = response.getString("url");
                            cUrl = ur;
                            Glide.with(MainActivity.this).load(ur).listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    Toast.makeText(MainActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.INVISIBLE);
                                    return false;

                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    progressBar.setVisibility(View.INVISIBLE);
                                    return false;
                                }
                            }).into(memeImage);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "We are getting error", Toast.LENGTH_SHORT).show();

                    }
                });

        queue.add(jsonObjectRequest);
    }

}