package com.rohos.logon1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.rohos.logon1.widget.UnlockPcService;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;

/*
*
*   Great Broadcast sender module. Send Authentication signal and receive an answer from desktop.
    *   @author AlexShilon
     *
 */
public class NetworkSender extends AsyncTask<AuthRecord, Void, Long> {
    private final String TAG = "NetworkSender";
	
	public Socket socket;
    private Context context;
    public String strResult;
    public String strHostIp;


    public NetworkSender(Context context) {
        this.context = context;

        socket = null;
    }

    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    /*InetAddress getLocalAddress() throws IOException {
        WifiManager wifi = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        return InetAddress.getByAddress(dhcp.ipAddress);
    }*/

    @Override
    protected Long doInBackground(AuthRecord... ai) {
    	
    	int attempts = 4;
        long result=0;
        
        //DatagramSocket udp_socket = null;

        if (ai[0].qr_user == null || ai[0].qr_user.isEmpty())
        {
            return result;
        }
        
        for(int i = 0; i < attempts; i++){
        	//Log.d(TAG, "step " + i);
        	if(sendPacket(ai)) break;
        }

        /*
        try {

            //InetSocketAddress bindSocketAddress = new InetSocketAddress("localhost", service.getNetworkConfiguration().getBindSocketAddress().getPort());

            udp_socket = new DatagramSocket(ai[0].qr_host_port);
            udp_socket.setBroadcast(true);
            udp_socket.setReuseAddress(true);

            String encryptedAuthString = ai[0].getEncryptedDataString();
            String str_data = String.format("%s.%s.%s", ai[0].qr_user, ai[0].qr_host_name, encryptedAuthString );

            DatagramPacket packet = new DatagramPacket(str_data.getBytes(), str_data.length(),
                    getBroadcastAddress(), ai[0].qr_host_port);
                        
            udp_socket.send(packet);


            udp_socket.setSoTimeout(1000);
            DatagramPacket recv_packet = new DatagramPacket(new byte[300], 300);
            String serverReply = "";

            try{
                udp_socket.receive(recv_packet);

                // if we have received ourself packet...
                // receive once again server reply...
                if (recv_packet.getData().length > 30)
                {
                    recv_packet.setData(new byte[100], 0, 100);
                    //Thread.sleep( 300 );//1 sec
                    udp_socket.receive(recv_packet);
                }

                serverReply = new String(recv_packet.getData());

            }catch(SocketTimeoutException err){
                // ... oops no server reply
            }

            udp_socket.close();
			*/
           //strResult = String.format("Authentication signal sent OK.\n%s %d %d\nUnlocked:%s",
           //         str_data.substring(0, 29), encryptedAuthString.length(), ai[0].qr_secret_key.length(),
           //         /*ai[0].plainHexAuthStr,*/
           //         serverReply);

           // return result;
            

            /*
            this is OLD version - Peer-to-Peer connection.
            doesnt work when HOST name cannot be resolved...
            socket = new Socket();

            InetAddress address = InetAddress.getByName(ai[0].qr_host_name );
            strHostIp = address.getHostAddress();

            if (strHostIp.length() == 0)
                strHostIp = ai[0].qr_host_ip;

            SocketAddress remoteaddr=new InetSocketAddress(strHostIp, ai[0].qr_host_port);
            socket.connect(remoteaddr, 900);

            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream()));

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            char [] received_data  = new char[500];
            int len = in.read(received_data);

            String hostHello = new StringBuffer()
                    .append(received_data, 0, len)
                    .toString();

            String encryptedAuthString = ai[0].getEncryptedDataString();
            String str_data = String.format("%s.%s.%s", ai[0].qr_user, ai[0].qr_host_name, encryptedAuthString );
            out.write(str_data, 0, str_data.length());
            out.flush();

            result = str_data.length();
            strResult = String.format("Send OK. %s %d\n%s \nto: %s (%s)",
                    str_data.substring(0, 20), encryptedAuthString.length(),
                    ai[0].plainHexAuthStr,
                    strHostIp,hostHello );

            //strResult = "";
            */
        /*
        } catch (IOException e) {

            strResult = String.format("IO Exception.. %s", e.toString());

        }

          catch ( Exception e)
           {
               strResult = String.format("Exception.. %s", e.toString());
               //if (udp_socket!=null) udp_socket.close();
           }

        finally {
            if (udp_socket!=null) udp_socket.close();

        }
	*/

     return result;
    }

