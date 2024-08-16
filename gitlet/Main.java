package gitlet;
import static gitlet.Repository.*;
/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // If args is empty, print the message "Please enter a command." and exit.
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                checkOperands(args, 1);
                Repository.init();
                break;

            case "add":
                checkOperands(args, 2);
                Repository.checkIfInitialized();
                Repository.add(args[1]);
                break;

            case "rm":
                checkOperands(args, 2);
                Repository.checkIfInitialized();
                Repository.remove(args[1]);
                break;


            case "commit":
                checkOperands(args, 2);
                Repository.checkIfInitialized();
                Repository.commit(args[1]);
                break;

            case "log":
                checkOperands(args, 1);
                Repository.checkIfInitialized();;
                Repository.log();
                break;

            case "global-log":
                checkOperands(args, 1);
                Repository.checkIfInitialized();;
                Repository.global_log();
                break;

            case "find":
                checkOperands(args, 2);
                Repository.checkIfInitialized();;
                Repository.find(args[1]);
                break;

            case "status":
                checkOperands(args, 1);
                Repository.checkIfInitialized();;
                Repository.status();
                break;


            case "checkout":
                Repository.checkIfInitialized();
                Repository repository = new Repository();
                if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    repository.checkout(args[2]);
                    break;
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    repository.checkout(args[1], args[3]);
                    break;
                } else if (args.length == 2) {
                    repository.checkoutBranch(args[1]);
                    break;
                }
                break;

            case "branch":
                checkOperands(args, 2);
                Repository.checkIfInitialized();;
                Repository.branch(args[1]);
                break;

            case "rm-branch":
                checkOperands(args, 2);
                Repository.checkIfInitialized();;
                Repository.rmBranch(args[1]);
                break;

            case "reset":
                checkOperands(args, 2);
                Repository.checkIfInitialized();;
                Repository.reset(args[1]);
                break;

            case "merge":
                checkOperands(args, 2);
                Repository.checkIfInitialized();
                Repository.merge(args[1]);
                break;

        }

    }

    // If a user inputs a command with the wrong number or format of operands, print the message "Incorrect operands." and exit.
    private static void checkOperands(String[] args, int num) {
        if (args.length != num) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }

    }
}