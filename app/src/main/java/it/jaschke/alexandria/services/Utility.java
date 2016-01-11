package it.jaschke.alexandria.services;

import java.io.IOException;

/**
 * Created by Admin on 8/01/2016.
 */
public class Utility {

        public Utility(){}

    public boolean isOnline() {

        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException e)          { e.printStackTrace(); }
        catch (InterruptedException e) { e.printStackTrace(); }

        return false;
    }

    // Status values to broadcast to the Activity

    // The download is starting
    public static final int STATE_ACTION_STARTED = 0;

    // The background thread is connecting to the RSS feed
    public static final int STATE_ACTION_CONNECTING = 1;

    // The background thread is parsing the RSS feed
    public static final int STATE_ACTION_PARSING = 2;

    // The background thread is writing data to the content provider
    public static final int STATE_ACTION_WRITING = 3;

    // The background thread is done
    public static final int STATE_ACTION_COMPLETE = 4;


    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS = "it.jaschke.alexandria.STATUS";

    //
    public static final String BROADCAST_ACTION = "it.jaschke.alexandria.BROADCAST";

}
