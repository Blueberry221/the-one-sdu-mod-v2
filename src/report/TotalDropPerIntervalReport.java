package report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.UpdateListener;

public class TotalDropPerIntervalReport extends Report implements MessageListener, UpdateListener {
    public static final String INTERVAL_SETTING = "interval";
    
    private double interval = 5000.0; 
    private double lastRecordTime = 0.0;
    
    // Menyimpan jumlah drop SEMENTARA untuk interval yang sedang berjalan
    private Map<String, Integer> currentIntervalDrops;
    
    // Menyimpan HISTORI jumlah drop tiap node dari awal sampai akhir
    private Map<String, List<Integer>> dropHistory;
    
    // Menyimpan daftar waktu untuk dijadikan Header kolom
    private List<Double> timeHeaders;

    public TotalDropPerIntervalReport() {
        super();
        
        Settings settings = getSettings();
        if (settings.contains(INTERVAL_SETTING)) {
            this.interval = settings.getDouble(INTERVAL_SETTING);
        }
        
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.currentIntervalDrops = new HashMap<String, Integer>();
        this.dropHistory = new LinkedHashMap<String, List<Integer>>();
        this.timeHeaders = new ArrayList<Double>();
        this.lastRecordTime = 0.0;
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) { return; }
        
        if (dropped) {
            String hostName = where.toString();
            // Catat drop di memori sementara untuk interval saat ini
            int currentDrops = currentIntervalDrops.getOrDefault(hostName, 0);
            currentIntervalDrops.put(hostName, currentDrops + 1);
        }
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        double currentTime = getSimTime();
        
        if (currentTime - lastRecordTime >= interval && currentTime > 0) {
            // Simpan catatan waktu untuk baris
            timeHeaders.add(currentTime);

            // Loop semua node untuk memindahkan data sementara ke history
            for (DTNHost host : hosts) {
                String hostName = host.toString();
                // Jika node tidak ada di daftar sementara, berarti drop-nya 0 di interval ini
                int drops = currentIntervalDrops.getOrDefault(hostName, 0);
                
                // Pastikan node punya antrean history
                if (!dropHistory.containsKey(hostName)) {
                    dropHistory.put(hostName, new ArrayList<Integer>());
                }
                
                // Tambahkan nilai drop ke antrean paling belakang
                dropHistory.get(hostName).add(drops);
            }
            
            // Bersihkan data sementara untuk bersiap di interval selanjutnya
            currentIntervalDrops.clear();
            lastRecordTime = currentTime;
        }
    }

    // Tulis ke dalam file TXT setelah simulasi selesai
    @Override
    public void done() {
        write("=== Laporan Total Drop Per Interval ===");
        write("Interval update: " + interval + " detik\n");
        
        // 1. Cetak Baris Header (Waktu(s) | Node_0 | Node_1 | Node_2 ...)
        StringBuilder header = new StringBuilder();
        header.append("Waktu(s)\t");
        for (String hostName : dropHistory.keySet()) {
            header.append("Node_").append(hostName).append("\t");
        }
        write(header.toString());
        
        // 2. Cetak Data Baris per Baris berdasarkan urutan Waktu
        for (int i = 0; i < timeHeaders.size(); i++) {
            StringBuilder row = new StringBuilder();
            
            // Tulis waktu di kolom pertama
            row.append(timeHeaders.get(i)).append("\t");
            
            // Tulis data drop tiap node untuk waktu tersebut
            for (String hostName : dropHistory.keySet()) {
                List<Integer> history = dropHistory.get(hostName);
                
                // Cek aman agar tidak error jika ada data yang kosong/terlewat
                if (i < history.size()) {
                    row.append(history.get(i)).append("\t");
                } else {
                    row.append("0\t"); 
                }
            }
            
            // Tulis baris ke file
            write(row.toString());
        }
        
        // Tutup file
        super.done();
    }

    @Override
    public void newMessage(Message m) { 
        if (isWarmup()) addWarmupID(m.getId()); 
    }
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {}
}