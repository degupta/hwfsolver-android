package com.devansh.hwfsolver;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.devansh.hwfsolver.com.devansh.dawg.DawgArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;

    TextView letters, striked;
    Button solve, findWords;
    ListView list;

    private DawgArray dawgArray;
    protected boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dawgArray = new DawgArray(getResources().openRawResource(R.raw.dict));

        letters = (TextView) findViewById(R.id.letters);
        striked = (TextView) findViewById(R.id.striked);
        solve = (Button) findViewById(R.id.solve);
        findWords = (Button) findViewById(R.id.findWords);
        list = (ListView) findViewById(R.id.list);
        list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1));

        findWords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findWords(letters.getText().toString());
            }
        });
    }

    private void findWords(String letters) {
        letters = letters.toLowerCase();
        startProgress();
        final int[] chars = new int[26];
        for (int i = 0; i < letters.length(); i++) {
            chars[letters.charAt(i) - 'a']++;
        }

        new AsyncTask<Void, Void, List<String>>() {

            private void findWordsHelper(int currentNode, StringBuilder prefix, List<String> res,
                                         int[] chars) {

                if (dawgArray.isEndOFWord(currentNode)) {
                    res.add(prefix.toString());
                }

                for (Map.Entry<Character, Integer> children : dawgArray
                        .getChildren(currentNode).entrySet()) {
                    char c = children.getKey();
                    if (chars[c - 'a'] > 0) {
                        chars[c - 'a']--;
                        prefix.append(c);
                        findWordsHelper(children.getValue(), prefix, res, chars);
                        chars[c - 'a']++;
                        prefix.replace(prefix.length() - 1, prefix.length(), "");
                    }
                }
            }

            @Override
            protected List<String> doInBackground(Void... params) {
                List<String> res = new ArrayList<>();
                findWordsHelper(0, new StringBuilder(), res, chars);
                return res;
            }

            @Override
            protected void onPostExecute(List<String> result) {
                if (isActivityDestroyed()) {
                    return;
                }
                stopProgress();
                ((ArrayAdapter) list.getAdapter()).clear();
                ((ArrayAdapter<String>) list.getAdapter()).addAll(result);
            }
        }.execute();
    }


    @Override
    protected void onDestroy() {
        destroyed = true;
        super.onDestroy();
    }

    public boolean isActivityDestroyed() {
        return (destroyed || isFinishing());
    }

    // Progress
    public void startProgress() {
        startProgress(getString(R.string.please_wait));
    }

    public void startProgress(String str) {
        startProgress(str, null);
    }

    public void startProgress(String str,
                              final DialogInterface.OnCancelListener cancelListener) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            if (cancelListener != null) {
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        stopProgress();
                        cancelListener.onCancel(dialog);
                    }
                });
            } else {
                progressDialog.setCancelable(false);
            }
            progressDialog.setIndeterminate(true);
        } else {
            progressDialog.dismiss();
        }
        progressDialog.setMessage(str);
        progressDialog.show();
    }

    public void stopProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
