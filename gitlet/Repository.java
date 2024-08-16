package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.util.*;
import static gitlet.Utils.*;
import static gitlet.Commit.*;
import static gitlet.Stage.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File OBJECTS = join(GITLET_DIR, "objects");
    public static final File REFS = join(GITLET_DIR, "refs");
    public static final File ALLHEADS = join(REFS, "heads");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File ADDSTAGE = join(GITLET_DIR, "addStage");
    public static final File REMOVESTAGE = join(GITLET_DIR, "removeStage");

    public static Commit currCommit;


    // If a user inputs a command that requires being in an initialized Gitlet working directory (i.e., one containing a .gitlet subdirectory),
    // but is not in such a directory, print the message "Not in an initialized Gitlet directory."
    public static void checkIfInitialized() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }


    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }

        GITLET_DIR.mkdir();
        OBJECTS.mkdir();
        REFS.mkdir();
        ALLHEADS.mkdir();

        // Initialize Commit object;
        initCommit();

        // Create a reference to the master branch, set it to point to the ID of the initial commit
        initALLHEADS();

        // Initialize HEAD, set current HEAD to point to master
        initHEAD();



    }

    private static void initHEAD() {
        writeContents(HEAD, "master");
    }

    private static void initALLHEADS() {
        File headsFile = join(ALLHEADS, "master");
        writeContents(headsFile, currCommit.getID());
    }

    private static void initCommit() {
        Commit initCommit = new Commit();
        currCommit = initCommit;
        initCommit.save();
    }

    /**
     * Ideas for implementation of ADD function:
     * 1. If file exists:
     *      - If not exist, print the error message "File does not exist." and exits.
     *      - If exists
     *          - Is the file path absolute
     *              - If absolute, use the file path directly
     *              - If not, parse it based on CWD
     * 2. Create Blob object based on existing file
     * 3. Store Blob based on the logic written in the Stage class add section
     *
     */

    public static void add(String file) {
        File fileName = getFile(file);
        if (!fileName.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        Blob blob = new Blob(fileName);
        // previous: Blob(content, fileName)
        // The issue here: How should the objects accepted by the blob be configured?
        // Clearly, it is easy for implementation to only accept fileName.
        // Therefore, the content needs to be modified within the blob class, thus allowing it to be retrieved
        // Change Blob Class    -DONE
        storeBlob(blob);

    }

    /**
     * For specific implementation of storeBlob, we need to read the serialized staging area object from ADDSTAGE.
     * Initial version for add function here is correct based on the test.
     *
     * Issue 1:
     * If file in removeStage, it needs to be removed and not add to addStage.
     *
     * Issue 2:
     * If file in currCommit, not add
     *
     * Rewrite ADD logic:
     * 1. Check if blobID not in currCommit || blobID in removeStage
     * 2. Check if blobID not in addStage
     * 3. Check if blobID not in removeStage
     *      - Save blob
     *      - Check if addStage isFilePathExists(blob)
     *          - addStage remove(blob)
     *      - addStage add(blob)
     *      - saveAddStage
     * 4. If blobID in removeStage
     *      - removeStage remove(blob)
     *      - saveRemoveStage
     *
     */
    private static void storeBlob(Blob blob) {
        // Stage addStage = readObject(ADDSTAGE, Stage.class);
        // ADDSTAGE is not initialized, which can lead to a 'No such file or directory' error.
        // Therefore, we need to first check if it exists, and if not, create a new Stage instance.
        Stage addStage = readAddStage();
        Stage removeStage = readRemoveStage();
        currCommit = readCurrCommit();
        List<Blob> currCommitBlobList = currCommit.getBlobList();

        if (!currCommitBlobList.contains(blob) || removeStage.isBlobIDExists(blob)) {
            if (!addStage.isBlobIDExists(blob)) {
                if (!removeStage.isBlobIDExists(blob)) {
                    blob.save();
                    if (addStage.isFilePathExists(blob)) {
                        addStage.remove(blob);
                    }
                    addStage.add(blob);
                    addStage.saveAddStage();
                } else {
                    removeStage.remove(blob);
                    removeStage.saveRemoveStage();
                }
            }
        }
    }

    static Stage readAddStage() {
        if (!ADDSTAGE.exists()) {
            return new Stage();
        }
        return readObject(ADDSTAGE, Stage.class);
    }


    private static File getFile(String file) {
        Path path = Paths.get(file);
        if (path.isAbsolute()) {
            return new File(file);
        } else {
            return new File(CWD, file);
        }
    }

    /**
     * Ideas for implementation of COMMIT function:
     * Since we currently only have the add function, we will initially focus only on the operations of the commit process that involve add.
     * 1. Read currCommit and ADDSTAGE
     * 2. Copy the currCommit filePath2ID
     * 3. Get new and modified files from addStage, add them to filePath2ID
     * 4. Create a new commit object using the updated filePath2ID, commit message, and parents
     * 5. Save commit object
     * 6. Update the HEAD file to point to the new commit
     * 7. Clear the Stage
     *
     * Change for remove function:
     * 1. Read REMOVESTAGE
     * 2. Removes the files in REMOVESTAGE from the currCommit.
     * 3. Clear removeStage
     *
     *
     */

    public static void commit(String message) {
        //  Every commit must have a non-blank message.
        //  If it doesn’t, print the error message "Please enter a commit message."
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }


        currCommit = readCurrCommit();
        Stage addStage = readAddStage();
        Stage removeStage = readRemoveStage();

        // If no files have been staged, abort. Print the message "No changes added to the commit."
        // Implement isEmpty() in Stage Class   -Done
        if (addStage.isEmpty() && removeStage.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit newCommit = getCommit(message, addStage, removeStage);
        newCommit.save();

        // writeContents(HEAD, newCommit.getID());
        // Update not only HEAD but also ALLHEADS
        updateHeads(newCommit);

        clearStage();
    }

    private static Commit getCommit(String message, Stage addStage, Stage removeStage) {
        Map<String, String> copyPath2ID = new HashMap<>(currCommit.getFilePath2ID());
        // Implement getfilePath2ID() in Commit Class     -Done

        for (Map.Entry<String, String> entry : addStage.getBlobMap().entrySet()) {
            copyPath2ID.put(entry.getKey(), entry.getValue());
        }
        // Implement getBlobMap() in Stage Class    -Done

        for (String path : removeStage.getBlobMap().keySet()) {
            copyPath2ID.remove(path);
        }

        Commit newCommit = new Commit(message, List.of(currCommit.getID()), copyPath2ID);

        return newCommit;
    }

    private static void updateHeads(Commit newCommit) {
        String branchName = getBranch();
        File HEAD = join(ALLHEADS, branchName);
        writeContents(HEAD, newCommit.getID());
    }



    /**
     * Read currCommit:
     * Read the commit ID of the current branch from the HEAD file and loads that commit object
     *
     * Since there is only one branch now, the current branch is master branch
     *
      */

    private static Commit readCurrCommit() {
        // Read the HEAD file to get the current branch name
        // String branchName = readContentsAsString(HEAD);
        String branchName = getBranch();

        // Read the branch file to get the current commit ID
        File branchFile = join(ALLHEADS, branchName);
        String commitID = readContentsAsString(branchFile);

        // Read the commit file to get the commit object
        File commitFile = join(OBJECTS, commitID);
        return readObject(commitFile, Commit.class);
    }

    private static String getBranch() {
        return readContentsAsString(HEAD);
    }

    /**
     * Remove:
     * rm [fileName]
     * 1. Check if fileName exist in ADDSTAGE
     *      - If exists, delete it in ADDSTAGE
     * 2. Check if fileName exist in currCommit
     *      - If exists, check if fileName exist in CWD
     *          - If exists, delete it in CWD
     *          - Add file(blob) in REMOVESTAGE
     * 3. Failure to meet the above
     *      - Print "No reason to remove the file."
     *
     * Similar to ADDSTAGE, we need to implement REMOVESTAGE in Stage Class first.
     *
     * Issue 1:
     * Like checkout, here is the fileName, not the filePath.
     * We also need to implement something like getFileNameList in Stage Class.
     *
     *
     */

    public static void remove(String fileName) {
        Stage addStage = readAddStage();
        Stage removeStage = readRemoveStage();
        currCommit = readCurrCommit();
        File file = getFile(fileName);
        if (addStage.containsFile(fileName)) {
            addStage.remove(fileName);
            addStage.saveAddStage();
        } else if (currCommit.containsFile(fileName)) {
            if (file.exists()) {
                file.delete();
            }
            Blob blob = currCommit.getBlob(fileName);
            removeStage.add(blob);
            removeStage.saveRemoveStage();
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    private static Stage readRemoveStage() {
        if (!REMOVESTAGE.exists()) {
            return new Stage();
        }
        return readObject(REMOVESTAGE, Stage.class);
    }



    /**
     * Log:
     * Ideas for implementation of Log function:
     * 1. Read the commit pointed to by the current branch from the HEAD file.
     * 2. Print the current commit message.
     * 3. Iterate through the parent commits of each commit until there are no parent commits left.
     *
     * Print Format:
     * ===
     * commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
     * Date: Thu Nov 9 20:00:05 2017 -0800
     * A commit message.
     *
     * ===
     * commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee49
     * Date: Thu Nov 9 20:00:06 2017 -0800
     * initial commit
     *
     * So we need to implement:
     *      - getMessage()  -DONE
     *      - getTimeStamp()    -DONE
     *      - getLogMessage()   -DONE
     *      - getParents()  -DONE
     *
     *
     */

    public static void log() {
        currCommit = readCurrCommit();
        Deque<Commit> stack = new ArrayDeque<>();
        stack.push(currCommit);

        while (!stack.isEmpty()) {
            Commit commit = stack.pop();
            System.out.println(commit.getLogMessage());
            List<String> parents = commit.getParents();

            for (String parentID : parents) {
                Commit parentCommit = readCommit(parentID);
                stack.push(parentCommit);
            }
        }
    }


    private static Commit readCommit(String ID) {
        File commitFile = join(OBJECTS, ID);
        return readObject(commitFile, Commit.class);
    }

    /**
     * global-log:
     * 1. Get all commit objects in the OBJECTS by using plainFilenamesIn(File dr)
     * 2. Read objects and print
     *
     * Issue 1:
     * OBJECTS contains Commit and Blob.
     * We need to Catch IllegalArgumentException.
     *
     */

    public static void global_log() {
        List<String> commitIDs = plainFilenamesIn(OBJECTS);
        if (commitIDs == null) {
            return;
        }

        for (String commitID : commitIDs) {
            try {
                Commit commit = readCommit(commitID);
                System.out.println(commit.getLogMessage());
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Find:
     * find [message]
     * 1. Get all commit objects in the OBJECTS by using plainFilenamesIn(File dr)
     * 2. Check messages of all commit objects
     * 3. If it matches, print these commit objects' ID
     * 4. If no match, print "Found no commit with that message."
     *
     *
     */

    public static void find(String message) {
        List<String> commitIDs = plainFilenamesIn(OBJECTS);
        if (commitIDs == null) {
            return;
        }
        boolean found = false;

        for (String commitID : commitIDs) {
            try {
                Commit commit = readCommit(commitID);
                if (commit.getMessage().equals(message)) {
                    System.out.println(commitID);
                    found = true;
                }
            } catch (IllegalArgumentException e) {
            }
        }

        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * Status:
     * 1. Branch:
     *      - Read ALLHEADS and get all branches
     * 2. Stage files:
     *      - Read ADDSTAGE
     * 3. Remove file:
     *      - Read REMOVESTAGE
     *
     * Extra Credit:
     * 4. Modifications Not Staged For Commit
     *      - getModifiedFiles();
     * 5. Untracked Files
     *      - getUntrackedFiles();
     *
     * getModifiedFiles():
     * For this part, the first step is that we need to determine the different states that a file can have.
     *      - Modified
     *      - Deleted
     *
     * Next, we need to determine how to ascertain which state a file belongs to.
     * - If files not in ADDSTAGE and REMOVESTAGE
     *      - If files in currCommit
     *          - If files in CWD
     *              - If filesContent in CWD != filesContent in currCommit
     *                  - Modified
     *          - If files not in CWD
     *              - Deleted
     *
     * - If files in ADDSTAGE
     *      - If files in CWD
     *          - If filesContent in CWD != filesContent in STAGE
     *              - Modified
     *          - If files not in CWD
     *              - Deleted
     *
     * PS: If file in REMOVESTAGE, and file in CWD, this file is belong to untracked file
     *
     * getUntrackedFiles():
     * Files in CWD but not in currCommit and ADDSTAGE
     *
     */

    public static void status() {
        System.out.println("=== Branches ===");
        String currBranch = getBranch();
        List<String> branches = plainFilenamesIn(ALLHEADS);
        Collections.sort(branches);
        for (String branch : branches) {
            if (branch.equals(currBranch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();

        Stage addStage = readAddStage();
        Stage removeStage = readRemoveStage();

        System.out.println("=== Staged Files ===");
        List<String> stageFiles = addStage.getFileNamesList();
        printFiles(stageFiles);
        System.out.println();

        System.out.println("=== Removed Files ===");
        List<String> removeFiles = removeStage.getFileNamesList();
        printFiles(removeFiles);
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> modifiedFiles = getModifiedFiles();
        printFiles(modifiedFiles);
        System.out.println();

        System.out.println("=== Untracked Files ===");
        List<String> untrackedFiles = getUntrackedFiles();
        printFiles(untrackedFiles);
        System.out.println();

    }

    private static void printFiles(List<String> files) {
        if (files != null && !files.isEmpty()) {
            Collections.sort(files);
            for (String file : files) {
                System.out.println(file);
            }
        }
    }


    private static List<String> getModifiedFiles() {
        currCommit = readCurrCommit();
        Stage addStage = readAddStage();
        Stage removeStage = readRemoveStage();

        List<String> modifiedFiles = new ArrayList<>();
        List<String> workingFiles = plainFilenamesIn(CWD);

        Map<String, String> currCommitFilePath2ID = currCommit.getFilePath2ID();
        Map<String, String> addStageFilePath2ID = addStage.getBlobMap();
        Map<String, String> removeStageFilePath2ID = removeStage.getBlobMap();


        for (String filePath : currCommitFilePath2ID.keySet()) {
            File file = new File(filePath);
            if (!addStageFilePath2ID.containsKey(filePath) && !removeStageFilePath2ID.containsKey(filePath)) {
                if (workingFiles.contains(file.getName())) {
                    String fileBlobID = generateFileBlobID(filePath);
                    String currCommitFileID = currCommitFilePath2ID.get(filePath);
                    if (!fileBlobID.equals(currCommitFileID)) {
                        modifiedFiles.add(file.getName() + " (modified)");
                    }
                } else {
                    modifiedFiles.add(file.getName() + " (deleted)");
                }
            }
        }

        for (String filePath : addStageFilePath2ID.keySet()) {
            File file = new File(filePath);
            if (workingFiles.contains(file.getName())) {
                String fileBlobID = generateFileBlobID(filePath);
                String addStageFileID = addStageFilePath2ID.get(filePath);
                if (!fileBlobID.equals(addStageFileID)) {
                    modifiedFiles.add(file.getName() + " (modified)");
                }
            } else {
                modifiedFiles.add(file.getName() + " (deleted)");
            }
        }

        return modifiedFiles;
    }


    private static String generateFileBlobID(String filePath) {
        File file = new File(filePath);
        Blob blob = new Blob(file);
        return blob.getID();
    }

    /**
     * getModifiedFiles() and getUntrackedFiles() need to be changed.
     * Use filePath2ID
     *
     */



    private static List<String> getUntrackedFiles() {
        currCommit = readCurrCommit();
        Stage addStage = readAddStage();
        List<String> untrackedFiles = new ArrayList<>();
        List<String> workingFiles = plainFilenamesIn(CWD);

        Map<String, String> currCommitFilePath2ID = currCommit.getFilePath2ID();
        Map<String, String> stageFilePath2ID = addStage.getBlobMap();

        for (String workingFile : workingFiles) {
            File file = getFile(workingFile);
            String path = file.getPath();
            if (!currCommitFilePath2ID.containsKey(path) && !stageFilePath2ID.containsKey(path)) {
                untrackedFiles.add(workingFile);
            }
        }
        return untrackedFiles;
    }




    /**
     * Checkout:
     * Checkout -- [fileName]
     * 1. Read the commit object pointed to by the current branch from the HEAD file.
     * 2. Check if the current commit contains the specified file.
     *      - If not exist, print "File does not exist in that commit."
     * 3. Recover files to the working directory
     *      - If exists, write its contents to the working directory
     *      - If the file of the same name exists, overwrite it
     *      - If not exist, create it
     *
     *
     * Issue 1:
     * The biggest issue concerns fileName and filePath, as well as whether the actual file's path or the blob's path is needed.
     * It's unclear what exactly needs to be checked in the command Checkout --[fileName].
     *
     * Example:
     * > init
     * + A.txt  A.txt (CWD, local file system)
     * > add A.txt (Specify add one of the A.txt)
     * > commit "added a.txt"
     * + A.txt  notA.txt (CWD, made a mistake in the file, change A.txt to notA.txt)
     * > checkout A.txt (What does this step mean?)
     *
     * This step means I need to check if the fileName A.txt exists in the current Commit objects.
     * How to check:
     *      Because in gitlet, only one file may be added at a time.
     *      currCommit --> filePath2ID --> blob objects --> fileName
     *      check if fileName == A.txt
     *
     * If exists:
     *      blob objects --> blob content --> writeContent in CWD
     *
     *
     *
     */

    public static void checkout(String fileName) {
        currCommit = readCurrCommit();
        // Issue: Again, here is the fileName, not the filePath. We need to change the implementation of containsFile(fileName).

        if (!currCommit.containsFile(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        // Implement containsFile(fileName) in Commit Class     -DONE
        recoverFiles(currCommit, fileName);
    }

    public static Blob readBlob(String ID) {
        File blobFile = join(OBJECTS, ID);
        return readObject(blobFile, Blob.class);
    }

    private static void recoverFiles(Commit commit, String fileName) {
        // String blobID = commit.getBlobID(fileName);
        // Implement getBlobID() in Commit Class    -DONE
        // Issue: fileName is not filePath. We need to get blobID based on filePath not fileName.   -DONE

        try {
            Blob blob = commit.getBlob(fileName);
            // Implement readBlob(blobID)   -DONE
            // readBlob(blobID) is used for getting the Blob Objects based on blobID. Similar with readCommit(commitID)

            File file = new File(CWD, blob.getFileName());
            byte[] content = blob.getContent();

            writeContents(file, content);
        } catch (Exception e) {
        }
    }

    /**
     * Checkout:
     * Checkout [commit id] -- [file name]
     * 1. Check if commit id exists
     *      - If not, print "No commit with that id exists."
     * 2. Check if fileName exists in the given Commit
     *      - If not, print "File does not exist in that commit."
     * 3. Recover files to the working directory
     *
     *
     */

    public static void checkout(String commitID, String fileName) {

        Commit givenCommit = findCommitByID(commitID);
        if (!givenCommit.containsFile(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        recoverFiles(givenCommit, fileName);
    }

    /**
     * findCommitByID:
     * 1. Check if commitID.length() == 40
     *      - If true, find it directly
     *      - If false
     *          - Iterate to find unique matches
     *
     *
     */

    public static Commit findCommitByID(String commitID) {
        if (commitID.length() == 40) {
            File commitFile = join(OBJECTS, commitID);
            if (commitFile.exists()) {
                return readCommit(commitID);
            }
        } else {
            List<String> allObjectID = plainFilenamesIn(OBJECTS);
            for (String id : allObjectID) {
                if (id.startsWith(commitID)) {
                    return readCommit(id);
                }
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }


    /**
     * Checkout:
     * Checkout [branchName]
     * 1. Check if branchName exists
     *      - If not, print "No such branch exists."
     * 2. Check if branchName is currBranch
     *      - If it is, print "No need to checkout the current branch."
     * 3. Get the latest commit pointed to by the branchName.
     * 4. Change document
     *      - If files in currBranch but not branchName
     *          - Delete files
     *      - If files in branchName
     *          - If fileName in currBranch = fileName in branchName && fileBlobID in currBranch != fileBlobID in branchName
     *              - writeContent(filesInBranchName)
     *          - If files in CWD but not in currBranch or ADDSTAGE
     *              - This means these files are untracked
     *              - Gitlet will feel confused because it doesn't know use the fileName in CWD or in branchName
     *              - Print "There is an untracked file in the way; delete it, or add and commit it first."
     *          - WriteContent(filesInBranchName)
     * 5. Update HEAD to branchName
     * 6. Clear stage
     *
     *
     * Issue 1:
     * Again, again, again.
     * It seems there are still something wrong with filePath and fileName.
     * I need to use filePath2ID instead of fileName list.
     *
     *
     */

    public static void checkoutBranch(String branchName) {
        List<String> branchList = plainFilenamesIn(ALLHEADS);

        if (!branchList.contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        String currBranch = getBranch();

        if (currBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }


        Commit branchCommit = readCommitByBranchName(branchName);
        currCommit = readCurrCommit();

        overWriteAndDelete(branchCommit, currCommit);
        writeContents(HEAD, branchName);
        clearStage();
    }

    private static Commit readCommitByBranchName(String branchName) {
        File branchFile = join(ALLHEADS, branchName);
        String branchCommitID = readContentsAsString(branchFile);
        Commit branchCommit = readCommit(branchCommitID);
        return branchCommit;
    }

    private static void overWriteAndDelete(Commit targetCommit, Commit currCommit) {
        Map<String, String> targetFilePath2ID = targetCommit.getFilePath2ID();
        Map<String, String> currFilePath2ID = currCommit.getFilePath2ID();

       for (Map.Entry<String, String> entry : targetFilePath2ID.entrySet()) {
            // I think it shouldn't use currFiles contains targetFiles. because they might have the same name but not the same files or
            // problems in recoverFiles like fileName couldn't point to correct file

           String filePath = entry.getKey();
           String targetBlobID = entry.getValue();

            if (currFilePath2ID.containsKey(filePath)) {
                String currBlobID = currFilePath2ID.get(filePath);

                if (!currBlobID.equals(targetBlobID)) {
                    recoverFilesBasedOnFilePath(targetCommit, filePath);
                }
            } else {
                List<String> untrackedFiles = getUntrackedFiles();
                if (untrackedFiles.contains(new File(filePath).getName())) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
                recoverFilesBasedOnFilePath(targetCommit, filePath);
            }
        }

       for (String filePath : currFilePath2ID.keySet()) {
           if (!targetFilePath2ID.containsKey(filePath)) {
               restrictedDelete(filePath);
           }
       }
    }

    private static void recoverFilesBasedOnFilePath(Commit commit, String filePath) {
        Blob blob = commit.getBlobByFilePath(filePath);
        if (blob != null) {
            File file = new File(filePath);
            byte[] content = blob.getContent();
            writeContents(file, content);
        }
    }




    /**
     * Branch:
     * Branch [branchName]
     * 1. Read the branch file pointed to by the current HEAD to get the ID of the current commit.
     * 2. Create a new file in the heads folder with the filename as branchName, containing the ID of the current commit.
     */

    public static void branch(String branchName) {
        currCommit = readCurrCommit();
        String currCommitID = currCommit.getID();

        File newBranchFile = join(ALLHEADS, branchName);
        if (newBranchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        writeContents(newBranchFile, currCommitID);
    }

    /**
     * rm-branch:
     * rm-branch [branchName]
     * 1. Check if branchName exists
     *      - If not, print "A branch with that name does not exist."
     * 2. Check if branchName = currBranch
     *      - If is, print "Cannot remove the current branch."
     * 3. Delete branchNameFile in ALLHEADS
     */

    public static void rmBranch(String branchName) {
        isBranchExist(branchName);

        String currBranch = getBranch();
        if (branchName.equals(currBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        File branchNameFile = join(ALLHEADS, branchName);
        branchNameFile.delete();
    }

    private static void isBranchExist(String branchName) {
        List<String> branchList = plainFilenamesIn(ALLHEADS);
        if (!branchList.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    /**
     * Reset:
     * reset [commit id]
     * 1. Check if commit id exists
     *      - If not, print "No commit with that id exists."
     * 2. Check if files in target commit exists in untracked files
     *      - If is, print "There is an untracked file in the way; delete it, or add and commit it first."
     * 3. Recover all target commit files in CWD
     * 4. Delete files in currCommit but not in target commit
     * 5. Update HEAD to target commit
     * 6. Clear Stage
     *
     */

    public static void reset(String commitID) {
        currCommit = readCurrCommit();
        Commit targetCommit = findCommitByID(commitID);
        overWriteAndDelete(targetCommit, currCommit);

        String currBranch = getBranch();
        File headFile = join(ALLHEADS, currBranch);
        writeContents(headFile, commitID);

        clearStage();
    }

    private static void clearStage() {
        Stage addStage = readAddStage();
        Stage removeStage = readRemoveStage();
        addStage.clear();
        removeStage.clear();
        addStage.saveAddStage();
        removeStage.saveRemoveStage();
    }

    /**
     * Merge:
     * merge [branchName]
     * 1. Initial check
     *      - Check if files in ADDSTAGE or REMOVESTAGE
     *          - If true, print "You have uncommitted changes."
     *      - Check if branchName exists
     *          - If false, print "A branch with that name does not exist."
     *      - Check if branchName is currBranch
     *          - If true, print "Cannot merge a branch with itself."
     *
     *
     * 2. Find Split Point:
     * Split point is the nearest common ancestor of currBranch and branchName
     *      - Check if splitCommit = givenCommit
     *          - If true, print "Given branch is an ancestor of the current branch."
     *      - Check if splitCommit = currCommit
     *          - If true, print "Current branch fast-forwarded."
     *
     * Idea:
     * Use BFS
     *
     * What is BFS?
     * 1. Initial
     *      - A queue: store the nodes to be visited.
     *      - A set: record the nodes that have already been visited, to avoid revisiting them.
     * 2. Enqueue the start node
     *      - Add the start node to the queue and mark it as visited.
     * 3. Iterate through the queue
     *      - Remove a node from the front of the queue and process it.
     *      - Add all unvisited adjacent nodes of this node to the queue and mark them as visited.
     *      - Repeat the above steps until the queue is empty or the target node is found.
     *
     * For merge:
     * Bidirectional BFS
     *
     * 1. Initial
     *      - Two queues:
     *          - currQueue
     *          - givenQueue
     *      - Two sets:
     *          - currVisited
     *          - givenVisited
     * 2. Enqueue the start node
     *      - currCommit add in currQueue
     *      - givenCommit add in givenQueue
     * 3. Bidirectional expansion:
     *      - currQueue
     *          - Pop a node
     *          - Check if commitID in givenVisited
     *              - If true, return this node
     *              - If false
     *                  - Add all parentsCommit in currQueue
     *                  - Add all parentsID in currVisited
     *      - givenQueue
     *          - Pop a node
     *          - Check if commitID in currVisited
     *          - If true, return this node
     *          - If false
     *              - Add all parentsCommit in givenQueue
     *              - Add all parentsID in givenVisited
     *
     *
     * 3. Merge
     *
     * Ideas for merge logic:
     * 1. Initial
     *      - currFilePath2ID
     *      - givenFilePath2ID
     *      - splitFilePath2ID
     *
     *      - Get allFiles
     *          - Add currFilePath2ID.keySet()
     *          - Add givenFilePath2ID.keySet()
     *          - Add splitFilePath2ID.keySet()
     *
     * 2. For file in allFiles
     *      - If splitBlobID != givenBlobID && splitBlobID = currBlobID
     *          - Change file in givenVision (recoverFile)
     *          - Add file
     *
     *      - If splitBlobID = givenBlobID && splitBlobID != currBlobID
     *          - No change
     *
     *      - If currBlobID = givenBlobID && splitBlobID != currBlobID
     *          - No change
     *
     *      - If currBlobID != null && splitBlobID = null && givenBlobID = null
     *          - No change
     *
     *      - If givenBlobID != null && splitBlobID = null && currBlobID = null
     *          - Change file in givenVision
     *          - Add file
     *
     *      - If splitBlobID != null && givenBlobID = null && currBlobID != null
     *          - If splitBlobID = currBlobID
     *              - remove file
     *      - If splitBlobID != null && currBlobID = null && givenBlobID != null
     *          - If splitBlobID = giveBlobID
     *              - No change
     *      - Else
     *          - Handle conflict
     *
     * 3. Handle conflict
     *      - Read content
     *          - currFileContent
     *          - givenFileContent
     *      - Generate conflict flag
     *      - Write it to the file
     *      - Add it
     *
     *
     * 4. Commit merge
     *
     *
     */



    public static void merge(String branchName) {
        Stage addStage = readAddStage();
        Stage removeStage = readRemoveStage();
        if (!addStage.isEmpty() || !removeStage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        isBranchExist(branchName);

        String currBranch = getBranch();
        if (currBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        currCommit = readCurrCommit();
        Commit givenCommit = readCommitByBranchName(branchName);
        Commit splitCommit = findSplitPoint(currCommit, givenCommit);

        if (splitCommit.equals(givenCommit)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        if (splitCommit.equals(currCommit)) {
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }


        mergeFiles(currCommit, givenCommit, splitCommit);

        String message = "Merged " + branchName + " into " + currBranch + ".";
        Map<String, String> newFilePath2ID = new HashMap<>(currCommit.getFilePath2ID());

        for (Map.Entry<String, String> entry : addStage.getBlobMap().entrySet()) {
            newFilePath2ID.put(entry.getKey(), entry.getValue());
        }
        for (String path : removeStage.getBlobMap().keySet()) {
            newFilePath2ID.remove(path);
        }

        List<String> parents = Arrays.asList(currCommit.getID(), givenCommit.getID());

        Commit newCommit = new Commit(message, parents, newFilePath2ID);
        newCommit.save();

        updateHeads(newCommit);
        clearStage();


    }

    private static Commit findSplitPoint(Commit currCommit, Commit givenCommit) {
        Queue<Commit> currQueue = new LinkedList<>();
        Queue<Commit> givenQueue = new LinkedList<>();

        Set<String> currVisited = new HashSet<>();
        Set<String> givenVisited = new HashSet<>();

        currQueue.add(currCommit);
        givenQueue.add(givenCommit);

        currVisited.add(currCommit.getID());
        givenVisited.add(givenCommit.getID());

        while (!currQueue.isEmpty() || !givenQueue.isEmpty()) {
            if (!currQueue.isEmpty()) {
                Commit curr = currQueue.poll();
                if (givenVisited.contains(curr.getID())) {
                    return curr;
                }
                for (String parentID : curr.getParents()) {
                    if (!currVisited.contains(parentID)) {
                        currVisited.add(parentID);
                        currQueue.add(readCommit(parentID));
                    }
                }
            }

            if (!givenQueue.isEmpty()) {
                Commit given = givenQueue.poll();
                if (currVisited.contains(given.getID())) {
                    return given;
                }
                for (String parentID : given.getParents()) {
                    if (!givenVisited.contains(parentID)) {
                        givenVisited.add(parentID);
                        givenQueue.add(readCommit(parentID));
                    }
                }
            }
        }

        return null;
    }

    private static void mergeFiles(Commit currCommit, Commit giveCommit, Commit splitCommit) {
        Map<String, String> currFilePath2ID = currCommit.getFilePath2ID();
        Map<String, String> givenFilePath2ID = giveCommit.getFilePath2ID();
        Map<String, String> splitFilePath2ID = splitCommit.getFilePath2ID();

        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(currFilePath2ID.keySet());
        allFiles.addAll(givenFilePath2ID.keySet());
        allFiles.addAll(splitFilePath2ID.keySet());

        Boolean conflict = false;

        for (String file : allFiles) {
            String currBlobID = currFilePath2ID.get(file);
            String givenBlobID = givenFilePath2ID.get(file);
            String splitBlobID = splitFilePath2ID.get(file);

            boolean checkNull = checkNull(currBlobID, givenBlobID, splitBlobID);
            String fileName = getFileName(file);

            // TODO: check here
            // Conflict has problem

            if (checkNull) {
                if (checkNull && !splitBlobID.equals(givenBlobID) && splitBlobID.equals(currBlobID)) {
                    recoverFilesBasedOnFilePath(giveCommit, file);
                    add(fileName);
                } else if (checkNull && splitBlobID.equals(givenBlobID) && !splitBlobID.equals(currBlobID)) {
                } else if (checkNull && givenBlobID.equals(currBlobID) && !splitBlobID.equals(currBlobID)) {
                } else {
                    conflict = true;
                    handleConflict(currCommit, giveCommit, file);
                }
            } else if (currBlobID != null && splitBlobID == null && givenBlobID == null) {
            } else if (givenBlobID != null && splitBlobID == null && currBlobID == null) {
                recoverFilesBasedOnFilePath(giveCommit, file);
                add(fileName);
            } else if (splitBlobID != null && givenBlobID == null && currBlobID != null) {
                if (splitBlobID.equals(currBlobID)) {
                    remove(fileName);
                }
            } else if (splitBlobID != null && givenBlobID != null && currBlobID == null) {
                if (splitBlobID.equals(givenBlobID)) {
                } else {
                    conflict = true;
                    handleConflict(currCommit, giveCommit, file);
                }
            } else {
                conflict = true;
                handleConflict(currCommit, giveCommit, file);
            }
        }

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    public static String getFileName(String filePath) {
        Path path = Paths.get(filePath);
        return path.getFileName().toString();
    }


    private static boolean checkNull(String id1, String id2, String id3) {
        return (id1 != null && id2 != null && id3 != null);
    }

    private static void handleConflict(Commit currCommit, Commit givenCommit, String filePath) {
        Blob currBlob = currCommit.getBlobByFilePath(filePath);
        Blob givenBlob = givenCommit.getBlobByFilePath(filePath);
        String fileName = getFileName(filePath);

        String currContent = currBlob != null ? new String(currBlob.getContent()) : "";
        String givenContent = givenBlob != null ? new String(givenBlob.getContent()) : "";

        String conflictContent = "<<<<<<< HEAD\n" + currContent + "=======\n" + givenContent + ">>>>>>>\n";
        File conflictFile = join(CWD, fileName);
        writeContents(conflictFile, conflictContent);

        add(fileName);
    }

}