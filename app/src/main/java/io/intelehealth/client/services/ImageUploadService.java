package io.intelehealth.client.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.intelehealth.client.R;
import io.intelehealth.client.database.DelayedJobQueueProvider;
import io.intelehealth.client.database.LocalRecordsDatabaseHelper;
import io.intelehealth.client.utilities.HelperMethods;

import static io.intelehealth.client.services.ClientService.STATUS_SYNC_IN_PROGRESS;
import static io.intelehealth.client.services.ClientService.STATUS_SYNC_STOPPED;

/**
 * Created by harshish on 28/6/17.
 */

public class ImageUploadService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ImageUploadService(String name) {
        super(name);
    }

    public ImageUploadService(){
        super(TAG);
    }

    private static final String TAG = ImageUploadService.class.getSimpleName();

    private static final String LOG_TAG = ImageUploadService.class.getSimpleName();
    NotificationManager mNotifyManager;
    public int mId = 3;
    NotificationCompat.Builder mBuilder;
    private String patientId, visitId,patientUUID,visitUUID;

    int queueId;


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        Log.i(LOG_TAG, "Running");
        patientId = intent.getStringExtra("patientID");
        visitId = intent.getStringExtra("visitID");
        visitUUID = intent.getStringExtra("visitUUID");
        patientUUID = intent.getStringExtra("patientUUID");

        if (!intent.hasExtra("queueId")) {
            int id = addJobToQueue(intent);
            intent.putExtra("queueId", id);
        }

        queueId = intent.getIntExtra("queueId", -1);

        String query = "SELECT image_path FROM image_records WHERE patient_id = ? AND visit_id = ?";
        LocalRecordsDatabaseHelper databaseHelper = new LocalRecordsDatabaseHelper(this);
        SQLiteDatabase localdb = databaseHelper.getWritableDatabase();
        Cursor cursor = localdb.rawQuery(query, new String[]{patientId, visitId});
        List<String> imagePaths = new ArrayList<>();
        while (cursor.moveToNext()) {
            imagePaths.add(cursor.getString(0));
            Log.i(LOG_TAG + ">", cursor.getString(0));
        }
        cursor.close();
        localdb.close();
        queueSyncStart(queueId);
        for (String imagePath : imagePaths) {
            File file = new File(imagePath);
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            int endIndex = imagePath.lastIndexOf(File.separator);
            String imageName = "Default.jpg";
            if (endIndex != -1) {
                imageName = imagePath.substring(endIndex + 1, imagePath.length());
                Log.i(LOG_TAG, imageName);
            }
            String classname = file.getParentFile().getName();
            classname = classname.replaceAll("\\s+", "");
            Log.i(LOG_TAG, classname);
            if (HelperMethods.isNetworkAvailable(this)) {
                uploadImage(classname, bitmap, imageName, intent, imagePath);
            } else {
                String newText = "Failed to Post Images.";
                mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Image Upload")
                        .setContentText(newText);
                mNotifyManager.notify(mId, mBuilder.build());
                Log.d(LOG_TAG, "Person Image Posting Unsuccessful");
            }
        }
        queueSyncStop(queueId);
    }


    public void uploadImage(String classname, Bitmap bitmap, final String imageName, final Intent intent, final String imagePath) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] image = stream.toByteArray();
        ParseFile file = new ParseFile(imageName, image);
        ParseObject imgupload = new ParseObject(classname);
        imgupload.put("Image", file);
        imgupload.put("PatientID", patientUUID);
        imgupload.put("VisitID", visitUUID);
        imgupload.saveInBackground(new SaveCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e == null) {
                    String newText = "Person Image Posted successfully.";
                    mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Image Upload")
                            .setContentText(newText);
                    mNotifyManager.notify(mId, mBuilder.build());
                        removeJobFromQueue(queueId);
                    deleteImageFromDatabase(imagePath);
                } else {
                    String newText = "Failed to Post Images.";
                    mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Image Upload")
                            .setContentText(newText);
                    mNotifyManager.notify(mId, mBuilder.build());
                }
            }
        });
    }

    private void deleteImageFromDatabase(String imagePath) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(this);
        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();
        localdb.execSQL("DELETE FROM image_records WHERE patient_id=" +
                "'" + patientId + "'" + " AND " +
                "visit_id=" + "'" + visitId + "'" + " AND " +
                "image_path=" + "'" + imagePath + "'");
    }

    private int addJobToQueue(Intent intent) {
        Log.d(LOG_TAG, "Adding to Queue");
        // Add a new Delayed Job record
        ContentValues values = new ContentValues();
        values.put(DelayedJobQueueProvider.JOB_TYPE, "imageUpload");
        values.put(DelayedJobQueueProvider.VISIT_ID, intent.getStringExtra("visitID"));
        values.put(DelayedJobQueueProvider.VISIT_UUID, intent.getStringExtra("visitUUID"));
        values.put(DelayedJobQueueProvider.JOB_PRIORITY, 1);
        values.put(DelayedJobQueueProvider.JOB_REQUEST_CODE, 0);
        values.put(DelayedJobQueueProvider.PATIENT_ID, intent.getStringExtra("patientID"));
        values.put(DelayedJobQueueProvider.DATA_RESPONSE, intent.getStringExtra("patientUUID"));
        values.put(DelayedJobQueueProvider.PATIENT_NAME, intent.getStringExtra("patientUUID"));
        values.put(DelayedJobQueueProvider.PATIENT_NAME, intent.getStringExtra("name"));

        Uri uri = getContentResolver().insert(
                DelayedJobQueueProvider.CONTENT_URI, values);


        Toast.makeText(getBaseContext(),
                uri.toString(), Toast.LENGTH_LONG).show();

        return Integer.valueOf(uri.getLastPathSegment());
    }

    private void removeJobFromQueue(int queueId) {
        Log.d(LOG_TAG, "Removing from Queue");
        if (queueId > -1) {
            String url = DelayedJobQueueProvider.URL + "/" + queueId;
            Uri uri = Uri.parse(url);
            int result = getContentResolver().delete(uri, null, null);
            if (result > 0) {
                Log.i(LOG_TAG, result + " row deleted");
            } else {
                Log.e(LOG_TAG, "Database error while deleting row!");
            }
        }

    }

    private void queueSyncStart(int queueId) {
        ContentValues values = new ContentValues();
        values.put(DelayedJobQueueProvider.SYNC_STATUS, STATUS_SYNC_IN_PROGRESS);
        String url = DelayedJobQueueProvider.URL + "/" + queueId;
        Uri uri = Uri.parse(url);
        getContentResolver().update(uri, values, null, null);
    }

    private void queueSyncStop(int queueId) {
        ContentValues values = new ContentValues();
        values.put(DelayedJobQueueProvider.SYNC_STATUS, STATUS_SYNC_STOPPED);
        String url = DelayedJobQueueProvider.URL + "/" + queueId;
        Uri uri = Uri.parse(url);
        int result = getContentResolver().update(uri, values, null, null);
    }

    @Override
    public void onDestroy() {
        queueSyncStop(queueId);
        super.onDestroy();
    }

}
