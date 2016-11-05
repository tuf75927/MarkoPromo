package edu.temple.markopromo;

import android.content.Context;

import java.io.FileOutputStream;
import java.util.Date;

public class PromoMessage {
    private Date dateReceived;
    private String fileName;
    private Context context;

    //public PromoMessage() {
    //}

    public PromoMessage(String fileName, Context context) {
        this.dateReceived = new Date();
        dateReceived.setTime(System.currentTimeMillis());
        this.fileName = fileName;
        this.context = context;
    }

    public PromoMessage(Date dateReceived, String fileName, Context context) {
        this.dateReceived = dateReceived;
        this.fileName = fileName;
        this.context = context;
    }

    public Date getDateReceived() {
        return dateReceived;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean save() {
        String test = "testing 123";
        boolean success = false;

        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(test.getBytes());
            fos.close();
            success = true;
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            return success;
        }
    }

    public boolean delete(String fileName) {
        return context.deleteFile(fileName);
    }
}
