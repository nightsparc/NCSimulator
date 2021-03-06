/**
 *
 * Copyright (C) 2010-2011 by Claas Anders "CaScAdE" Rathje
 * admiralcascade@gmail.com
 * Licensed under: Creative Commons / Non Commercial / Share Alike
 * http://creativecommons.org/licenses/by-nc-sa/2.0/de/
 *
 */
package de.mylifesucks.oss.ncsimulator.protocol;

import de.mylifesucks.oss.ncsimulator.datastorage.DataStorage;
import de.mylifesucks.oss.ncsimulator.gui.LogPanel;
import java.io.OutputStream;

/**
 *
 * @author Claas Anders "CaScAdE" Rathje
 */
public class Encode {

    public java.io.OutputStream writer;

    public Encode(OutputStream writer) {
        this.writer = writer;
    }

    public void setWriter(OutputStream writer) {
        this.writer = writer;
    }

    /**
     *
     * @param in_arr
     * @param len
     * @return
     *
     * @author  Marcus -LiGi- Bueschleb
     * http://github.com/ligi/DUBwise/blob/master/shared/src/org/ligi/ufo/MKCommunicator.java
     */
    public static int[] Decode64(byte[] in_arr, int len) {
        return Decode64(in_arr, 0, len);
    }

    /**
     *
     * @param in_arr array of received data
     * @param offset the index where the first char to decode is
     * @param len the length of data to decode
     * @return array representing the decoded data
     *      
     * @author  Marcus -LiGi- Bueschleb
     * @see <a href="http://github.com/ligi/DUBwise/blob/master/shared/src/org/ligi/ufo/MKCommunicator.java">MKCommunicator.java</a>
     */
    public static int[] Decode64(byte[] in_arr, int offset, int len) {
        int ptrIn = offset;
        int a, b, c, d, x, y, z;
        int ptr = 0;

        int[] out_arr = new int[len];

        while (len != 0) {
            a = 0;
            b = 0;
            c = 0;
            d = 0;
            try {
                a = in_arr[ptrIn++] - '=';
                b = in_arr[ptrIn++] - '=';
                c = in_arr[ptrIn++] - '=';
                d = in_arr[ptrIn++] - '=';
            } catch (Exception e) {
            }
            //if(ptrIn > max - 2) break;     // nicht mehr Daten verarbeiten, als empfangen wurden

            x = (a << 2) | (b >> 4);
            y = ((b & 0x0f) << 4) | (c >> 2);
            z = ((c & 0x03) << 6) | d;

            if ((len--) != 0) {
                out_arr[ptr++] = x;
            } else {
                break;
            }
            if ((len--) != 0) {
                out_arr[ptr++] = y;
            } else {
                break;
            }
            if ((len--) != 0) {
                out_arr[ptr++] = z;
            } else {
                break;
            }
        }

        return out_arr;

    }

    /**
     *
     * @param modul
     * @param cmd
     * @param params
     *
     * @author  Marcus -LiGi- Bueschleb
     * http://github.com/ligi/DUBwise/blob/master/shared/src/org/ligi/ufo/MKCommunicator.java
     */
    public synchronized void send_command_nocheck(byte modul, char cmd, int[] params) {

        DataStorage.statusBar.uartTX.toggle();

        if (DataStorage.serial == null) {
            writer = System.out;
        } else {
            writer = DataStorage.serial.getOutputStream();
        }

        if (writer != null) {
            byte[] send_buff = new byte[3 + (params.length / 3 + (params.length % 3 == 0 ? 0 : 1)) * 4]; // 5=1*start_char+1*addr+1*cmd+2*crc
            send_buff[0] = '#';
            send_buff[1] = (byte) (modul + 'a');
            send_buff[2] = (byte) cmd;

            for (int param_pos = 0; param_pos < (params.length / 3 + (params.length % 3 == 0 ? 0 : 1)); param_pos++) {
                int a = (param_pos * 3 < params.length) ? params[param_pos * 3] : 0;
                int b = ((param_pos * 3 + 1) < params.length) ? params[param_pos * 3 + 1] : 0;
                int c = ((param_pos * 3 + 2) < params.length) ? params[param_pos * 3 + 2] : 0;

                send_buff[3 + param_pos * 4] = (byte) ((a >> 2) + '=');
                send_buff[3 + param_pos * 4 + 1] = (byte) ('=' + (((a & 0x03) << 4) | ((b & 0xf0) >> 4)));
                send_buff[3 + param_pos * 4 + 2] = (byte) ('=' + (((b & 0x0f) << 2) | ((c & 0xc0) >> 6)));
                send_buff[3 + param_pos * 4 + 3] = (byte) ('=' + (c & 0x3f));

            }
            try {
                int tmp_crc = 0;
                for (int tmp_i = 0; tmp_i < send_buff.length; tmp_i++) {
                    tmp_crc += (int) send_buff[tmp_i];
                }



                writer.write(send_buff, 0, send_buff.length);
                tmp_crc %= 4096;



                writer.write((char) (tmp_crc / 64 + '='));
                writer.write((char) (tmp_crc % 64 + '='));
                writer.write('\r');
                writer.flush();


                String out = "";
                for (int i : send_buff) {
                    out += (char) i;
                }

                out += (char) (tmp_crc / 64 + '=');
                out += (char) (tmp_crc % 64 + '=');
                out += '\r';

                //System.out.println(out + " sent");
                if (LogPanel.showOutput.isSelected()) {
                    LogPanel.giveMessage(out, LogPanel.red);
                }
            } catch (Exception e) { // problem sending data to FC
            }
        }
    }

