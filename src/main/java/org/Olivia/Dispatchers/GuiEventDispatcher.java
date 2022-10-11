package org.Olivia.Dispatchers;
/**
 * Listens command from a given UiHandler
 * parse the command and execute the corresponding function
 * syncs with hard disk after each execution
 *
 * @author ZHANG TONGJUN (albertZhangTJ)
 */

import org.Olivia.IO.FileHandler;
import org.Olivia.IO.UiHandler;
import org.Olivia.calendar.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class GuiEventDispatcher {

    private final Calendar table;
    private final FileHandler disk;

    public GuiEventDispatcher(Calendar table, FileHandler disk) {
        this.table = table;
        this.disk = disk;
    }

    //==============================HELPER METHODS========================================================

    private static void validateCommand(String input) throws Exception {
        if (input == null) {
            throw new InvalidParameterException("command string array is not expected to be null, internal error");
        }
        if (input.length() == 0) {
            throw new IllegalArgumentException("Sorry, command length cannot be zero");
        }
    }

    private static String[] tokenizeCommand(String input) throws Exception {
        String[] splited_input = input.toLowerCase().split(" ");
        if (splited_input.length == 0) {
            throw new IllegalArgumentException("Sorry, I don't seem to understand you");
        }
        return splited_input;
    }

    public static List<String> parseTags(String input) {
        List<String> ans = new ArrayList<>();
        String[] splited_input = input.toLowerCase().split(" ");
        for (String s : splited_input) {
            if (s.length()>1 && s.charAt(0) == '#') {
                ans.add(s.substring(1));
            }
        }
        return ans;
    }

    public static String parseEventTitleInputLine(String line) {
        line = line.trim().toLowerCase();
        //strip the command
        if (line.indexOf("todo") == 0) {
            line=line.substring(5);
        }
        else if (line.indexOf("deadline") == 0) {
            line=line.substring(9);
        }
        else if (line.indexOf("event") == 0) {
            line=line.substring(6);
        }
        //strip the tags
        if (line.indexOf("#") != -1) {
            line = line.substring(0, line.indexOf("#"));
        }
        //strip the time
        if (line.indexOf(" /at ") != -1) {
            line = line.substring(0, line.indexOf(" /at "));
        }
        if (line.indexOf(" /by ") != -1) {
            line = line.substring(0, line.indexOf(" /by "));
        }
        return line.trim();
    }

    //=========================MARK AND UNMARK===============================================================

    private String markEntry(String[] args) throws Exception {
        int status = this.table.markAsDone(Integer.parseInt(args[1]));
        if (status == 200 || status == 208) {
            disk.syncToFile(this.table);
            return "Jawohl, I have marked the following event as completed:\n     " +
                    this.table.getEntry(Integer.parseInt(args[1])) + "\n";
        }
        throw new RuntimeException(
                "Sorry, there seems to be some difficulties processing your command, could you check the syntax and try again later?"
        );
    }

    private String unmarkEntry(String[] args) throws Exception {
        int status = this.table.markAsUndone(Integer.parseInt(args[1]));
        if (status == 200 || status == 208) {
            disk.syncToFile(this.table);
            return "Jawohl, I have marked the following event as incomplete:\n     " +
                    this.table.getEntry(Integer.parseInt(args[1])) + "\n";
        }
        throw new RuntimeException(
                "Sorry, there seems to be some difficulties processing your command, could you check the syntax and try again later?"
        );
    }

    private String toggleStatus(String input) throws Exception {
        String[] args = input.toLowerCase().split(" ");
        if (args.length != 2) {
            throw new InvalidParameterException("Sorry, which entry do you want me to mark/unmark?");
        }
        if (args[0].equals("mark")) {
            return markEntry(args);
        }
        if (args[0].equals("unmark")) {
            return unmarkEntry(args);
        }
        throw new RuntimeException("Sorry, there seems to be some difficulties processing your command, could you check the syntax and try again later?");
    }

    //==============================ADD========================================================

    private String addTodo(String input, List<String> tags) throws Exception {
        CalendarEntryTodo entry = new CalendarEntryTodo(parseEventTitleInputLine(input), tags);
        int status = this.table.addEntry(entry);
        if (status == 200) {
            disk.syncToFile(this.table);
            return "Verstehe, added: " + entry + "\n";
        }
        throw new InvalidParameterException("Sorry, I don't seem to understand you");
    }

    private String addDeadline(String input, List<String> tags) throws Exception {
        if (input.indexOf("/by") == -1) {
            throw new InvalidParameterException(
                    "Sorry, what is the exact time of the deadline?\nCheck the help message for information on command syntax"
            );
        }
        input = input.substring(9);
        String time = input.substring(input.indexOf("/by") + 4);
        CalendarEntryDeadline entry = new CalendarEntryDeadline(parseEventTitleInputLine(input), time, tags);
        int status = this.table.addEntry(entry);
        if (status == 200) {
            disk.syncToFile(this.table);
            return "Verstehe, added: " + entry + "\n";
        }
        throw new InvalidParameterException("Sorry, I don't seem to understand you");
    }

    private String addEvent(String input, List<String> tags) throws Exception {
        if (input.indexOf("/at") == -1 || input.indexOf(" - ") == -1) {
            throw new InvalidParameterException("Sorry, what is the exact time of the event?\nCheck the help message for information on command syntax");
        }
        input = input.substring(6);
        String time = input.substring(input.indexOf("/at") + 4);
        CalendarEntryEvent entry = new CalendarEntryEvent(parseEventTitleInputLine(input), time.split(" - ")[0], time.split(" - ")[1], tags);
        int status = this.table.addEntry(entry);
        if (status == 200) {
            disk.syncToFile(this.table);
            return "Verstehe, added: " + entry + "\n";
        }
        throw new InvalidParameterException("Sorry, I don't seem to understand you");
    }

    private String addEntryToCalendar(String input) throws Exception {
        String[] args = input.toLowerCase().split(" ");
        List<String> tags = parseTags(input);
        if (args[0].equals("todo") && args.length >= 2) {
            return addTodo(input, tags);
        } else if (args[0].equals("deadline") && args.length >= 4) {
            return addDeadline(input, tags);
        } else if (args[0].equals("event") && args.length >= 4) {
            return addEvent(input, tags);
        }
        throw new InvalidParameterException("Sorry, I don't seem to understand you");
    }

    //==============================FIND======================================================

    private String find(String input) {
        input = input.substring(5);
        List<CalendarEntry> entries = this.table.getEntriesContains(input);
        String content = "";
        for (CalendarEntry e : entries) {
            content = content + e.toString() + "\n";
        }
        if (entries.size() == 0) {
            return "Es tut mir leid, cannot find any match entries";
        }
        return content;
    }

    //==============================OTHERS========================================================

    private String help() {
        return UiHandler.generateHelpMsg();
    }

    private String list() {
        return this.table.toString();
    }

    private String delete(String input) throws Exception {
        String[] args = input.toLowerCase().split(" ");
        if (args.length != 2) {
            throw new InvalidParameterException("Sorry, which entry do you want me to delete?");
            //return 400;
        }
        disk.syncToFile(this.table);
        return "Jawohl, I have removed the following entry from your calendar:\n     " +
                this.table.deleteEntry(Integer.parseInt(args[1])) + "\n";
    }

    //==============================MAIN PARSER========================================================

    /**
     * calls corresponding functions based on the input command
     *
     * @param input the array of string that contains the command and parameters
     * @return status code (adapted from http, with exceptions such as status 0 represents exit)
     */
    public String dispatchCommand(String input) throws Exception {
        validateCommand(input);
        String[] splited_input = tokenizeCommand(input);

        if (splited_input[0].equals("list") || splited_input[0].equals("ls")) {
            return list();
        }
        if (splited_input[0].equals("help")) {
            return help();
        }
        if (splited_input[0].equals("mark") || splited_input[0].equals("unmark")) {
            return toggleStatus(input);
        }
        if (splited_input[0].equals("todo") || splited_input[0].equals("event") || splited_input[0].equals("deadline")) {
            return addEntryToCalendar(input);
        }
        if (splited_input[0].equals("delete")) {
            return delete(input);
        }
        if (splited_input[0].equals("find") && splited_input.length > 1) {
            return find(input);
        }
        throw new IllegalArgumentException("Sorry, I don't seem to understand you");
    }

    public void initialize() throws Exception {
        this.disk.syncFromFile(this.table);
    }
}
