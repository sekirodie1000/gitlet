package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;
import static gitlet.Repository.*;

public class Blob implements Serializable {

    /**
     * For implementation of Blob objects, the initial idea was that it need to hold
     * File Content, File Name, Blob ID
     *
     */

    private byte[] content;
    private File fileName;
    private String ID;
    private String filePath; // We need this because of the Stage class
    private File blobSaveID;


    // Encapsulates the contents of a file and generates an ID for its contents
    public Blob(File fileName) {
        this.fileName = fileName;
        this.content = readContent();
        this.ID = generateID();
        this.filePath = fileName.getPath();
        this.blobSaveID = generatePath2ID();
    }

    private String generateID() {
        return sha1(content);
    }

    private File generatePath2ID() {
        return join(OBJECTS, ID);
    }

    public String getID() {
        return ID;
    }

    public String getFileName() {
        return fileName.getName();
    }

    public String getFilePath() {
        return filePath;
    }

    public byte[] readContent() {
        return readContents(fileName);
    }

    public byte[] getContent() {
        return content;
    }


    public void save() {
        // Similar to Commit. So we need blobSaveID.
        writeObject(blobSaveID, this);
    }



}
