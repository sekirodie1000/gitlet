package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.Repository.*;

public class Stage implements Serializable {
    /**
     * For implementation of Stage, the first step is to consider what data structure we are using to store and manage the staged file
     * Using HashMap to Maintain the Mapping of File Paths to Blob IDs
     *
     * Why file paths, not the file name?
     * Example:
     *      File Name: a.txt
     *      File Path: src/a.txt; test/a.txt;
     * If we only use the file name, we couldn't distinguish between the two files in the src and test directories.
     * This involves a question: when generating an ID in the blob class, should both the file path and the file content be considered, rather than just the file content?
     * TODO: check if it need a modification.
     *
     *
     */

    private Map<String, String> filePath2ID = new HashMap<>();

    /**
     * What steps need to be completed by Add:
     * 1. Check if the added file is already in the stage
     *      - if the File Path already exists
     *      - if the blob id already exists
     *
     * 2. If not, then proceed to the subsequent operation
     * 3. Update the filePath2ID so that the file path is associated with the Blob's ID.
     * 4. Serialize an instance of the Stage class (i.e., the current stage state) and save it to ADDSTAGE
     *
     * In summary, stage class currently needs to fulfill the following functions:
     *      - isFilePathExists (boolean)    -DONE
     *      - isBlobIDExists (boolean)      -DONE
     *      - add(blob): update the filePath2ID     -DONE
     *      - saveAddStage(): save the current stage state to ADDSTAGE  -DONE
     */
    public void add(Blob blob) {
        filePath2ID.put(blob.getFilePath(), blob.getID());
        // So we need to implement getFilePath in Blob class    -Done
    }

    public void saveAddStage() {
        writeObject(ADDSTAGE, this);
    }

    public boolean isFilePathExists(Blob blob) {
        return filePath2ID.containsKey(blob.getFilePath());
    }

    public boolean isBlobIDExists(Blob blob) {
        return filePath2ID.containsValue(blob.getID());
    }

    public Map<String, String> getBlobMap() {
        return this.filePath2ID;
    }

    public void clear() {
        filePath2ID.clear();
    }

    public boolean isEmpty() {
        return getBlobMap().size() == 0;
    }

    /**
     * Ideas for implementation of REMOVESTAGE:
     * The basic logic is written in the rm section of Repository.
     *
     * Like ADDSTAGE, we need to implement:
     *      - remove(blob)      -DONE
     *      - saveRemoveStage()     -DONE
     */

    public void remove(Blob blob) {
        filePath2ID.remove(blob.getFilePath());
    }

    public void remove(String fileName) {
        String blobID = getBlobID(fileName);
        Blob blob = readBlob(blobID);
        remove(blob);
    }


    public void saveRemoveStage() {
        writeObject(REMOVESTAGE, this);
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
        List<String> fileNameList = getFileNamesList();
        return fileNameList.contains(fileName);
    }

    public String getBlobID(String fileName) {
        File file = join(CWD, fileName);
        String path = file.getPath();
        String blobID = filePath2ID.get(path);
        return blobID;
    }

    public static Blob readBlob(String ID) {
        File blobFile = join(OBJECTS, ID);
        return readObject(blobFile, Blob.class);
    }

    public byte[] getContent(String fileName) {
        File file = join(CWD, fileName);
        return readContents(file);
    }

}
