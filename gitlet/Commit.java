package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.List;
import java.util.*;
import static gitlet.Utils.*;
import static gitlet.Repository.*;
import java.text.DateFormat;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    /* TODO: fill in the rest of this class. */

    /**
     * For the implementation of Commit, first identify which instance variables we need
     *      - Message: commit message
     *      - Parents: list of parent submission IDs
     *      - CurrTime: commit time
     *      - timeStamp: formatted timeStamp "EEE MMM d HH:mm:ss yyyy Z"
     *      - filePath2ID: mapping of file path to blob ID
     *
     *      - ID: commit ID
     *      - commitSaveID: generate a path to a file in the OBJECTS folder that holds the current Commit instance
     *          - After completing the commit operation, the commit object needs to be stored in the OBJECTS folder.
     *          - Therefore, we require commitSaveID to ensure that the current instance can be saved into the OBJECTS folder.
     *
     */
    private List<String> parents;
    private Date currTime;
    private String ID;
    private Map<String, String> filePath2ID = new HashMap<>();
    private String timeStamp;
    private File commitSaveID;

    public Commit(String message, List<String> parents, Map<String, String> filePath2ID) {
        this.message = message;
        this.parents = parents;
        this.currTime = new Date();
        this.ID = generateID();
        this.filePath2ID = filePath2ID;
        this.timeStamp = standardizeTime(this.currTime);
        this.commitSaveID = generatePath2Folder();
    }

    // Initialize Commit
    public Commit() {
        this.message = "initial commit";
        this.parents = new ArrayList<>();
        this.currTime = new Date(0);
        this.ID = generateID();
        this.filePath2ID = new HashMap<>();
        this.timeStamp = standardizeTime(this.currTime);
        this.commitSaveID = generatePath2Folder(); // Ensure the correct file paths for subsequent data writes
    }


    // The input variable to the sha1() function must be of type String
    private String generateID() {
        return sha1(standardizeTime(currTime), message, parents.toString(), filePath2ID.toString());
    }

    // Generate standard format timestamps
    private static String standardizeTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.UK);
        return dateFormat.format(date);
    }

    private File generatePath2Folder() {
        return join(OBJECTS, ID);
    }

    // Serializes and physically writes the current Commit object to the file system at the file path specified by commitSaveFileName.
    public void save() {
        writeObject(commitSaveID, this);
    }

    public String getID() {
        return ID;
    }

    public Map<String, String> getFilePath2ID() {
        return filePath2ID;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getLogMessage() {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("===\n");
        logMessage.append("commit ").append(ID).append("\n");

        if (parents.size() > 1) {
            logMessage.append("Merge: ");
            for (int i = 0; i < parents.size(); i++) {
                String parentID = parents.get(i);
                logMessage.append(parentID.substring(0, 7));
                if (i < parents.size() - 1) {
                    logMessage.append(" ");
                }
            }
            logMessage.append("\n");
        }

        logMessage.append("Date: ").append(timeStamp).append("\n");
        logMessage.append(message).append("\n");

        return logMessage.toString();
    }

    public List<String> getParents() {
        return parents;
    }

    public List<String> getFileNamesList() {
        List<String> fileNamesList = new ArrayList<>();
        List<Blob> bloblist = getBlobList();
        for (Blob b : bloblist) {
            String fileName = b.getFileName();
            fileNamesList.add(fileName);
        }
        return fileNamesList;
    }

    public List<Blob> getBlobList() {
        List<Blob> blobList = new ArrayList<>();
        for (String blobID : filePath2ID.values()) {
            Blob blob = readBlob(blobID);
            blobList.add(blob);
        }
        return blobList;
    }

    public boolean containsFile(String fileName) {
        // In gitlet, only one file may be added at a time.
        // The original idea was to iterate through the blob IDs in filePath2ID, obtain the blob objects and get fileNames, and then check if these fileNames include the fileName I am looking for.
        // One nice way is to create a list to maintain fileName.
        List<String> fileNameList = getFileNamesList();
        return fileNameList.contains(fileName);
    }

    public String getBlobID(String fileName) {
        File file = join(CWD, fileName);
        String path = file.getPath();
        String blobID = filePath2ID.get(path);
        return blobID;
    }

    public Blob getBlob(String fileName) {
        String blobID = getBlobID(fileName);
        Blob blob = readBlob(blobID);
        return blob;
    }

    public byte[] getContent(String fileName) {
        File file = join(CWD, fileName);
        return readContents(file);
    }

    public Blob getBlobByFilePath(String filePath) {
        String blobID = filePath2ID.get(filePath);
        if (blobID != null) {
            return Repository.readBlob(blobID);
        }
        return null;
    }






}