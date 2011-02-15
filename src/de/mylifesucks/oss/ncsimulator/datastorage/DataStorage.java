/**
 *
 * Copyright (C) 2010-2011 by Claas Anders "CaScAdE" Rathje
 * admiralcascade@gmail.com
 * Licensed under: Creative Commons / Non Commercial / Share Alike
 * http://creativecommons.org/licenses/by-nc-sa/2.0/de/
 *
 */
package de.mylifesucks.oss.ncsimulator.datastorage;

import de.mylifesucks.oss.ncsimulator.datatypes.Data3D_t;
import de.mylifesucks.oss.ncsimulator.datatypes.LCDData;
import de.mylifesucks.oss.ncsimulator.datatypes.MixerTable_t;
import de.mylifesucks.oss.ncsimulator.datatypes.NaviData_t;
import de.mylifesucks.oss.ncsimulator.datatypes.PPMArray;
import de.mylifesucks.oss.ncsimulator.datatypes.Waypoint_t;
import de.mylifesucks.oss.ncsimulator.datatypes.c_int;
import de.mylifesucks.oss.ncsimulator.datatypes.paramset_t;
import de.mylifesucks.oss.ncsimulator.datatypes.s16Debug;
import de.mylifesucks.oss.ncsimulator.datatypes.str_DebugOut;
import de.mylifesucks.oss.ncsimulator.datatypes.str_VersionInfo;
import de.mylifesucks.oss.ncsimulator.gui.CoordVizualizer;
import de.mylifesucks.oss.ncsimulator.gui.LogPanel;
import de.mylifesucks.oss.ncsimulator.gui.StatusBar;
import de.mylifesucks.oss.ncsimulator.gui.datawindow.DataWindow;
import de.mylifesucks.oss.ncsimulator.gui.datawindow.DataWindowPanel;
import de.mylifesucks.oss.ncsimulator.protocol.Encode;
import de.mylifesucks.oss.ncsimulator.protocol.SendThread;
import de.mylifesucks.oss.ncsimulator.protocol.Serial;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JTextField;

/**
 * Singleton class to keep the data somewhere
 * 
 * @author Claas Anders "CaScAdE" Rathje
 */
public class DataStorage {

    volatile public static int requestOSDtime;
    volatile public static int requestDEBUGtime;
    //public static ThreadGroup threadGroup = new ThreadGroup("MyGroup");
    //public static ExecutorService executors = Executors.newSingleThreadExecutor();
    public static ExecutorService executors = Executors.newCachedThreadPool();

    public static enum UART_CONNECTION {

        FC, MK3MAG, MKGPS, NC
    }
    public static ImageIcon iconHome;
    public static ImageIcon iconCurrent;
    public static ImageIcon iconTarget;
    public static ImageIcon mainLogo;
    public static NaviData_t naviData = new NaviData_t();
    public static str_VersionInfo NCversion = new str_VersionInfo("NC");
    public static str_VersionInfo FCversion = new str_VersionInfo("FC");
    public static str_VersionInfo MK3version = new str_VersionInfo("MK3Mag");
    public static str_DebugOut FCDebugOut = new str_DebugOut("FC", Serial.FC_ADDRESS);
    public static str_DebugOut NCDebugOut = new str_DebugOut("NC", Serial.NC_ADDRESS);
    public static paramset_t paramset[] = new paramset_t[5];
    public static PPMArray ppmarray = new PPMArray();
    public static MixerTable_t mixerset = new MixerTable_t();
    public static LCDData lcddata = new LCDData();
    public static Data3D_t data3d_t = new Data3D_t();
    public static Preferences preferences;
    public static Encode encoder;
    public static Serial serial;
    private static DataStorage instance = new DataStorage();
    private static LinkedList<c_int> serializePool;
    public static SendThread sendThread;
    public volatile static UART_CONNECTION UART = UART_CONNECTION.NC;
    public static CoordVizualizer coordVizualizer = new CoordVizualizer();
    public static StatusBar statusBar = new StatusBar();
    public static LogPanel logPanel = new LogPanel();
    public static LinkedList<DataWindow> dataWindows = new LinkedList<DataWindow>();
    public static final String nodeName = "NC Simulator";
    public static DataWindowPanel dataWindowPanel;

    private DataStorage() {
        iconHome = new ImageIcon(getClass().getResource("/de/mylifesucks/oss/ncsimulator/img/home.png"));
        iconCurrent = new ImageIcon(getClass().getResource("/de/mylifesucks/oss/ncsimulator/img/airport.png"));
        iconTarget = new ImageIcon(getClass().getResource("/de/mylifesucks/oss/ncsimulator/img/target.png"));
        mainLogo = new ImageIcon(getClass().getResource("/de/mylifesucks/oss/ncsimulator/img/logo_big.png"));
        serializePool = new LinkedList<c_int>();
        preferences = Preferences.userRoot().node(nodeName);


        if (paramset == null || paramset[0] == null) {
            paramset = new paramset_t[5];
            for (int i = 0; i < paramset.length; i++) {
                paramset[i] = new paramset_t(i + 1);
            }
        }

    }

    public static synchronized DataStorage getInstance() {
        if (instance == null) {
            instance = new DataStorage();
        }
        return instance;
    }

    public static void setUART(UART_CONNECTION u) {
        UART = u;
        DataStorage.statusBar.uartMode.setText(u.name());
    }

    public static void addToSerializePool(c_int re) {
        getInstance();
        //System.out.println("added: " + re + "  ::: " + re.getSerializeName() + "\t\t" + serializePool);
        if (re.allAttribs == null) {
            serializePool.add(re);
            re.setValue(preferences.getLong(re.getSerializeName(), re.value), false);
        } else {
            for (c_int c : re.allAttribs) {
                if (re.allAttribs != null || c.getSerializeName() == null) {
                    addToSerializePool(c);
                    //System.err.println("NULL: " + c);
                } else {
                    serializePool.add(c);
                    c.setValue(preferences.getLong(c.getSerializeName(), c.value), false);
                }
            }
        }
    }

    public static void serializePool() {
        for (c_int c : serializePool) {
            preferences.putLong(c.getSerializeName(), c.value);
            if (c instanceof s16Debug) {
                s16Debug s = ((s16Debug) c);
                preferences.put(s.getSerializeName() + "LABEL", ((JTextField) s.nameLabel).getText());
                //System.out.println("save: " + s.getSerializeName() + "LABEL" + "\t value:" + s.name);
            }

        }
    }

    public static void loadPool() {

        for (c_int c : serializePool) {
            c.setValue(preferences.getLong(c.getSerializeName(), c.value), false);
            //System.out.println(c.getSerializeName());
            if (c instanceof s16Debug) {
                s16Debug s = ((s16Debug) c);
                s.setName(preferences.get(s.getSerializeName() + "LABEL", ((JTextField) s.nameLabel).getText()));

                //System.out.println("load: " + s.getSerializeName() + "LABEL" + "\t value:" + s.name);
            }

        }
    }

    public static void deltePool() {
        try {
            preferences.removeNode();
            preferences = Preferences.userRoot().node(nodeName);
            System.out.println("deleted");
        } catch (BackingStoreException ex) {
            System.err.println("delete fail");
        }

    }
}