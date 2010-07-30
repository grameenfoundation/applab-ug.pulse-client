/**
 * Copyright (C) 2010 Grameen Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package yo.applab.pulse;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class About extends Activity {
    private TextView applicationNameAndVersion;
    private TextView releaseDate;
    private TextView phoneId;
    private TextView organizationContactInformation;
    private Button closeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_view);

        // setup the version
        this.applicationNameAndVersion = (TextView)findViewById(R.id.name_version);
        this.applicationNameAndVersion.setText(getString(R.string.app_name) + "\nVersion: " + getString(R.string.app_version));

        // the release date
        this.releaseDate = (TextView)findViewById(R.id.release);
        this.releaseDate.setText("Release Date: " + getString(R.string.release_date));

        // Get the phone ID (i.e. IMEI number)
        this.phoneId = (TextView)findViewById(R.id.phone_id);
        this.phoneId.setText("Phone ID: " + Global.getImei(this));

        this.organizationContactInformation = (TextView)findViewById(R.id.info);
        this.organizationContactInformation.setText(getString(R.string.info));

        // finally, allow the user a way of dismissing this dialog
        this.closeButton = (Button)findViewById(R.id.close);
        this.closeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }
}