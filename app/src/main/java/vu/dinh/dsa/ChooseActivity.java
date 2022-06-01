package vu.dinh.dsa;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import com.simplify.ink.InkView;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;

public class ChooseActivity extends AppCompatActivity {

    private static final int OPEN_REQUEST_CODE = 123;
    private static final int OPEN_REQUEST_ENC = 234;

    private InkView ink;

    private TextView mFileNameTextView;


    private Uri mFileUri;

    private EditText mReasonEditText;

    private EditText mLocationEditText;

    private ProgressDialog mProgressDialog;

    private Button mSignButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Sign PDF");
        initView();


        final Activity a = this;

        /** Button to clean ink panel */
        findViewById(R.id.iv_clean).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ink.clear();
            }
        });


        findViewById(R.id.iv_browse_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, OPEN_REQUEST_CODE);
            }
        });


        mSignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFileUri == null) {
                    Toast.makeText(getApplicationContext(), "Vui lòng chọn file PDF!", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveImage();
                KeyChain.choosePrivateKeyAlias(a,
                        new KeyChainAliasCallback() {
                            public void alias(String alias) {
                                if (alias != null) {
                                    Handler h = new Handler(Looper.getMainLooper());
                                    h.post(new Runnable() {
                                        public void run() {
                                            showProgressDialog("Đang ký File PDF");
                                        }
                                    });
                                    sign(alias, mReasonEditText.getText().toString(), mLocationEditText.getText().toString());
                                }
                            }
                        }, null, null, null, -1, null);
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

    private void initView() {
        ink = (InkView) findViewById(R.id.ink);
        mSignButton = (Button) findViewById(R.id.button_sign);
        mFileNameTextView = (TextView) findViewById(R.id.tv_file_name);
        mReasonEditText = (EditText) findViewById(R.id.et_reason);
        mLocationEditText = (EditText) findViewById(R.id.et_location);
        mProgressDialog = new ProgressDialog(this);

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

    private void saveImage() {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/image.png");
            ink.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sign PDF.
     * @param alias to get privateKey
     * @param reason
     * @param location
     */
    private void sign(final String alias, final String reason, final String location) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mProgressDialog.dismiss();
                Intent i = new Intent(getApplicationContext(), DoneActivity.class);

                String f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/signed_" + getFileName(mFileUri);
                String n = "Đã ký thành công file PDF!";
                i.putExtra("uri", f);
                i.putExtra("n", n);
                startActivity(i);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            protected Void doInBackground(Void... params) {
                PrivateKey privateKey = null;
                try {

                    privateKey = KeyChain.getPrivateKey(getApplicationContext(), alias);


                    KeyFactory keyFactory =
                            KeyFactory.getInstance(privateKey.getAlgorithm(), "AndroidKeyStore");


                    Certificate[] chain = KeyChain.getCertificateChain(getApplicationContext(), alias);

                    BouncyCastleProvider provider = new BouncyCastleProvider();
                    Security.addProvider(provider);

                    ExternalSignature pks = new CustomPrivateKeySignature(privateKey, DigestAlgorithms.SHA256, provider.getName());

                    File tmp = File.createTempFile("eid", ".pdf", getCacheDir());
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "signed_" + getFileName(mFileUri));
                    FileOutputStream fos = new FileOutputStream(file);

                    sign(mFileUri, fos, chain, pks, DigestAlgorithms.SHA256, provider.getName(), CryptoStandard.CADES, reason, location);

                } catch (KeyChainException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
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


    public void sign(Uri uri, FileOutputStream os,
                     Certificate[] chain,
                     ExternalSignature pk, String digestAlgorithm, String provider,
                     CryptoStandard subfilter,
                     String reason, String location)
            throws GeneralSecurityException, IOException, DocumentException {
        PdfReader reader = new PdfReader(getContentResolver().openInputStream(uri));
        PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');

        PdfSignatureAppearance appearance = stamper.getSignatureAppearance();

        appearance.setReason(reason);
        appearance.setLocation(location);
        appearance.setVisibleSignature(new Rectangle(100, 0, 500, 400), 1, "sig");
        appearance.setImage(Image.getInstance(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/image.png"));

        appearance.setImageScale(-1);
        ExternalDigest digest = new BouncyCastleDigest();
        CustomMakeSignature.signDetached(appearance, digest, pk, chain, null, null, null, 0, subfilter);
    }

    private void showProgressDialog(String message) {
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }
}