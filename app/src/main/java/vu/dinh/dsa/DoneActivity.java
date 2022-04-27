package vu.dinh.dsa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

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

    public void DecryptData(File encryptedFileReceived, File decryptedFile, SecretKey secretKey)
            throws IOException, GeneralSecurityException {

        this.cipher = Cipher.getInstance("AES");
        decryptFile(dgetFileInBytes(encryptedFileReceived), decryptedFile, secretKey);

    }


    public void decryptFile(byte[] input, File output, SecretKey key)
            throws IOException, GeneralSecurityException {

        this.cipher.init(Cipher.DECRYPT_MODE, key);
        dwriteToFile(output, this.cipher.doFinal(input));

    }
    private void dwriteToFile(File output, byte[] toWrite)
            throws IllegalBlockSizeException, BadPaddingException, IOException{

        output.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(output);
        fos.write(toWrite);
        fos.flush();
        fos.close();

    }

    public byte[] dgetFileInBytes(File f) throws IOException{

        FileInputStream fis = new FileInputStream(f);
        byte[] fbytes = new byte[(int) f.length()];
        fis.read(fbytes);
        fis.close();
        return fbytes;

    }


    public void EncryptData(Uri originalFile, File encrypted)
            throws IOException, GeneralSecurityException{

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey key = keyGenerator.generateKey();

        this.cipher = Cipher.getInstance("AES");
        encryptFile(getFileInBytes(originalFile), encrypted, key);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ".pdf");
        DecryptData(encrypted, file, key);
    }

    public void encryptFile(byte[] input, File output, SecretKey key)
            throws IOException, GeneralSecurityException {



        this.cipher.init(Cipher.ENCRYPT_MODE, key);
        writeToFile(output, this.cipher.doFinal(input));
        Toast.makeText(getApplicationContext(), "Done Cipher!", Toast.LENGTH_SHORT).show();

    }

    private void writeToFile(File output, byte[] toWrite)
            throws IllegalBlockSizeException, BadPaddingException, IOException{
        Toast.makeText(getApplicationContext(), "Write!", Toast.LENGTH_SHORT).show();
        output.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(output);
        fos.write(toWrite);
        fos.flush();
        fos.close();
        Toast.makeText(getApplicationContext(), "Wite Done!", Toast.LENGTH_SHORT).show();
    }

    public byte[] getFileInBytes(Uri f) throws IOException{
        Toast.makeText(getApplicationContext(), "getFileInBytes!", Toast.LENGTH_SHORT).show();


        File field = new File(f.getPath());
        byte[] fbytes = new byte[(int) field.length()];
        Toast.makeText(getApplicationContext(), "getFileInBytes Done!", Toast.LENGTH_SHORT).show();
        return fbytes;

    }

    public static IvParameterSpec generateIv(){
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_done);
        setTitle("Digital Signature PDF");

        mSignedFileUri = Uri.fromFile(new File(getIntent().getStringExtra("uri")));

        findViewById(R.id.button_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSignedFileUri != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(mSignedFileUri);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "No PDF File!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });
        findViewById(R.id.button_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 Intent shareIntent = new Intent(Intent.ACTION_SEND);
                 shareIntent.setType("pdf/*");
                 shareIntent.putExtra(Intent.EXTRA_STREAM, mSignedFileUri);
                 startActivity(Intent.createChooser(shareIntent, "Share Document"));
            }
        });

        findViewById(R.id.button_enc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getApplicationContext(), "Start", Toast.LENGTH_SHORT).show();
                    FileInputStream inputStream = null;
// FileOutputStream outputStream = null;

                    inputStream = new FileInputStream(mSignedFileUri.getPath());
                    File field = new File(mSignedFileUri.getPath());

// outputStream = new FileOutputStream(mSignedFileUri.getPath()+"ENCRYPT.txt");
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ".txt");



                    EncryptData(mSignedFileUri, file);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}