    /**
     *
     * @param modul
     * @param cmd
     * @param params
     *
     * @author  Marcus -LiGi- Bueschleb
     * http://github.com/ligi/DUBwise/blob/master/shared/src/org/ligi/ufo/MKCommunicator.java
     */
    public void send_command(int modul, char cmd, int[] params) {
        send_command_nocheck((byte) modul, cmd, params);
    }

    /**
     *
     * @param modul
     * @param cmd
     *
     * @author  Marcus -LiGi- Bueschleb
     * http://github.com/ligi/DUBwise/blob/master/shared/src/org/ligi/ufo/MKCommunicator.java
     */
    public void send_command(int modul, char cmd) {
        send_command(modul, cmd, new int[0]);
    }
    //#define REQUEST_NC_VERSION "#bv====Dl\r"


    /**
     * Verify the checksum in a received MK-Dataframe
     *
     * @param buffer byte array containing the received bytes
     * @return true if received checksum is valid, fals otherwise
     */
    public static boolean mkCRC(byte[] buffer) {
        int buf_ptr = 0, end;

        while (buffer[buf_ptr] != '#' && buf_ptr < buffer.length) {
            buf_ptr++;
        }

        end = buf_ptr;
        while (buffer[end] != '\r' && end < buffer.length) {
            end++;
        }
        return mkCRC(buffer, buf_ptr, end);
    }

    /**
     * Verify the checksum in a received MK-Dataframe
     *
     * @param buffer byte array containing the received bytes
     * @param buf_ptr index of the #
     * @param end index of the \r
     * @return true if received checksum is valid, fals otherwise
     */
    public static boolean mkCRC(byte[] buffer, int buf_ptr, int end) {
        int crc = 0;
        int crc1, crc2;

        for (int i = buf_ptr; i < end - 2; i++) {
            crc += buffer[i];
        }
        crc %= 4096;
        crc1 = '=' + crc / 64;
        crc2 = '=' + crc % 64;

        if ((crc1 == buffer[end - 2]) && (crc2 == buffer[end - 1])) {
            return true;
        }
        return false;
    }

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) {
//
//        Encode e = new Encode(System.out);
//
//        e.send_command(CommunicationBase.NC_ADDRESS, 'l', new int[] {14});
//        System.out.println("_________________________________");
//
//
//        //#aaD|]iF: Analog label request: 31
//        //#bADqZMQrzOXsniE?=]E?=]E?==_G sent
//        String test = "#cAMaBkUSnlVo=m============\\l\r";
//        test = "#aa=L]iE\r";
//        test = "#bADaZMQrzKWSJhE?=]E?=]E?==]m\r";
//        test = "#bADqZMQrzOXsniE?=]E?=]E?==_G\r";   // (31) G(71) P(80) S(83) _(95) R(82) o(111) l(108) l(108)  (32)  (32)  (32)  (32)  (32)  (32)  (32)  (32)  (0) (136) ￴(-12)  (0)  (0)  (0)  (0)  (0)  (0)  (0)  (0)  (0) BUILD SUCCESSFUL (total time: 0 seconds)
//        //test = "#bADqZMQrzOXsniE?=]E?=]E?==_G\r"; // (31) G(71) P(80) S(83) _(95) R(82) o(111) l(108) l(108)  (32)  (32)  (32)  (32)  (32)  (32)  (32)  (32)  (0) (136) ￴(-12)  (0)  (0)  (0)  (0)  (0)  (0)  (0)  (0)  (0) BUILD SUCCESSFUL (total time: 0 seconds)
//        //test = "#aaDmUTE\r";
//        test = "#bq|mi>GC\r";
//        test = "#bq|nZBFy\r";
//        //test = "#aq[m==Et\r";
//        test = "#bo?]==EG\r";
//        test = "#cl@]==EF\r";
////        byte[] foo = new byte[test.length()];
////        int index = 0;
////        for (char c : test.toCharArray()) {
////            System.out.print((byte) c + " ");
////            foo[index++] = (byte) c;
////        }
//        System.out.println("\ncrc: " + mkCRC(test.getBytes()));
//        System.out.println("\ncrc: " + mkCRC(test.getBytes(), test.indexOf("#"), test.indexOf("\r")));
//
//        int[] dec = Decode64(test.getBytes(), 3, test.indexOf("\r"));
//        for (int d : dec) {
//            System.out.print((char) d + "(" + d + ")" + " ");
//        }
//
//    }
}
