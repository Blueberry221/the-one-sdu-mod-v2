package report;

import java.util.HashMap;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class MessageCopyReport extends Report implements MessageListener {
    private Map<DTNHost, Integer> originalPerNode;
    private Map<DTNHost, Integer> copiesPerNode;
    private int totalOriginal;
    private int totalCopies;

    public MessageCopyReport() {
        init();
    }

    @Override
    protected void init() {
        this.originalPerNode = new HashMap<DTNHost, Integer>();
        this.copiesPerNode = new HashMap<DTNHost, Integer>();
        this.totalOriginal = 0;
        this.totalCopies = 0;
        super.init();
    }

    @Override
    public void newMessage(Message m) {
        if (isWarmupID(m.getId())) return;

        this.totalOriginal++;
        DTNHost creator = m.getFrom();
        
        int count = originalPerNode.containsKey(creator) ? originalPerNode.get(creator) : 0;
        originalPerNode.put(creator, count + 1);
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (isWarmupID(m.getId())) return;

        // Setiap transfer yang berhasil (bukan ke tujuan akhir saja) dihitung sebagai pembuatan copy di node penerima
        this.totalCopies++;
        
        int count = copiesPerNode.containsKey(to) ? copiesPerNode.get(to) : 0;
        copiesPerNode.put(to, count + 1);
    }

    @Override
    public void done() {
        write("--- Message Copy Statistics ---");
        write("Total Original Messages (All): " + totalOriginal);
        write("Total Copy Messages (All): " + totalCopies);
        
        double avgCopies = totalOriginal > 0 ? (double) totalCopies / totalOriginal : 0;
        write("Average Copies per Original Message: " + format(avgCopies));

        write("\n[ Original Messages Per Node ]");
        for (DTNHost host : originalPerNode.keySet()) {
            write("Node " + host.getAddress() + ": " + originalPerNode.get(host));
        }

        write("\n[ Copy Messages Received Per Node ]");
        for (DTNHost host : copiesPerNode.keySet()) {
            write("Node " + host.getAddress() + ": " + copiesPerNode.get(host));
        }

        super.done();
    }

    // Method interface lainnya dikosongkan
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
}