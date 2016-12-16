/* Marko Promo
 * Temple University
 * CIS 4398 Projects in Computer Science
 * Fall 2016
 *
 * mobile app by Robyn McCue
 *
 * MessageFragment.java
 *
 * A fragment that contains one saved promo message (text or image). Allows for side scrolling between
 * saved messages.
 */

package edu.temple.markopromo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class MessageFragment extends Fragment {
    private static final String ARG_FILENAME = "filename";
    private static final String ARG_PARAM2 = "param2";

    public static final String ARG_PAGE = "page";

    private String filename;
    private String mParam2;

    private WebView fileWebView;
    private ImageView fileImageView;
    private FrameLayout frameLayout;
    private Button deleteButton;

    private OnFragmentInteractionListener mListener;

    public MessageFragment() {
        // Required empty public constructor
    }

    public static MessageFragment create(int pageNumber, String filename) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        args.putString(ARG_FILENAME, filename);
        fragment.setArguments(args);
        return fragment;
    }

    public static MessageFragment newInstance(String param1, String param2) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILENAME, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filename = getArguments().getString(ARG_FILENAME);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup vg = (ViewGroup) inflater.inflate(R.layout.fragment_message, container, false);

        frameLayout = (FrameLayout) vg.findViewById(R.id.message_frag_layout);

        if(isTextFile(filename)) {
            loadTextFile(filename);
        } else if (isImageFile(filename)) {
            loadImageFile(filename);
        } else {
            Toast.makeText(this.getContext(), "Unsupported file type!", Toast.LENGTH_SHORT).show();
        }

        deleteButton = (Button) vg.findViewById(R.id.delete_button);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DisplayMessageActivity act = (DisplayMessageActivity) getActivity();
                act.deleteFilename(filename);
            }
        });

        return vg;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void deleteFilename(String newFilename);
    }

    protected boolean isTextFile(String filename) {
        String ext = getFileExtension(filename);

        if(ext.equalsIgnoreCase("txt")) {
            //Log.i("IS_TEXT_FILE", "TRUE");
            return true;
        }

        return false;
    }

    protected boolean isImageFile(String filename) {
        String ext = getFileExtension(filename);

        if(ext.equalsIgnoreCase("jpg")) {
            //Log.i("IS_IMAGE_FILE", "TRUE");
            return true;
        }

        return false;
    }

    protected String getFileExtension(String filename) {
        if(filename == null || filename == "")
            return "";

        boolean notFound = true;

        int periodLocation = 0;
        for(int i = 0; i < filename.length(); i++) {
            if (filename.charAt(i) == '.') {
                periodLocation = i;
                notFound = false;
            }
        }

        if(notFound)
            return "";

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

            Toast.makeText(this.getContext(), "Error opening " + filename, Toast.LENGTH_SHORT).show();
        }

        return result;
    }

    private void loadTextFile(String filename) {
        fileWebView = new WebView(this.getContext());
        StringBuilder sb = new StringBuilder("<html><body><font size=\"6\">");
        sb.append(readTextFile(filename));
        sb.append("</font></body></html>");
        fileWebView.loadData(sb.toString(), "text/html", "UTF-8");

        frameLayout.addView(fileWebView);
    }

    private void loadImageFile(String filename) {
        fileImageView = new ImageView(this.getContext());

        File imgFile = new File(SavedMessagesActivity.directory, filename);

        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            fileImageView.setImageBitmap(myBitmap);
        }

        frameLayout.addView(fileImageView);
    }
}
