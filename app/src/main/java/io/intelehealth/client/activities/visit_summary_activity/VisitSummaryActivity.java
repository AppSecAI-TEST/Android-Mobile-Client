package io.intelehealth.client.activities.visit_summary_activity;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.intelehealth.client.R;
import io.intelehealth.client.activities.additional_documents_activity.AdditionalDocumentsActivity;
import io.intelehealth.client.activities.complaint_node_activity.ComplaintNodeActivity;
import io.intelehealth.client.activities.family_history_activity.FamilyHistoryActivity;
import io.intelehealth.client.activities.home_activity.HomeActivity;
import io.intelehealth.client.activities.past_medical_history_activity.PastMedicalHistoryActivity;
import io.intelehealth.client.activities.physical_exam_activity.PhysicalExamActivity;
import io.intelehealth.client.activities.vitals_activity.VitalsActivity;
import io.intelehealth.client.database.DelayedJobQueueProvider;
import io.intelehealth.client.database.LocalRecordsDatabaseHelper;
import io.intelehealth.client.objects.Obs;
import io.intelehealth.client.objects.Patient;
import io.intelehealth.client.services.ClientService;
import io.intelehealth.client.services.PrescriptionDownloadService;
import io.intelehealth.client.services.UpdateVisitService;
import io.intelehealth.client.utilities.ConceptId;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * This class updates data about patient to database. It also creates a summary about it which can be viewed
 * using WebView and printed.
 */
@RuntimePermissions
public class VisitSummaryActivity extends AppCompatActivity {


    private static final String TAG = "VisitSummaryActivity";

    //Change when used with a different organization.
    //This is a demo server.

    private WebView mWebView;
    private LinearLayout mLayout;

    String mHeight, mWeight, mBMI, mBP, mPulse, mTemp, mSPO2;
    String identifierNumber;

    boolean uploaded = false;
    boolean dataChanged = false;
    String failedMessage;

    Context context;

    String patientID;
    String visitID;
    String state;
    String patientName;
    String intentTag;
    String visitUUID;

    LocalRecordsDatabaseHelper mDbHelper;
    SQLiteDatabase db;

    Patient patient = new Patient();
    Obs complaint = new Obs();
    Obs famHistory = new Obs();
    Obs patHistory = new Obs();
    Obs phyExam = new Obs();
    Obs height = new Obs();
    Obs weight = new Obs();
    Obs pulse = new Obs();
    Obs bpSys = new Obs();
    Obs bpDias = new Obs();
    Obs temperature = new Obs();
    Obs spO2 = new Obs();

    String diagnosisReturned = "";
    String rxReturned = "";
    String testsReturned = "";
    String adviceReturned = "";
    String doctorName = "";
    String additionalReturned = "";

    ImageButton editVitals;
    ImageButton editComplaint;
    ImageButton editPhysical;
    ImageButton editFamHist;
    ImageButton editMedHist;
    ImageButton editAddDocs;

    TextView nameView;
    TextView idView;
    TextView heightView;
    TextView weightView;
    TextView pulseView;
    TextView bpView;
    TextView tempView;
    TextView spO2View;
    TextView bmiView;
    TextView complaintView;
    TextView famHistView;
    TextView patHistView;
    TextView physFindingsView;
    TextView mCHWname;

    String medHistory;
    String baseDir;
    String filePathPhyExam;
    File phyExamDir;

    NotificationManager mNotificationManager;
    NotificationCompat.Builder mBuilder;

    Button uploadButton;
    Button downloadButton;
    ArrayList<String> physicalExams;

    CardView diagnosisCard;
    CardView prescriptionCard;
    CardView medicalAdviceCard;
    CardView requestedTestsCard;
    CardView additionalCommentsCard;

    TextView diagnosisTextView;
    TextView prescriptionTextView;
    TextView medicalAdviceTextView;
    TextView requestedTestsTextView;
    TextView additionalCommentsTextView;

    Boolean isPastVisit = false;
    Boolean isReceiverRegistered = false;

    public static final String FILTER = "io.intelehealth.client.activities.visit_summary_activity.REQUEST_PROCESSED";

    private NetworkChangeReceiver receiver;
    private boolean isConnected = false;
    private Menu mymenu;
    MenuItem internetCheck = null;

    private RecyclerView mAdditionalDocsRecyclerView;
    private RecyclerView.LayoutManager mAdditionalDocsLayoutManager;

    private RecyclerView mPhysicalExamsRecyclerView;
    private RecyclerView.LayoutManager mPhysicalExamsLayoutManager;


    String additionalDocumentDir = "Additional Documents";
    String physicalExamDocumentDir = "Physical Exam";

