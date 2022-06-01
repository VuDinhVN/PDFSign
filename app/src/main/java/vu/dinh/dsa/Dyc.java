package vu.dinh.dsa;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;


public class Dyc extends AppCompatActivity {

    private static final int OPEN_REQUEST_CODE = 123;
    private static final int OPEN_REQUEST_ENC = 234;


    private TextView mFileNameTextView;
    private TextView mFileNameTextCer;

    private Uri mFileUri;
    private Uri mFileUriEnc;

    private ProgressDialog mProgressDialog;

    private Button mSignButton;

    private EditText mReasonEditText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dyc);
        setTitle("Decrypt PDF");
        initView();


        final Activity a = this;

        /** Button to clean ink panel */

        findViewById(R.id.iv_browse_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, OPEN_REQUEST_CODE);
            }
        });

        findViewById(R.id.iv_browse_cer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, OPEN_REQUEST_ENC);
            }
        });

        mSignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFileUri == null) {
                    Toast.makeText(getApplicationContext(), "Vui lòng chọn file PDF!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Handler h = new Handler(Looper.getMainLooper());
                h.post(new Runnable() {
                    public void run() {
                        showProgressDialog("Đang Giải Mã File PDF");
                    }
                });
                sign(mReasonEditText.getText().toString());
            }
        });
    }



    private void viewPDF() {

        PDFView pdfView = (PDFView) findViewById(R.id.pdfView);
        pdfView.fromUri(mFileUri)
                .enableSwipe(true)
                .swipeHorizontal(true)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .password(null)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .spacing(0)
                .pageFitPolicy(FitPolicy.WIDTH)
                .load();

    }
    private void viewPDF2(File fos) {

        PDFView pdfView = (PDFView) findViewById(R.id.pdfView);
        pdfView.fromFile(fos)
                .enableSwipe(true)
                .swipeHorizontal(true)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .password(null)
                .scrollHandle(null)
                .enableAntialiasing(true)
                .spacing(0)
                .pageFitPolicy(FitPolicy.WIDTH)
                .load();

    }

    private void initView() {
        mSignButton = (Button) findViewById(R.id.button_sign);
        mFileNameTextView = (TextView) findViewById(R.id.tv_file_name);
        mFileNameTextCer = (TextView) findViewById(R.id.tv_file_name_cer);
        mProgressDialog = new ProgressDialog(this);
        mReasonEditText = (EditText) findViewById(R.id.et_reason);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OPEN_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    mFileUri = data.getData();
                    mFileNameTextView.setText(getFileName(mFileUri));
                    if(mFileUri != null){
                        viewPDF();
                    }
                } else {
                    Toast.makeText(this, "Đã huỷ chọn file!", Toast.LENGTH_SHORT).show();
                }
                break;
            case OPEN_REQUEST_ENC:
                if (resultCode == Activity.RESULT_OK) {
                    mFileUriEnc = data.getData();
                    mFileNameTextCer.setText(getFileName(mFileUriEnc));
                } else {
                    Toast.makeText(this, "Đã huỷ chọn file!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    /**
     * Get actual file name from uri
     * @param uri
     * @return actual file name
     */
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void sign(String s) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDialog.dismiss();
            }

            @SuppressLint("StaticFieldLeak")
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    BouncyCastleProvider provider = new BouncyCastleProvider();
                    Security.addProvider(provider);

                    String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +"/"+ getFileName(mFileUriEnc);
                    char [] pass = s.toCharArray();

                    KeyStore ks = KeyStore.getInstance("pkcs12", provider.getName());
                    ks.load(new FileInputStream(path), pass);
                    String aliases = (String)ks.aliases().nextElement();
                    Key pk = ks.getKey(aliases, pass);
                    Certificate ce = ks.getCertificate(aliases);

                    File tmp = File.createTempFile("eid", ".pdf", getCacheDir());
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "decrypt_" + getFileName(mFileUri));
                    FileOutputStream fos = new FileOutputStream(file);

                    String f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +"/"+ getFileName(mFileUri);

                    PdfReader reader = new PdfReader(f, ce, pk, provider.getName());
                    PdfStamper stamper = new PdfStamper(reader,fos);
                    stamper.close();
                    reader.close();
                    viewPDF2(file);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private void showProgressDialog(String message) {
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }
}


