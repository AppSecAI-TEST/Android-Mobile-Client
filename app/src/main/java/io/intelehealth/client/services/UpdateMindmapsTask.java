package io.intelehealth.client.services;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import io.intelehealth.client.R;
import io.intelehealth.client.utilities.HelperMethods;

/**
 * Created by Dexter Barretto on 8/7/17.
 * Github : @dbarretto
 */

public class UpdateMindmapsTask extends AsyncTask<String, Void, String> {
    String FILENAME, COLLECTION_NAME, FILE_LIST;
    static ProgressDialog progress;

    private static final String TAG = UpdateMindmapsTask.class.getSimpleName();

    WeakReference<Activity> mWeakActivity;

    private boolean isLastFile;

    public File base_dir;
    public String[] FILES;
    public UpdateMindmapsTask(Activity activity) {
        mWeakActivity = new WeakReference<>(activity);
    }

    public UpdateMindmapsTask(Activity activity, File base_dir, boolean isLastFile) {
        mWeakActivity = new WeakReference<>(activity);
        this.base_dir = base_dir;
        this.isLastFile = isLastFile;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Activity activity = mWeakActivity.get();
        if (activity != null  && progress == null) {
            progress = new ProgressDialog(activity);
        }
        if(progress!=null && !progress.isShowing()){
            progress.setTitle(activity.getString(R.string.please_wait_progress));
            progress.setMessage(activity.getString(R.string.downloading_mindmaps));
            progress.setCanceledOnTouchOutside(false);
            progress.show();
        }
    }

    @Override
    protected String doInBackground(String... params) {

        //SPACE SEPARATED NAMES ARE MADE UNDERSCORE SEPARATED
        FILENAME = params[0];
        COLLECTION_NAME = params[1];
        FILE_LIST = params[2];

        try {
            String servStr = HelperMethods.MIND_MAP_SERVER_URL + "classes/" + COLLECTION_NAME;
            URL url = new URL(servStr);
            Log.i("Connect", HelperMethods.MIND_MAP_SERVER_URL + "classes/" + COLLECTION_NAME);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("X-Parse-Application-Id", "app");
            urlConnection.setRequestProperty("X-Parse-REST-API-Key", "undefined");
            Log.i("RES->", "" + urlConnection.getResponseMessage());
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                return stringBuilder.toString();
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            deleteFolder(base_dir);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String response) {

        Activity activity = mWeakActivity.get();

        if (activity != null) {
            if (response == null) {
                Toast.makeText(activity, activity.getString(R.string.error_downloading_mindmaps), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String writable = "";
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("results");
            JSONObject finalresponse = jsonArray.getJSONObject(0);
            if (FILE_LIST == null)
                writable = finalresponse.getJSONObject("Main").toString();
            else
                writable = finalresponse.getString("FILES");
            Log.i("INFO", writable);
        } catch (JSONException e) {
            Log.e(TAG, "onPostExecute: ", e);
            deleteFolder(base_dir);
        }

        if (FILE_LIST == null) {
            //WRITE FILE in base_dir
            try {
                File mydir = new File(base_dir.getAbsolutePath(), FILENAME);
                if (!mydir.exists())
                    mydir.getParentFile().mkdirs();
                Log.i(TAG, "FNAM : " + FILENAME);
                FileOutputStream fileout = new FileOutputStream(mydir);
                OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
                outputWriter.write(writable);
                outputWriter.close();
            } catch (Exception e) {
                deleteFolder(base_dir);
                Log.e(TAG, "onPostExecute: ", e);
            }
        } else {
            String files[] = writable.split("\n");
            Log.i("FLEN", "" + files.length);
            FILES = new String[files.length];
            FILES = files;
            downloadMindMaps(activity,files.length);
        }
        

        Log.i(TAG, "onPostExecute: " + isLastFile);
        if(isLastFile){
            deleteFolder(new File(activity.getFilesDir().getAbsolutePath(), HelperMethods.JSON_FOLDER));
            File enginesDir = new File(activity.getFilesDir().getAbsolutePath(), HelperMethods.JSON_FOLDER);
            base_dir.renameTo(enginesDir);
            Log.i(TAG, "onPostExecute: " + isLastFile);
            progress.dismiss();
        }
    }

    //DOWNLOAD ALL MIND MAPS
    private void downloadMindMaps(Activity activity,Integer length) {
        base_dir = new File(activity.getFilesDir().getAbsolutePath(), HelperMethods.JSON_FOLDER_Update);
        Log.i(TAG, "downloadMindMaps: " + activity.getFilesDir().getAbsolutePath());
        if (!base_dir.exists()) base_dir.mkdirs();
        for (int i=0; i<length; i++){
            String file = FILES[i];
            String[] parts = file.split(".json");
            //Log.i("DOWNLOADING-->",parts[0].replaceAll("\\s+",""));
            if(i==length-1){
                new UpdateMindmapsTask(activity, base_dir,true).execute(file, parts[0].replaceAll("\\s+", ""), null);
            }else {
                new UpdateMindmapsTask(activity, base_dir,false).execute(file, parts[0].replaceAll("\\s+", ""), null);
            }
        }
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

}

