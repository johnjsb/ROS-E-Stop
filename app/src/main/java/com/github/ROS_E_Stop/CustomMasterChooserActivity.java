package com.github.ROS_E_Stop;

import com.google.common.base.Preconditions;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.LinearLayout;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.xmlrpc.XmlRpcTimeoutException;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Allows the user to configue a master {@link URI} then it returns that
 * {@link URI} to the calling {@link Activity}.
 * <p>
 * When this {@link Activity} is started, the last used (or the default)
 * {@link URI} is displayed to the user.
 *
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class CustomMasterChooserActivity extends Activity {

    /**
     * The key with which the last used {@link URI} will be stored as a
     * preference.
     */
    private static final String PREFS_KEY_NAME = "URI_KEY";

    /**
     * Package name of the QR code reader used to scan QR codes.
     */
    private static final String BAR_CODE_SCANNER_PACKAGE_NAME =
            "com.google.zxing.client.android.SCAN";

    private EditText uriText;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_master_chooser);
        uriText = (EditText) findViewById(R.id.master_chooser_uri);
        connectButton = (Button) findViewById(R.id.master_chooser_ok);
        uriText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    connectButton.setEnabled(true);
                } else {
                    connectButton.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        // Get the URI from preferences and display it. Since only primitive types
        // can be saved in preferences the URI is stored as a string.
        String uri =
                getPreferences(MODE_PRIVATE).getString(PREFS_KEY_NAME,
                        NodeConfiguration.DEFAULT_MASTER_URI.toString());
        uriText.setText(uri);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // If the Barcode Scanner returned a string then display that string.
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String scanResultFormat = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Preconditions.checkState(scanResultFormat.equals("TEXT_TYPE")
                        || scanResultFormat.equals("QR_CODE"));
                String contents = intent.getStringExtra("SCAN_RESULT");
                uriText.setText(contents);
            }
        }
    }

    public void okButtonClicked(View unused) {
        // Prevent further edits while we verify the URI.
        uriText.setEnabled(false);
        connectButton.setEnabled(false);
        final String uri = uriText.getText().toString();

        // Make sure the URI can be parsed correctly and that the master is
        // reachable.
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    toast("Trying to reach master...");
                    MasterClient masterClient = new MasterClient(new URI(uri));
                    masterClient.getUri(GraphName.of("android/master_chooser_activity"));
                    toast("Connected!");
                    return true;
                } catch (URISyntaxException e) {
                    toast("Invalid URI.");
                    return false;
                } catch (XmlRpcTimeoutException e) {
                    toast("Master unreachable!");
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    // If the displayed URI is valid then pack that into the intent.
                    SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                    editor.putString(PREFS_KEY_NAME, uri);
                    editor.commit();
                    // Package the intent to be consumed by the calling activity.
                    Intent intent = createNewMasterIntent(false);
                    CustomMasterChooserActivity.this.startActivity(intent);
                    //setResult(RESULT_OK, intent);
                    finish();
                } else {
                    connectButton.setEnabled(true);
                    uriText.setEnabled(true);
                }
            }
        }.execute();
    }

    protected void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CustomMasterChooserActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void qrCodeButtonClicked(View unused) {
        Intent intent = new Intent(BAR_CODE_SCANNER_PACKAGE_NAME);
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        // Check if the Barcode Scanner is installed.
        if (!isQRCodeReaderInstalled(intent)) {
            // Open the Market and take them to the page from which they can download the Barcode Scanner
            // app.
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.google.zxing.client.android")));
        } else {
            // Call the Barcode Scanner to let the user scan a QR code.
            startActivityForResult(intent, 0);
        }
    }

    public void advancedCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        LinearLayout advancedOptions = (LinearLayout) findViewById(R.id.advancedOptions);
        if (checked) {
            advancedOptions.setVisibility(View.VISIBLE);
        } else {
            advancedOptions.setVisibility(View.GONE);
        }
    }

    public Intent createNewMasterIntent(boolean useDefaultMaster) {
        Intent intent = new Intent(CustomMasterChooserActivity.this, MainActivity.class);
        final String uri = uriText.getText().toString();
        if(useDefaultMaster)
            MainActivity.MASTER_URI = null;
        else
            MainActivity.MASTER_URI = uri;
        //MainActivity.MASTER_URI = null;
        return intent;
    }

    public void defaultMasterButtonClicked(View unused) {
        Intent intent = createNewMasterIntent(true);
        CustomMasterChooserActivity.this.startActivity(intent);
    }


    public void cancelButtonClicked(View unused) {
        //setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Check if the specified app is installed.
     *
     * @param intent
     *          The activity that you wish to look for.
     * @return true if the desired activity is install on the device, false
     *         otherwise.
     */
    protected boolean isQRCodeReaderInstalled(Intent intent) {
        List<ResolveInfo> list =
                getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (list.size() > 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("DEBUG", "Function is running");
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(com.github.ROS_E_Stop.R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.restart_app:

                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle("Restart App");
                adb.setIcon(android.R.drawable.ic_dialog_alert);
                adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent mStartActivity = new Intent(getBaseContext(), MainActivity.class);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(getBaseContext(), mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager)getBaseContext().getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        System.exit(0);
                    } });


                adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    } });
                adb.show();
                return true;
            case R.id.quit_app:
                System.exit(0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
