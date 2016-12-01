package edu.temple.markopromo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class DisplayMessageActivity extends AppCompatActivity {

    private String filename;
    private WebView fileWebView;
    private ImageView fileImageView;
    private FrameLayout frameLayout;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        filename = extras.getString("filename");

        TextView filenameTextView = (TextView) findViewById(R.id.filename_textview);
        filenameTextView.setText(filename);

        frameLayout = (FrameLayout) findViewById(R.id.file_frame_layout);

        if(isTextFile(filename)) {
            loadTextFile(filename);
        } else if (isImageFile(filename)) {
            loadImageFile(filename);
        } else {
            Toast.makeText(getApplicationContext(), "Unsupported file type!", Toast.LENGTH_SHORT).show();
        }

        deleteButton = (Button) findViewById(R.id.delete_button);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deletePromo(filename);
            }
        });
    }

    private boolean isTextFile(String filename) {
        String ext = getFileExtension(filename);

        if(ext.equals("txt")) {
            //Log.i("IS_TEXT_FILE", "TRUE");
            return true;
        }

        return false;
    }

    private boolean isImageFile(String filename) {
        String ext = getFileExtension(filename);

        if(ext.equals("jpg")) {
            //Log.i("IS_IMAGE_FILE", "TRUE");
            return true;
        }

        return false;
    }

    private String getFileExtension(String filename) {
        int periodLocation = 0;
        for(int i = 0; i < filename.length(); i++) {
            if (filename.charAt(i) == '.')
                periodLocation = i;
        }

        //Log.i("FILE_EXTENSION", filename.substring(periodLocation + 1, filename.length()));

        return filename.substring(periodLocation + 1, filename.length());
    }

    private String readTextFile(String filename) {
        File promoFile = new File(SavedMessagesActivity.directory, filename);

        String result = "";

        try(BufferedReader br = new BufferedReader(new FileReader(promoFile))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("<br>");
                line = br.readLine();
            }
            result = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(getApplicationContext(), "Error opening " + filename, Toast.LENGTH_SHORT).show();
        }

        return result;
    }

    private void loadTextFile(String filename) {
        fileWebView = new WebView(this);
        StringBuilder sb = new StringBuilder("<html><body><font size=\"6\">");
        sb.append(readTextFile(filename));
        sb.append("</font></body></html>");
        fileWebView.loadData(sb.toString(), "text/html", "UTF-8");

        frameLayout.addView(fileWebView);
    }

    private void loadImageFile(String filename) {
        fileImageView = new ImageView(this);

        File imgFile = new File(SavedMessagesActivity.directory, filename);

        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            fileImageView.setImageBitmap(myBitmap);
        }

        frameLayout.addView(fileImageView);
    }

    private void deletePromo(String filename) {
        File file = new File(SavedMessagesActivity.directory, filename);

        if(file.delete()) {
            Intent returnIntent = new Intent();
            setResult(SavedMessagesActivity.DELETE_RESULT, returnIntent);
            returnIntent.putExtra("fileToDelete", filename);
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Delete failed!", Toast.LENGTH_SHORT).show();
        }
    }
}
