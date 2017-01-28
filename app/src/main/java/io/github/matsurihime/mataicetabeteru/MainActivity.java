package io.github.matsurihime.mataicetabeteru;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.util.List;
import java.util.Map;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class MainActivity extends AppCompatActivity {
    Twitter mTwitter;
    String filePath = "";
    // メンバ変数
    Uri m_uri; // ファイルの場所を示すもの
    final int RESULT_PICK_IMAGEFILE = 1001;
    final int REQ_PERM = 101;
    final int SUCCESS = 200;
    final int EMPTY = 300;
    final int FILESIZE_EXCEED = 400;
    final int ERROR = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!TwitterUtils.hasAccessToken(this)) {
            Intent intent = new Intent(this, TwitterOAuthActivity.class);
            startActivity(intent);
            finish();
        } else {
            mTwitter = TwitterUtils.getTwitterInstance(this);
        }


        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest
                .Builder()
                .build();
        mAdView.loadAd(adRequest);


        findViewById(R.id.btnTweet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String iceName = ((EditText) findViewById(R.id.iceName)).getText().toString();
                String taste = ((EditText) findViewById(R.id.taste)).getText().toString();
                String company = ((EditText) findViewById(R.id.company)).getText().toString();
                tweet(iceName, taste, company);
            }
        });

    }

    private void tweet(final String iceName, final String taste, final String company) {
        new AsyncTask<Void, Void, Integer>() {
            Boolean isLimited;

            @Override
            protected void onPreExecute() {
                CheckBox chkLimited = (CheckBox) findViewById(R.id.checkLimited);
                isLimited = chkLimited.isChecked();
            }

            @Override
            protected Integer doInBackground(Void... voids) {
                try {
                    String tweetText;
                    if (company.isEmpty()) {
                        if (isLimited) {
                            tweetText = "またアイス食べてる：" + iceName + " 期間限定 " + taste;
                        } else {
                            tweetText = "またアイス食べてる：" + iceName + " " + taste;
                        }
                    } else {
                        if (isLimited) {
                            if (taste.isEmpty()) {
                                tweetText = "またアイス食べてる：" + iceName + " 期間限定（" + company + "）";
                            } else {
                                tweetText = "またアイス食べてる：" + iceName + " 期間限定 " + taste + "（" + company + "）";
                            }
                        } else {
                            if (taste.isEmpty()) {
                                tweetText = "またアイス食べてる：" + iceName + "(" + company + "）";
                            } else {
                                tweetText = "またアイス食べてる：" + iceName + " " + taste + "（" + company + "）";
                            }
                        }

                    }
                    if (!iceName.isEmpty()) {
                        StatusUpdate statusUpdate = new StatusUpdate(tweetText);
                        if (!filePath.isEmpty()) {
                            statusUpdate.setMedia(new File(filePath));
                        }
                        mTwitter.updateStatus(statusUpdate);
                        return SUCCESS;
                    } else {
                        return EMPTY;
                    }
                } catch (TwitterException e) {
                    e.printStackTrace();
                    switch (e.getStatusCode()) {
                        case 193:
                            return FILESIZE_EXCEED;
                        default:
                            return ERROR;
                    }
                }
            }

            @Override
            protected void onPostExecute(Integer result) {
                switch (result) {
                    case SUCCESS:
                        showToast("アイスを食べました");
                        finish();
                        break;
                    case FILESIZE_EXCEED:
                        showToast("画像は5MB以内にしてください");
                        break;
                    case EMPTY:
                        showToast("アイスの名前を入力してください");
                        break;
                    case ERROR:
                        showToast("アイスを食べられませんでした");
                        break;
                }
            }
        }.execute();
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQ_PERM) {
            if ((grantResults.length == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) ||
                    (grantResults.length == 2 && (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED))) {
                showToast("写真撮影をするためにはカメラとストレージの権限を許可してください");
            } else {
                showGallery();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == RESULT_PICK_IMAGEFILE && resultCode == Activity.RESULT_OK) {
            Uri uri = m_uri;

            if (resultData != null) {
                // ギャラリーからの時
                uri = resultData.getData();

                // pathの取得 apiによって違う
                if (Build.VERSION.SDK_INT < 19) {
                    ContentResolver cr = getContentResolver();
                    String[] columns = {MediaStore.Images.Media.DATA};
                    Cursor c = cr.query(uri, columns, null, null, null);
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        filePath = c.getString(0);
                        c.close();
                    }
                } else {
                    String strDocId = DocumentsContract.getDocumentId(uri);
                    String[] strSplittedDocId = strDocId.split(":");
                    String strId = strSplittedDocId[strSplittedDocId.length - 1];
                    Cursor crsCursor = getContentResolver().query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.MediaColumns.DATA},
                            "_id=?",
                            new String[]{strId},
                            null
                    );
                    if (crsCursor != null && crsCursor.getCount() > 0) {
                        crsCursor.moveToFirst();
                        filePath = crsCursor.getString(0);
                        crsCursor.close();
                    }
                }
            } else {
                // カメラの時、pathの取得 apiによって違う
                if (Build.VERSION.SDK_INT < 19) {
                    ContentResolver cr = getContentResolver();
                    String[] columns = {MediaStore.Images.Media.DATA};
                    Cursor c = cr.query(uri, columns, null, null, null);
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        filePath = c.getString(0);
                        c.close();
                    }
                } else {
                    final List<String> paths = uri.getPathSegments();
                    String strId = paths.get(3);
                    Cursor crsCursor = getContentResolver().query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.MediaColumns.DATA},
                            "_id=?",
                            new String[]{strId},
                            null
                    );
                    if (crsCursor != null && crsCursor.getCount() > 0) {
                        crsCursor.moveToFirst();
                        filePath = crsCursor.getString(0);
                        crsCursor.close();
                    }
                }
            }
            ImageView imageView = (ImageView) findViewById(R.id.imageView2);
            imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            imageView.setImageURI(Uri.parse(filePath));
        } else {
            IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, resultData);
            if (intentResult != null) {
                final String content = intentResult.getContents();
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
                new AsyncTask<String, Void, Map<String, String>>() {
                    @Override
                    protected Map<String, String> doInBackground(String... params) {
                        try {
                            return YahooAPI.requestYahooAPI(content);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Map<String, String> map) {
                        makeDialog(map);
                    }
                }.execute();
            }
        }
    }

    AlertDialog.Builder listDlg;

    private void makeDialog(final Map<String, String> map) {
        if (map == null || map.get("name") == null || map.get("name").isEmpty()) {
            Toast.makeText(MainActivity.this, "よくわからないアイスだよ", Toast.LENGTH_SHORT);
        } else {
            String[] first = map.get("name").split(" |　");
            final String[] firstTemp = new String[first.length + 1];
            for (int i = 0; i < first.length + 1; i++) {
                if (i < first.length) {
                    firstTemp[i] = first[i];
                } else {
                    firstTemp[i] = "一覧にない";
                }
            }

            listDlg = new AlertDialog.Builder(this);

            listDlg.setTitle("アイスのなまえを選んでね")
                    .setItems(
                            firstTemp,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    EditText txtName = (EditText) findViewById(R.id.iceName);
                                    if (which == firstTemp.length - 1) {
                                        //一覧にないが選択された場合は
                                        txtName.setText("");
                                    } else {
                                        txtName.setText(firstTemp[which]);
                                    }
                                }
                            });
            listDlg.create().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // メニューの要素を追加して取得
        MenuItem actionItem = menu.add("camera");
        MenuItem actionItem2 = menu.add("barcode");
        // アイコンを設定
        actionItem.setIcon(R.drawable.camera);
        actionItem2.setIcon(R.drawable.barcode_button);

        // SHOW_AS_ACTION_ALWAYS:常に表示
        actionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        actionItem2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ("camera".equals(item.getTitle())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permCount = 0;
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permCount += 1;
                }
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    permCount += 10;
                }
                String[] permissions;
                switch (permCount) {
                    case 1:
                        permissions = new String[1];
                        permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        requestPermissions(permissions, REQ_PERM);
                        break;
                    case 10:
                        permissions = new String[1];
                        permissions[0] = Manifest.permission.CAMERA;
                        requestPermissions(permissions, REQ_PERM);
                        break;
                    case 11:
                        permissions = new String[2];
                        permissions[0] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        permissions[1] = Manifest.permission.CAMERA;
                        requestPermissions(permissions, REQ_PERM);
                        break;
                    default:
                        showGallery();
                }

            } else {
                showGallery();
            }
        } else {
            new IntentIntegrator(MainActivity.this).initiateScan();
        }
        return true;
    }

    private void showGallery() {
        String photoName = DateFormat.format("P_yyyyMMdd_kkmmss", System.currentTimeMillis()).toString() + ".jpg";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, photoName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        m_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, m_uri);
        startActivityForResult(intentCamera, RESULT_PICK_IMAGEFILE);
    }
}
