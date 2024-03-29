package org.alliance.ui.windows.search;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.ui.JHtmlLabel;
import com.stendahls.util.TextUtils;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.core.PacedRunner;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.util.CutCopyPastePopup;
import org.alliance.ui.windows.AllianceMDIWindow;
import org.jdesktop.swingx.JXTreeTable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class SearchMDIWindow extends AllianceMDIWindow {

	private JXTreeTable table;
    private SearchTreeTableModel model;
    private JComboBox type;
    private JPopupMenu popup;
    private JLabel left, right;
    private PacedRunner pacedRunner;

    private JTextField search;

    private ImageIcon[] fileTypeIcons;

    private UISubsystem ui;

    public SearchMDIWindow(final UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "search", ui);

        this.ui = ui;
        fileTypeIcons = new ImageIcon[8];
        for(int i=0;i<fileTypeIcons.length;i++) fileTypeIcons[i] = new ImageIcon(ui.getRl().getResource("gfx/filetypes/"+i+".png"));

        left = (JLabel)xui.getComponent("left");
        right = (JLabel)xui.getComponent("right");
        search = (JTextField)xui.getComponent("search1");
        new CutCopyPastePopup(search);

        pacedRunner = new PacedRunner(500);

        JHtmlLabel label = (JHtmlLabel)xui.getComponent("label");
        label.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                String s = e.getURL().toString();
                s = s.substring(s.length()-1);
                FileType ft = FileType.getFileTypeById(Integer.parseInt(s));

                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getSearchWindow());
                        ui.getMainWindow().getSearchWindow().searchForNewFilesOfType(ft);
                    } catch (IOException e1) {
                        ui.handleErrorInEventLoop(e1);
                    }
                } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    ui.getMainWindow().setStatusMessage("Click here to search for new and popular files in type "+ft.description());
                }
            }
        });

        table = new JXTreeTable(model = new SearchTreeTableModel(ui.getCore(), pacedRunner));
        table.setColumnControlVisible(true);

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JTableHeader h = (JTableHeader) e.getSource();
                TableColumnModel columnModel = h.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = columnModel.getColumn(viewColumn).getModelIndex();
                if (column != -1) {
                    switch(column) {
                        case 0:
                            model.getRoot().sortByName();
                            break;
                        case 1:
                            model.getRoot().sortBySize();
                            break;

                        case 3:
                            model.getRoot().sortByDaysAgo();
                            break;
                        case 4:
                            model.getRoot().sortBySources();
                            break;
                        case 5:
                            model.getRoot().sortBySpeed();
                            break;
                    }
                }
            }
        });
        table.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(MouseEvent e) {
                if (table.getPathForLocation(e.getX(), e.getY()) != null) {
                    SearchTreeNode n = (SearchTreeNode)table.getPathForLocation(e.getX(), e.getY()).getLastPathComponent();
                    if (n != null) {
                        if (n instanceof FileNode) {
                            FileNode fn = (FileNode)n;
                            double d = fn.getTotalMaxCPS();
                            double r = ui.getCore().getSettings().getInternal().getRecordinspeed();
                            left.setText("<html>"+fn.getListOfUsers(ui.getCore())+" (Max speed: <b>"+ TextUtils.formatByteSize((long)d)+"/s</b>, ETA: "+
                                    formatETA((int)Math.round(fn.getSize()/(d > r ? r : d)))
                                    +"</font>)</html>");
                            right.setText("<html><b>"+simplifyPath(fn)+"</b></html>");
                        }
                    }
                }
            }

            private String formatETA(int eta) {
                if (eta < 0) {
                    return "?";
                } else 	if (eta<=60) {
                    return eta+" sec";
                } else if (eta/60<60) {
                    return eta/60+" min";
                } else {
                    return (eta/60/60)+"h "+(eta/60%60)+"m";
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    EVENT_download(null);
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    boolean b = false;
                    for(int r : table.getSelectedRows()) {
                        if (r == row) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) table.getSelectionModel().setSelectionInterval(row,row);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        table.setAutoCreateColumnsFromModel(false);
        table.setTreeCellRenderer(new NameCellRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new BytesizeCellRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new DaysOldCellRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new SourcesCellRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new SpeedCellRenderer());

        table.getColumnModel().getColumn(0).setPreferredWidth(500);
        setFixedColumnSize(table.getColumnModel().getColumn(1), 50);
        setFixedColumnSize(table.getColumnModel().getColumn(2), 50);
        setFixedColumnSize(table.getColumnModel().getColumn(3), 55);
        setFixedColumnSize(table.getColumnModel().getColumn(4), 25);
        setFixedColumnSize(table.getColumnModel().getColumn(5), 40);

        table.getColumnExt(2).setVisible(false); //tricky! the index points to the visible columns!
//        table.getColumnExt(1).setVisible(false);

        ((JScrollPane)xui.getComponent("treepanel")).setViewportView(table);

        type = (JComboBox)xui.getComponent("type");
        popup = (JPopupMenu)xui.getComponent("popup");

        for(FileType v : FileType.values()) type.addItem(v.description());

        setTitle("File search");
        postInit();
    }

    private String simplifyPath(FileNode fn) {
        String path = fn.getSh().getPath();
        String s = fn.getOriginalFilename();
        if (fn.getParent() != null && fn.getParent() instanceof FolderNode) s = ((FolderNode)fn.getParent()).getOriginalName()+"/"+s;
        if (path.endsWith(s)) path = path.substring(0,path.length()-s.length());
        if (path.endsWith("/")) path = path.substring(0,path.length()-1);
        if (path.equalsIgnoreCase(fn.getName())) path = "";
        return path;
    }

    private void setFixedColumnSize(TableColumn column, int i) {
        column.setPreferredWidth(i);
        column.setMaxWidth(i);
        column.setMinWidth(i);
    }

    public void EVENT_download(ActionEvent e) {
        int selection[] = table.getSelectedRows();

        if (selection != null && selection.length > 0) for(int i : selection) {
            boolean changeWindow = false;
            final String path = getPathIfPathSelected(i);
            for(final FileNode n : getFileNodesByRow(i)) {
                if (ui.getCore().getFileManager().containsComplete(n.getSh().getRoot())) {
                    ui.getCore().getUICallback().statusMessage("You already have the file "+n.getName()+"!");
                } else if (ui.getCore().getNetworkManager().getDownloadManager().getDownload(n.getSh().getRoot()) != null) {
                    ui.getCore().getUICallback().statusMessage("You are already downloading "+n.getName()+"!");
                } else {
                    ui.getCore().invokeLater(new Runnable() {
                        public void run() {
                            try {
                                String name = n.getOriginalFilename();
                                if (path.trim().length() > 0) name = path + "/" + name;
                                ui.getCore().getNetworkManager().getDownloadManager().queDownload(n.getSh().getRoot(), name, n.getUserGuids());
                            } catch(IOException e1) {
                                ui.handleErrorInEventLoop(e1);
                            }
                        }
                    });
                    changeWindow = true;
                }
            }
            if (changeWindow) ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getDownloadsWindow());
        }
    }

    private String getPathIfPathSelected(int row) {
        SearchTreeNode n = (SearchTreeNode)table.getPathForRow(row).getLastPathComponent();
        if (n instanceof FolderNode) {
            FolderNode fn = (FolderNode)n;
            return fn.getName();
        }
        return "";
    }

    private ArrayList<FileNode> getFileNodesByRow(int row) {
        SearchTreeNode n = (SearchTreeNode)table.getPathForRow(row).getLastPathComponent();
        ArrayList<FileNode> al = new ArrayList<FileNode>();
        if (n instanceof FileNode) {
            al.add((FileNode)n);
        } else {
            FolderNode fn = (FolderNode)n;
            for(int i=0;i<fn.getChildCount();i++) al.add((FileNode)fn.getChildAt(i));
        }
        return al;
    }

    public void searchHits(int sourceGuid, int hops, java.util.List<SearchHit> hits) {
        model.addSearchHits(sourceGuid, hops, hits);

        SearchTreeNode root = model.getRoot();
        for(int i=0;i<root.getChildCount();i++) {
            SearchTreeNode n = (SearchTreeNode)root.getChildAt(i);
            if (n.getChildCount() == 1) {
                table.expandPath(new TreePath(new Object[] {root, n}));
            }
        }
    }

    public void windowSelected() {
        super.windowSelected();
    }

    public void EVENT_search1(ActionEvent e) throws IOException {
        search(search.getText());
    }

    public void EVENT_search2(ActionEvent e) throws IOException {
        search(search.getText());
    }

    private void search(String text) throws IOException {
        search(text, FileType.values()[type.getSelectedIndex()]);
//        if (keywords.isSelected()) search.setText("");
    }

    public void searchForNewFilesOfType(FileType ft) throws IOException {
        search("", ft);
    }

    public void search(String text, final FileType ft) throws IOException {
        final String t = text;

        String s;
        if (t.trim().length() == 0)
            s = "Searching for all files of type "+ft.description();
        else
            s = "Searching for "+t +" in "+ft.description()+"...";
        left.setText(s);
        table.setTreeTableModel(model = new SearchTreeTableModel(ui.getCore(), pacedRunner));
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                try {
                    ui.getCore().getFriendManager().getNetMan().sendSearch(t, ft);
                } catch(IOException e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    public String getIdentifier() {
        return "Search";
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }

    public void EVENT_keywords(ActionEvent e) {
        search.setEnabled(true);
    }

    public void EVENT_newfiles(ActionEvent e) {
        search.setEnabled(false);
    }

    public class NameCellRenderer extends DefaultTreeCellRenderer {

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof FileNode) {
                FileNode fn = (FileNode)value;
                setIcon(fileTypeIcons[FileType.getByFileName(fn.getSh().getPath()).id()]);
                if (sel)
                    setForeground(Color.white);
                else
                    setForeground(fn.containedInShare(SearchMDIWindow.this.ui.getCore()) ? Color.gray : Color.black);
            } else if (value instanceof FolderNode) {
                FolderNode fn = (FolderNode)value;
                if (sel)
                    setForeground(Color.white);
                else
                    setForeground(fn.containedInShare(SearchMDIWindow.this.ui.getCore()) ? Color.gray : Color.black);
            }
            return this;
        }
    }

    public class BytesizeCellRenderer extends DefaultTableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            super.getTableCellRendererComponent(table, value,  isSelected,  hasFocus,  rowIndex, vColIndex);
            setText(TextUtils.formatByteSize((Long)value));
            setToolTipText(String.valueOf(value));
            return this;
        }
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }

    public class DaysOldCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            super.getTableCellRendererComponent(table, value,  isSelected,  hasFocus,  rowIndex, vColIndex);
            int val = (Integer)value;
            if (val == 255)
                setText("Old");
            else
                setText(String.valueOf(val));
            return this;
        }
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }

    public class SourcesCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            super.getTableCellRendererComponent(table, value,  isSelected,  hasFocus,  rowIndex, vColIndex);

            double val = (Double)value;
            if (val >= 10) {
                setText(""+Math.round(val));
            } else {
                val *= 10;
                val = Math.round(val);
                String s = String.valueOf(val);
                if (s.substring(1,2).equals("0")) {
                    setText(s.substring(0,1));
                } else {
                    setText(s.substring(0,1)+"."+s.substring(1,2));

                }
            }
            return this;
        }
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }

    public class SpeedCellRenderer extends JProgressBar implements TableCellRenderer {


		public SpeedCellRenderer() {
            super(0, 100);
            setStringPainted(false);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            if (value != null && value instanceof Double) {
                double v = (Double)value;
                setValue((int)(v*100));
                setString(((int)(v*100))+"%");
                setToolTipText(""+v);
            }

            return this;
        }
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}


        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2 = (Graphics2D)g;

            g2.setColor(new Color(255,255,255,180));
            g.fillRect(0,0,getWidth(),getHeight());
        }
    }
}
