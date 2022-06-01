package vu.dinh.dsa;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;


public class Enc extends AppCompatActivity {

    private static final int OPEN_REQUEST_CODE = 123;
    private static final int OPEN_REQUEST_ENC = 234;


    private TextView mFileNameTextView;
    private TextView mFileNameTextCer;

    private Uri mFileUri;
    private Uri mFileUriEnc;


    private Button mEnc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enc);
        setTitle("Encrypt PDF");
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
        mEnc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFileUriEnc == null) {
                    Toast.makeText(getApplicationContext(), "Vui lòng chọn file Certificate!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Toast.makeText(getApplicationContext(), "Đang mã hoá File PDF!", Toast.LENGTH_SHORT).show();
                    enc();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @SuppressLint("LongLogTag")
    public Certificate getPublicCertificate(Uri path) throws IOException, CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(getContentResolver().openInputStream(mFileUriEnc));
        return  cert;
    }

    private void enc () throws Exception{
        Security.addProvider(new BouncyCastleProvider());
        Certificate cert = getPublicCertificate(mFileUriEnc);

        File tmp = File.createTempFile("eid", ".pdf", getCacheDir());
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "encrypt_" + getFileName(mFileUri));
        FileOutputStream fos = new FileOutputStream(file);

        PdfReader reader = new PdfReader(getContentResolver().openInputStream(mFileUri));
        PdfStamper stamper = new PdfStamper(reader, fos);
        stamper.setEncryption( new Certificate[]{cert}, new int[]{PdfWriter.ALLOW_PRINTING}, PdfWriter.ENCRYPTION_AES_128);
        stamper.close();
        reader.close();
        Intent i = new Intent(getApplicationContext(), DoneActivity.class);
        String f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/encrypt_" + getFileName(mFileUri);
        String n = "Đã mã hoá thành công file PDF!";
        i.putExtra("uri", f);
        i.putExtra("n", n);
        startActivity(i);
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
        mEnc = (Button) findViewById(R.id.button_enc);
        mFileNameTextView = (TextView) findViewById(R.id.tv_file_name);
        mFileNameTextCer = (TextView) findViewById(R.id.tv_file_name_cer);
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
}