    protected void onPostExecute(Long result) {
    	//Log.d(TAG, "result " + result + ", strResult " + strResult);
    	if(MainActivity.mHandler != null){
    		Message msg = MainActivity.mHandler.obtainMessage(MainActivity.SET_RESULT_TEXT,
    				new String(strResult));
    		MainActivity.mHandler.sendMessage(msg);
    	}
    	
    	// Stop service if it's started
    	if(UnlockPcService.mHandler != null){
    		Message stop = UnlockPcService.mHandler.obtainMessage(UnlockPcService.FINISH_SERVICE);
    		UnlockPcService.mHandler.sendMessage(stop);
    	}

        if(strResult.indexOf("Rohos:", 0) >0){
            // vibarate if result contains server reply
            ((Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100L);
            
            //Stop recognizing service
            //RohosApplication app = RohosApplication.getInstance();
            //if(app != null){
            //	Message stopService = app.mHandler.obtainMessage(app.STOP_RECOGNIZING_SERVICE);
            //	app.mHandler.sendMessage(stopService);
            //}
        }
    }
    
    
    private boolean sendPacket(AuthRecord... ai){
    	DatagramSocket udp_socket = null;
    	boolean result = true;
    	try {
        	//Log.d(TAG, ai[0].qr_host_name);
            //InetSocketAddress bindSocketAddress = new InetSocketAddress("localhost", service.getNetworkConfiguration().getBindSocketAddress().getPort());

            udp_socket = new DatagramSocket(ai[0].qr_host_port);
            udp_socket.setBroadcast(true);
            udp_socket.setReuseAddress(true);

            String encryptedAuthString = ai[0].getEncryptedDataString();
            String str_data = String.format("%s.%s.%s", ai[0].qr_user, ai[0].qr_host_name, encryptedAuthString );

            DatagramPacket packet = new DatagramPacket(str_data.getBytes(), str_data.length(),
                    getBroadcastAddress(), ai[0].qr_host_port);
                        
            udp_socket.send(packet);


            udp_socket.setSoTimeout(400);
            DatagramPacket recv_packet = new DatagramPacket(new byte[300], 300);
            String serverReply = "";

            try{
                udp_socket.receive(recv_packet);

                // if we have received ourself packet...
                // receive once again server reply...
                if (recv_packet.getData().length > 30)
                {
                    recv_packet.setData(new byte[100], 0, 100);
                    //Thread.sleep( 300 );//1 sec
                    udp_socket.receive(recv_packet);
                }

                serverReply = new String(recv_packet.getData());

            }catch(SocketTimeoutException err){
                // ... oops no server reply
            	result = false;
            }

            udp_socket.close();
            
           strResult = String.format("Authentication signal sent OK.\n%s %d %d\nUnlocked:%s",
                    str_data.substring(0, 29), encryptedAuthString.length(), ai[0].qr_secret_key.length(),
                    /*ai[0].plainHexAuthStr,*/
                    serverReply);

            if(!result)
            	return result;
        }catch(IOException e){
            strResult = String.format("IO Exception.. %s", e.toString());
            result = false;
        }catch(Exception e){
            strResult = String.format("Exception.. %s", e.toString());
            //if (udp_socket!=null) udp_socket.close();
            result = false;   
        }finally{
            if (udp_socket!=null) udp_socket.close();            
        }
    	
    	return result;
    }
}
