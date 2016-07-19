package com.test.kai.sendmailauto;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Created by kai-chuan on 7/19/16.
 */
public class TaskManager {
    final private String TAG = "TaskManager";
    final private String TASK_DATA_XML = "taskdata.xml";
    final private int INPUT_BUFFER_SIZE = 2048;
    private Context mContext;
    private ArrayList<TaskConfiguration> taskConfigurations;

    public TaskManager(Context context) {
        mContext = context;
        taskConfigurations = new ArrayList<TaskConfiguration>();
        syncTaskConfigurations();
    }

    public ArrayList<TaskConfiguration> getTaskConfigurations() {
        return taskConfigurations;
    }

    private void syncTaskConfigurations() {
        String xmldata = getXMLData();
        TaskConfiguration taskConfiguration = null;
        boolean inTask = false;
        String textValue = null;
        if (xmldata!=null) {
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(xmldata));
                int eventType = xpp.getEventType();
                while (eventType!=XmlPullParser.END_DOCUMENT) {
                    String tagName = xpp.getName();
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            Log.d(TAG, "starting tag for " + tagName);
                            if (tagName.equalsIgnoreCase("task")) {
                                taskConfiguration = new TaskConfiguration();
                                inTask = true;
                            }
                            break;
                        case XmlPullParser.TEXT:
                            textValue = xpp.getText();
                            break;
                        case XmlPullParser.END_TAG:
                            if (inTask) {
                                Log.d(TAG, "ending tag for " + tagName);
                                if (tagName.equalsIgnoreCase("username")) {
                                    taskConfiguration.setUsername(textValue);
                                }
                                if (tagName.equalsIgnoreCase("password")) {
                                    taskConfiguration.setPassword(textValue);
                                }
                                if (tagName.equalsIgnoreCase("recipient")) {
                                    taskConfiguration.setRecipient(textValue);
                                }
                                if (tagName.equalsIgnoreCase("subject")) {
                                    taskConfiguration.setSubject(textValue);
                                }
                                if (tagName.equalsIgnoreCase("message")) {
                                    taskConfiguration.setMessage(textValue);
                                }
                                if (tagName.equalsIgnoreCase("year")) {
                                    taskConfiguration.setYear(Integer.parseInt(textValue));
                                }
                                if (tagName.equalsIgnoreCase("month")) {
                                    taskConfiguration.setMonth(Integer.parseInt(textValue));
                                }
                                if (tagName.equalsIgnoreCase("day")) {
                                    taskConfiguration.setDay(Integer.parseInt(textValue));
                                }
                                if (tagName.equalsIgnoreCase("hour")) {
                                    taskConfiguration.setHour(Integer.parseInt(textValue));
                                }
                                if (tagName.equalsIgnoreCase("minute")) {
                                    taskConfiguration.setMinute(Integer.parseInt(textValue));
                                }
                                if (tagName.equalsIgnoreCase("task")) {
                                    inTask = false;
                                    taskConfigurations.add(taskConfiguration);
                                    taskConfiguration = null;
                                }
                            }
                            break;
                        default:
                            // Nothing to do
                    }
                    eventType = xpp.next();
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getXMLData() {
        String xmldata = null;
        if (isExternalStorageReadable()) {
            File file = new File(mContext.getExternalFilesDir(null), TASK_DATA_XML);
            try {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis);
                // READ STRING OF UNKNOWN LENGTH
                StringBuilder sb = new StringBuilder();
                char[] inputBuffer = new char[INPUT_BUFFER_SIZE];
                int byteRead = -1;
                // FILL BUFFER WITH DATA
                while ((byteRead = isr.read(inputBuffer)) != -1) {
                    sb.append(inputBuffer, 0, byteRead);
                }
                // CONVERT BYTES TO STRING
                xmldata = sb.toString();
                fis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "external storage is not readable");
        }
        return xmldata;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public TaskConfiguration getTaskConfiguration(int year, int month, int day,
                                                  int hour, int minute, String username,
                                                  String password, String recipient,
                                                  String subject, String message) {
        TaskConfiguration taskConfiguration = new TaskConfiguration();
        taskConfiguration.setYear(year);
        taskConfiguration.setMonth(month);
        taskConfiguration.setDay(day);
        taskConfiguration.setHour(hour);
        taskConfiguration.setMinute(minute);
        taskConfiguration.setUsername(username);
        taskConfiguration.setPassword(password);
        taskConfiguration.setRecipient(recipient);
        taskConfiguration.setSubject(subject);
        taskConfiguration.setMessage(message);
        return taskConfiguration;
    }

    public void addTaskConfiguration(TaskConfiguration taskConfiguration) {
        taskConfigurations.add(taskConfiguration);
    }

    public void saveTaskConfigurations() {
        if (isExternalStorageWritable()) {
            File file = new File(mContext.getExternalFilesDir(null), TASK_DATA_XML);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                XmlSerializer xmlSerializer = Xml.newSerializer();
                StringWriter writer = new StringWriter();
                xmlSerializer.setOutput(writer);
                xmlSerializer.startDocument("UTF-8", true);
                for (TaskConfiguration taskConfiguration : taskConfigurations) {
                    xmlSerializer.startTag("", "task");
                    xmlSerializer.startTag("", "username");
                    xmlSerializer.text(taskConfiguration.getUsername());
                    xmlSerializer.endTag("", "username");
                    xmlSerializer.startTag("", "password");
                    xmlSerializer.text(taskConfiguration.getPassword());
                    xmlSerializer.endTag("", "password");
                    xmlSerializer.startTag("", "recipient");
                    xmlSerializer.text(taskConfiguration.getRecipient());
                    xmlSerializer.endTag("", "recipient");
                    xmlSerializer.startTag("", "subject");
                    xmlSerializer.text(taskConfiguration.getSubject());
                    xmlSerializer.endTag("", "subject");
                    xmlSerializer.startTag("", "message");
                    xmlSerializer.text(taskConfiguration.getMessage());
                    xmlSerializer.endTag("", "message");
                    xmlSerializer.startTag("", "year");
                    xmlSerializer.text(Integer.toString(taskConfiguration.getYear()));
                    xmlSerializer.endTag("", "year");
                    xmlSerializer.startTag("", "month");
                    xmlSerializer.text(Integer.toString(taskConfiguration.getMonth()));
                    xmlSerializer.endTag("", "month");
                    xmlSerializer.startTag("", "day");
                    xmlSerializer.text(Integer.toString(taskConfiguration.getDay()));
                    xmlSerializer.endTag("", "day");
                    xmlSerializer.startTag("", "hour");
                    xmlSerializer.text(Integer.toString(taskConfiguration.getHour()));
                    xmlSerializer.endTag("", "hour");
                    xmlSerializer.startTag("", "minute");
                    xmlSerializer.text(Integer.toString(taskConfiguration.getMinute()));
                    xmlSerializer.endTag("", "minute");
                    xmlSerializer.endTag("", "task");
                }
                xmlSerializer.endDocument();
                xmlSerializer.flush();
                String dataWrite = writer.toString();
                fos.write(dataWrite.getBytes());
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "external storage is not readable");
        }
    }

    public void removeTaskConfiguration(int index) {
        taskConfigurations.remove(index);
    }
}
