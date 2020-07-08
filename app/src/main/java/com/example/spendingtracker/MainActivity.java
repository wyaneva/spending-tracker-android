package com.example.spendingtracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText editTextDate,editTextSum, editTextItem;
    Button buttonAddItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextDate = (EditText)findViewById(R.id.editText_Date);
        editTextSum = (EditText)findViewById(R.id.editText_Sum);
        editTextItem = (EditText)findViewById(R.id.editText_Item);

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

    private void addItemToSheet() {

        final String date = editTextDate.getText().toString().trim();
        final String sum = editTextSum.getText().toString().trim();
        final String item = editTextItem.getText().toString().trim();
        if(sum.equals("£0.00")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please enter a sum greater than £0.00.");
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }

        final ProgressDialog loading = ProgressDialog.show(this,"Adding Item","Please wait");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbzz93Yb_MDZbljoENvVWFY2lBwOalZpTXosoQ8jcS5FedCFMpk/exec",
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
                Map<String, String> parmas = new HashMap<>();

                //here we pass params
                parmas.put("action","addItem");
                parmas.put("date", date);
                parmas.put("sum", sum);
                parmas.put("item", item);

                return parmas;
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
