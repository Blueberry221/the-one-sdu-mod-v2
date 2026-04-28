package report;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import core.DTNHost;
import core.Settings;
import core.UpdateListener;

public class BufferOccupancyPerIntervalReport extends Report implements UpdateListener {

    public static final String INTERVAL_SETTING = "interval";
    
    private double interval = 5000.0;
    private double lastRecordTime = 0.0;
    
    // Menyimpan histori occupancy tiap node
    private Map<String, List<Double>> occupancyHistory;
    // Menyimpan daftar waktu untuk dijadikan patokan baris (row)
    private List<Double> timeHeaders; 

    public BufferOccupancyPerIntervalReport() {
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
        this.lastRecordTime = 0.0;
        this.occupancyHistory = new LinkedHashMap<String, List<Double>>();
        this.timeHeaders = new ArrayList<Double>();
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        double currentTime = getSimTime();
        
        if (currentTime - lastRecordTime >= interval && currentTime > 0) {
            
            // Catat waktu saat ini untuk dijadikan baris
            timeHeaders.add(currentTime);
            
            for (DTNHost host : hosts) {
                String hostName = host.toString();
                double occupancy = host.getBufferOccupancy();
                
                if (!occupancyHistory.containsKey(hostName)) {
                    occupancyHistory.put(hostName, new ArrayList<Double>());
                }
                
                occupancyHistory.get(hostName).add(occupancy);
            }
            lastRecordTime = currentTime;
        }
    }
    
    @Override
    public void done() {
        write("=== Laporan Buffer Occupancy ===");
        write("Interval update: " + interval + " detik\n");
        
        // 1. Cetak Baris Header (Waktu(s) | Node 0 | Node 1 | Node 2 ...)
        StringBuilder header = new StringBuilder();
        header.append("Waktu(s)\t");
        for (String hostName : occupancyHistory.keySet()) {
            header.append("Node_").append(hostName).append("\t");
        }
        write(header.toString());
        
        // 2. Cetak Data Baris per Baris berdasarkan urutan Waktu
        for (int i = 0; i < timeHeaders.size(); i++) {
            StringBuilder row = new StringBuilder();
            
            // Tulis waktu di kolom pertama
            row.append(timeHeaders.get(i)).append("\t");
            
            // Tulis data occupancy tiap node untuk waktu tersebut
            for (String hostName : occupancyHistory.keySet()) {
                List<Double> history = occupancyHistory.get(hostName);
                
                // Cek aman agar tidak error jika ada data yang kosong/terlewat
                if (i < history.size()) {
                    row.append(format(history.get(i))).append("\t");
                } else {
                    row.append("0.00\t"); 
                }
            }
            
            // Tulis baris ke file
            write(row.toString());
        }
        
        super.done();
    }
}