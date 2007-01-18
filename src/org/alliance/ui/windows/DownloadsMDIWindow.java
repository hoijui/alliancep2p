package org.alliance.ui.windows;

import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.util.TextUtils;
import org.alliance.core.comm.filetransfers.Download;
import org.alliance.core.comm.filetransfers.DownloadConnection;
import org.alliance.ui.T;
import org.alliance.ui.UISubsystem;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class DownloadsMDIWindow extends AllianceMDIWindow {
    private DownloadsTableModel model;
    private JXTable table;
    private JLabel status;

    private ArrayList<DownloadWrapper> rows = new ArrayList<DownloadWrapper>();
    private boolean inTable;

    public DownloadsMDIWindow(final UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "downloads", ui);

        table = new JXTable();
        table.setModel(model = new DownloadsTableModel());
        table.setAutoCreateColumnsFromModel(false);
        table.getColumnModel().getColumn(1).setCellRenderer(new ProgressBarCellRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);

        status = (JLabel)xui.getComponent("status");

        table.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
            }

            public void mouseMoved(final MouseEvent e) {
                if (table.getSelectedRow() != -1) {
                    ui.getCore().invokeLater(new Runnable() {
                        public void run() {
                            if (table.rowAtPoint(e.getPoint()) == -1) return;
                            final String s = getDownloadingFromText(rows.get(table.rowAtPoint(e.getPoint())));
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    status.setText(s);
                                }
                            });
                        }
                    });
                } else {
                    showTotalBytesReceived();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                inTable=true;
            }

            public void mouseExited(MouseEvent e) {
                inTable=false;
                showTotalBytesReceived();
            }
        });


        setFixedColumnSize(table.getColumnModel().getColumn(2), 60);
        setFixedColumnSize(table.getColumnModel().getColumn(3), 60);
        setFixedColumnSize(table.getColumnModel().getColumn(4), 60);
        setFixedColumnSize(table.getColumnModel().getColumn(5), 10);

        table.setColumnControlVisible(true);

        ((JScrollPane)xui.getComponent("scroll")).setViewportView(table);

        update();

        setTitle("Downloads");
        postInit();
    }

    private void showTotalBytesReceived() {
        status.setText("Total bytes received: "+TextUtils.formatByteSize(ui.getCore().getNetworkManager().getBandwidthIn().getTotalBytes()));
    }

    private String getDownloadingFromText(DownloadWrapper w) {
        String text = null;
        final String s;
        for(DownloadConnection c : w.download.connections()) {
            if (text == null) text = "Downloading from ";
            if (c.getRemoteFriend() != null)
                text += c.getRemoteFriend().nickname()+", ";
            else
                text += "<unknown>, ";
        }
        if (text != null) {
            text = text.substring(0,text.length()-2);
            s = text;
        } else {
            s = " ";
        }
        return s;
    }
    private void setFixedColumnSize(TableColumn column, int i) {
        column.setPreferredWidth(i);
        column.setMaxWidth(i);
        column.setMinWidth(i);
    }

    public void update() {
        boolean structureChanged = false;

        ArrayList<Download> al = new ArrayList<Download>(ui.getCore().getNetworkManager().getDownloadManager().downloads());
        for(Download d : al) {
            DownloadWrapper dw = getWrapperFor(d);
            if (dw == null) {
                structureChanged = true;
                dw = new DownloadWrapper(d);
                rows.add(dw);
            }
            dw.update();
        }

        for(Iterator i = rows.iterator();i.hasNext();) {
            DownloadWrapper w = (DownloadWrapper)i.next();
            if (!ui.getCore().getNetworkManager().getDownloadManager().contains(w.download)) {
                structureChanged = true;
                i.remove();
            }
        }

        if (structureChanged) {
            model.fireTableStructureChanged();
        } else
            model.fireTableRowsUpdated(0, rows.size());

        if (!inTable) showTotalBytesReceived();
    }

    private DownloadWrapper getWrapperFor(Download d) {
        for(DownloadWrapper cw : rows) {
            if (cw.download == d) return cw;
        }
        return null;
    }

    public String getIdentifier() {
        return "downloads";
    }

    public void save() throws Exception {}
    public void revert() throws Exception {}
    public void serialize(ObjectOutputStream out) throws IOException {}
    public MDIWindow deserialize(ObjectInputStream in) throws IOException { return null; }

    private class DownloadWrapper {
        public Download download;
        public String name, speed, size;
        public int percentComplete, numberOfConnections;
        public boolean complete;
        public Download.State state;
        public String eta;

        public DownloadWrapper(Download download) {
            this.download = download;
        }

        public void update() {
            try {
                if (download.getFd() == null) {
                    if (download.getNConnections() == 0) {
                        name = download.getAuxInfoFilename();
                    } else {
                        name = download.getAuxInfoFilename()+ " - starting...";
                    }
                    size = "?";
                } else {
                    name = download.getFd().getSubpath();
                    size = TextUtils.formatByteSize(download.getFd().getSize());
                }
                percentComplete = download.getPercentComplete();
                numberOfConnections = download.getNConnections();
                speed = download.getBandwidth().getHumanReadable();
                complete = download.isComplete();
                state = download.getState();
                if (download.getBandwidth().hasGoodAverage())
                    eta = formatETA(download.getETAInMinutes());
                else
                    eta = "?";
            } catch(IOException e) {
                if(T.t)T.error("Exception while updating downloadwrapper: "+e);
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
    }

    private class DownloadsTableModel extends AbstractTableModel {
        public int getRowCount() {
            return rows.size();
        }

        public int getColumnCount() {
            return 6;
        }

        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "Name";
                case 1:
                    return "Progress";
                case 2:
                    return "Size";
                case 3:
                    return "ETA";
                case 4:
                    return "Speed";
                case 5:
                    return "#";
                default:
                    return "undefined";
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return rows.get(rowIndex).name;
                case 1:
                    return rows.get(rowIndex).percentComplete;
                case 2:
                    return rows.get(rowIndex).size;
                case 3:
                    return rows.get(rowIndex).eta;
                case 4:
                    return rows.get(rowIndex).speed;
                case 5:
                    return rows.get(rowIndex).numberOfConnections;
                default:
                    return "undefined";
            }
        }
    }

    public void EVENT_cleanup(ActionEvent e) {
        ui.getCore().invokeLater(new Runnable() {
            public void run() {
                ui.getCore().getNetworkManager().getDownloadManager().removeCompleteDownloads();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        update();
                    }
                });
            }
        });
    }

    public void EVENT_moveDown(ActionEvent e) {
        int selection[] = table.getSelectedRows();
        if (selection != null && selection.length > 0) {
            for(int i : selection) {
                DownloadWrapper dw = rows.get(i);
                final Download d = dw.download;
                ui.getCore().invokeLater(new Runnable() {
                    public void run() {
                        ui.getCore().getNetworkManager().getDownloadManager().moveDown(d);
                    }
                });
                moveDown(i, dw);
            }
            model.fireTableStructureChanged();
            for(int i : selection) if (i<rows.size()-1) table.getSelectionModel().addSelectionInterval(i+1,i+1);
        }
    }

    public void EVENT_moveUp(ActionEvent e) {
        int selection[] = table.getSelectedRows();
        if (selection != null && selection.length > 0) {
            for(int i : selection) {
                DownloadWrapper dw = rows.get(i);
                final Download d = dw.download;
                ui.getCore().invokeLater(new Runnable() {
                    public void run() {
                        ui.getCore().getNetworkManager().getDownloadManager().moveUp(d);
                    }
                });
                moveUp(i, dw);
            }
            model.fireTableStructureChanged();
            for(int i : selection) if (i>0) table.getSelectionModel().addSelectionInterval(i-1,i-1);
        }
    }

    public void moveUp(int i, DownloadWrapper dw) {
        if (i==0) return;
        rows.remove(i);
        rows.add(i-1, dw);
    }

    public void moveDown(int i, DownloadWrapper dw) {
        if (i==rows.size()-1) return;
        rows.remove(dw);
        rows.add(i+1, dw);
    }

    public void EVENT_remove(ActionEvent e) {
        int selection[] = table.getSelectedRows();

        if (selection != null && selection.length > 0) {
            for(int i : selection) {
                Download d = rows.get(i).download;
                if (!d.isComplete()) {
                    if (OptionDialog.showQuestionDialog(
                            ui.getMainWindow(),
                            "Are you sure you want to remove the selected downloads from you harddrive and download queue?")) {
                        break;
                    } else return;
                }
            }

            final ArrayList<Download> dls = new ArrayList<Download>();
            for(int i : selection) dls.add(rows.get(i).download);
            ui.getCore().invokeLater(new Runnable() {
                public void run() {
                    for(Download d : dls) {
                        if (d.isComplete()) {
                            ui.getCore().getNetworkManager().getDownloadManager().remove(d);
                        } else {
                            try {
                                ui.getCore().getNetworkManager().getDownloadManager().deleteDownload(d);
                            } catch(IOException e1) {
                                ui.handleErrorInEventLoop(e1);
                            }
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            update();
                        }
                    });
                }
            });
        }
    }

    public class ProgressBarCellRenderer extends JProgressBar implements TableCellRenderer {
        public ProgressBarCellRenderer() {
            super(0, 100);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            if (value != null && value instanceof Integer) {
                setStringPainted(true);
                int v = (Integer)value;
                DownloadWrapper w = rows.get(rowIndex);
                if (w.state == Download.State.WAITING_TO_START) {
                    setString("queued");
                    setValue(0);
                } else if (w.state == Download.State.COMPLETED) {
                    setString("complete");
                    setValue(100);
                } else {
                    setValue(v);
                    setString(v+"%");
                }
                setToolTipText(getDownloadingFromText(w));
            }

            return this;
        }
        public void validate() {}
        public void revalidate() {}
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
    }
}