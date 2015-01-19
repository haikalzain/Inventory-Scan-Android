package com.haikalzain.inventorypro.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.haikalzain.inventorypro.R;
import com.haikalzain.inventorypro.common.Spreadsheet;
import com.haikalzain.inventorypro.ui.dialogs.NewTemplateDialog;
import com.haikalzain.inventorypro.utils.DropboxUtils;
import com.haikalzain.inventorypro.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


public class TemplatesActivity extends Activity {
    private static final String TAG = "com.haikalzain.inventorypro.ui.TemplatesActivity";

    private static final int NEW_TEMPLATE_REQUEST = 1;
    private ListView templateListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_templates);
        setTitle("Spreadsheet Templates");

        templateListView = (ListView)findViewById(R.id.list_view);
        updateTemplateListView();

        Button newTemplateBtn = (Button) findViewById(R.id.new_template_btn);
        newTemplateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final NewTemplateDialog newTemplateDialog =
                        new NewTemplateDialog(
                                TemplatesActivity.this,
                                "New Template",
                                FileUtils.getTemplateFiles(TemplatesActivity.this));

                newTemplateDialog.setOnPositiveButtonClicked(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(TemplatesActivity.this,
                                NewTemplateActivity.class);
                        intent.putExtra(NewTemplateActivity.TEMPLATE_NAME,
                                newTemplateDialog.getFileName());
                        startActivityForResult(intent, NEW_TEMPLATE_REQUEST);
                    }
                });

                newTemplateDialog.show();

            }
        });
    }

    private void updateTemplateListView(){
        AsyncTask<String, Integer, Long> syncTask = new AsyncTask<String, Integer, Long>() {
            private ProgressDialog dialog;
            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(TemplatesActivity.this);
                dialog.setMessage("Syncing Files");

                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(false);
                        finish();
                    }
                });
                dialog.show();
            }

            @Override
            protected void onPostExecute(Long aLong) {
                if(dialog.isShowing()){
                    dialog.dismiss();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        TemplatesActivity.this,
                        android.R.layout.simple_list_item_1,
                        FileUtils.getFileNames(FileUtils.getTemplateFiles(TemplatesActivity.this)));
                templateListView.setAdapter(adapter);

            }

            @Override
            protected Long doInBackground(String... params) {
                try {
                    DropboxUtils.importAllFromDropbox(getApplicationContext());
                } catch (IOException e) {
                    finish();
                    Log.v(TAG, "Dropbox import failed");
                }
                return 0l;
            }
        };
        syncTask.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_templates, menu);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == NEW_TEMPLATE_REQUEST){
            if(resultCode == RESULT_OK){
                String fileName = data.getStringExtra(NewTemplateActivity.TEMPLATE_NAME);
                Spreadsheet template = (Spreadsheet)
                        data.getSerializableExtra(NewTemplateActivity.TEMPLATE_PARCEL);
                Log.v(TAG, template.toString());

                File outfile = new File(
                        FileUtils.getTemplatesDirectory(this) +
                        File.separator +
                        fileName +
                        ".xls");

                try{
                    template.exportExcelToFile(outfile);
                    DropboxUtils.copyInFile(getApplicationContext(), outfile,
                            DropboxUtils.getTemplatesPath(getApplicationContext()));
                } catch (IOException e) {
                    Log.e(TAG, "Can't save file: " + outfile.toString());
                }

                updateTemplateListView();
            }
        }
    }
}
