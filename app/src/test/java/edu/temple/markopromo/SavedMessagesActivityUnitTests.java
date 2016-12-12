package edu.temple.markopromo;

import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SavedMessagesActivityUnitTests {

    @Test
    public void isNewPromo_emptyList() throws Exception {
        SavedMessagesActivity sma = new SavedMessagesActivity();
        sma.newMessageList = new ArrayList<String>();
        sma.savedMessageList = new ArrayList<String>();

        boolean result = sma.isNewPromo("test.txt");

        assertTrue(result);
    }

    @Test
    public void isNewPromo_true() throws Exception {
        SavedMessagesActivity sma = new SavedMessagesActivity();
        sma.newMessageList = new ArrayList<String>();
        sma.newMessageList.add("1.txt");
        sma.savedMessageList = new ArrayList<String>();
        sma.savedMessageList.add("2.txt");

        boolean result = sma.isNewPromo("3.txt");

        assertTrue(result);
    }

    @Test
    public void isNewPromo_false() throws Exception {
        SavedMessagesActivity sma = new SavedMessagesActivity();
        sma.newMessageList = new ArrayList<String>();
        sma.newMessageList.add("1.txt");
        sma.savedMessageList = new ArrayList<String>();
        sma.savedMessageList.add("2.txt");

        boolean result = sma.isNewPromo("1.txt");

        assertFalse(result);
    }
}