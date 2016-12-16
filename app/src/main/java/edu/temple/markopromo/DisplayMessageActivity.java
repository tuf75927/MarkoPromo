/* Marko Promo
 * Temple University
 * CIS 4398 Projects in Computer Science
 * Fall 2016
 *
 * mobile app by Robyn McCue
 *
 * DisplayMessageActivity.java
 *
 * The secondary page of the Marko Promo mobile application, displaying received and downloaded promos.
 */

package edu.temple.markopromo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class DisplayMessageActivity extends AppCompatActivity implements MessageFragment.OnFragmentInteractionListener {

    private String filename;
    private ArrayList<String> filelist;
    private WebView fileWebView;
    private ImageView fileImageView;
    private FrameLayout frameLayout;
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

        frameLayout = (FrameLayout) findViewById(R.id.file_frame_layout);
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
    public void deleteFilename(String deleteFilename) {
        deletePromo(deleteFilename);
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