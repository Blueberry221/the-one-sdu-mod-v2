package report;

import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class TotalDropPerNodeReport extends Report implements MessageListener {
    private Map<String, Integer> dropPerNode;

    public TotalDropPerNodeReport() {
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.dropPerNode = new HashMap<String, Integer>();
        
        // 1. Tambahkan header untuk log historis kejadian drop
        write("Waktu(s)\tNode\tTotal_Drop_Sementara");
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (isWarmupID(m.getId())) {
            return;
        }

        if (dropped) {
            String hostName = where.toString();

            int count = dropPerNode.getOrDefault(hostName, 0) + 1;
            dropPerNode.put(hostName, count);

            // 2. Ganti System.out.println dengan write() dan format Tab (\t)
            write(format(getSimTime()) + "\t" + hostName + "\t" + count);
        }
    }

    @Override
    public void newMessage(Message m) {
        if (isWarmup()) {
            addWarmupID(m.getId());
        }
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean finalTarget) {
    }

    // --- Rekap di akhir simulasi ---
    @Override
    public void done() {
        // Beri jarak sedikit dari log kejadian di atasnya
        write("\n=== SUMMARY TOTAL DROP PER NODE ===");
        
        // 3. Buat header untuk bagian summary
        write("Node\tTotal_Akhir_Drop");
        
        for (Map.Entry<String, Integer> entry : dropPerNode.entrySet()) {
            // 4. Format output rekap menjadi dua kolom rapi
            write(entry.getKey() + "\t" + entry.getValue());
        }
        super.done();
    }
}