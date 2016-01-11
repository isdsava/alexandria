package it.jaschke.alexandria.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.squareup.okhttp.internal.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.R;
import it.jaschke.alexandria.content.AlexandriaContract;
import it.jaschke.alexandria.data.BookObject;
import it.jaschke.alexandria.data.IApiMethods;
import it.jaschke.alexandria.data.VolumeInfo;
import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import it.jaschke.alexandria.services.Utility;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class BookService extends IntentService {

    private final String LOG_TAG = BookService.class.getSimpleName();

    public static final String FETCH_BOOK = "it.jaschke.alexandria.services.action.FETCH_BOOK";
    public static final String DELETE_BOOK = "it.jaschke.alexandria.services.action.DELETE_BOOK";

    public static final String EAN = "it.jaschke.alexandria.services.extra.EAN";

    // Defines and instantiates an object for handling status updates.
    private BroadcastNotifier mBroadcaster = new BroadcastNotifier(this);

    public BookService() {
        super("Alexandria");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (FETCH_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                fetchBook(ean);
            } else if (DELETE_BOOK.equals(action)) {
                final String ean = intent.getStringExtra(EAN);
                deleteBook(ean);
            }
        }
    }

    /**
     * Handle action deleteBook in the provided background thread with the provided
     * parameters.
     */
    private void deleteBook(String ean) {
        if(ean!=null) {
            getContentResolver().delete(AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)), null, null);
        }
    }

    /**
     * Handle action fetchBook in the provided background thread with the provided
     * parameters.
     */
    private void fetchBook(String ean) {
        Utility utility = new Utility();
        if(ean.length()!=13){
            return;
        }

        Cursor bookEntry = getContentResolver().query(
                AlexandriaContract.BookEntry.buildBookUri(Long.parseLong(ean)),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        if(bookEntry.getCount()>0){
            bookEntry.close();
            mBroadcaster.broadcastIntentWithState(Utility.STATE_ACTION_COMPLETE);
            return;
        }

        bookEntry.close();

        //don't  try load if not online
     if (utility.isOnline()) {
         try {

             final String FORECAST_BASE_URL = "https://www.googleapis.com/";
             final String ISBN_PARAM = "isbn:";   //+ ean;

             Retrofit retrofit = new Retrofit.Builder()
                     .baseUrl(FORECAST_BASE_URL)
                     .addConverterFactory(GsonConverterFactory.create())
                     .build();


             IApiMethods iApiMethods = retrofit.create(IApiMethods.class);

             Call<BookObject> bookObjectCall = iApiMethods.getBooks(ISBN_PARAM + ean,getString(R.string.api_key));

             try {
                 Response<BookObject> bookObjectResponse = bookObjectCall.execute();

                 //This should take care of 200 response but with no data and a 400 response
                 if (!bookObjectResponse.isSuccess() || bookObjectResponse.body().totalItems == 0) {
                     Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
                     messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.not_found));
                     LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
                     return;
                 }
                 //Assuming singular ISBN which is completely reasonable
                 VolumeInfo volumeInfo = bookObjectResponse.body().items.get(0).volumeInfo;
                 String title = volumeInfo.title == null ? "" : volumeInfo.title;
                 ;
                 String subtitle = volumeInfo.subtitle == null ? "" : volumeInfo.subtitle;
                 ;
                 String desc = volumeInfo.description == null ? "" : volumeInfo.description;
                 String imgUrl = volumeInfo.imageLinks == null ? "" : volumeInfo.imageLinks.thumbnail;

                 // might be able to use the definitions of POJO instead to create default values.
                 writeBackBook(ean, title, subtitle, desc, imgUrl);
                 writeBackAuthors(ean, volumeInfo.authors);
                 writeBackCategories(ean, volumeInfo.categories);


                 mBroadcaster.broadcastIntentWithState(Utility.STATE_ACTION_COMPLETE);

             } catch (IOException e) {
                 Log.v("alexandria", "retrofit call failed");
             }
         } catch (Exception e) {

         }

     }else{
         Intent messageIntent = new Intent(MainActivity.MESSAGE_EVENT);
         messageIntent.putExtra(MainActivity.MESSAGE_KEY, getResources().getString(R.string.no_internets));
         LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(messageIntent);
         return;
     }



    }

    private void writeBackBook(String ean, String title, String subtitle, String desc, String imgUrl) {
        ContentValues values= new ContentValues();
        values.put(AlexandriaContract.BookEntry._ID, ean);
        values.put(AlexandriaContract.BookEntry.TITLE, title);
        values.put(AlexandriaContract.BookEntry.IMAGE_URL, imgUrl);
        values.put(AlexandriaContract.BookEntry.SUBTITLE, subtitle);
        values.put(AlexandriaContract.BookEntry.DESC, desc);
        getContentResolver().insert(AlexandriaContract.BookEntry.CONTENT_URI,values);
    }

    private void writeBackAuthors(String ean, List listArray) throws Exception {
        ContentValues values = new ContentValues();

        if (listArray.size() == 0) {
            values.put(AlexandriaContract.AuthorEntry._ID, ean);
            values.put(AlexandriaContract.AuthorEntry.AUTHOR, "Unknown");
            getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);


        } else {
            for (int i = 0; i < listArray.size(); i++) {
                values.put(AlexandriaContract.AuthorEntry._ID, ean);
                values.put(AlexandriaContract.AuthorEntry.AUTHOR, listArray.get(i).toString());
                getContentResolver().insert(AlexandriaContract.AuthorEntry.CONTENT_URI, values);
                values = new ContentValues();
            }

        }
    }

    private void writeBackCategories(String ean, List listArray) throws JSONException {
        ContentValues values= new ContentValues();
        for (int i = 0; i < listArray.size(); i++) {
            values.put(AlexandriaContract.CategoryEntry._ID, ean);
            values.put(AlexandriaContract.CategoryEntry.CATEGORY, listArray.get(i).toString());
            getContentResolver().insert(AlexandriaContract.CategoryEntry.CONTENT_URI, values);
            values= new ContentValues();
        }
    }
 }