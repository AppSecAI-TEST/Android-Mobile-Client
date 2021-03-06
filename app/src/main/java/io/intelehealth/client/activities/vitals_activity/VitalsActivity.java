package io.intelehealth.client.activities.vitals_activity;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import io.intelehealth.client.R;
import io.intelehealth.client.activities.family_history_activity.FamilyHistoryActivity;
import io.intelehealth.client.activities.past_medical_history_activity.PastMedicalHistoryActivity;
import io.intelehealth.client.activities.visit_summary_activity.VisitSummaryActivity;
import io.intelehealth.client.activities.physical_exam_activity.PhysicalExamActivity;
import io.intelehealth.client.database.LocalRecordsDatabaseHelper;
import io.intelehealth.client.objects.TableExam;
import io.intelehealth.client.utilities.ConceptId;

/**
 * Records the patient vitals in the {@link TableExam} container.
 */
public class VitalsActivity extends AppCompatActivity {


    EditText mHeight, mWeight, mPulse, mBpSys, mBpDia, mTemperature, mSpo2, mBMI;
    Long obsID;
    final String LOG_TAG = "VitalsActivity";
    int flag_height =0, flag_weight=0;

    String patientID = "1";
    String visitID;
    String state;
    String patientName;
    String intentTag;
    String heightvalue;
    String weightvalue;

    String maxh = "272";
    String maxw = "150";
    String maxbpsys = "300";
    String minbpsys = "50";
    String maxbpdys = "150";
    String minbpdys = "30";
    String maxpulse = "200";
    String minpulse = "30";
    String maxte = "120";
    String minte = "80";
    String maxspo2 = "100";
    String minspo2 = "1";



    ArrayList<String> physicalExams;

    LocalRecordsDatabaseHelper mDbHelper;
    SQLiteDatabase db;

    TableExam results = new TableExam();


