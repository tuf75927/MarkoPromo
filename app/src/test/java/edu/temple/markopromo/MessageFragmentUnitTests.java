package edu.temple.markopromo;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageFragmentUnitTests {

    @Test
    public void isTextFile_true() {
        MessageFragment mf = new MessageFragment();

        boolean result = mf.isTextFile("text.txt");

        Assert.assertTrue(result);
    }

    @Test
    public void isTextFile_false() {
        MessageFragment mf = new MessageFragment();

        boolean result = mf.isTextFile("img.jpg");

        Assert.assertFalse(result);
    }

    @Test
    public void isTextFile_emptystring() {
        MessageFragment mf = new MessageFragment();

        boolean result = mf.isTextFile("");

        Assert.assertFalse(result);
    }

    @Test
    public void isTextFile_null() {
        MessageFragment mf = new MessageFragment();

        boolean result = mf.isTextFile(null);

        Assert.assertFalse(result);
    }

    @Test
    public void isImageFile_true() {
        MessageFragment mf = new MessageFragment();

        boolean result = mf.isImageFile("img.jpg");

        Assert.assertTrue(result);
    }

    @Test
    public void isImageFile_false() {
        MessageFragment mf = new MessageFragment();

        boolean result = mf.isImageFile("text.txt");

        Assert.assertFalse(result);
    }

    @Test
    public void isImageFile_emptystring() {
        MessageFragment mf = new MessageFragment();

        boolean result = mf.isImageFile("");

        Assert.assertFalse(result);
    }

    @Test
    public void isImageFile_null() {
        MessageFragment mf = new MessageFragment();

        boolean result = mf.isImageFile(null);

        Assert.assertFalse(result);
    }

    @Test
    public void getFileExtension_txt() {
        MessageFragment mf = new MessageFragment();

        String ext = mf.getFileExtension("text.txt");

        Assert.assertEquals("txt", ext);
    }

    @Test
    public void getFileExtension_jpg() {
        MessageFragment mf = new MessageFragment();

        String ext = mf.getFileExtension("img.jpg");

        Assert.assertEquals("jpg", ext);
    }

    @Test
    public void getFileExtension_noperiod() {
        MessageFragment mf = new MessageFragment();

        String ext = mf.getFileExtension("texttxt");

        Assert.assertEquals("", ext);
    }

    @Test
    public void getFileExtension_periodatend() {
        MessageFragment mf = new MessageFragment();

        String ext = mf.getFileExtension("text.");

        Assert.assertEquals("", ext);
    }

    @Test
    public void getFileExtension_periodatbeginning() {
        MessageFragment mf = new MessageFragment();

        String ext = mf.getFileExtension(".test");

        Assert.assertEquals("test", ext);
    }

}
