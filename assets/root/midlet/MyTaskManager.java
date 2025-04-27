public class MyTaskManager implements CommandListener 
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import javax.bluetooh.*;
import java.util.*;
import java.io.*;

public class MyTaskManager extends MIDlet implements CommandListener { 
    private List tasks = new List(form.getTitle(), List.IMPLICIT); 
    private TextBox taskname = new TextBox("Create Task", "", 256, TextField.ANY); 
    private Command backCommand = new Command("Back", Command.BACK, 1), 
            saveCommand = new Command("Save", Command.OK, 1), 
            newCommand = new Command("New Task", Command.SCREEN, 2), 
            readCommand = new Command("View more", Command.SCREEN, 3), 
            toggleCommand = new Command("Toggle Status", Command.SCREEN, 4), 
            clearCommand = new Command("Clear finished", Command.SCREEN, 5), 
            deleteCommand = new Command("Delete", Command.SCREEN, 6); 
            
    private String file = ".tasks"; 
    
    public startApp() { 
        tasks.addCommand(backCommand); 
        tasks.addCommand(newCommand); 
        tasks.addCommand(toggleCommand); 
        tasks.addCommand(readCommand); 
        tasks.addCommand(clearCommand); 
        tasks.addCommand(deleteCommand); 
        tasks.setCommandListener(this); 
        
        taskname.addCommand(saveCommand); 
        taskname.setCommandListener(this); 
        
        readTasks(); 
        display.setCurrent(tasks); 
        
    } 

    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { }
    
    public void commandAction(Command c, Displayable d) { 
        if (display.getCurrent() == tasks) { 
            if (c == backCommand) { notifyDestroyed(); } 
            else if (c == newCommand) { display.setCurrent(taskname); } 
            else if (c == toggleCommand) { toggleTask(tasks.getString(tasks.getSelectedIndex())); } 
            else if (c == readCommand) { viewmore(tasks.getString(tasks.getSelectedIndex())); } 
            else if (c == clearCommand) { clearFinished(); } 
            else if (c == deleteCommand) { deleteTask(tasks.getString(tasks.getSelectedIndex())); } 
        } else if (display.getCurrent() == taskname) { 
            if (c == saveCommand) { 
                if (taskname.getString().equals("")) { display.setCurrent(tasks); } 
                else { writeRMS(file, loadRMS(file, 1) + "\n" + taskname.getString()); readTasks(); taskname.setString(""); display.setCurrent(tasks); } 
            } 
        } 
    } 
    
    private void readTasks() { tasks.deleteAll(); String[] tasklist = split(loadRMS(file, 1), '\n'); for (int i = 0; i < tasklist.length; i++) { if (!tasklist[i].trim().equals("")) { tasks.append(tasklist[i].trim(), null); } } } 
    private void clearFinished() { String[] tasklist = split(loadRMS(file, 1), '\n'); StringBuffer newlist = new StringBuffer(); for (int i = 0; i < tasklist.length; i++) { String task = tasklist[i].trim(); if (!task.startsWith("[COMPLETE] ") && !task.equals("")) { newlist.append(task + "\n"); } } writeRMS(file, newlist.toString().trim()); readTasks(); } 
    private void deleteTask(String task) { String[] tasklist = split(loadRMS(file, 1), '\n'); StringBuffer newlist = new StringBuffer(); for (int i = 0; i < tasklist.length; i++) { if (!task.equals(tasklist[i]) && !task.equals("")) { newlist.append(tasklist[i] + "\n"); } } writeRMS(file, newlist.toString().trim()); readTasks(); } 
    private void toggleTask(String task) { String[] tasklist = split(loadRMS(file, 1), '\n'); StringBuffer newlist = new StringBuffer(); for (int i = 0; i < tasklist.length; i++) { if (tasklist[i].equals(task)) { if (task.startsWith("[COMPLETE] ")) { newlist.append(task.substring("[COMPLETE] ".length()).trim() + "\n"); } else if (task.startsWith("[WORKING] ")) { newlist.append("[COMPLETE] " + task.substring("[WORKING] ".length()).trim() + "\n"); } else { if (task.endsWith("*")) { newlist.append("[COMPLETE] " + task + "\n"); } else { newlist.append("[WORKING] " + task + "\n"); } } } else { newlist.append(tasklist[i] + "\n"); } } writeRMS(file, newlist.toString().trim()); readTasks(); } 
    private void viewmore(String task) { String status = "Needs action"; if (task.startsWith("[COMPLETE] ")) { status = "Finished"; } else if (task.startsWith("[WORKING] ")) { status = "Working"; } warnCommand(form.getTitle(), env("Task Name: " + replace(replace(task, "[COMPLETE] ", ""), "[WORKING] ", "") + "\nStatus: " + status)); } 
    
    private void warnCommand(String title, String message) { if (message == null || message.length() == 0) { return; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); }
    
    private void deleteFile(String filename) { if (filename == null || filename.length() == 0) { return; } try { RecordStore.deleteRecordStore(filename); } catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename + ": not found"); } catch (RecordStoreException e) { echoCommand("rm: " + e.getMessage()); } }
    private void writeRMS(String recordStoreName, String data) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } }
    private String loadRMS(String recordStoreName, int recordId) { RecordStore recordStore = null; String result = ""; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); if (recordStore.getNumRecords() >= recordId) { byte[] data = recordStore.getRecord(recordId); if (data != null) { result = new String(data); } } } catch (RecordStoreException e) { result = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return result; }
    
}