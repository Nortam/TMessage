package com.textmessages.textmessagessimple;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.database.sqlite.SQLiteDatabase;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

import android.content.ActivityNotFoundException;
import android.speech.RecognizerIntent;
import com.textmessages.textmessagessimple.database.DBHelper;
import com.textmessages.textmessagessimple.notice.Notice;

public class MainActivity extends AppCompatActivity {

    private Cursor cursor;
    private Spinner categorySpinner, listSpinner;
    private TableLayout tableLayout;
    private ScrollView scrollView;
    private Toolbar toolbar;
    private SearchView searchView;
    private ImageView syncImageView;
    private ImageView searchImageView;
    private boolean voiceSearch = false;
    private boolean globalSearch = false;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private static DBHelper dbHelper = null;
    private static SQLiteDatabase db;
    private static int widthFirstColumn = 200, widthSecondColumn;
    private static int messagesCount = 0;

    private static int[] idKeys, idLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        categorySpinner = (Spinner) findViewById(R.id.categorySpinner);
        listSpinner = (Spinner) findViewById(R.id.toolbarSpinner);
        tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        searchView = (SearchView) findViewById(R.id.searchView);
        syncImageView = (ImageView) findViewById(R.id.syncImageView);
        searchImageView = (ImageView) findViewById(R.id.searchImageView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        try {
            dbHelper = new DBHelper(getApplicationContext());
            dbHelper.open_db();
            super.onResume();
            db = dbHelper.open();
        } catch (Exception e) {
            Notice.showNoticeToast(getApplicationContext(), e.getMessage());
        }
        Display display = getWindowManager().getDefaultDisplay();
        int wight = display.getWidth();
        widthSecondColumn = (wight-120 - widthFirstColumn) / 2;
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setPositiveButton(getString(R.string.add), null);
                    builder.setNegativeButton(getString(R.string.cancel), null);
                    builder.setTitle(getString(R.string.new_message));
                    View view = getLayoutInflater().inflate(R.layout.add_message, null);
                    final Spinner categorySpinner_AddMessage = (Spinner) view.findViewById(R.id.categorySpinner_AddMessage);
                    cursor = db.rawQuery("SELECT * FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()] + "' ORDER BY key;", null);
                    categorySpinner_AddMessage.setAdapter(selectFromKeys(cursor, (short) 1, categorySpinner_AddMessage));
                    final EditText messageTextEdit_AddMessage = (EditText) view.findViewById(R.id.messageTextEdit_AddMessage);
                    final EditText translateTextEdit_AddMessage = (EditText) view.findViewById(R.id.translateTextEdit_AddMessage);
                    builder.setView(view);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(final DialogInterface dialog) {
                            Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (categorySpinner_AddMessage.getSelectedItemPosition() != 0) {
                                        if (messageTextEdit_AddMessage.getText().length() != 0) {
                                            if (translateTextEdit_AddMessage.getText().length() != 0) {
                                                try {
                                                    String message = messageTextEdit_AddMessage.getText().toString().replace("'", "''");
                                                    String translate = translateTextEdit_AddMessage.getText().toString().replace("'", "''");
                                                    db.execSQL("INSERT INTO messages VALUES (" +
                                                            "NULL,'" + message + "', '"
                                                            + translate + "', '"
                                                            + idKeys[categorySpinner_AddMessage.getSelectedItemPosition()] + "');");
                                                    Notice.showNoticeSnackbar(categorySpinner, getString(R.string.message_is_added));
                                                    dialog.dismiss();
                                                } catch (Exception e) {
                                                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                                                }
                                            } else {
                                                translateTextEdit_AddMessage.setError(getString(R.string.field_is_empty));
                                            }
                                        } else {
                                            messageTextEdit_AddMessage.setError(getString(R.string.field_is_empty));
                                        }
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                        builder.setMessage(getString(R.string.select_category))
                                                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                    }
                                                }).show();
                                    }
                                }
                            });
                        }
                    });
                    alertDialog.show();
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (categorySpinner.isEnabled()) {
                    try {
                        String queryStr = searchView.getQuery().toString();
                        queryStr = queryStr.replace("'", "''");
                        if (globalSearch) {
                            cursor = db.rawQuery("SELECT messages.id, messages.message, messages.translate FROM messages INNER JOIN keys ON messages.id_key=keys.id " +
                                    "WHERE (messages.message like '%" + queryStr + "%' " +
                                    "OR messages.translate like '%" + queryStr + "%');", null);
                            selectFromMessages(cursor);
                        } else {
                            cursor = db.rawQuery("SELECT messages.id, messages.message, messages.translate FROM messages INNER JOIN keys ON messages.id_key=keys.id " +
                                    "WHERE keys.id_list='" + idLists[listSpinner.getSelectedItemPosition()]
                                    + "'AND (messages.message like '%" + queryStr + "%' "
                                    + "OR messages.translate like '%" + queryStr + "%');", null);
                            selectFromMessages(cursor);
                        }
                    } catch (Exception e) {
                        copyText(e.getMessage());
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(getString(R.string.search_error))
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        try {
            cursor = db.rawQuery("SELECT * FROM lists;", null);
            listSpinner.setAdapter(selectFromLists(cursor, (short) 0));
        } catch (Exception e) {
            Notice.showNoticeToast(getApplicationContext(), e.getMessage());
        }
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (position != 0) {
                        cursor = db.rawQuery("SELECT messages.id, messages.message, messages.translate " +
                                "FROM messages INNER JOIN keys ON messages.id_key=keys.id WHERE keys.id_list='"
                                + idLists[listSpinner.getSelectedItemPosition()] + "' AND messages.id_key='" + idKeys[position] + "';", null);
                        selectFromMessages(cursor);
                    } else {
                        cursor = db.rawQuery("SELECT messages.id, messages.message, messages.translate " +
                                "FROM messages INNER JOIN keys ON messages.id_key=keys.id WHERE keys.id_list='"
                                + idLists[listSpinner.getSelectedItemPosition()] + "';", null);
                        selectFromMessages(cursor);
                    }
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        listSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    cursor = db.rawQuery("SELECT * FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()] + "' ORDER BY key;", null);
                    messagesCount = db.rawQuery("SELECT messages.id, messages.message, messages.translate " +
                            "FROM messages INNER JOIN keys ON messages.id_key=keys.id WHERE keys.id_list='"
                            + idLists[listSpinner.getSelectedItemPosition()] + "';", null).getCount();
                    categorySpinner.setAdapter(selectFromKeys(cursor, (short) 0, categorySpinner));
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        syncImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    cursor = db.rawQuery("SELECT * FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()] + "' ORDER BY key;", null);
                    messagesCount = db.rawQuery("SELECT messages.id, messages.message, messages.translate " +
                            "FROM messages INNER JOIN keys ON messages.id_key=keys.id WHERE keys.id_list='"
                            + idLists[listSpinner.getSelectedItemPosition()] + "';", null).getCount();
                    categorySpinner.setAdapter(selectFromKeys(cursor, (short) 0, categorySpinner));
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
            }
        });
        searchImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceSearch = true;
                promptSpeechInput();
            }
        });
    }

    private void promptSpeechInput() {
        try {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                        getString(R.string.speech_prompt));
                startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
            } catch (Exception e) {
                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
            }
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    searchView.setQuery(result.get(0), true);
                }
                break;
            }
        }
    }

    private void selectFromMessages(Cursor cursor) {
        try {
            tableLayout.removeAllViews();
            progressBar.setMax(cursor.getCount());
            LayoutInflater inflater = LayoutInflater.from(this);
            TableRow tableRow = (TableRow) inflater.inflate(R.layout.title_table, null);
            addRowTable(tableRow, getString(R.string.id), getString(R.string.message), getString(R.string.translate));
            if (voiceSearch && cursor.getCount() == 0) {
                Notice.showNoticeSnackbar(categorySpinner, getString(R.string.messages_not_found));
                voiceSearch = false;
            } else {
                progressBar.setVisibility(View.VISIBLE);
                setEnabled();
                int color = this.getResources().getColor(R.color.TableRow);
                new LoadMessagesAsyncTask(cursor, inflater, color).execute();
            }
        } catch (Exception e) {
            Notice.showNoticeToast(getApplicationContext(), e.getMessage());
        }
    }

    private void setEnabled() {
        listSpinner.setEnabled(!listSpinner.isEnabled());
        categorySpinner.setEnabled(!categorySpinner.isEnabled());
        syncImageView.setEnabled(!syncImageView.isEnabled());
        searchImageView.setEnabled(!searchImageView.isEnabled());
        toolbar.setEnabled(!toolbar.isEnabled());
        if (categorySpinner.isEnabled()) {
            fab.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.INVISIBLE);
        }
    }

    class LoadMessagesAsyncTask extends AsyncTask<Void, TableRow, Void> {
        private int progressBarValue = 0;
        private Cursor cursor;
        private LayoutInflater inflater;
        private TableRow tableRow;
        private int color;
        private int idColIndex = 1;
        private int messageColIndex = 2;
        private int translateColIndex = 3;
        private boolean statusProgressUpdate = false;

        public LoadMessagesAsyncTask(Cursor cursor, LayoutInflater inflater, int color) {
            this.cursor = cursor;
            this.inflater = inflater;
            this.color = color;
        }

        @Override
        protected void onPostExecute(Void v) {
            tableLayout.addView(inflater.inflate(R.layout.table, null));
            progressBar.setVisibility(View.INVISIBLE);
            setEnabled();
        }

        @Override
        protected void onProgressUpdate(TableRow... rows) {
            super.onProgressUpdate(rows);
            try {
                tableLayout.addView(rows[0]);
                progressBar.setProgress(progressBarValue);
            } catch (Exception e) {
                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                while (cursor.moveToNext()) {
                    statusProgressUpdate = true;
                    idColIndex = cursor.getColumnIndex("id");
                    messageColIndex = cursor.getColumnIndex("message");
                    translateColIndex = cursor.getColumnIndex("translate");
                    tableRow = (TableRow) inflater.inflate(R.layout.table, null);
                    progressBarValue++;
                    publishProgress(editTableRow(tableRow, idColIndex, messageColIndex, translateColIndex, progressBarValue, color));
                }
            } catch (Exception e) {
                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
            }
            return null;
        }
    }

    public TableRow editTableRow(TableRow tableRow, int idColIndex, int messageColIndex, int translateColIndex, int progressBarValue, int color) {
        final TextView textView1 = (TextView) tableRow.findViewById(R.id.column_0);
        textView1.setText(String.valueOf(cursor.getInt(idColIndex)));
        textView1.setPadding(5,15,0,15);
        textView1.setWidth(widthFirstColumn);
        final TextView textView2 = (TextView) tableRow.findViewById(R.id.column_1);
        textView2.setText(cursor.getString(messageColIndex));
        textView2.setPadding(10,15,15,15);
        textView2.setWidth(widthSecondColumn);
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textView2.getText() != getString(R.string.message)) {
                    copyText(textView2.getText());
                }
            }
        });
        final TextView textView3 = (TextView) tableRow.findViewById(R.id.column_2);
        textView3.setText(cursor.getString(translateColIndex));
        textView3.setPadding(15,15,0,15);
        textView3.setWidth(widthSecondColumn);
        textView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textView3.getText() != getString(R.string.translate)) {
                    copyText(textView2.getText());
                }
            }
        });
        if (progressBarValue % 2 != 0) {
            tableRow.setBackgroundColor(color);
        }
        return tableRow;
    }

    private ArrayAdapter selectFromKeys(Cursor cursor, short mode, Spinner spinner) {
        idKeys = new int[cursor.getCount()+1];
        idKeys[0] = -1;
        String[] keys = new String[cursor.getCount()+1];
        if (mode == 0) {
            keys[0] = getString(R.string.all_messages) + " (" + messagesCount + ")";
        } else {
            keys[0] = ". . .";
        }
        int i = 1;
        while (cursor.moveToNext()) {
            int idColIndex = cursor.getColumnIndex("id");
            int keyColIndex = cursor.getColumnIndex("key");
            idKeys[i] = cursor.getInt(idColIndex);
            keys[i] = cursor.getString(keyColIndex);
            i++;
        }
        ArrayAdapter adapter = new ArrayAdapter(
                MainActivity.this, android.R.layout.simple_spinner_item, keys
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private ArrayAdapter selectFromLists(Cursor cursor, short mode) {
        idLists = new int[cursor.getCount()];
        String[] lists = new String[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            int idColIndex = cursor.getColumnIndex("id");
            int titleColIndex = cursor.getColumnIndex("title");
            idLists[i] = cursor.getInt(idColIndex);
            lists[i] = cursor.getString(titleColIndex);
            i++;
        }
        ArrayAdapter adapter;
        if (mode == 0) {
            adapter = new ArrayAdapter(
                    MainActivity.this, R.layout.list_spinner, lists
            );
        } else {
            adapter = new ArrayAdapter(
                    MainActivity.this, android.R.layout.simple_spinner_item, lists
            );
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void addRowTable(TableRow tableRow, String id, String message, String translate) {
        final TextView textView1 = (TextView) tableRow.findViewById(R.id.column_0);
        textView1.setText(id);
        textView1.setPadding(5,15,0,15);
        textView1.setWidth(widthFirstColumn);
        final TextView textView2 = (TextView) tableRow.findViewById(R.id.column_1);
        textView2.setText(message);
        textView2.setPadding(10,15,15,15);
        textView2.setWidth(widthSecondColumn);
        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textView2.getText() != getString(R.string.message)) {
                    copyText(textView2.getText());
                }
            }
        });
        final TextView textView3 = (TextView) tableRow.findViewById(R.id.column_2);
        textView3.setText(translate);
        textView3.setPadding(15,15,0,15);
        textView3.setWidth(widthSecondColumn);
        textView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textView3.getText() != getString(R.string.translate)) {
                    copyText(textView2.getText());
                }
            }
        });
        tableLayout.addView(tableRow);
    }

    private void copyText(CharSequence text) {
        ClipboardManager clipboardManager = (ClipboardManager) MainActivity.super.getSystemService(MainActivity.super.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("", text);
        clipboardManager.setPrimaryClip(clipData);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private EditText messageIdEditText;
    private Spinner spinner_Delete;
    private RadioButton radioButton1_EditMenu;
    private RadioButton radioButton2_EditMenu;
    private MenuItem item;
    private Button categoryButton_Delete;

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (categorySpinner.isEnabled()) {
            int id = item.getItemId();
            if (id == R.id.search_check) {
                item.setChecked(!item.isChecked());
                globalSearch = item.isChecked();
                //return true;
            }
            if (id == R.id.add_list) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setPositiveButton(getString(R.string.add), null);
                    builder.setNegativeButton(getString(R.string.cancel), null);
                    builder.setTitle(getString(R.string.new_list));
                    View view = getLayoutInflater().inflate(R.layout.add_list, null);
                    final EditText editText_AddList = (EditText) view.findViewById(R.id.editText_AddList);
                    builder.setView(view);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(final DialogInterface dialog) {
                            Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (editText_AddList.getText().length() != 0) {
                                        if (db.rawQuery("SELECT * FROM lists WHERE title='" + editText_AddList.getText().toString().replace("'", "''") + "';", null).getCount() == 0) {
                                            try {
                                                db.execSQL("INSERT INTO lists VALUES (NULL,'" + editText_AddList.getText().toString().replace("'", "''") + "');");
                                                cursor = db.rawQuery("SELECT * FROM lists;", null);
                                                listSpinner.setAdapter(selectFromLists(cursor, (short) 0));
                                                for (int i = 0; i < listSpinner.getAdapter().getCount(); i++) {
                                                    if (new String(String.valueOf(listSpinner.getAdapter().getItem(i))).equals(String.valueOf(editText_AddList.getText().toString().replace("'", "''")))) {
                                                        listSpinner.setSelection(i);
                                                    }
                                                }
                                                Notice.showNoticeSnackbar(categorySpinner, getString(R.string.list_is_added));
                                                dialog.dismiss();
                                            } catch (Exception e) {
                                                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                                            }
                                        } else {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setMessage(getString(R.string.list_already_exists))
                                                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                        }
                                                    }).show();
                                        }
                                    } else {
                                        editText_AddList.setError(getString(R.string.field_is_empty));
                                    }
                                }
                            });
                        }
                    });
                    alertDialog.show();
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
                return true;
            }
            if (id == R.id.add_category) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setPositiveButton(getString(R.string.add), null);
                    builder.setNegativeButton(getString(R.string.cancel), null);
                    builder.setTitle(getString(R.string.new_category));
                    View view = getLayoutInflater().inflate(R.layout.add_category, null);
                    final EditText editText_AddCategory = (EditText) view.findViewById(R.id.editText_AddCategory);
                    builder.setView(view);
                    final AlertDialog alertDialog = builder.create();

                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(final DialogInterface dialog) {
                            Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (editText_AddCategory.getText().length() != 0) {
                                        if (db.rawQuery("SELECT * FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()]
                                                + "' AND key='" + editText_AddCategory.getText().toString().replace("'", "''") + "';", null).getCount() == 0) {
                                            try {
                                                db.execSQL("INSERT INTO keys VALUES (NULL,'" + editText_AddCategory.getText().toString().replace("'", "''") + "', '"
                                                        + idLists[listSpinner.getSelectedItemPosition()] + "');");
                                                Notice.showNoticeSnackbar(categorySpinner, getString(R.string.category_is_added));
                                                dialog.dismiss();
                                            } catch (Exception e) {
                                                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                                            }
                                        } else {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setMessage(getString(R.string.category_already_exists))
                                                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                        }
                                                    }).show();
                                        }
                                    } else {
                                        editText_AddCategory.setError(getString(R.string.field_is_empty));
                                    }
                                }
                            });
                        }
                    });
                    alertDialog.show();
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
                return true;
            }
            if (id == R.id.delete) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(getString(R.string.delete));
                    View view = getLayoutInflater().inflate(R.layout.delete, null);
                    messageIdEditText = (EditText) view.findViewById(R.id.messageIdEditText);
                    spinner_Delete = (Spinner) view.findViewById(R.id.spinner_Delete);
                    categoryButton_Delete = (Button) view.findViewById(R.id.categoryButton_Delete);
                    cursor = db.rawQuery("SELECT * FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()] + "' ORDER BY key;", null);
                    spinner_Delete.setAdapter(selectFromKeys(cursor, (short) 1, spinner_Delete));
                    builder.setView(view);
                    builder.setPositiveButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) { }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
                return true;
            }
            if (id == R.id.edit) {
                this.item = item;
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    View view = getLayoutInflater().inflate(R.layout.edit_menu, null);
                    radioButton1_EditMenu = (RadioButton) view.findViewById(R.id.radioButton1_EditMenu);
                    radioButton2_EditMenu = (RadioButton) view.findViewById(R.id.radioButton2_EditMenu);
                    builder.setView(view);
                    builder.setTitle(getString(R.string.edit));
                    builder.setPositiveButton(getString(R.string.to_continue), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (radioButton1_EditMenu.isChecked()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setPositiveButton(getString(R.string.apply), null);
                                builder.setNegativeButton(getString(R.string.cancel), null);
                                builder.setTitle(getString(R.string.message));
                                View view = getLayoutInflater().inflate(R.layout.edit_message, null);
                                final EditText id_EditMessage = (EditText) view.findViewById(R.id.id_EditMessage);
                                final EditText message_EditMessage = (EditText) view.findViewById(R.id.message_EditMessage);
                                final EditText translate_EditMessage = (EditText) view.findViewById(R.id.translate_EditMessage);
                                final ImageView search_EditMessage = (ImageView) view.findViewById(R.id.search_EditMessage);
                                search_EditMessage.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (id_EditMessage.getText().length() != 0) {
                                            try {
                                                cursor = db.rawQuery("SELECT messages.id, messages.message, messages.translate " +
                                                        "FROM messages INNER JOIN keys ON messages.id_key=keys.id WHERE keys.id_list='"
                                                        + idLists[listSpinner.getSelectedItemPosition()]
                                                        + "' AND messages.id='" + id_EditMessage.getText() + "';", null);
                                                if (cursor.moveToFirst()) {
                                                    int messageColIndex = cursor.getColumnIndex("message");
                                                    int translateColIndex = cursor.getColumnIndex("translate");
                                                    message_EditMessage.setText(cursor.getString(messageColIndex));
                                                    translate_EditMessage.setText(cursor.getString(translateColIndex));
                                                } else {
                                                    id_EditMessage.setError(getString(R.string.non_existent_id));
                                                }
                                            } catch (Exception e) {
                                                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                                            }
                                        } else {
                                            id_EditMessage.setError(getString(R.string.field_is_empty));
                                        }
                                    }
                                });
                                builder.setView(view);
                                final AlertDialog alertDialog = builder.create();

                                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(final DialogInterface dialog) {
                                        Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                        positive.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                if (id_EditMessage.getText().length() != 0) {
                                                    if (message_EditMessage.getText().length() != 0) {
                                                        if (translate_EditMessage.getText().length() != 0) {
                                                            try {
                                                                db.execSQL("UPDATE messages SET message='" + message_EditMessage.getText().toString().replace("'", "''")
                                                                        + "', translate='" + translate_EditMessage.getText().toString().replace("'", "''")
                                                                        + "' WHERE (messages.id_key IN (SELECT keys.id FROM keys WHERE (keys.id_list='"
                                                                        + idLists[listSpinner.getSelectedItemPosition()] + "'))) AND messages.id='" + id_EditMessage.getText() + "';");
                                                                Notice.showNoticeSnackbar(categorySpinner, getString(R.string.message_was_successfully_changed));
                                                            } catch (Exception e) {
                                                                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                                                            }
                                                            dialog.dismiss();
                                                        } else {
                                                            translate_EditMessage.setError(getString(R.string.field_is_empty));
                                                        }
                                                    } else {
                                                        message_EditMessage.setError(getString(R.string.field_is_empty));
                                                    }
                                                } else {
                                                    id_EditMessage.setError(getString(R.string.field_is_empty));
                                                }
                                            }
                                        });
                                        Button negative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                                        negative.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                dialog.dismiss();
                                                onOptionsItemSelected(item);
                                            }
                                        });
                                    }
                                });
                                alertDialog.show();
                            } else if (radioButton2_EditMenu.isChecked()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setPositiveButton(getString(R.string.apply), null);
                                builder.setNegativeButton(getString(R.string.cancel), null);
                                builder.setTitle(getString(R.string.category));
                                View view = getLayoutInflater().inflate(R.layout.edit_category, null);
                                final Spinner spinner_EditCategory = (Spinner) view.findViewById(R.id.spinner_EditCategory);
                                final EditText editText_EditCategory = (EditText) view.findViewById(R.id.editText_EditCategory);
                                cursor = db.rawQuery("SELECT * FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()] + "' ORDER BY key;", null);
                                spinner_EditCategory.setAdapter(selectFromKeys(cursor, (short) 1, spinner_EditCategory));
                                spinner_EditCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                        if (spinner_EditCategory.getSelectedItem() != ". . .") {
                                            editText_EditCategory.setText((CharSequence) spinner_EditCategory.getSelectedItem());
                                        } else {
                                            editText_EditCategory.setText("");
                                        }
                                    }
                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) { }
                                });
                                builder.setView(view);
                                final AlertDialog alertDialog = builder.create();

                                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(final DialogInterface dialog) {
                                        Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                        positive.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                if (spinner_EditCategory.getSelectedItemPosition() != 0) {
                                                    if (editText_EditCategory.getText().length() != 0) {
                                                        if (!new String(String.valueOf(spinner_EditCategory.getSelectedItem()))
                                                                .equals(String.valueOf(editText_EditCategory.getText()))) {
                                                            if (db.rawQuery("SELECT * FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()]
                                                                    + "' AND key='" + editText_EditCategory.getText().toString().replace("'", "''") + "';", null).getCount() == 0) {
                                                                try {
                                                                    db.execSQL("UPDATE keys SET key='" + editText_EditCategory.getText().toString().replace("'", "''")
                                                                            + "' WHERE key='" + spinner_EditCategory.getSelectedItem().toString().replace("'", "''") + "';");
                                                                    Notice.showNoticeSnackbar(categorySpinner, getString(R.string.category_was_successfully_changed));
                                                                    dialog.dismiss();
                                                                } catch (Exception e) {
                                                                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                                                                }
                                                            } else {
                                                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                                builder.setMessage(getString(R.string.category_already_exists))
                                                                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                                            @Override
                                                                            public void onClick(DialogInterface dialog, int which) {
                                                                            }
                                                                        }).show();
                                                            }
                                                        } else {
                                                            dialog.dismiss();
                                                            Notice.showNoticeSnackbar(categorySpinner, getString(R.string.category_was_successfully_changed));
                                                        }
                                                    } else {
                                                        editText_EditCategory.setError(getString(R.string.field_is_empty));
                                                    }
                                                } else {
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                    builder.setMessage(getString(R.string.select_category))
                                                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                }
                                                            }).show();
                                                }
                                            }
                                        });
                                        Button negative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                                        negative.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                dialog.dismiss();
                                                onOptionsItemSelected(item);
                                            }
                                        });
                                    }
                                });
                                alertDialog.show();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setPositiveButton(getString(R.string.apply), null);
                                builder.setNegativeButton(getString(R.string.cancel), null);
                                builder.setTitle(getString(R.string.list));
                                View view = getLayoutInflater().inflate(R.layout.edit_list, null);
                                final Spinner spinner_EditList = (Spinner) view.findViewById(R.id.spinner_EditList);
                                final EditText editText_EditList = (EditText) view.findViewById(R.id.editText_EditList);
                                cursor = db.rawQuery("SELECT * FROM lists;", null);
                                spinner_EditList.setAdapter(selectFromLists(cursor, (short) 1));
                                spinner_EditList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                        editText_EditList.setText((CharSequence) spinner_EditList.getSelectedItem());
                                    }
                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) { }
                                });
                                builder.setView(view);
                                final AlertDialog alertDialog = builder.create();

                                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(final DialogInterface dialog) {
                                        Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                        positive.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                if (editText_EditList.getText().length() != 0) {
                                                    if (!new String(String.valueOf(spinner_EditList.getSelectedItem()))
                                                            .equals(String.valueOf(editText_EditList.getText()))) {
                                                        if (db.rawQuery("SELECT * FROM lists WHERE title='"
                                                                + editText_EditList.getText().toString().replace("'", "''") + "';", null).getCount() == 0) {
                                                            try {
                                                                db.execSQL("UPDATE lists SET title='" + editText_EditList.getText().toString().replace("'", "''")
                                                                        + "' WHERE title='" + spinner_EditList.getSelectedItem().toString().replace("'", "''") + "';");
                                                                Notice.showNoticeSnackbar(categorySpinner, getString(R.string.list_was_successfully_changed));
                                                                cursor = db.rawQuery("SELECT * FROM lists;", null);
                                                                listSpinner.setAdapter(selectFromLists(cursor, (short) 0));
                                                                for (int i = 0; i < listSpinner.getAdapter().getCount(); i++) {
                                                                    if (new String(String.valueOf(listSpinner.getAdapter().getItem(i)))
                                                                            .equals(String.valueOf(editText_EditList.getText().toString().replace("'", "''")))) {
                                                                        listSpinner.setSelection(i);
                                                                    }
                                                                }
                                                                dialog.dismiss();
                                                            } catch (Exception e) {
                                                                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                                                            }
                                                        } else {
                                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                            builder.setMessage(getString(R.string.list_already_exists))
                                                                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                        }
                                                                    }).show();
                                                        }
                                                    } else {
                                                        Notice.showNoticeSnackbar(categorySpinner, getString(R.string.list_was_successfully_changed));
                                                    }
                                                } else {
                                                    editText_EditList.setError(getString(R.string.field_is_empty));
                                                }
                                            }
                                        });
                                        Button negative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                                        negative.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                dialog.dismiss();
                                                onOptionsItemSelected(item);
                                            }
                                        });
                                    }
                                });
                                alertDialog.show();
                            }
                        }
                    }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });;
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
                return true;
            }
            if (id == R.id.about) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(getString(R.string.reference));
                    View view = getLayoutInflater().inflate(R.layout.about, null);
                    final TextView textView = (TextView) view.findViewById(R.id.emailTextView_About);
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            copyText(getString(R.string.email));
                        }
                    });
                    builder.setView(view);
                    builder.setPositiveButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) { }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } catch (Exception e) {
                    Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                }
                return true;
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(getString(R.string.function_unavailable))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteAllMessages(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.delete_messages_current_list_Q))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            db.execSQL("DELETE FROM messages WHERE messages.id_key IN (SELECT keys.id FROM keys WHERE(keys.id_list='"
                                    + idLists[listSpinner.getSelectedItemPosition()] + "'));");
                            Notice.showNoticeSnackbar(view, getString(R.string.messages_deleted));
                        } catch (Exception e) {
                            Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                        }
                    }
                }).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    public void deleteList(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.delete_current_list_Q))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            db.execSQL("DELETE FROM lists WHERE id='" + idLists[listSpinner.getSelectedItemPosition()] + "';");
                            db.execSQL("DELETE FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()] + "';");
                            db.execSQL("DELETE FROM messages WHERE messages.id_key IN (SELECT keys.id FROM keys WHERE(keys.id_list='"
                                    + idLists[listSpinner.getSelectedItemPosition()] + "'));");
                            cursor = db.rawQuery("SELECT * FROM lists;", null);
                            listSpinner.setAdapter(selectFromLists(cursor, (short) 0));
                            Notice.showNoticeSnackbar(view, getString(R.string.list_deleted));
                        } catch (Exception e) {
                            Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                        }
                    }
                }).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    public void deleteMessageById(final View view) {
        if (messageIdEditText.getText().length() != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.delete_message_Q) + " (" + getString(R.string.id) + ":" + messageIdEditText.getText() + ")?")
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                db.execSQL("DELETE FROM messages WHERE (messages.id_key IN (SELECT keys.id FROM keys WHERE (keys.id_list='"
                                        + idLists[listSpinner.getSelectedItemPosition()] + "'))) AND messages.id='" + messageIdEditText.getText() + "';");
                                Notice.showNoticeSnackbar(view, getString(R.string.message_deleted));
                                messageIdEditText.setText("");
                            } catch (Exception e) {
                                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                            }
                        }
                    }).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
        } else {
            messageIdEditText.setError(getString(R.string.field_is_empty));
        }
    }

    public void deleteCategoryById(final View view) {
        if (spinner_Delete.getSelectedItemPosition() != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.delete_category_Q) + " (" + spinner_Delete.getSelectedItem() + ")?")
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                db.execSQL("DELETE FROM keys WHERE key='" + spinner_Delete.getSelectedItem() + "' AND id_list='"
                                        + idLists[listSpinner.getSelectedItemPosition()] + "';");
                                db.execSQL("DELETE FROM messages WHERE (messages.id_key IN (SELECT keys.id FROM keys WHERE (keys.id_list='" + idLists[listSpinner.getSelectedItemPosition()]
                                        + "' AND keys.key='" + spinner_Delete.getSelectedItem().toString().replace("'", "''") + "')));");
                                Notice.showNoticeSnackbar(view, getString(R.string.category_deleted));
                                cursor = db.rawQuery("SELECT * FROM keys WHERE id_list='" + idLists[listSpinner.getSelectedItemPosition()] + "' ORDER BY key;", null);
                                categorySpinner.setAdapter(selectFromKeys(cursor, (short) 1, categorySpinner));
                            } catch (Exception e) {
                                Notice.showNoticeToast(getApplicationContext(), e.getMessage());
                            }
                        }
                    }).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.select_category))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        db.close();
        cursor.close();
    }
}