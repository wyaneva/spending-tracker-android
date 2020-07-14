package com.example.spendingtracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText editTextDate,editTextSum, editTextItem, editTextReason;
    Spinner spinnerCategory, spinnerWho;
    CheckBox checkBoxEssential;
    Button buttonAddItem;

    String sheetUrl = "https://script.google.com/macros/s/AKfycbzz93Yb_MDZbljoENvVWFY2lBwOalZpTXosoQ8jcS5FedCFMpk/exec";

    public static final String MyPreferences = "MyPrefs";
    SharedPreferences sharedPreferences;
    final String prefWhoKey = "Who";
    final String Vanya = "Vanya";
    final String Fraser = "Fraser";
    String startingWhoString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextDate = (EditText)findViewById(R.id.editText_Date);
        editTextSum = (EditText)findViewById(R.id.editText_Sum);
        editTextItem = (EditText)findViewById(R.id.editText_Item);
        editTextReason = (EditText)findViewById(R.id.editText_Reason);
        spinnerCategory = (Spinner)findViewById(R.id.spinner_Category);
        spinnerWho = (Spinner)findViewById(R.id.spinner_Who);
        checkBoxEssential = (CheckBox) findViewById(R.id.checkBox_Essential);

        // create a date picker
        final SimpleDateFormat date_format = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String date_str = date_format.format(new Date());
        editTextDate.setText(date_str);
        editTextDate.setInputType(InputType.TYPE_DATETIME_VARIATION_DATE);
        editTextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            final Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH);
            int year = calendar.get(Calendar.YEAR);
            // date picker dialog
            DatePickerDialog picker = new DatePickerDialog(MainActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Date date = new GregorianCalendar(year, monthOfYear, dayOfMonth).getTime();
                    editTextDate.setText(date_format.format(date));
                }
                        }, year, month, day);
            picker.show();
            }
        });

        // create a currency edit
        editTextSum.addTextChangedListener(new TextWatcher() {
            String currentCurrency = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!s.toString().equals(currentCurrency)) {
                    editTextSum.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("[£,.]", "");
                    Double d = cleanString.equals("") ? 0 : Double.parseDouble(cleanString);
                    String f = NumberFormat.getCurrencyInstance().format(d/100);
                    currentCurrency = f;
                    editTextSum.setText(f);
                    editTextSum.setSelection(f.length());
                    editTextSum.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        editTextSum.setText("0");

        // set Category spinner
        getCategories();

        // set Who spinner
        ArrayList<String> list = new ArrayList<String>();
        list.add(Vanya);
        list.add(Fraser);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWho.setAdapter(adapter);

        // get the Who preference and set the spinner
        SharedPreferences sharedPref = getSharedPreferences(MyPreferences, Context.MODE_PRIVATE);
        startingWhoString = sharedPref.getString(prefWhoKey, Fraser);
        spinnerWho.setSelection(adapter.getPosition(startingWhoString));

        buttonAddItem = (Button)findViewById(R.id.btn_add_item);
        buttonAddItem.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getCategories() {
        // Important: add the action at the end of the url
        final ProgressDialog loading = ProgressDialog.show(this,"Loading","Please wait");
        StringRequest stringRequest = new StringRequest(Request.Method.GET, sheetUrl+"?action=getCategories",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        ArrayList<String> list = new ArrayList<>();

                        try {
                            JSONObject jobj = new JSONObject(response);
                            JSONArray jarray = jobj.getJSONArray("categories");

                            for (int i = 0; i < jarray.length(); i++) {

                                JSONObject jo = jarray.getJSONObject(i);
                                String category = jo.getString("category");
                                list.add(category);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, list);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategory.setAdapter(adapter);

                        loading.dismiss();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {}
                }
        );
        int socketTimeOut = 50000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(policy);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }

    private void addItemToSheet() {

        final String date = editTextDate.getText().toString().trim();
        final String sum = editTextSum.getText().toString().trim();
        final String item = editTextItem.getText().toString().trim();
        final String category = spinnerCategory.getSelectedItem().toString().trim();
        final String essential = checkBoxEssential.isChecked() ? "Yes" : "No";
        final String reason = editTextReason.getText().toString().trim();
        final String who = spinnerWho.getSelectedItem().toString().trim();

        if(sum.equals("£0.00")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please enter a sum greater than £0.00.");
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }

        // save Who preference if it has changed
        if(!startingWhoString.equals(who)) {
            SharedPreferences sharedPref = this.getSharedPreferences(MyPreferences, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(prefWhoKey, who);
            editor.commit();
        }

        final ProgressDialog loading = ProgressDialog.show(this,"Adding Item","Please wait");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, sheetUrl,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    loading.dismiss();
                    Toast.makeText(MainActivity.this,response,Toast.LENGTH_LONG).show();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action","addItem");
                params.put("date", date);
                params.put("sum", sum);
                params.put("item", item);
                params.put("category", category);
                params.put("essential", essential);
                params.put("reason", reason);
                params.put("who", who);
                return params;
            }
        };

        int socketTimeOut = 50000; // 50 seconds
        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }

    @Override
    public void onClick(View v) {
        if(v==buttonAddItem){
            addItemToSheet();
        }
    }
}