    SharedPreferences mSharedPreference;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_visit_summary, menu);
        MenuItem menuItem = menu.findItem(R.id.summary_endVisit);

        internetCheck = menu.findItem(R.id.internet_icon);
        MenuItemCompat.getActionView(internetCheck);

        isNetworkAvailable(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mCHWname = (TextView) findViewById(R.id.chw_details);
        mCHWname.setText(sharedPreferences.getString("chwname", "----"));

        if (isPastVisit) menuItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        //do nothing
        //Use the buttons on the screen to navigate
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.summary_home: {
                NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            case R.id.summary_print: {
                doWebViewPrint();
                return true;
            }
            case R.id.summary_sms: {
                VisitSummaryActivityPermissionsDispatcher.sendSMSWithCheck(this);
                return true;
            }
            case R.id.summary_endVisit: {
                endVisit();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final Intent intent = this.getIntent(); // The intent was passed to the activity

        if (intent != null) {
            patientID = intent.getStringExtra("patientID");
            visitID = intent.getStringExtra("visitID");
            mSharedPreference = this.getSharedPreferences(
                    "visit_summary", Context.MODE_PRIVATE);
            patientName = intent.getStringExtra("name");
            intentTag = intent.getStringExtra("tag");
            isPastVisit = intent.getBooleanExtra("pastVisit", false);
            if (!isPastVisit) {
                if (intent.hasExtra("exams")) {
                    physicalExams = intent.getStringArrayListExtra("exams"); //Pass it along
                    SharedPreferences.Editor editor = mSharedPreference.edit();
                    Set<String> selectedExams = new LinkedHashSet<>(physicalExams);
                    editor.putStringSet("exam_" + patientID, selectedExams);
                    editor.commit();
                } else {
                    Set<String> selectedExams = mSharedPreference.getStringSet("exam_" + patientID, null);
                    if (physicalExams == null) physicalExams = new ArrayList<>();
                    physicalExams.clear();
                    if (selectedExams != null && !selectedExams.isEmpty()) {
                        physicalExams.addAll(selectedExams);
                    } else {
                        Intent return_intent = new Intent(this, ComplaintNodeActivity.class);
                        return_intent.putExtra("patientID", patientID);
                        return_intent.putExtra("visitID", visitID);
                        return_intent.putExtra("name", patientName);
                        return_intent.putExtra("tag", intentTag);
                        startActivity(return_intent);
                    }
                }

            }


//            Log.v(TAG, "Patient ID: " + patientID);
//            Log.v(TAG, "Visit ID: " + visitID);
//            Log.v(TAG, "Patient Name: " + patientName);
//            Log.v(TAG, "Intent Tag: " + intentTag);
        }


        String titleSequence = patientName + ": " + getTitle();
        setTitle(titleSequence);

        mDbHelper = new LocalRecordsDatabaseHelper(this.getApplicationContext());
        db = mDbHelper.getWritableDatabase();

        identifierNumber = patientID;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visit_summary);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        mLayout = (LinearLayout) findViewById(R.id.summary_layout);
        context = getApplicationContext();

        mAdditionalDocsRecyclerView = (RecyclerView) findViewById(R.id.recy_additional_documents);
        mPhysicalExamsRecyclerView = (RecyclerView) findViewById(R.id.recy_physexam);

        diagnosisCard = (CardView) findViewById(R.id.cardView_diagnosis);
        prescriptionCard = (CardView) findViewById(R.id.cardView_rx);
        medicalAdviceCard = (CardView) findViewById(R.id.cardView_medical_advice);
        requestedTestsCard = (CardView) findViewById(R.id.cardView_tests);
        additionalCommentsCard = (CardView) findViewById(R.id.cardView_additional_comments);

        diagnosisTextView = (TextView) findViewById(R.id.textView_content_diagnosis);
        prescriptionTextView = (TextView) findViewById(R.id.textView_content_rx);
        medicalAdviceTextView = (TextView) findViewById(R.id.textView_content_medical_advice);
        requestedTestsTextView = (TextView) findViewById(R.id.textView_content_tests);
        additionalCommentsTextView = (TextView) findViewById(R.id.textView_content_additional_comments);

        baseDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();

        filePathPhyExam = baseDir + File.separator + "Patient Images" + File.separator + patientID + File.separator +
                visitID + File.separator + physicalExamDocumentDir;

        phyExamDir = new File(filePathPhyExam);
        if (!phyExamDir.exists()) {
            phyExamDir.mkdirs();
            Log.v(TAG, "directory ceated " + phyExamDir.getAbsolutePath());
        } else {
            File[] files = phyExamDir.listFiles();
            List<File> fileList = Arrays.asList(files);
            HorizontalAdapter horizontalAdapter = new HorizontalAdapter(fileList, this);
            mPhysicalExamsLayoutManager = new LinearLayoutManager(VisitSummaryActivity.this, LinearLayoutManager.HORIZONTAL, false);
            mPhysicalExamsRecyclerView.setLayoutManager(mPhysicalExamsLayoutManager);
            mPhysicalExamsRecyclerView.setAdapter(horizontalAdapter);
        }

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        editVitals = (ImageButton) findViewById(R.id.imagebutton_edit_vitals);
        editComplaint = (ImageButton) findViewById(R.id.imagebutton_edit_complaint);
        editPhysical = (ImageButton) findViewById(R.id.imagebutton_edit_physexam);
        editFamHist = (ImageButton) findViewById(R.id.imagebutton_edit_famhist);
        editMedHist = (ImageButton) findViewById(R.id.imagebutton_edit_pathist);
        editAddDocs = (ImageButton) findViewById(R.id.imagebutton_edit_additional_document);
        uploadButton = (Button) findViewById(R.id.button_upload);

        if (isPastVisit) {
            editVitals.setVisibility(View.GONE);
            editComplaint.setVisibility(View.GONE);
            editPhysical.setVisibility(View.GONE);
            editFamHist.setVisibility(View.GONE);
            editMedHist.setVisibility(View.GONE);
            editAddDocs.setVisibility(View.GONE);
            uploadButton.setVisibility(View.GONE);
            invalidateOptionsMenu();
        } else {
            String[] columnsToReturn = {"openmrs_visit_uuid"};
            String visitIDorderBy = "start_datetime";
            String visitIDSelection = "_id = ?";
            String[] visitIDArgs = {visitID};
            final Cursor visitIDCursor = db.query("visit", columnsToReturn, visitIDSelection, visitIDArgs, null, null, visitIDorderBy);
            if (visitIDCursor != null && visitIDCursor.moveToFirst() && visitIDCursor.getCount() > 0) {
                visitIDCursor.moveToFirst();
                visitUUID = visitIDCursor.getString(visitIDCursor.getColumnIndexOrThrow("openmrs_visit_uuid"));
            }
            if (visitIDCursor != null) visitIDCursor.close();
            if (visitUUID != null && !visitUUID.isEmpty()) {
                downloadButton = new Button(VisitSummaryActivity.this);
                downloadButton.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT));
                downloadButton.setText(R.string.visit_summary_button_download);


                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view, "Downloading from doctor", Snackbar.LENGTH_LONG).show();
                        Intent startDownload = new Intent(VisitSummaryActivity.this, PrescriptionDownloadService.class);
                        startDownload.putExtra("patientID", patientID);
                        startDownload.putExtra("visitID", visitID);
                        startDownload.putExtra("name", patientName);
                        startDownload.putExtra("visitUUID", visitUUID);
                        startService(startDownload);

                        //  retrieveOpenMRS(view);
                    }
                });
                mLayout.addView(downloadButton, mLayout.getChildCount());
            }

        }


        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (visitUUID == null || visitUUID.isEmpty()) {
                    String[] columnsToReturn = {"openmrs_visit_uuid"};
                    String visitIDorderBy = "start_datetime";
                    String visitIDSelection = "_id = ?";
                    String[] visitIDArgs = {visitID};
                    final Cursor visitIDCursor = db.query("visit", columnsToReturn, visitIDSelection, visitIDArgs, null, null, visitIDorderBy);
                    if (visitIDCursor != null && visitIDCursor.moveToFirst() && visitIDCursor.getCount() > 0) {
                        visitIDCursor.moveToFirst();
                        visitUUID = visitIDCursor.getString(visitIDCursor.getColumnIndexOrThrow("openmrs_visit_uuid"));
                    }
                    if (visitIDCursor != null) visitIDCursor.close();
                }

                Snackbar.make(view, "Uploading to doctor.", Snackbar.LENGTH_LONG).show();

                String[] DELAYED_JOBS_PROJECTION = new String[]{DelayedJobQueueProvider._ID, DelayedJobQueueProvider.JOB_TYPE, DelayedJobQueueProvider.SYNC_STATUS};
                String SELECTION = DelayedJobQueueProvider.JOB_TYPE + " IN (\"visit\",\"prescriptionDownload\") AND " +
                        DelayedJobQueueProvider.PATIENT_ID + "=? AND " +
                        DelayedJobQueueProvider.VISIT_ID + "=?";
                String[] ARGS = new String[]{patientID, visitID};

                Cursor c = getContentResolver().query(DelayedJobQueueProvider.CONTENT_URI,
                        DELAYED_JOBS_PROJECTION, SELECTION, ARGS, null);


                Log.i(TAG, "onClick: " + c.getCount());

                if (c == null || c.getCount() == 0) {

                    Intent serviceIntent;
                    if (visitUUID != null && !visitUUID.isEmpty()) {
                        Log.i(TAG, "onClick: new update");
                        serviceIntent = new Intent(VisitSummaryActivity.this, UpdateVisitService.class);
                        serviceIntent.putExtra("serviceCall", "obsUpdate");
                        serviceIntent.putExtra("patientID", patientID);
                        serviceIntent.putExtra("visitID", visitID);
                        serviceIntent.putExtra("name", patientName);
                        startService(serviceIntent);
                    } else {
                        Log.i(TAG, "onClick: new visit");
                        serviceIntent = new Intent(VisitSummaryActivity.this, ClientService.class);
                        serviceIntent.putExtra("serviceCall", "visit");
                        serviceIntent.putExtra("patientID", patientID);
                        serviceIntent.putExtra("visitID", visitID);
                        serviceIntent.putExtra("name", patientName);
                        startService(serviceIntent);
                    }

                } else if (c != null && c.moveToFirst()) {
                    Log.d(TAG, "onClick: Not In Null");
                    int sync_status = c.getInt(c.getColumnIndexOrThrow(DelayedJobQueueProvider.SYNC_STATUS));
                    switch (sync_status) {
                        case ClientService.STATUS_SYNC_STOPPED: {
                            Intent serviceIntent;
                            Log.i(TAG, "onClick: old visit delayed");
                            if (c.getString(c.getColumnIndex(DelayedJobQueueProvider.JOB_TYPE)).equals("visit")) {
                                serviceIntent = new Intent(VisitSummaryActivity.this, ClientService.class);
                                serviceIntent.putExtra("serviceCall", "visit");
                                serviceIntent.putExtra("patientID", patientID);
                                serviceIntent.putExtra("visitID", visitID);
                                serviceIntent.putExtra("name", patientName);
                                serviceIntent.putExtra("queueId", c.getInt(c.getColumnIndex(DelayedJobQueueProvider._ID)));
                                startService(serviceIntent);
                            } else if (c.getString(c.getColumnIndex(DelayedJobQueueProvider.JOB_TYPE)).equals("obsUpdate")) {
                                Log.i(TAG, "onClick: old obs delayed");
                                serviceIntent = new Intent(VisitSummaryActivity.this, UpdateVisitService.class);
                                serviceIntent.putExtra("serviceCall", "obsUpdate");
                                serviceIntent.putExtra("patientID", patientID);
                                serviceIntent.putExtra("visitID", visitID);
                                serviceIntent.putExtra("name", patientName);
                                serviceIntent.putExtra("queueId", c.getInt(c.getColumnIndex(DelayedJobQueueProvider._ID)));
                                startService(serviceIntent);
                            }
                            ;
                            break;
                        }
                        case ClientService.STATUS_SYNC_IN_PROGRESS: {
                            Toast.makeText(context, getString(R.string.sync_in_progress), Toast.LENGTH_SHORT).show();
                            break;
                        }
                        default:
                    }
                }

                c.close();

            }
        });

        if (intentTag != null && intentTag.equals("prior")) {
            uploadButton.setEnabled(false);
        }


        queryData(String.valueOf(patientID));
        nameView = (TextView) findViewById(R.id.textView_name_value);
        idView = (TextView) findViewById(R.id.textView_id_value);

        nameView.setText(patientName);
        idView.setText(patientID);

        heightView = (TextView) findViewById(R.id.textView_height_value);
        weightView = (TextView) findViewById(R.id.textView_weight_value);
        pulseView = (TextView) findViewById(R.id.textView_pulse_value);
        bpView = (TextView) findViewById(R.id.textView_bp_value);
        tempView = (TextView) findViewById(R.id.textView_temp_value);
        spO2View = (TextView) findViewById(R.id.textView_pulseox_value);
        bmiView = (TextView) findViewById(R.id.textView_bmi_value);
        complaintView = (TextView) findViewById(R.id.textView_content_complaint);
        famHistView = (TextView) findViewById(R.id.textView_content_famhist);
        patHistView = (TextView) findViewById(R.id.textView_content_pathist);
        physFindingsView = (TextView) findViewById(R.id.textView_content_physexam);

        heightView.setText(height.getValue());
        weightView.setText(weight.getValue());
        pulseView.setText(pulse.getValue());

        String bpText = bpSys.getValue() + "/" + bpDias.getValue();
        if (bpText.equals("/")) {
            bpView.setText("");
        } else {
            bpView.setText(bpText);
        }

        Log.d(TAG, "onCreate: " + weight.getValue());
        String mWeight = weight.getValue();
        String mHeight = height.getValue();
        if ((mHeight != null && mWeight != null) && !mHeight.isEmpty() && !mWeight.isEmpty()) {
            double numerator = Double.parseDouble(mWeight) * 10000;
            double denominator = Double.parseDouble(mHeight) * Double.parseDouble(mHeight);
            double bmi_value = numerator / denominator;
            mBMI = String.format(Locale.ENGLISH, "%.2f", bmi_value);
        } else {
            mBMI = "";
        }
        patHistory.setValue(medHistory);

        bmiView.setText(mBMI);
        tempView.setText(temperature.getValue());
        spO2View.setText(spO2.getValue());
        complaintView.setText(complaint.getValue());
        famHistView.setText(famHistory.getValue());
        patHistView.setText(patHistory.getValue());

        physFindingsView.setText(phyExam.getValue());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor e = sharedPreferences.edit();


        editVitals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder vitalsDialog = new AlertDialog.Builder(VisitSummaryActivity.this);
                vitalsDialog.setTitle(getString(R.string.visit_summary_vitals));
                final LayoutInflater inflater = getLayoutInflater();
                View convertView = inflater.inflate(R.layout.dialog_edit_entry, null);
                vitalsDialog.setView(convertView);

                final TextView vitalsEditText = (TextView) convertView.findViewById(R.id.textView_entry);
                vitalsEditText.setText(R.string.visit_summary_edit_vitals);

                vitalsDialog.setPositiveButton(getString(R.string.generic_erase_redo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent1 = new Intent(VisitSummaryActivity.this, VitalsActivity.class);
                        intent1.putExtra("patientID", patientID);
                        intent1.putExtra("visitID", visitID);
                        intent1.putExtra("name", patientName);
                        intent.putStringArrayListExtra("exams", physicalExams);
                        intent1.putExtra("tag", "edit");
                        startActivity(intent1);
                        dialogInterface.dismiss();
                    }
                });

                vitalsDialog.setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                vitalsDialog.show();
            }
        });

        editFamHist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder famHistDialog = new AlertDialog.Builder(VisitSummaryActivity.this);
                famHistDialog.setTitle(getString(R.string.visit_summary_family_history));
                final LayoutInflater inflater = getLayoutInflater();
                View convertView = inflater.inflate(R.layout.dialog_edit_entry, null);
                famHistDialog.setView(convertView);

                final TextView famHistTest = (TextView) convertView.findViewById(R.id.textView_entry);
                famHistTest.setText(famHistory.getValue());
                famHistTest.setEnabled(false);

                famHistDialog.setPositiveButton(getString(R.string.generic_manual_entry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final AlertDialog.Builder textInput = new AlertDialog.Builder(VisitSummaryActivity.this);
                        textInput.setTitle(R.string.question_text_input);
                        final EditText dialogEditText = new EditText(VisitSummaryActivity.this);
                        dialogEditText.setText(famHistory.getValue());
                        textInput.setView(dialogEditText);
                        textInput.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                famHistory.setValue(dialogEditText.getText().toString());
                                famHistTest.setText(famHistory.getValue());
                                famHistView.setText(famHistory.getValue());
                                updateDatabase(famHistory.getValue(), ConceptId.RHK_FAMILY_HISTORY_BLURB);
                                dialog.dismiss();
                            }
                        });
                        textInput.setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        textInput.show();
                        dialogInterface.dismiss();
                    }
                });

                famHistDialog.setNeutralButton(getString(R.string.generic_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                famHistDialog.setNegativeButton(R.string.generic_erase_redo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        Intent intent1 = new Intent(VisitSummaryActivity.this, FamilyHistoryActivity.class);
                        intent1.putExtra("patientID", patientID);
                        intent1.putExtra("visitID", visitID);
                        intent1.putExtra("name", patientName);
                        intent1.putExtra("tag", "edit");
                        startActivity(intent1);
                        dialogInterface.dismiss();
                    }
                });

                famHistDialog.show();
            }
        });

        editComplaint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder complaintDialog = new AlertDialog.Builder(VisitSummaryActivity.this);
                complaintDialog.setTitle(getString(R.string.visit_summary_complaint));
                final LayoutInflater inflater = getLayoutInflater();
                View convertView = inflater.inflate(R.layout.dialog_edit_entry, null);
                complaintDialog.setView(convertView);

                final TextView complaintText = (TextView) convertView.findViewById(R.id.textView_entry);
                complaintText.setText(complaint.getValue());
                complaintText.setEnabled(false);

                complaintDialog.setPositiveButton(getString(R.string.generic_manual_entry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final AlertDialog.Builder textInput = new AlertDialog.Builder(VisitSummaryActivity.this);
                        textInput.setTitle(R.string.question_text_input);
                        final EditText dialogEditText = new EditText(VisitSummaryActivity.this);
                        dialogEditText.setText(complaint.getValue());
                        textInput.setView(dialogEditText);
                        textInput.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                complaint.setValue(dialogEditText.getText().toString());
                                complaintText.setText(complaint.getValue());
                                complaintView.setText(complaint.getValue());
                                updateDatabase(complaint.getValue(), ConceptId.CURRENT_COMPLAINT);
                                dialog.dismiss();
                            }
                        });
                        textInput.setNeutralButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        textInput.show();
                        dialogInterface.dismiss();
                    }
                });

                complaintDialog.setNegativeButton(getString(R.string.generic_erase_redo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent1 = new Intent(VisitSummaryActivity.this, ComplaintNodeActivity.class);
                        intent1.putExtra("patientID", patientID);
                        intent1.putExtra("visitID", visitID);
                        intent1.putExtra("name", patientName);
                        intent1.putExtra("tag", "edit");
                        startActivity(intent1);
                        dialogInterface.dismiss();
                    }
                });

                complaintDialog.setNeutralButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                complaintDialog.show();
            }
        });

        editPhysical.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder physicalDialog = new AlertDialog.Builder(VisitSummaryActivity.this);
                physicalDialog.setTitle(getString(R.string.visit_summary_on_examination));
                final LayoutInflater inflater = getLayoutInflater();
                View convertView = inflater.inflate(R.layout.dialog_edit_entry, null);
                physicalDialog.setView(convertView);

                final TextView physicalText = (TextView) convertView.findViewById(R.id.textView_entry);
                physicalText.setText(phyExam.getValue());
                physicalText.setEnabled(false);

                physicalDialog.setPositiveButton(getString(R.string.generic_manual_entry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final AlertDialog.Builder textInput = new AlertDialog.Builder(VisitSummaryActivity.this);
                        textInput.setTitle(R.string.question_text_input);
                        final EditText dialogEditText = new EditText(VisitSummaryActivity.this);
                        dialogEditText.setText(phyExam.getValue());
                        textInput.setView(dialogEditText);
                        textInput.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                phyExam.setValue(dialogEditText.getText().toString());
                                physicalText.setText(phyExam.getValue());
                                physFindingsView.setText(phyExam.getValue());
                                updateDatabase(phyExam.getValue(), ConceptId.PHYSICAL_EXAMINATION);
                                dialog.dismiss();
                            }
                        });
                        textInput.setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        textInput.show();
                        dialogInterface.dismiss();
                    }
                });

                physicalDialog.setNegativeButton(getString(R.string.generic_erase_redo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (phyExamDir.exists()) {
                            String[] children = phyExamDir.list();
                            List<String> childList = Arrays.asList(children);
                            SQLiteDatabase localdb = mDbHelper.getWritableDatabase();
                            for (String child : childList) {
                                new File(phyExamDir, child).delete();
                                localdb.execSQL("DELETE FROM image_records WHERE image_path=" +
                                        "'" + phyExamDir.getAbsolutePath() + File.separator + child + "'");
                            }
                            phyExamDir.delete();
                            localdb.close();
                        }
                        Intent intent1 = new Intent(VisitSummaryActivity.this, PhysicalExamActivity.class);
                        intent1.putExtra("patientID", patientID);
                        intent1.putExtra("visitID", visitID);
                        intent1.putExtra("name", patientName);
                        intent1.putExtra("tag", "edit");
                        intent1.putStringArrayListExtra("exams", physicalExams);
                        for (String string : physicalExams)
                            Log.i(TAG, "onClick: " + string);
                        startActivity(intent1);
                        dialogInterface.dismiss();
                    }
                });

                physicalDialog.setNeutralButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                physicalDialog.show();
            }
        });

        editMedHist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder historyDialog = new AlertDialog.Builder(VisitSummaryActivity.this);
                historyDialog.setTitle(getString(R.string.visit_summary_medical_history));
                final LayoutInflater inflater = getLayoutInflater();
                View convertView = inflater.inflate(R.layout.dialog_edit_entry, null);
                historyDialog.setView(convertView);

                final TextView historyText = (TextView) convertView.findViewById(R.id.textView_entry);
                historyText.setText(patHistory.getValue());
                historyText.setEnabled(false);

                historyDialog.setPositiveButton(getString(R.string.generic_manual_entry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final AlertDialog.Builder textInput = new AlertDialog.Builder(VisitSummaryActivity.this);
                        textInput.setTitle(R.string.question_text_input);
                        final EditText dialogEditText = new EditText(VisitSummaryActivity.this);
                        dialogEditText.setText(patHistory.getValue());
                        textInput.setView(dialogEditText);
                        textInput.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                patHistory.setValue(dialogEditText.getText().toString());
                                historyText.setText(patHistory.getValue());
                                patHistView.setText(patHistory.getValue());
                                updateDatabase(patHistory.getValue(), ConceptId.RHK_MEDICAL_HISTORY_BLURB);
                                dialog.dismiss();
                            }
                        });
                        textInput.setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        textInput.show();
                        dialogInterface.dismiss();
                    }
                });

                historyDialog.setNegativeButton(getString(R.string.generic_erase_redo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent1 = new Intent(VisitSummaryActivity.this, PastMedicalHistoryActivity.class);
                        intent1.putExtra("patientID", patientID);
                        intent1.putExtra("visitID", visitID);
                        intent1.putExtra("name", patientName);
                        intent1.putExtra("tag", "edit");
                        startActivity(intent1);
                        dialogInterface.dismiss();
                    }
                });

                historyDialog.setNeutralButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                historyDialog.show();
            }
        });

        editAddDocs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addDocs = new Intent(VisitSummaryActivity.this, AdditionalDocumentsActivity.class);
                addDocs.putExtra("patientID", patientID);
                addDocs.putExtra("visitID", visitID);
                startActivity(addDocs);
            }
        });


    }


    private void endVisit() {
        Log.d(TAG, "endVisit: ");
        if (visitUUID == null || visitUUID.isEmpty()) {
            String[] columnsToReturn = {"openmrs_visit_uuid"};
            String visitIDorderBy = "start_datetime";
            String visitIDSelection = "_id = ?";
            String[] visitIDArgs = {visitID};
            final Cursor visitIDCursor = db.query("visit", columnsToReturn, visitIDSelection, visitIDArgs, null, null, visitIDorderBy);
            if (visitIDCursor != null && visitIDCursor.moveToFirst() && visitIDCursor.getCount() > 0) {
                visitIDCursor.moveToFirst();
                visitUUID = visitIDCursor.getString(visitIDCursor.getColumnIndexOrThrow("openmrs_visit_uuid"));
            }
            if (visitIDCursor != null) visitIDCursor.close();
        }
        if (visitUUID != null && !visitUUID.isEmpty()) {
            Log.d(TAG, "endVisit: uuid ok");
            String[] DELAYED_JOBS_PROJECTION = new String[]{DelayedJobQueueProvider._ID, DelayedJobQueueProvider.SYNC_STATUS};
            String SELECTION = DelayedJobQueueProvider.JOB_TYPE + "=? AND " +
                    DelayedJobQueueProvider.PATIENT_ID + "=? AND " +
                    DelayedJobQueueProvider.VISIT_ID + "=?";
            String[] ARGS = new String[]{"endVisit", patientID, visitID};

            Cursor c = getContentResolver().query(DelayedJobQueueProvider.CONTENT_URI,
                    DELAYED_JOBS_PROJECTION, SELECTION, ARGS, null);

            if (c != null && c.moveToFirst() && c.getCount() > 0) {
                int sync_status = c.getInt(c.getColumnIndexOrThrow(DelayedJobQueueProvider.SYNC_STATUS));
                switch (sync_status) {
                    case ClientService.STATUS_SYNC_STOPPED: {
                        Intent serviceIntent = new Intent(VisitSummaryActivity.this, ClientService.class);
                        serviceIntent.putExtra("serviceCall", "endVisit");
                        serviceIntent.putExtra("patientID", patientID);
                        serviceIntent.putExtra("visitUUID", visitUUID);
                        serviceIntent.putExtra("name", patientName);
                        startService(serviceIntent);
                        Intent intent = new Intent(VisitSummaryActivity.this, HomeActivity.class);
                        startActivity(intent);
                        break;
                    }
                    case ClientService.STATUS_SYNC_IN_PROGRESS: {
                        Toast.makeText(context, getString(R.string.sync_in_progress), Toast.LENGTH_SHORT).show();
                        break;
                    }
                    default:
                }
            } else {
                Log.d(TAG, "endVisit: delayed job first");
                Intent serviceIntent = new Intent(VisitSummaryActivity.this, ClientService.class);
                serviceIntent.putExtra("serviceCall", "endVisit");
                serviceIntent.putExtra("patientID", patientID);
                serviceIntent.putExtra("visitUUID", visitUUID);
                serviceIntent.putExtra("name", patientName);
                startService(serviceIntent);
                SharedPreferences.Editor editor = context.getSharedPreferences(patientID + "_" + visitID, MODE_PRIVATE).edit();
                editor.remove("exam_" + patientID + "_" + visitID);
                editor.commit();
                Intent intent = new Intent(VisitSummaryActivity.this, HomeActivity.class);
                startActivity(intent);
            }
            c.close();
        } else {

            Log.d(TAG, "endVisit: null");
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Please upload first before attempting to end the visit.");
            alertDialogBuilder.setNeutralButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }


    }

    /**
     * This methods retrieves patient data from database.
     *
     * @param dataString variable of type String
     * @return void
     */

    public void queryData(String dataString) {

        String patientSelection = "_id MATCH ?";
        String[] patientArgs = {dataString};

        String table = "patient";
        String[] columnsToReturn = {"first_name", "middle_name", "last_name",
                "date_of_birth", "address1", "address2", "city_village", "state_province", "country",
                "postal_code", "phone_number", "gender", "sdw", "occupation", "patient_photo"};
        final Cursor idCursor = db.query(table, columnsToReturn, patientSelection, patientArgs, null, null, null);

        if (idCursor.moveToFirst()) {
            do {
                patient.setFirstName(idCursor.getString(idCursor.getColumnIndex("first_name")));
                patient.setMiddleName(idCursor.getString(idCursor.getColumnIndex("middle_name")));
                patient.setLastName(idCursor.getString(idCursor.getColumnIndex("last_name")));
                patient.setDateOfBirth(idCursor.getString(idCursor.getColumnIndex("date_of_birth")));
                patient.setAddress1(idCursor.getString(idCursor.getColumnIndex("address1")));
                patient.setAddress2(idCursor.getString(idCursor.getColumnIndex("address2")));
                patient.setCityVillage(idCursor.getString(idCursor.getColumnIndex("city_village")));
                patient.setStateProvince(idCursor.getString(idCursor.getColumnIndex("state_province")));
                patient.setCountry(idCursor.getString(idCursor.getColumnIndex("country")));
                patient.setPostalCode(idCursor.getString(idCursor.getColumnIndex("postal_code")));
                patient.setPhoneNumber(idCursor.getString(idCursor.getColumnIndex("phone_number")));
                patient.setGender(idCursor.getString(idCursor.getColumnIndex("gender")));
                patient.setSdw(idCursor.getString(idCursor.getColumnIndexOrThrow("sdw")));
                patient.setOccupation(idCursor.getString(idCursor.getColumnIndexOrThrow("occupation")));
                patient.setPatientPhoto(idCursor.getString(idCursor.getColumnIndex("patient_photo")));
            } while (idCursor.moveToNext());
        }
        idCursor.close();


        String[] columns = {"value", " concept_id"};
        String orderBy = "visit_id";

        try {
            String famHistSelection = "patient_id = ? AND concept_id = ?";
            String[] famHistArgs = {dataString, String.valueOf(ConceptId.RHK_FAMILY_HISTORY_BLURB)};
            Cursor famHistCursor = db.query("obs", columns, famHistSelection, famHistArgs, null, null, orderBy);
            famHistCursor.moveToLast();
            String famHistText = famHistCursor.getString(famHistCursor.getColumnIndexOrThrow("value"));
            famHistory.setValue(famHistText);
            famHistCursor.close();
        } catch (CursorIndexOutOfBoundsException e) {
            famHistory.setValue(""); // if family history does not exist
        }

        try {
            String medHistSelection = "patient_id = ? AND concept_id = ?";

            String[] medHistArgs = {dataString, String.valueOf(ConceptId.RHK_MEDICAL_HISTORY_BLURB)};

            Cursor medHistCursor = db.query("obs", columns, medHistSelection, medHistArgs, null, null, orderBy);
            medHistCursor.moveToLast();
            String medHistText = medHistCursor.getString(medHistCursor.getColumnIndexOrThrow("value"));
            patHistory.setValue(medHistText);

            if (medHistText != null && !medHistText.isEmpty()) {

                medHistory = patHistory.getValue();


                medHistory = medHistory.replace("\"", "");
                medHistory = medHistory.replace("\n", "");
                do {
                    medHistory = medHistory.replace("  ", "");
                } while (medHistory.contains("  "));
            }
            medHistCursor.close();
        } catch (CursorIndexOutOfBoundsException e) {
            patHistory.setValue(""); // if medical history does not exist
        }

        String visitSelection = "patient_id = ? AND visit_id = ?";
        String[] visitArgs = {dataString, visitID};
        Cursor visitCursor = db.query("obs", columns, visitSelection, visitArgs, null, null, orderBy);
        if (visitCursor.moveToFirst()) {
            do {
                int dbConceptID = visitCursor.getInt(visitCursor.getColumnIndex("concept_id"));
                String dbValue = visitCursor.getString(visitCursor.getColumnIndex("value"));
                parseData(dbConceptID, dbValue);
            } while (visitCursor.moveToNext());
        }
        visitCursor.close();
    }

    /**
     * This method distinguishes between different concepts using switch case to populate the information into the relevant sections (eg:complaints, physical exam, vitals, etc.).
     *
     * @param concept_id variable of type int.
     * @param value      variable of type String.
     */
    private void parseData(int concept_id, String value) {
        switch (concept_id) {
            case ConceptId.CURRENT_COMPLAINT: { //Current Complaint
                complaint.setValue(value);
                break;
            }
            case ConceptId.PHYSICAL_EXAMINATION: { //Physical Examination
                phyExam.setValue(value);
                break;
            }
            case ConceptId.HEIGHT: //Height
            {
                height.setValue(value);
                break;
            }
            case ConceptId.WEIGHT: //Weight
            {
                weight.setValue(value);
                break;
            }
            case ConceptId.PULSE: //Pulse
            {
                pulse.setValue(value);
                break;
            }
            case ConceptId.SYSTOLIC_BP: //Systolic BP
            {
                bpSys.setValue(value);
                break;
            }
            case ConceptId.DIASTOLIC_BP: //Diastolic BP
            {
                bpDias.setValue(value);
                break;
            }
            case ConceptId.TEMPERATURE: //Temperature
            {
                temperature.setValue(value);
                break;
            }
            case ConceptId.SPO2: //SpO2
            {
                spO2.setValue(value);
                break;
            }
            case ConceptId.TELEMEDICINE_DIAGNOSIS: {
                if (!diagnosisReturned.isEmpty()) {
                    diagnosisReturned = diagnosisReturned + "," + value;
                } else {
                    diagnosisReturned = value;
                }
                if (diagnosisCard.getVisibility() != View.VISIBLE) {
                    diagnosisCard.setVisibility(View.VISIBLE);
                }
                diagnosisTextView.setText(diagnosisReturned);
                break;
            }
            case ConceptId.JSV_MEDICATIONS: {
                if (!rxReturned.isEmpty()) {
                    rxReturned = rxReturned + "," + value;
                } else {
                    rxReturned = value;
                }
                if (prescriptionCard.getVisibility() != View.VISIBLE) {
                    prescriptionCard.setVisibility(View.VISIBLE);
                }
                prescriptionTextView.setText(rxReturned);
                break;
            }
            case ConceptId.MEDICAL_ADVICE: {
                if (!adviceReturned.isEmpty()) {
                    adviceReturned = adviceReturned + "," + value;
                } else {
                    adviceReturned = value;
                }
                if (medicalAdviceCard.getVisibility() != View.VISIBLE) {
                    medicalAdviceCard.setVisibility(View.VISIBLE);
                }
                medicalAdviceTextView.setText(adviceReturned);
                break;
            }
            case ConceptId.REQUESTED_TESTS: {
                if (!testsReturned.isEmpty()) {
                    testsReturned = testsReturned + "," + value;
                } else {
                    testsReturned = value;
                }
                if (requestedTestsCard.getVisibility() != View.VISIBLE) {
                    requestedTestsCard.setVisibility(View.VISIBLE);
                }
                requestedTestsTextView.setText(testsReturned);
                break;
            }
            case ConceptId.ADDITIONAL_COMMENTS: {
                if (!additionalReturned.isEmpty()) {
                    additionalReturned = additionalReturned + "," + value;
                } else {
                    additionalReturned = value;
                }
                if (additionalCommentsCard.getVisibility() != View.VISIBLE) {
                    additionalCommentsCard.setVisibility(View.VISIBLE);
                }
                additionalCommentsTextView.setText(additionalReturned);
                break;
            }
            default:
                Log.i(TAG, "parseData: " + value);
                break;

        }
    }

    /**
     * This method creates a web view for printing patient's various details.
     *
     * @return void
     */
    private void doWebViewPrint() {
        // Create a WebView object specifically for printing
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i("Patient WebView", "page finished loading " + url);
                createWebPrintJob(view);
                mWebView = null;
            }
        });

        String mPatientName = patient.getFirstName() + " " + patient.getMiddleName() + " " + patient.getLastName();
        String mPatientDob = patient.getDateOfBirth();
        String mAddress = patient.getAddress1() + "\n" + patient.getAddress2();
        String mCityState = patient.getCityVillage();
        String mPhone = patient.getPhoneNumber();
        String mState = patient.getStateProvince();
        String mCountry = patient.getCountry();

        String mSdw = patient.getSdw();
        String mOccupation = patient.getOccupation();
        String mGender = patient.getGender();

        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String mDate = df.format(c.getTime());

        String mPatHist = patHistory.getValue();
        String mFamHist = famHistory.getValue();
        mHeight = height.getValue();
        mWeight = weight.getValue();
        mBP = bpSys.getValue() + "/" + bpDias.getValue();
        mPulse = pulse.getValue();
        mTemp = temperature.getValue();
        mSPO2 = spO2.getValue();
        String mComplaint = complaint.getValue();
        String mExam = patHistory.getValue();


        // Generate an HTML document on the fly:
        String htmlDocument =
                String.format("<h1 id=\"intelecare-patient-detail\">Intelehealth Visit Summary</h1>\n" +
                                "<h1>%s</h1>\n" +
                                "<p>Patient Id: %s &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                                "Date: %s</p>\n" +
                                "<h2 id=\"patient-information\">Patient Information</h2>\n" +
                                "<ul>\n" +
                                "<li>%s</li>\n" +
                                "<li>Son/Daughter/Wife of: %s</li>\n" +
                                "<li>Occupation: %s</li>\n" +
                                "</ul>\n" +
                                "<h2 id=\"address-and-contact\">Address and Contact</h2>\n" +
                                "<p>%s</p>\n" +
                                "<p>%s</p>\n" +
                                "<p>%s</p>\n" +
                                "<h2 id=\"vitals\">Vitals</h2>\n" +
                                "<li>Height: %s</li>\n" +
                                "<li>Weight: %s</li>\n" +
                                "<li>BMI: %s</li>\n" +
                                "<li>Blood Pressure: %s</li>\n" +
                                "<li>Pulse: %s</li>\n" +
                                "<li>Temperature: %s</li>\n" +
                                "<li>SpO2: %s</li>\n" +
                                "<h2 id=\"patient-history\">Patient History</h2>\n" +
                                "<li>%s</li>\n" +
                                "<h2 id=\"family-history\">Family History</h2>\n" +
                                "<li>%s</li>\n" +
                                "<h2 id=\"complaint\">Complaint and Observations</h2>" +
                                "<li>%s</li>\n" +
                                "<h2 id=\"examination\">On Examination</h2>" +
                                "<p>%s</p>\n" +
                                "<h2 id=\"complaint\">Diagnosis</h2>" +
                                "<li>%s</li>\n" +
                                "<h2 id=\"complaint\">Prescription</h2>" +
                                "<li>%s</li>\n" +
                                "<h2 id=\"complaint\">Tests To Be Performed</h2>" +
                                "<li>%s</li>\n" +
                                "<h2 id=\"complaint\">General Advices</h2>" +
                                "<li>%s</li>\n" +
                                "<h2 id=\"complaint\">Doctor's Name</h2>" +
                                "<li>%s</li>\n" +
                                "<h2 id=\"complaint\">Additional Comments</h2>" +
                                "<li>%s</li>\n",
                        mPatientName, patientID, mDate, mPatientDob, mSdw, mOccupation, mAddress, mCityState, mPhone, mHeight, mWeight,
                        mBMI, mBP, mPulse, mTemp, mSPO2, mPatHist, mFamHist, mComplaint, mExam, diagnosisReturned, rxReturned, testsReturned, adviceReturned, doctorName, additionalReturned);
        webView.loadDataWithBaseURL(null, htmlDocument, "text/HTML", "UTF-8", null);

        // Keep a reference to WebView object until you pass the PrintDocumentAdapter
        // to the PrintManager
        mWebView = webView;
    }

    /**
     * This method creates a print job using PrintManager instance and PrintAdapter Instance
     *
     * @param webView object of type WebView.
     */
    private void createWebPrintJob(WebView webView) {

        // Get a PrintManager instance
        PrintManager printManager = (PrintManager) this
                .getSystemService(Context.PRINT_SERVICE);

        // Get a print adapter instance
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter();

        // Create a print job with name and adapter instance
        String jobName = getString(R.string.app_name) + " Visit Summary";
        PrintJob printJob = printManager.print(jobName, printAdapter,
                new PrintAttributes.Builder().build());

    }


    /**
     * @param title   variable of type String
     * @param content variable of type String
     * @param index   variable of type int
     */
    private void createNewCardView(String title, String content, int index) {
        final LayoutInflater inflater = VisitSummaryActivity.this.getLayoutInflater();
        View convertView = inflater.inflate(R.layout.card_doctor_content, null);
        TextView titleView = (TextView) convertView.findViewById(R.id.textview_heading);
        TextView contentView = (TextView) convertView.findViewById(R.id.textview_content);
        titleView.setText(title);
        contentView.setText(content);
        mLayout.addView(convertView, index);
    }

    /**
     * This method updates patient details to database.
     *
     * @param string    variable of type String
     * @param conceptID variable of type int
     */

    private void updateDatabase(String string, int conceptID) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(this);
        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("value", string);

        String selection = "patient_id = ? AND visit_id = ? AND concept_id = ?";
        String[] args = {patientID, visitID, String.valueOf(conceptID)};

        localdb.update(
                "obs",
                contentValues,
                selection,
                args
        );

    }


    public void callBroadcastReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            receiver = new NetworkChangeReceiver();
            registerReceiver(receiver, filter);
            isReceiverRegistered = true;
        }
    }

    @Override
    public void onResume() // register the receiver here
    {
        super.onResume();

        callBroadcastReceiver();

        String filePathAddDoc = baseDir + File.separator + "Patient Images" + File.separator + patientID + File.separator +
                visitID + File.separator + additionalDocumentDir;

        File addDocDir = new File(filePathAddDoc);
        if (!addDocDir.exists()) {
            addDocDir.mkdirs();
            Log.v(TAG, "directory ceated " + addDocDir.getAbsolutePath());
        } else {
            File[] files = addDocDir.listFiles();
            List<File> fileList = Arrays.asList(files);
            HorizontalAdapter horizontalAdapter = new HorizontalAdapter(fileList, this);
            mAdditionalDocsLayoutManager = new LinearLayoutManager(VisitSummaryActivity.this, LinearLayoutManager.HORIZONTAL, false);
            mAdditionalDocsRecyclerView.setLayoutManager(mAdditionalDocsLayoutManager);
            mAdditionalDocsRecyclerView.setAdapter(horizontalAdapter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        isReceiverRegistered = false;
    }


    public class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            isNetworkAvailable(context);
        }

    }


    @NeedsPermission({Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE})
    public void sendSMS() {
        final AlertDialog.Builder textInput = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        textInput.setTitle(R.string.identification_screen_prompt_phone_number);
        final EditText phoneNumberEditText = new EditText(context);
        phoneNumberEditText.setInputType(InputType.TYPE_CLASS_PHONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            phoneNumberEditText.setTextColor(getColor(R.color.gray));
        } else {
            phoneNumberEditText.setTextColor(getResources().getColor(R.color.gray));
        }

        textInput.setView(phoneNumberEditText);

        textInput.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String body = "";
                String header = "";
                String message = "";

                header = "Patient Id: " + patientID + "\n"
                        + "Patient Name: " + patient.getFirstName() + " " + patient.getLastName() + "\n"
                        + "Patient DOB: " + patient.getDateOfBirth() + "\n"
                        + "Patient Id: " + patientID + "\n";


                if (diagnosisCard.getVisibility() == View.VISIBLE) {
                    if (!diagnosisTextView.getText().toString().trim().isEmpty())
                        body = body + getString(R.string.visit_summary_diagnosis) + ":" +
                                diagnosisTextView.getText().toString() + "\n";
                }
                if (prescriptionCard.getVisibility() == View.VISIBLE) {
                    if (!prescriptionTextView.getText().toString().trim().isEmpty())
                        body = body + getString(R.string.visit_summary_rx) + ":" +
                                prescriptionTextView.getText().toString() + "\n";
                }
                if (medicalAdviceCard.getVisibility() == View.VISIBLE) {
                    if (!medicalAdviceTextView.getText().toString().trim().isEmpty())
                        body = body + getString(R.string.visit_summary_advice) + ":" +
                                medicalAdviceTextView.getText().toString() + "\n";
                }
                if (requestedTestsCard.getVisibility() == View.VISIBLE) {
                    if (!requestedTestsTextView.getText().toString().trim().isEmpty())
                        body = body + getString(R.string.visit_summary_tests_prescribed) + ":" +
                                requestedTestsTextView.getText().toString() + "\n";
                }
                if (additionalCommentsCard.getVisibility() == View.VISIBLE) {
                    if (!additionalCommentsTextView.getText().toString().trim().isEmpty())
                        body = body + getString(R.string.visit_summary_additional_comments) + ":" +
                                additionalCommentsTextView.getText().toString() + "\n";
                }


                if (!phoneNumberEditText.getText().toString().trim().isEmpty()) {
                    if (!body.isEmpty()) {
                        if (body != null && body.length() > 0) {
                            body = body.substring(0, body.length() - 2);
                            message = header + body;
                        }
                        try {
                            SmsManager sm = SmsManager.getDefault();
                            String number = phoneNumberEditText.getText().toString();
                            ArrayList<String> parts = sm.divideMessage(message);

                            sm.sendMultipartTextMessage(number, null, parts, null, null);

                            Toast.makeText(getApplicationContext(), getString(R.string.sms_success),
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.error_sms),
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "onClick: " + e.getMessage());
                        }

                    } else {
                        Toast.makeText(context, getString(R.string.error_no_data), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, getString(R.string.error_phone_number), Toast.LENGTH_SHORT).show();
                }
            }
        });


        textInput.setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        textInput.show();
    }

    @OnShowRationale({Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE})
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_sms_rationale)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied({Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE})
    void showDeniedForCamera() {
        Toast.makeText(this, R.string.permission_sms_denied, Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain({Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE})
    void showNeverAskForCamera() {
        Toast.makeText(this, R.string.permission_sms_never_askagain, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), new IntentFilter(FILTER));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onStop();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleMessage(intent);
        }
    };

    private void handleMessage(Intent msg) {
        Log.i(TAG, "handleMessage: Entered");
        Bundle data = msg.getExtras();
        int check = data.getInt("Restart");
        if (check == 100) {
            Log.i(TAG, "handleMessage: 100");
            diagnosisReturned = "";
            rxReturned = "";
            testsReturned = "";
            adviceReturned = "";
            additionalReturned = "";
            String[] columns = {"value", " concept_id"};
            String orderBy = "visit_id";
            String visitSelection = "patient_id = ? AND visit_id = ?";
            String[] visitArgs = {patientID, visitID};
            Cursor visitCursor = db.query("obs", columns, visitSelection, visitArgs, null, null, orderBy);
            if (visitCursor.moveToFirst()) {
                do {
                    int dbConceptID = visitCursor.getInt(visitCursor.getColumnIndex("concept_id"));
                    String dbValue = visitCursor.getString(visitCursor.getColumnIndex("value"));
                    parseData(dbConceptID, dbValue);
                } while (visitCursor.moveToNext());
            }
            visitCursor.close();
        } else if (check == 200) {
            Log.i(TAG, "handleMessage: 200");
            addDownloadButton();
        }
    }

    private void addDownloadButton() {
        if (downloadButton == null) {
            downloadButton = new Button(VisitSummaryActivity.this);
            downloadButton.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                    LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT));
            downloadButton.setText(R.string.visit_summary_button_download);

            Toast.makeText(this, getString(R.string.visit_summary_button_download), Toast.LENGTH_SHORT).show();

            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String[] DELAYED_JOBS_PROJECTION = new String[]{DelayedJobQueueProvider._ID, DelayedJobQueueProvider.SYNC_STATUS};
                    String SELECTION = DelayedJobQueueProvider.JOB_TYPE + "=? AND " +
                            DelayedJobQueueProvider.PATIENT_ID + "=? AND " +
                            DelayedJobQueueProvider.VISIT_ID + "=?";
                    String[] ARGS = new String[]{"prescriptionDownload", patientID, visitID};

                    Cursor c = getContentResolver().query(DelayedJobQueueProvider.CONTENT_URI,
                            DELAYED_JOBS_PROJECTION, SELECTION, ARGS, null);


                    Log.i(TAG, "onClick: " + c.getCount());

                    if (c == null || c.getCount() == 0) {
                        Snackbar.make(view, "Downloading from doctor", Snackbar.LENGTH_LONG).show();
                        Intent startDownload = new Intent(VisitSummaryActivity.this, PrescriptionDownloadService.class);
                        startDownload.putExtra("patientID", patientID);
                        startDownload.putExtra("visitID", visitID);
                        startDownload.putExtra("name", patientName);
                        startDownload.putExtra("visitUUID", visitUUID);
                        startService(startDownload);
                    } else if (c != null && c.moveToFirst()) {
                        Log.d(TAG, "onClick: Not In Null");
                        int sync_status = c.getInt(c.getColumnIndexOrThrow(DelayedJobQueueProvider.SYNC_STATUS));
                        switch (sync_status) {
                            case ClientService.STATUS_SYNC_STOPPED: {

                                Intent serviceIntent = new Intent(VisitSummaryActivity.this, PrescriptionDownloadService.class);
                                serviceIntent.putExtra("serviceCall", "prescriptionDownload");
                                serviceIntent.putExtra("patientID", patientID);
                                serviceIntent.putExtra("visitID", visitID);
                                serviceIntent.putExtra("name", patientName);
                                serviceIntent.putExtra("visitUUID", visitUUID);
                                serviceIntent.putExtra("visitUUID", visitUUID);
                                serviceIntent.putExtra("queueId", c.getInt(c.getColumnIndex(DelayedJobQueueProvider._ID)));
                                startService(serviceIntent);

                                break;
                            }
                            case ClientService.STATUS_SYNC_IN_PROGRESS: {
                                Toast.makeText(context, getString(R.string.sync_in_progress), Toast.LENGTH_SHORT).show();
                                break;
                            }
                            default:
                        }
                    }
                    //  retrieveOpenMRS(view);
                }
            });
            mLayout.addView(downloadButton, mLayout.getChildCount());
        }

    }

    private void isNetworkAvailable(Context context) {
        int flag = 0;

        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {

                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        if (!isConnected) {
                            if (internetCheck != null) {
                                internetCheck.setIcon(R.drawable.ic_action_circle_green);
                                flag = 1;
                            }
                        }
                    }
                }
            }
        }

        if (flag == 0) {
            if (internetCheck != null) {
                internetCheck.setIcon(R.drawable.ic_action_circle_red);
            }

        }

    }
}