    @Override
    protected void onCreate(Bundle savedInstanceState) {



        //For Testing
        //patientID = Long.valueOf("1");


        Intent intent = this.getIntent(); // The intent was passed to the activity
        if (intent != null) {
            patientID = intent.getStringExtra("patientID");
            visitID = intent.getStringExtra("visitID");
            state = intent.getStringExtra("state");
            patientName = intent.getStringExtra("name");
            intentTag = intent.getStringExtra("tag");
            physicalExams = intent.getStringArrayListExtra("exams"); //Pass it along


//            Log.v(TAG, "Patient ID: " + patientID);
//            Log.v(TAG, "Visit ID: " + visitID);
//            Log.v(TAG, "Patient Name: " + patientName);
//            Log.v(TAG, "Intent Tag: " + intentTag);
        }


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitals);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);


        setTitle(patientName + ": " + getTitle());

        mHeight = (EditText) findViewById(R.id.table_height);
        mWeight = (EditText) findViewById(R.id.table_weight);
        mPulse = (EditText) findViewById(R.id.table_pulse);
        mBpSys = (EditText) findViewById(R.id.table_bpsys);
        mBpDia = (EditText) findViewById(R.id.table_bpdia);
        mTemperature = (EditText) findViewById(R.id.table_temp);
        mSpo2 = (EditText) findViewById(R.id.table_spo2);

        mBMI = (EditText) findViewById(R.id.table_bmi);

        if (intentTag != null && intentTag.equals("edit")) {
            loadPrevious();
        }

        //BMI calculation is done in metric units
      /* mBMI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String heightValue = mHeight.getText().toString();
                String weightValue = mWeight.getText().toString();

                if (heightValue.matches("") || weightValue.matches("")) {
                    String message = "Please enter height and weight first.";
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(VitalsActivity.this);
                    alertDialogBuilder.setMessage(message);
                    alertDialogBuilder.setNeutralButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } else {

                    double numerator = Double.parseDouble(mWeight.getText().toString()) * 10000;
                    double denominator = (Double.parseDouble(mHeight.getText().toString())) * (Double.parseDouble(mHeight.getText().toString()));
                    double bmi_value = numerator / denominator;
                    mBMI.setText(String.format(Locale.ENGLISH, "%,2f", bmi_value));
                    //Log.d("BMI", String.valueOf(bmi_value));
                }


            }
        });

        */

        mHeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0)
                {
                    mBMI.getText().clear();
                    flag_height =1;
                    heightvalue = mHeight.getText().toString();

                }
                else
                {
                    flag_height=0;
                    mBMI.getText().clear();
                }
            }

            @Override
            public void afterTextChanged(Editable s) { calculateBMI();}
        });

        mWeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0)
                {
                    mBMI.getText().clear();
                    flag_weight =1;
                    weightvalue = mWeight.getText().toString();
                }
                else
                {
                    flag_weight=0;
                    mBMI.getText().clear();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

                calculateBMI(); }
        });



        mSpo2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.table_spo2 || id == EditorInfo.IME_NULL) {
                    validateTable();
                    return true;
                }
                return false;
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateTable();
            }
        });

       }

    public void calculateBMI()
    {
        if(flag_height==1 && flag_weight==1)
        {
            mBMI.getText().clear();
            double numerator = Double.parseDouble(weightvalue) * 10000;
            double denominator = (Double.parseDouble(heightvalue)) * (Double.parseDouble(heightvalue));
            double bmi_value = numerator / denominator;
            mBMI.setText(String.format(Locale.ENGLISH, "%,2f", bmi_value));
        }
        else if(flag_height==0 || flag_weight==0)
        {
            // do nothing
            mBMI.getText().clear();
        }
       /* else
        {
            String message = "Please enter height and weight first.";
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(VitalsActivity.this);
            alertDialogBuilder.setMessage(message);
            alertDialogBuilder.setNeutralButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }*/

    }


    public void loadPrevious() {
        mDbHelper = new LocalRecordsDatabaseHelper(this.getApplicationContext());
        db = mDbHelper.getWritableDatabase();
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
    }

    private void parseData(int concept_id, String value) {
        switch (concept_id) {
            case ConceptId.HEIGHT: //Height
                mHeight.setText(value);
                break;
            case ConceptId.WEIGHT: //Weight
                mWeight.setText(value);
                break;
            case ConceptId.PULSE: //Pulse
                mPulse.setText(value);
                break;
            case ConceptId.SYSTOLIC_BP: //Systolic BP
                mBpSys.setText(value);
                break;
            case ConceptId.DIASTOLIC_BP: //Diastolic BP
                mBpDia.setText(value);
                break;
            case ConceptId.TEMPERATURE: //Temperature
                mTemperature.setText(value);
                break;
            case ConceptId.SPO2: //SpO2
                mSpo2.setText(value);
                break;
            default:
                break;

        }
    }

    public void validateTable() {
        boolean cancel = false;
        View focusView = null;

        // Store values at the time of the fab is clicked.
        ArrayList<EditText> values = new ArrayList<EditText>();
        values.add(mHeight);
        values.add(mWeight);
        values.add(mPulse);
        values.add(mBpSys);
        values.add(mBpDia);
        values.add(mTemperature);
        values.add(mSpo2);

        // Check to see if values were inputted.
        for (int i = 0; i < values.size(); i++) {
            if (i == 0) {
                EditText et = values.get(i);
                String abc = et.getText().toString().trim();
                if (abc != null && !abc.isEmpty()) {
                    if (Double.parseDouble(abc) > Double.parseDouble(maxh)) {
                        et.setError("Height should be between 0 and " + maxh + "cm");
                        focusView = et;
                        cancel = true;
                        break;
                    } else {
                        cancel = false;
                    }
//       }
                } else {
                    cancel = false;
                }
            }else if(i==1) {
                EditText et = values.get(i);
                String abc1 = et.getText().toString().trim();
                if (abc1 != null && !abc1.isEmpty()) {
                    if (Double.parseDouble(abc1) > Double.parseDouble(maxw)) {
                        et.setError("Weight should be less than " + maxw + "kg");
                        focusView = et;
                        cancel = true;
                        break;
                    } else {
                        cancel = false;
                    }
//       }
                } else {
                    cancel = false;
                }

            } else if(i==2)
            {
                EditText et = values.get(i);
                String abc2 = et.getText().toString().trim();
                if (abc2 != null && !abc2.isEmpty() && (!abc2.equals("0.0"))) {
                    if ((Double.parseDouble(abc2) > Double.parseDouble(maxpulse)) ||
                            (Double.parseDouble(abc2) < Double.parseDouble(minpulse))) {
                        et.setError("Pulse should be in between " + minpulse + " and " + maxpulse);
                        focusView = et;
                        cancel = true;
                        break;
                    } else {
                        cancel = false;
                    }
//       }
                } else {
                    cancel = false;
                }

            }else if (i==3)
            {
                EditText et = values.get(i);
                String abc1 = et.getText().toString().trim();
                if (abc1 != null && !abc1.isEmpty() && (!abc1.equals("0.0"))) {
                    if ((Double.parseDouble(abc1) > Double.parseDouble(maxbpsys)) ||
                            (Double.parseDouble(abc1) < Double.parseDouble(minbpsys))) {
                        et.setError("Systolic pressure should be in between " + minbpsys + " and " + maxbpsys);
                        focusView = et;
                        cancel = true;
                        break;
                    } else {
                        cancel = false;
                    }
//       }
                } else {
                    cancel = false;
                }

            }else if (i==4)
            {
                EditText et = values.get(i);
                String abc1 = et.getText().toString().trim();
                if (abc1 != null && !abc1.isEmpty() && (!abc1.equals("0.0"))) {
                    if ((Double.parseDouble(abc1) > Double.parseDouble(maxbpdys)) ||
                            (Double.parseDouble(abc1) < Double.parseDouble(minbpdys))) {
                        et.setError("Diastolic pressure should be in between " + minbpdys + " and " + maxbpdys);
                        focusView = et;
                        cancel = true;
                        break;
                    } else {
                        cancel = false;
                    }
//       }
                } else {
                    cancel = false;
                }

            }else if (i==5)
            {
                EditText et = values.get(i);
                String abc1 = et.getText().toString().trim();
                if (abc1 != null && !abc1.isEmpty() && (!abc1.equals("0.0"))) {
                    if ((Double.parseDouble(abc1) > Double.parseDouble(maxte)) ||
                            (Double.parseDouble(abc1) < Double.parseDouble(minte))) {
                        et.setError("Temperature should be in between "+maxte+" and "+minte);
                        focusView = et;
                        cancel = true;
                        break;
                    } else {
                        cancel = false;
                    }
//       }
                } else {
                    cancel = false;
                }
            }
            else
            {
                EditText et = values.get(i);
                String abc1 = et.getText().toString().trim();
                if (abc1 != null && !abc1.isEmpty() && (!abc1.equals("0.0"))) {
                    if ((Double.parseDouble(abc1) > Double.parseDouble(maxspo2)) ||
                    (Double.parseDouble(abc1) < Double.parseDouble(minspo2))) {
                        et.setError("SpO2 should be in between "+minspo2+" and "+maxspo2);
                        focusView = et;
                        cancel = true;
                        break;
                    } else {
                        cancel = false;
                    }
//       }
                } else {
                    cancel = false;
                }
            }
        }

        if (cancel) {
            // There was an error - focus the first form field with an error.
            focusView.requestFocus();
            return;
        } else {
            try {
                if (mHeight.getText()!=null) {
                    results.setHeight((mHeight.getText().toString()));
                }
                if(mWeight.getText()!=null)
                {
                    results.setWeight((mWeight.getText().toString()));
                }
                if(mPulse.getText()!=null)
                {
                    results.setPulse((mPulse.getText().toString()));
                }
                if(mBpDia.getText()!=null)
                {
                    results.setBpdia((mBpDia.getText().toString()));
                }
                if (mBpSys.getText()!=null)
                {
                    results.setBpsys((mBpSys.getText().toString()));
                }
                if(mTemperature.getText()!=null)
                {
                    results.setTemperature((mTemperature.getText().toString()));
                }
                if(mSpo2.getText()!=null)
                {
                    results.setSpo2((mSpo2.getText().toString()));
                }



            } catch (NumberFormatException e) {
                Snackbar.make(findViewById(R.id.cl_table), "Error: non-decimal number entered.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }

//
        }

        if (intentTag != null && intentTag.equals("edit")) {
            updateDatabase(results.getHeight(), ConceptId.HEIGHT);
            updateDatabase(results.getWeight(), ConceptId.WEIGHT);
            updateDatabase(results.getPulse(), ConceptId.PULSE);
            updateDatabase(results.getBpsys(), ConceptId.SYSTOLIC_BP);
            updateDatabase(results.getBpdia(), ConceptId.DIASTOLIC_BP);
            updateDatabase(results.getTemperature(), ConceptId.TEMPERATURE);
            updateDatabase(results.getSpo2(), ConceptId.SPO2);
            Intent intent = new Intent(VitalsActivity.this, VisitSummaryActivity.class);
            intent.putExtra("patientID", patientID);
            intent.putExtra("visitID", visitID);
            intent.putExtra("state", state);
            intent.putExtra("name", patientName);
            intent.putExtra("tag", intentTag);
            startActivity(intent);
        } else {

            insertDb(results.getHeight(), ConceptId.HEIGHT);
            insertDb(results.getWeight(), ConceptId.WEIGHT);
            insertDb(results.getPulse(), ConceptId.PULSE);
            insertDb(results.getBpsys(), ConceptId.SYSTOLIC_BP);
            insertDb(results.getBpdia(), ConceptId.DIASTOLIC_BP);
            insertDb(results.getTemperature(), ConceptId.TEMPERATURE);
            insertDb(results.getSpo2(), ConceptId.SPO2);
            Intent intent = new Intent(VitalsActivity.this, VisitSummaryActivity.class);

            intent.putExtra("patientID", patientID);
            intent.putExtra("visitID", visitID);
            intent.putExtra("state", state);
            intent.putExtra("name", patientName);
            intent.putExtra("tag", intentTag);
            intent.putStringArrayListExtra("exams", physicalExams);
            startActivity(intent);
        }
    }


    private long insertDb(String objValue, int CONCEPT_ID) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String CREATOR_ID = prefs.getString("creatorid", null);

        String value = objValue;

        ContentValues complaintEntries = new ContentValues();

        complaintEntries.put("patient_id", patientID);
        complaintEntries.put("visit_id", visitID);
        complaintEntries.put("creator", CREATOR_ID);
        complaintEntries.put("value", value);
        complaintEntries.put("concept_id", CONCEPT_ID);

        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();
        return localdb.insert("obs", null, complaintEntries);
    }

    private void updateDatabase(String objValue, int CONCEPT_ID) {
        LocalRecordsDatabaseHelper mDbHelper = new LocalRecordsDatabaseHelper(this);
        SQLiteDatabase localdb = mDbHelper.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("value", objValue);

        String selection = "patient_id = ? AND visit_id = ? AND concept_id = ?";
        String[] args = {patientID, visitID, String.valueOf(CONCEPT_ID)};

        localdb.update(
                "obs",
                contentValues,
                selection,
                args
        );

    }

    @Override
    public void onBackPressed() {
    }

}
