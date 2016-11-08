package com.devansh.hwfsolver;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.devansh.hwfsolver.com.devansh.dawg.DawgArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;

    TextView letters, striked, mostProbableLetter;
    Button solve, findWords;
    CheckBox onlyHwfWords;
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
        mostProbableLetter = (TextView) findViewById(R.id.mostProbableLetter);
        solve = (Button) findViewById(R.id.solve);
        findWords = (Button) findViewById(R.id.findWords);
        onlyHwfWords = (CheckBox) findViewById(R.id.onlyHwfWords);
        list = (ListView) findViewById(R.id.list);
        list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1));

        findWords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findWords(letters.getText().toString(), onlyHwfWords.isChecked());
            }
        });

        solve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solve(letters.getText().toString(), striked.getText().toString());
            }
        });


    }

    private boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private void solve(String word, String strikedLetters) {
        strikedLetters = strikedLetters.toLowerCase().trim();
        final String finalWord = word.toLowerCase().trim();
        startProgress();

        final boolean[] strikedMap = new boolean[26];
        for (int i = 0; i < strikedLetters.length(); i++) {
            if (strikedLetters.charAt(i) >= 'a' && strikedLetters.charAt(i) <= 'z') {
                strikedMap[strikedLetters.charAt(i) - 'a'] = true;
            }
        }

        int lastVowel = -1;

        for (int i = 0; i < finalWord.length(); i++) {
            if (finalWord.charAt(i) >= 'a' && finalWord.charAt(i) <= 'z') {
                strikedMap[finalWord.charAt(i) - 'a'] = true;
                if (isVowel(finalWord.charAt(i))) {
                    lastVowel = i;
                }
            }
        }

        final int finalLastVowel = lastVowel;

        new AsyncTask<Void, Void, List<String>>() {
            String probableText = "";

            private void solveHelper(int currentNode, int currentIndex, StringBuilder prefix,
                                     List<String> res) {

                if (currentIndex == finalWord.length()) {
                    if (dawgArray.isEndOFWord(currentNode)) {
                        res.add(prefix.toString());
                    }
                    return;
                }

                char current = finalWord.charAt(currentIndex);

                for (Map.Entry<Character, Integer> children : dawgArray
                        .getChildren(currentNode).entrySet()) {
                    char child = children.getKey();
                    int childNode = children.getValue();

                    if (current == child || (current == '.' && !strikedMap[child - 'a'] &&
                            (!isVowel(child) || currentIndex < finalLastVowel))) {
                        prefix.append(child);
                        solveHelper(childNode, currentIndex + 1, prefix, res);
                        prefix.replace(prefix.length() - 1, prefix.length(), "");
                    }

                }
            }

            @Override
            protected List<String> doInBackground(Void... params) {
                List<String> res = new ArrayList<>();
                solveHelper(0, 0, new StringBuilder(), res);
                Collections.sort(res);

                if (res.size() == 0) {
                    probableText = "No solutions";
                } else {

                    int[] charCounts = new int[26];
                    HashSet<Character> seen = new HashSet<>();
                    for (String word : res) {
                        int len = word.length();
                        seen.clear();
                        for (int i = 0; i < len; i++) {
                            char c = (char) (word.charAt(i) - 'a');
                            if (!seen.contains(c)) {
                                charCounts[c]++;
                                seen.add(c);
                            }
                        }
                    }

                    char bestLetter = 'a';
                    int bestValue = Integer.MIN_VALUE;
                    for (int i = 0; i < charCounts.length; i++) {
                        if (charCounts[i] > bestValue && !strikedMap[i]) {
                            bestValue = charCounts[i];
                            bestLetter = (char) ('a' + i);
                        }
                    }

                    probableText = "Most Probable Letter: " + bestLetter + " (" +
                            charCounts[bestLetter - 'a'] + "/" + res.size() + ")";
                }

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
                mostProbableLetter.setText(probableText);
            }
        }.execute();
    }

    private void findWords(String letters, final boolean onlyHwfWords) {
        letters = letters.toLowerCase().trim();
        startProgress();
        final int[] chars = new int[26];
        for (int i = 0; i < letters.length(); i++) {
            if (letters.charAt(i) >= 'a' && letters.charAt(i) <= 'z') {
                chars[letters.charAt(i) - 'a']++;
            }
        }
        mostProbableLetter.setText("");

        new AsyncTask<Void, Void, List<String>>() {

            private void findWordsHelper(int currentNode, StringBuilder prefix, List<String> res,
                                         int[] chars) {

                if (dawgArray.isEndOFWord(currentNode) && (prefix.length() >= 4 || !onlyHwfWords)) {
                    res.add(prefix.toString());
                }

                if (prefix.length() == 8 && onlyHwfWords) {
                    return;
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
                Collections.sort(res, new Comparator<String>() {
                    @Override
                    public int compare(String lhs, String rhs) {
                        return rhs.length() - lhs.length();
                    }
                });
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
