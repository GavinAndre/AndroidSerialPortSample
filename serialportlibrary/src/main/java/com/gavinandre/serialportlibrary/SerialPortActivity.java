/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.gavinandre.serialportlibrary;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import androidx.appcompat.app.AppCompatActivity;


public abstract class SerialPortActivity extends AppCompatActivity {

    protected SerialPortUtil mSerialPortUtil;
    protected SerialPort mSerialPort;
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;

    HandlerThread sendingHandlerThread = new HandlerThread("sendingHandlerThread");

    {
        sendingHandlerThread.start();
    }

    protected Handler sendingHandler = new Handler(sendingHandlerThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (mOutputStream != null) {
                try {
                    mOutputStream.write((byte[]) msg.obj);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] b = new byte[64];
                    if (mInputStream == null) {
                        return;
                    }
                    size = mInputStream.read(b);
                    byte[] buffer = new byte[size];
                    System.arraycopy(b, 0, buffer, 0, buffer.length);
                    if (size > 0) {
                        onDataReceived(buffer, size);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private void displayError(String resourceId) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Error");
        b.setMessage(resourceId);
        b.setPositiveButton("OK", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SerialPortActivity.this.finish();
            }
        });
        b.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSerialPortUtil = new SerialPortUtil();
        try {
            mSerialPort = mSerialPortUtil.getSerialPort();
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            //Create a receiving thread
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (SecurityException e) {
            // You do not have read/write permission to the serial port.
            displayError("You do not have read/write permission to the serial port.");
        } catch (IOException e) {
            // The serial port can not be opened for an unknown reason.
            displayError("The serial port can not be opened for an unknown reason.");
        } catch (InvalidParameterException e) {
            // Please configure your serial port first.
            displayError("Please configure your serial port first.");
        }
    }

    protected abstract void onDataReceived(final byte[] buffer, final int size);

    @Override
    protected void onDestroy() {
        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        mSerialPortUtil.closeSerialPort();
        mSerialPort = null;
        if (null != sendingHandlerThread) {
            sendingHandlerThread.quit();
        }
        super.onDestroy();
    }
}
