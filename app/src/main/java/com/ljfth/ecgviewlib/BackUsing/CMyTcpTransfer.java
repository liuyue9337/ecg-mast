package com.ljfth.ecgviewlib.BackUsing;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class CMyTcpTransfer implements Runnable {

    private Socket socket = null;
    private boolean m_bRunSta = true;
    private String m_ip = "39.98.220.57";
    private int m_port = 9099;
    private boolean m_bReCnfTcp = false;
    private OutputStream buffWriter = null;
    CSingleInstance gTransferData = CSingleInstance.getInstance();

    public boolean isM_bReCnfTcp() {
        return m_bReCnfTcp;
    }

    public void setM_bReCnfTcp(boolean m_bReCnfTcp) {
        this.m_bReCnfTcp = m_bReCnfTcp;
    }


    boolean initSocket(String ip, int port) {
        InetSocketAddress SerAddr = new InetSocketAddress(ip, port);
        socket = new Socket();
        try {
            socket.connect(SerAddr, 3000);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            socket = null;
            return false;
        }
    }

    boolean getConnectedSta() {
        if (socket != null) {
            if (socket.isConnected()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    void changeAddr(String ip, int port) {
        m_ip = ip;
        m_port = port;
        setM_bReCnfTcp(true);
    }

    @Override
    public void run() {
        while (m_bRunSta) {
            //检测是否需要重新连接
            if (m_bReCnfTcp) {
                Log.i("bodystmLiuYue", "重新建立连接，ip : " + m_ip + ", port : "+m_port);
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                socket = null;
                m_bReCnfTcp = false;
            }

            //初始化Tcp连接
            if (socket == null) {
                Log.i("bodystmLiuYue", "尝试建立连接，ip : " + m_ip + ", port : "+m_port);

                //如果连接不存在，清空所有缓冲区标记
                gTransferData.m_atomTcpBuffLen.set(0);
                gTransferData.m_atomTcpBuffWriteIndex.set(0);
                gTransferData.m_atomTcpBuffReadIndex.set(0);

                if (!initSocket(m_ip, m_port)) {
                    continue;
                }
                else {
                    try {
                        buffWriter = socket.getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("bodystmLiuYue", "BufferWriter Init error, " + e.toString() + ", 重新建立连接。");
                        setM_bReCnfTcp(true);
                    }
                }
            }

            //检测缓冲区发送数据
            try {
                //查看发送缓冲区长度
                int nSendLen = gTransferData.m_atomTcpBuffLen.get();
                if (nSendLen == 0) {
                    //没有数据睡眠
                    if (!socket.isConnected()) {
                        setM_bReCnfTcp(true);
                    } else {
                        //只发送尾部字节，更新读指针为0，减少长度nTailLen
//                        String senStr = "abcdefghijklmnopqrstuvwxyz";
//                        buffWriter.write(senStr.getBytes(), 0, senStr.getBytes().length);
//                        gTransferData.m_atomTcpBuffReadIndex.set(0);
                    }
                    Thread.sleep(10);
                } else {
                    int nReadIndex = gTransferData.m_atomTcpBuffReadIndex.get();
                    int nTailLen = gTransferData.m_nTcpSendBuffLen - nReadIndex;
                    if (nSendLen >= nTailLen) {
                        Log.e("bodystmLiuYue", "sendLen is " + nTailLen + ", nReadIndex is " + nReadIndex);
                        //只发送尾部字节，更新读指针为0，减少长度nTailLen
                        buffWriter.write(gTransferData.m_cTcpSendBuffer, nReadIndex, nTailLen);
                        gTransferData.m_atomTcpBuffReadIndex.set(0);
                        gTransferData.m_atomTcpBuffLen.getAndAdd(-nTailLen);
                    } else {
                        //发送当前所有数据
                        Log.e("bodystmLiuYue", "sendLen is " + nSendLen + ", nReadIndex is " + nReadIndex);
                        buffWriter.write(gTransferData.m_cTcpSendBuffer, nReadIndex, nSendLen);
                        gTransferData.m_atomTcpBuffReadIndex.getAndAdd(nSendLen);
                        gTransferData.m_atomTcpBuffLen.getAndAdd(-nSendLen);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                e.printStackTrace();
                Log.e("bodystmLiuYue", "BufferWriter write 1 error, " + e.toString() + ", 重新建立连接。");
                setM_bReCnfTcp(true);
            } catch (InterruptedException e) {
                Log.e("bodystmLiuYue", "BufferWriter write 2 error, " + e.toString() + ", 重新建立连接。");
                setM_bReCnfTcp(true);
            }
        }
    }
}
