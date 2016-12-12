package edu.temple.markopromo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class DisplayMessageActivity extends AppCompatActivity implements MessageFragment.OnFragmentInteractionListener{

    private String filename;
    private ArrayList<String> filelist;
    private WebView fileWebView;
    private ImageView fileImageView;
    private FrameLayout frameLayout;
    private Button deleteButton;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        filename = extras.getString("filename");
        Log.i("filename", filename);
        filelist = extras.getStringArrayList("filelist");
        Log.i("filelist.get(0)", filelist.get(0));

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new MessagePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(getFileIndex(filename));

        TextView filenameTextView = (TextView) findViewById(R.id.filename_textview);
        filenameTextView.setText(filename);

        frameLayout = (FrameLayout) findViewById(R.id.file_frame_layout);

        deleteButton = (Button) findViewById(R.id.delete_button);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deletePromo(filename);
            }
        });
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

    protected int getFileIndex(String filename) {
        int i = 0;

        for(String s : filelist) {
            if(s.equalsIgnoreCase(filename))
                return i;
            i++;
        }

        return i;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private class MessagePagerAdapter extends FragmentStatePagerAdapter {
        public MessagePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.i("Frag_position", ""+position);
            return MessageFragment.create(position, filelist.get(position));
        }

        @Override
        public int getCount() {
            return filelist.size();
        }
    }
}
