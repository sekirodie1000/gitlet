# Gitlet Design Document


## Classes and Data Structures

### Class 1: `Main`

#### Fields
- No instance fields are present as this is the driver class.

#### Description
- The `Main` class serves as the entry point for the Gitlet application. It processes command-line arguments and calls the corresponding methods in the `Repository` class to perform version control operations.

### Class 2: `Repository`

#### Fields
1. **CWD**: `File` - The current working directory.
2. **GITLET_DIR**: `File` - The `.gitlet` directory that stores all version control data.
3. **OBJECTS**: `File` - The directory within `.gitlet` that stores serialized `Commit` and `Blob` objects.
4. **REFS**: `File` - The directory within `.gitlet` that stores branch references.
5. **ALLHEADS**: `File` - The directory within `REFS` that stores the HEAD pointers for all branches.
6. **HEAD**: `File` - The file that stores the name of the current branch.
7. **ADDSTAGE**: `File` - The file that stores the staging area for added files.
8. **REMOVESTAGE**: `File` - The file that stores the staging area for removed files.
9. **currCommit**: `Commit` - The current `Commit` object.

#### Description
- The `Repository` class manages the state of the Gitlet repository. It handles initialization, commits, staging, and other core version control functionalities.

### Class 3: `Commit`

#### Fields
1. **message**: `String` - The commit message.
2. **parents**: `List<String>` - The list of parent commit IDs.
3. **currTime**: `Date` - The date and time of the commit.
4. **ID**: `String` - The unique identifier for the commit, generated using SHA-1.
5. **filePath2ID**: `Map<String, String>` - A map from file paths to blob IDs, representing the snapshot of the working directory.
6. **timeStamp**: `String` - The formatted timestamp of the commit.
7. **commitSaveID**: `File` - The file where this commit is saved.

#### Description
- The `Commit` class represents a snapshot of the repository at a given point in time. It includes metadata like the commit message, parent commits, and a mapping of file paths to blob IDs.

### Class 4: `Stage`

#### Fields
1. **filePath2ID**: `Map<String, String>` - A map from file paths to blob IDs, representing files staged for addition or removal.

#### Description
- The `Stage` class represents the staging area where files are prepared before being committed. It tracks both added and removed files.

### Class 5: `Blob`

#### Fields
1. **content**: `byte[]` - The content of the file.
2. **fileName**: `File` - The file's name.
3. **ID**: `String` - The unique identifier for the blob, generated using SHA-1.
4. **filePath**: `String` - The path of the file within the repository.
5. **blobSaveID**: `File` - The file where this blob is saved.

#### Description
- The `Blob` class represents a snapshot of a file's content at a given point in time. Each blob is identified by its content and is stored in the `.gitlet/objects` directory.

## Algorithms

### Commit Algorithm
1. **Initialization**: Read the current commit and staging area (add and remove stages).
2. **Modification**: Update the filePath2ID map with changes from the staging area.
3. **Save**: Create and save the new commit object, then update the branch reference in `ALLHEADS` and clear the staging area.

### Merge Algorithm
1. **Initial Checks**: Verify if there are uncommitted changes, if the branch exists, and if the branch is not the current branch.
2. **Find Split Point**: Use a bidirectional BFS to find the nearest common ancestor of the current and given branches.
3. **Merge Logic**: Iterate over all files, applying changes based on the comparison of the current, given, and split commits.
4. **Conflict Handling**: If conflicts arise, create a conflict file with both versions of the content.

## Persistence

### Commit Persistence
- Each commit is serialized and saved in the `.gitlet/objects` directory. The commit's ID is generated using the SHA-1 hash of its contents, ensuring uniqueness and consistency.

### Blob Persistence
- Each `Blob` object is serialized and saved similarly to commits. The blob's ID is based on the SHA-1 hash of its content.

### Staging Area Persistence
- The staging areas for added and removed files are stored in `ADDSTAGE` and `REMOVESTAGE`, respectively. These are serialized and saved between operations to maintain state.

### Branches and HEAD
- Branches are stored as files in the `.gitlet/refs/heads` directory, with each file containing the ID of the latest commit on that branch. The `HEAD` file stores the name of the current branch.
