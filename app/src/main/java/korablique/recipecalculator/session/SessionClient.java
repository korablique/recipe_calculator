package korablique.recipecalculator.session;

/**
 * Elements of the enum are stored persistently on the File System - don't reuse or change names.
 */
public enum SessionClient {
    MAIN_ACTIVITY_FRAGMENT("MAIN_ACTIVITY_FRAGMENT"),
    MAIN_ACTIVITY_SELECTED_DATE("MAIN_ACTIVITY_SELECTED_DATE");

    public final String name;

    SessionClient(String name) {
        this.name = name;
    }
}
