/* Marko Promo
 * Temple University
 * CIS 4398 Projects in Computer Science
 * Fall 2016
 *
 * mobile app by Robyn McCue
 *
 * PromoMessage.java
 *
 * A POJO helper class defining promo messages. Not used in final version.
 */

package edu.temple.markopromo;

import android.content.Context;

import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Date;

public class PromoMessage implements Serializable {
    private String filename;
    private String datetime;
    private String location;

    public PromoMessage() {
    }

    public PromoMessage(String filename, String datetime) {
        this.filename = filename;
        this.datetime = datetime;
        this.location = "Unknown Location";
    }

    public PromoMessage(String filename, String datetime, String location) {
        this.filename = filename;
        this.datetime = datetime;
        this.location = location;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) { this.datetime = datetime; }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }
}