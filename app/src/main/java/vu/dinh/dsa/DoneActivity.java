package vu.dinh.dsa;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.simplify.ink.InkView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DoneActivity extends AppCompatActivity {

    private Uri mSignedFileUri;

    private Cipher cipher;

    private TextView Notyfi;
    private TextView Notyfi2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_done);
        setTitle("PDFSign");
        initView();

        mSignedFileUri = Uri.fromFile(new File(getIntent().getStringExtra("uri")));
        Intent iin= getIntent();
        Bundle b = iin.getExtras();
        if(b!=null)
        {
            String j =(String) b.get("n");
            Notyfi.setText(j);
            Notyfi2.setText("File PDF đã được lưu lại và có thể gửi đi!");
        }
        viewPDF();
        findViewById(R.id.button_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 Intent shareIntent = new Intent(Intent.ACTION_SEND);
                 shareIntent.setType("pdf/*");
                 shareIntent.putExtra(Intent.EXTRA_STREAM, mSignedFileUri);
                 startActivity(Intent.createChooser(shareIntent, "Share Document"));

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void initView() {
        Notyfi = (TextView) findViewById(R.id.notify);
        Notyfi2 = (TextView) findViewById(R.id.notify2);

    }
    private void viewPDF(){
        PDFView pdfView = (PDFView) findViewById(R.id.pdfViews);
        pdfView.fromUri(mSignedFileUri)
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
}