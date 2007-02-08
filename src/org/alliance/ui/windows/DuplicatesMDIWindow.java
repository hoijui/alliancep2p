package org.alliance.ui.windows;

import org.alliance.ui.UISubsystem;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.filetransfers.UploadConnection;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.awt.event.ActionEvent;

import com.stendahls.util.TextUtils;
import com.stendahls.nif.ui.mdi.MDIWindow;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class DuplicatesMDIWindow extends AllianceMDIWindow {
    private DuplicatesMDIWindow.TableModel model;
    private JTable table;
    private ArrayList<Dup> dups = new ArrayList<Dup>();

    public DuplicatesMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "duplicates", ui);

        table = (JTable)xui.getComponent("table");
        table.setModel(model = new DuplicatesMDIWindow.TableModel());
        table.setAutoCreateColumnsFromModel(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);

        setTitle("Duplicates in my share");

        TreeSet<String> ts = new TreeSet<String>(ui.getCore().getFileManager().getFileDatabase().getDuplicates());
        for(String s : ts) {
            Hash h = ui.getCore().getFileManager().getFileDatabase().getHashForDuplicate(s);
            if (h == null) {
                dups.add(new Dup(s, "<lost>"));
            } else {
                FileDescriptor fd = ui.getCore().getFileManager().getFd(h);
                dups.add(new Dup(s, fd.getFullPath()));
            }
        }

        ((JLabel)xui.getComponent("status")).setText("Number of duplicates: "+dups.size());
        postInit();
    }

    public String getIdentifier() {
        return "duplicates";
    }

    public void save() throws Exception {}
    public void revert() throws Exception {
        manager.recreateWindow(this, new DuplicatesMDIWindow(ui));
    }
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }

    private class TableModel extends AbstractTableModel {
        public int getRowCount() {
            return dups.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "In share";
                case 1:
                    return "Duplicate";
                default:
                    return "undefined";
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return dups.get(rowIndex).inShare;
                case 1:
                    return dups.get(rowIndex).duplicate;
                default:
                    return "undefined";
            }
        }
    }

    private class Dup {
        public Dup(String inShare, String duplicate) {
            this.inShare = inShare;
            this.duplicate = duplicate;
        }
        String inShare, duplicate;
    }
}