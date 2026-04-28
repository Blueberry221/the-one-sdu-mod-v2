package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class BufferOccupancyPerNodeReport extends Report implements MessageListener {

    public BufferOccupancyPerNodeReport() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        // Menulis header tabel di awal file teks agar rapi saat dibuka
        write("Waktu(s)\tNode\tKejadian\tBuffer_Terpakai(%)");
    }

    private void cetakStatusBuffer(DTNHost host, String namaKejadian) {
        double occupancy = host.getBufferOccupancy(); 
        
        // Menggunakan write() dan pemisah Tab (\t) agar lurus seperti tabel
        write(format(getSimTime()) + "\t" + host.toString() + "\t" + namaKejadian + "\t" + format(occupancy));
    }

    @Override
    public void newMessage(Message m) {
        if (isWarmup()) { 
            addWarmupID(m.getId()); 
            return; 
        }
        cetakStatusBuffer(m.getFrom(), "PESAN DIBUAT");
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
        if (isWarmupID(m.getId())) { 
            return; 
        }
        
        cetakStatusBuffer(to, "PESAN DITERIMA");
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) { 
            return; 
        }
        String label = dropped ? "PESAN DI-DROP" : "PESAN DIHAPUS";
        cetakStatusBuffer(where, label);
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }
}