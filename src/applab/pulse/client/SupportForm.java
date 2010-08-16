/**
 * Copyright (C) 2010 Grameen Foundation
Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
 */

package applab.pulse.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import applab.client.*;

public class SupportForm extends ApplabActivity {
    public static final int ABOUT_ID = Menu.FIRST;
    public static final int EXIT_ID = Menu.FIRST + 2;
    public static final int SETTINGS_ID = Menu.FIRST + 1;
    private final String TAG = "Form";
    private Button sendButton;
    private EditText editText;
    private String response;
    private String edit;
    private ProgressDialog progress;
    private HttpClient client;
    private int ConnectionTimeout = Global.TIMEOUT;
    private int SocketTimeout = Global.TIMEOUT;
    private AlertDialog alert;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            progress.dismiss();

            switch (msg.what) {
                case 0:
                    /*
                     * Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG) .show();
                     */
                    setupDialog().show();
                    break;
                case 1:
                    /*
                     * Toast.makeText(getApplicationContext(), "Message not sent", Toast.LENGTH_LONG) .show();
                     */

                    setupDialog().show();
                    break;
            }
        }
    };
    
    public SupportForm() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        setTitle(getString(R.string.label));
        sendButton = (Button)findViewById(R.id.send_button);
        sendButton.setText(R.string.send);
        editText = (EditText)findViewById(R.id.msg);
        sendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                edit = editText.getText().toString().trim();
                if (edit.length() > 0) {
                    try {
                        postData();
                        editText.setText("");
                    }
                    catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Interrupted", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), getString(R.string.empty_msg), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String getUrl() throws UnsupportedEncodingException {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String url = settings.getString(Settings.KEY_SERVER, getString(R.string.default_server));
        if (url.endsWith("/")) {
            url = url.concat(getString(R.string.server_path2));
        }
        else {
            url = url.concat("/" + getString(R.string.server_path2));
        }

        edit = URLEncoder.encode(edit, "UTF-8");
        url = url.concat("?handset_id=" + Handset.getImei() + "&message=" + edit);
        return url;
    }

    public void postData() throws UnsupportedEncodingException, InterruptedException {
        Thread netThread = new Thread() {

            public void run() {

                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                client = new DefaultHttpClient();
                HttpParams httpParameters = new BasicHttpParams();
                HttpConnectionParams.setSoTimeout(httpParameters, SocketTimeout);
                HttpConnectionParams.setConnectionTimeout(httpParameters, ConnectionTimeout);
                String raw = "ERROR";
                try {
                    HttpGet getMethod = new HttpGet(getUrl());

                    raw = client.execute(getMethod, responseHandler);
                    response = raw;
                    handler.sendEmptyMessage(1);

                }
                catch (ClientProtocolException e) {

                }
                catch (IOException e) {
                    handler.sendEmptyMessage(0);
                    android.util.Log.e(TAG, "IOException:" + e);
                }
                catch (Exception e) {
                    handler.sendEmptyMessage(0);
                    android.util.Log.e(TAG, "Exception:" + e);
                }

            }
        };
        progress = ProgressDialog.show(this, "", getString(R.string.submitting), true);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.connection_error)).setCancelable(false).setTitle(getString(R.string.app_name))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();

                    }

                }).setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        try {
                            postData();
                        }
                        catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
        alert = builder.create();
        netThread.start();
    }

    public AlertDialog setupDialog() {
        if (response.length() < 5)
            response = getString(R.string.empty_response);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(response).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog message = builder.create();
        return message;
    }
}
